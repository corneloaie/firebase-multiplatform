// Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.

import FirebaseAILogic
import Foundation

/// The Objective-C face of an open Live API session of the SDK (see ``AIBridge/connect(_:setup:timeout:completion:)``).
/// The server's messages are delivered as the JSON text of the Live API's server messages (see
/// `ResponseJSON.liveServerMessage`); what is sent is JSON text too. Failures are `NSError`s of
/// ``AIBridge/errorDomain`` (see ``AIBridge`` for the codes).
///
/// The SDK sends what it is given in order through a queue and reports no failures of sending; a server message it
/// cannot decode ends the session.
@objc(FAILiveSession)
public final class LiveSessionBridge: NSObject {
  private let session: LiveSession
  private let state = LiveSessionState()

  init(session: LiveSession) {
    self.session = session
    super.init()
  }

  /// Starts delivering the server's messages to `onMessage` until the session ends; `onClose` is then called once,
  /// with the failure that ended it, or with nil when ``close(_:)`` closed it or the server closed it normally. Call it
  /// once (the SDK's messages have a single consumer).
  @objc(start:onClose:)
  public func start(_ onMessage: @escaping (String) -> Void, onClose: @escaping (NSError?) -> Void) {
    guard state.begin(onClose: onClose) else {
      return
    }
    let responses = session.responses
    let state = self.state
    state.track(Task {
      do {
        for try await message in responses {
          if let json = try? ResponseJSON.liveServerMessage(message) {
            onMessage(json)
          }
        }
        // The SDK ends its stream when the session goes away, or when the task is cancelled (by close).
        state.finish(nil)
      } catch {
        state.finish(Task.isCancelled ? nil : BridgeError.liveSessionError(for: error))
      }
    })
  }

  /// Sends `turns` (the JSON text of an array of contents) as the client's content, with `turnComplete`, and completes
  /// with nil once the SDK queued it, or with an error.
  @objc(sendClientContent:turnComplete:completion:)
  public func sendClientContent(_ turns: String, turnComplete: Bool, completion: @escaping (NSError?) -> Void) {
    send(completion) { session in
      let contents = try RequestJSON.contents(RequestJSON.array(turns))
      await session.sendContent(contents, turnComplete: turnComplete)
    }
  }

  /// Sends the realtime input `input` (the JSON text of an object with `audio` or `video`, both inline data
  /// `{"mimeType": ..., "data": base64}`, `text`, `activityStart` or `activityEnd`) and completes with nil once the SDK
  /// queued it, or with an error. The SDK sends audio as `audio/pcm` (16-bit PCM at 16 kHz), whatever its MIME type.
  @objc(sendRealtimeInput:completion:)
  public func sendRealtimeInput(_ input: String, completion: @escaping (NSError?) -> Void) {
    send(completion) { session in
      let json = try RequestJSON.object(input)
      if let audio = json.object("audio") {
        let (data, _) = try RequestJSON.inlineData(audio)
        await session.sendAudioRealtime(data)
      }
      if let video = json.object("video") {
        let (data, mimeType) = try RequestJSON.inlineData(video)
        await session.sendVideoRealtime(data, mimeType: mimeType)
      }
      if let text = json.string("text") {
        await session.sendTextRealtime(text)
      }
      if json.has("activityStart") {
        await session.sendStartActivityRealtime()
      }
      if json.has("activityEnd") {
        await session.sendStopActivityRealtime()
      }
    }
  }

  /// Sends `functionResponses` (the JSON text of an array of `{"name": ..., "response": {...}, "id": ...}`) as the
  /// tool response and completes with nil once the SDK queued it, or with an error. The SDK's function responses have
  /// no `parts`.
  @objc(sendToolResponse:completion:)
  public func sendToolResponse(_ functionResponses: String, completion: @escaping (NSError?) -> Void) {
    send(completion) { session in
      let responses = try RequestJSON.functionResponses(RequestJSON.array(functionResponses))
      await session.sendFunctionResponses(responses)
    }
  }

  /// Closes the session and calls `completion` once the SDK closed its WebSocket; the `onClose` callback passed to
  /// ``start(_:onClose:)`` is called with nil (the SDK does not end its stream of messages on close).
  @objc(close:)
  public func close(_ completion: @escaping () -> Void) {
    let session = self.session
    let state = self.state
    Task {
      await session.close()
      state.finish(nil)
      completion()
    }
  }

  /// Runs `operation` with the session in a task and completes with its failure or nil.
  private func send(_ completion: @escaping (NSError?) -> Void, _ operation: @escaping (LiveSession) async throws -> Void) {
    let session = self.session
    let state = self.state
    Task {
      do {
        guard !state.isClosed else {
          throw BridgeError(kind: .unknown, message: "The live session is closed.")
        }
        try await operation(session)
        completion(nil)
      } catch {
        completion(BridgeError.nsError(for: error))
      }
    }
  }
}

/// Whether a session ended, and the callback that reports its end, called once; shared with the task that delivers
/// the session's messages.
private final class LiveSessionState: @unchecked Sendable {
  private let lock = NSLock()
  private var onClose: ((NSError?) -> Void)?
  private var task: Task<Void, Never>?
  private var closed = false

  var isClosed: Bool {
    lock.lock()
    defer { lock.unlock() }
    return closed
  }

  /// Registers `onClose`; when the session ended already, calls it with nil instead and returns false.
  func begin(onClose: @escaping (NSError?) -> Void) -> Bool {
    lock.lock()
    guard !closed else {
      lock.unlock()
      onClose(nil)
      return false
    }
    self.onClose = onClose
    lock.unlock()
    return true
  }

  /// Keeps the task that delivers the messages, to cancel it when the session ends.
  func track(_ task: Task<Void, Never>) {
    lock.lock()
    guard !closed else {
      lock.unlock()
      task.cancel()
      return
    }
    self.task = task
    lock.unlock()
  }

  /// Ends the session: cancels the task that delivers the messages and reports `error` to the `onClose` callback, the
  /// first time only.
  func finish(_ error: NSError?) {
    lock.lock()
    closed = true
    let onClose = self.onClose
    let task = self.task
    self.onClose = nil
    self.task = nil
    lock.unlock()
    task?.cancel()
    onClose?(error)
  }
}
