// Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.

import FirebaseAILogic
import FirebaseCore
import Foundation

/// The Objective-C face of the Firebase AI Logic SDK for one app, backend and App Check token setting, for
/// Kotlin/Native: requests and responses are the JSON text of the Firebase AI Logic REST API (see `RequestJSON` and
/// `ResponseJSON` for what the SDK takes and returns of it), and failures are `NSError`s of ``AIBridge/errorDomain``.
///
/// Codes (`NSError.code`), the ordinals of the Kotlin side's `NativeAIError.Kind`:
/// - `0`: the server answered with an error. `NSLocalizedDescriptionKey` holds the server's message,
///   `userInfo["status"]` the HTTP status (an `NSNumber`) and `userInfo["details"]` the JSON text of the error's
///   details (`[{"@type": "...", "reason": "...", "domain": "...", "metadata": {...}}]`), when the SDK has them.
/// - `1`: the request did not complete in time.
/// - `2`: a response could not be decoded (by the SDK) or written as JSON (by the bridge).
/// - `3`: the request was rejected before it was sent (it could not be read, or the SDK rejected it).
/// - `4`: a Live API session could not be opened.
/// - `5`: anything else.
///
/// Backends: `0` the Gemini Developer API, `1` the Agent Platform Gemini API (formerly Vertex AI) at a location.
///
/// Responses that report a blocked prompt or a candidate that stopped early, which the SDK throws as errors, complete as
/// responses: the Kotlin side checks responses as the Android SDK does.
@objc(FAIBridge)
public final class AIBridge: NSObject {
  /// The domain of the errors the bridge reports.
  @objc public static let errorDomain = "io.github.corneloaie.firebase.ai"

  private static let backendAgentPlatform = 1

  private let appName: String
  private let backend: Int
  private let location: String
  private let useLimitedUseAppCheckTokens: Bool

  /// The SDK for the app named `appName` (the default app is `__FIRAPP_DEFAULT`), the backend `backend` (`location` is
  /// the Agent Platform's) and the App Check token setting. The SDK's instance is looked up on each request (the SDK
  /// caches it), so that a missing app or an invalid configuration fails the request rather than stopping the process,
  /// as the SDK does.
  @objc public init(appName: String, backend: Int, location: String, useLimitedUseAppCheckTokens: Bool) {
    self.appName = appName
    self.backend = backend
    self.location = location
    self.useLimitedUseAppCheckTokens = useLimitedUseAppCheckTokens
    super.init()
  }

  // MARK: Generative models

  /// Runs `generateContent` on `model` with `request` (a `GenerateContentRequest` without its model: `contents`, and
  /// optionally `generationConfig`, `safetySettings`, `tools`, `toolConfig` and `systemInstruction`) and completes with
  /// the JSON text of the `GenerateContentResponse`, or with an error. `timeout` is in seconds.
  @objc(generateContent:request:timeout:completion:)
  @discardableResult
  public func generateContent(
    _ model: String,
    request: String,
    timeout: Double,
    completion: @escaping (String?, NSError?) -> Void
  ) -> AICancellable {
    run(completion) {
      let json = try RequestJSON.object(request)
      let generativeModel = try self.generativeModel(model, request: json, timeout: timeout)
      let contents = try RequestJSON.contents(json["contents"])
      let response = try await Self.response { try await generativeModel.generateContent(contents) }
      return try ResponseJSON.generateContentResponse(response)
    }
  }

  /// Runs `generateContentStream` on `model` with `request` (see ``generateContent(_:request:timeout:completion:)``),
  /// passing the JSON text of each `GenerateContentResponse` chunk to `onChunk`, and completes with nil at the end of
  /// the stream or with an error. The SDK leaves out the chunks whose candidates are all empty.
  @objc(generateContentStream:request:timeout:onChunk:completion:)
  @discardableResult
  public func generateContentStream(
    _ model: String,
    request: String,
    timeout: Double,
    onChunk: @escaping (String) -> Void,
    completion: @escaping (NSError?) -> Void
  ) -> AICancellable {
    stream(onChunk, completion) {
      let json = try RequestJSON.object(request)
      let generativeModel = try self.generativeModel(model, request: json, timeout: timeout)
      let contents = try RequestJSON.contents(json["contents"])
      return try generativeModel.generateContentStream(contents)
    }
  }

  /// Runs `countTokens` on `model` with `request` (see ``generateContent(_:request:timeout:completion:)``) and completes
  /// with the JSON text of the `CountTokensResponse`, or with an error. With the Gemini Developer API, the code
  /// execution parts of the contents are counted as text (the SDK cannot count them there).
  @objc(countTokens:request:timeout:completion:)
  @discardableResult
  public func countTokens(
    _ model: String,
    request: String,
    timeout: Double,
    completion: @escaping (String?, NSError?) -> Void
  ) -> AICancellable {
    run(completion) {
      let json = try RequestJSON.object(request)
      let generativeModel = try self.generativeModel(model, request: json, timeout: timeout)
      let contents = try self.backend == Self.backendAgentPlatform
        ? RequestJSON.contents(json["contents"])
        : RequestJSON.countTokensContents(json["contents"])
      return try ResponseJSON.countTokensResponse(try await generativeModel.countTokens(contents))
    }
  }

  // MARK: Server prompt templates

  /// Runs the server prompt template `templateId` with `request` (`inputs`, and optionally `history`, whose last
  /// content is the new message, `tools` and `toolConfig`) and completes with the JSON text of the
  /// `GenerateContentResponse`, or with an error.
  @objc(templateGenerateContent:request:timeout:completion:)
  @discardableResult
  public func templateGenerateContent(
    _ templateId: String,
    request: String,
    timeout: Double,
    completion: @escaping (String?, NSError?) -> Void
  ) -> AICancellable {
    run(completion) {
      let json = try RequestJSON.object(request)
      let model = try self.templateModel(json, timeout: timeout)
      let inputs = RequestJSON.templateInputs(json)
      let history = try RequestJSON.contents(json["history"])
      let response = try await Self.response {
        guard let message = history.last else {
          return try await model.generateContent(templateID: templateId, inputs: inputs)
        }
        let chat = model.startChat(templateID: templateId, inputs: inputs, history: Array(history.dropLast()))
        return try await chat.sendMessage([message])
      }
      return try ResponseJSON.generateContentResponse(response)
    }
  }

  /// Runs the server prompt template `templateId` with `request` (see
  /// ``templateGenerateContent(_:request:timeout:completion:)``) as a stream, passing the JSON text of each
  /// `GenerateContentResponse` chunk to `onChunk`, and completes with nil at the end of the stream or with an error.
  @objc(templateGenerateContentStream:request:timeout:onChunk:completion:)
  @discardableResult
  public func templateGenerateContentStream(
    _ templateId: String,
    request: String,
    timeout: Double,
    onChunk: @escaping (String) -> Void,
    completion: @escaping (NSError?) -> Void
  ) -> AICancellable {
    stream(onChunk, completion) {
      let json = try RequestJSON.object(request)
      let model = try self.templateModel(json, timeout: timeout)
      let inputs = RequestJSON.templateInputs(json)
      let history = try RequestJSON.contents(json["history"])
      guard let message = history.last else {
        return try model.generateContentStream(templateID: templateId, inputs: inputs)
      }
      let chat = model.startChat(templateID: templateId, inputs: inputs, history: Array(history.dropLast()))
      return try chat.sendMessageStream([message])
    }
  }

  // MARK: Live API

  /// Opens a Live API session with `model` and `setup` (the Live API's `setup` message without its model: optionally
  /// `generationConfig`, `tools`, `systemInstruction`, `inputAudioTranscription`, `outputAudioTranscription`,
  /// `sessionResumption`, `contextWindowCompression` and `realtimeInputConfig`) and completes with the session once the
  /// server has completed the setup, or with an error (code `4` when the SDK could not open the session).
  @objc(connect:setup:timeout:completion:)
  @discardableResult
  public func connect(
    _ model: String,
    setup: String,
    timeout: Double,
    completion: @escaping (LiveSessionBridge?, NSError?) -> Void
  ) -> AICancellable {
    AICancellable(Task {
      do {
        let json = try RequestJSON.object(setup)
        try Self.checkModelName(model)
        let tools = try RequestJSON.tools(json.objects("tools"))
        let systemInstruction = try json.object("systemInstruction").map { try RequestJSON.content($0) }
        let liveModel = try self.firebaseAI().liveModel(
          modelName: model,
          generationConfig: RequestJSON.liveGenerationConfig(json),
          tools: tools,
          systemInstruction: systemInstruction,
          requestOptions: RequestOptions(timeout: timeout)
        )
        let session: LiveSession
        do {
          session = try await liveModel.connect(
            sessionResumption: RequestJSON.sessionResumption(json.object("sessionResumption"))
          )
        } catch {
          completion(nil, BridgeError.handshakeError(for: error))
          return
        }
        // When the task is cancelled, the SDK stops waiting for the server's setup and returns the session anyway.
        guard !Task.isCancelled else {
          await session.close()
          completion(nil, BridgeError.nsError(for: CancellationError()))
          return
        }
        completion(LiveSessionBridge(session: session), nil)
      } catch {
        completion(nil, BridgeError.nsError(for: error))
      }
    })
  }

  // MARK: Helpers

  /// The SDK's instance, after the checks whose failure stops the process in the SDK (`fatalError`): the app must exist
  /// and have a project ID and an API key, and the location must be a single path segment.
  private func firebaseAI() throws -> FirebaseAI {
    guard let app = FirebaseApp.app(name: appName) else {
      throw RequestJSON.invalid("The Firebase app named \"\(appName)\" does not exist.")
    }
    guard app.options.projectID != nil else {
      throw RequestJSON.invalid("The Firebase app named \"\(appName)\" has no project ID in its configuration.")
    }
    guard app.options.apiKey != nil else {
      throw RequestJSON.invalid("The Firebase app named \"\(appName)\" has no API key in its configuration.")
    }
    let sdkBackend: Backend
    if backend == Self.backendAgentPlatform {
      guard Self.isPathSegment(location) else {
        throw RequestJSON.invalid("""
        Invalid location "\(location)"; see https://firebase.google.com/docs/ai-logic/locations#available-locations \
        for a list of available locations.
        """)
      }
      sdkBackend = .agentPlatform(location: location)
    } else {
      sdkBackend = .googleAI()
    }
    return FirebaseAI.firebaseAI(
      app: app,
      backend: sdkBackend,
      useLimitedUseAppCheckTokens: useLimitedUseAppCheckTokens
    )
  }

  /// A model of the SDK for `model` with the configuration of `request`.
  private func generativeModel(_ model: String, request: [String: Any], timeout: Double) throws -> GenerativeModel {
    try Self.checkModelName(model)
    let generationConfig = try RequestJSON.generationConfig(request.object("generationConfig"))
    let safetySettings = try RequestJSON.safetySettings(request.objects("safetySettings"))
    let tools = try RequestJSON.tools(request.objects("tools"))
    let systemInstruction = try request.object("systemInstruction").map { try RequestJSON.systemInstruction($0) }
    return try firebaseAI().generativeModel(
      modelName: model,
      generationConfig: generationConfig,
      safetySettings: safetySettings,
      tools: tools,
      toolConfig: RequestJSON.toolConfig(request.object("toolConfig")),
      systemInstruction: systemInstruction,
      requestOptions: RequestOptions(timeout: timeout)
    )
  }

  /// A template model of the SDK with the tools of `request`.
  private func templateModel(_ request: [String: Any], timeout: Double) throws -> TemplateGenerativeModel {
    let tools = try RequestJSON.templateTools(request.objects("tools"))
    return try firebaseAI().templateGenerativeModel(
      tools: tools,
      toolConfig: RequestJSON.templateToolConfig(request.object("toolConfig")),
      requestOptions: RequestOptions(timeout: timeout)
    )
  }

  /// The SDK stops the process for a model name that is not a single path segment (`models/gemini-...`, for one).
  private static func checkModelName(_ model: String) throws {
    guard isPathSegment(model) else {
      throw RequestJSON.invalid("""
      Invalid model name "\(model)"; see https://firebase.google.com/docs/ai-logic/models#available-model-names for a \
      list of available model names.
      """)
    }
  }

  private static func isPathSegment(_ value: String) -> Bool {
    !value.isEmpty && value.allSatisfy { !$0.isWhitespace && !$0.isNewline && $0 != "/" }
  }

  /// The response of `operation`, also when the SDK throws it as a blocked prompt or a candidate that stopped early.
  private static func response(
    _ operation: () async throws -> GenerateContentResponse
  ) async throws -> GenerateContentResponse {
    do {
      return try await operation()
    } catch let GenerateContentError.promptBlocked(response) {
      return response
    } catch let GenerateContentError.responseStoppedEarly(_, response) {
      return response
    }
  }

  /// Runs `operation` in a task and completes with its result or its failure.
  private func run(
    _ completion: @escaping (String?, NSError?) -> Void,
    _ operation: @escaping () async throws -> String
  ) -> AICancellable {
    AICancellable(Task {
      do {
        let json = try await operation()
        completion(json, nil)
      } catch {
        completion(nil, BridgeError.nsError(for: error))
      }
    })
  }

  /// Runs the stream `operation` starts in a task, passing each response to `onChunk`, and completes with its failure or
  /// nil. A blocked prompt or a candidate that stopped early ends the SDK's stream with an error carrying the response:
  /// that response is passed on as the last chunk.
  private func stream(
    _ onChunk: @escaping (String) -> Void,
    _ completion: @escaping (NSError?) -> Void,
    _ operation: @escaping () throws -> AsyncThrowingStream<GenerateContentResponse, Error>
  ) -> AICancellable {
    AICancellable(Task {
      do {
        let responses = try operation()
        do {
          for try await response in responses {
            onChunk(try ResponseJSON.generateContentResponse(response))
          }
        } catch let GenerateContentError.promptBlocked(response) {
          onChunk(try ResponseJSON.generateContentResponse(response))
        } catch let GenerateContentError.responseStoppedEarly(_, response) {
          onChunk(try ResponseJSON.generateContentResponse(response))
        }
        completion(nil)
      } catch {
        completion(BridgeError.nsError(for: error))
      }
    })
  }
}

/// A running request of ``AIBridge``, which can be cancelled.
@objc(FAICancellable)
public final class AICancellable: NSObject {
  private let task: Task<Void, Never>

  init(_ task: Task<Void, Never>) {
    self.task = task
    super.init()
  }

  /// Cancels the request; it then completes with an error, unless it completed already.
  @objc public func cancel() {
    task.cancel()
  }
}
