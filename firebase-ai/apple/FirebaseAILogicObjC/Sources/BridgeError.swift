// Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.

import FirebaseAILogic
import Foundation

/// The kinds of failure the bridge reports, as the `code` of its `NSError`s: the ordinals of the Kotlin side's
/// `NativeAIError.Kind`.
enum BridgeErrorKind: Int {
  /// The server answered with an error.
  case server = 0
  /// The request did not complete in time.
  case timeout = 1
  /// A response could not be decoded (by the SDK) or encoded (by the bridge).
  case serialization = 2
  /// The request was rejected before it was sent.
  case invalidRequest = 3
  /// A Live API session could not be opened.
  case handshake = 4
  /// Anything else.
  case unknown = 5
}

/// A failure the bridge reports itself (a request it cannot pass to the SDK, a response it cannot encode), and the
/// mapping of the SDK's errors to the bridge's `NSError`s.
struct BridgeError: Error {
  /// The `userInfo` key of the HTTP status of a server error (an `NSNumber`).
  static let statusKey = "status"
  /// The `userInfo` key of the JSON text of the details of a server error.
  static let detailsKey = "details"

  let kind: BridgeErrorKind
  let message: String

  /// The error the bridge reports for `kind` and `message`.
  static func make(_ kind: BridgeErrorKind, _ message: String, userInfo: [String: Any] = [:]) -> NSError {
    var info = userInfo
    info[NSLocalizedDescriptionKey] = message
    return NSError(domain: AIBridge.errorDomain, code: kind.rawValue, userInfo: info)
  }

  /// The error the bridge reports for a failed request. `generateContent` and `generateContentStream` wrap the SDK's
  /// errors in `GenerateContentError.internalError`; `countTokens` and the templates throw them as they are.
  static func nsError(for error: Error) -> NSError {
    switch error {
    case let error as BridgeError:
      return make(error.kind, error.message)
    case let error as GenerateContentError:
      switch error {
      case let .internalError(underlying):
        return nsError(for: underlying)
      case let .promptImageContentError(underlying):
        return make(.invalidRequest, describe(underlying))
      case .promptBlocked, .responseStoppedEarly:
        // The callers return the response of these instead (the Kotlin side checks it as Android does).
        return make(.unknown, String(describing: error))
      @unknown default:
        return make(.unknown, String(describing: error))
      }
    case is DecodingError:
      return make(.serialization, describe(error))
    case is EncodingError:
      return make(.invalidRequest, describe(error))
    case is CancellationError:
      return make(.unknown, "The request was cancelled.")
    case let error as URLError where error.code == .timedOut:
      return make(.timeout, error.localizedDescription)
    default:
      break
    }
    if let error = serverError(error) {
      return error
    }
    switch String(describing: type(of: error)) {
    case "UnrecognizedRPCError":
      // A server error whose body is not the JSON of an RPC error (the HTML of a 404 for an unknown location, for
      // one); the SDK keeps the body but not the HTTP status.
      let body = Mirror(reflecting: error).descendant("responseBody") as? String ?? describe(error)
      return make(.server, "Unexpected Response:\n\(body)")
    case "InvalidCandidateError":
      // All candidates were empty (the SDK rejects such responses), or a candidate's content could not be decoded.
      return make(.serialization, String(describing: error))
    default:
      return make(.unknown, describe(error))
    }
  }

  /// The error the bridge reports when opening a Live API session failed: the SDK's failures to open the WebSocket or
  /// to complete the setup are handshake failures.
  static func handshakeError(for error: Error) -> NSError {
    if error is BridgeError || error is CancellationError {
      return nsError(for: error)
    }
    return make(.handshake, describe(error))
  }

  /// The error that ended a Live API session, or nil when the server closed it normally.
  static func liveSessionError(for error: Error) -> NSError? {
    let nsError = error as NSError
    if error is LiveSessionUnexpectedClosureError,
       let closure = nsError.userInfo[NSUnderlyingErrorKey] as? NSError,
       closure.code == URLSessionWebSocketTask.CloseCode.normalClosure.rawValue {
      return nil
    }
    if error is LiveSessionUnsupportedMessageError {
      // The SDK ends the session on a server message it cannot decode.
      return make(.serialization, nsError.localizedDescription)
    }
    return make(.unknown, nsError.localizedDescription)
  }

  /// The SDK's `BackendError`, the error of a server answer (internal: it arrives as an error of the domain below, whose
  /// code is the HTTP status). The server's message and error details are not exposed otherwise, so they are read with
  /// `Mirror` from its fields `httpResponseCode`, `message` and `details` (each with `type`, `reason`, `domain` and
  /// `metadata`); should the SDK rename them, the error's description and code are used.
  private static func serverError(_ error: Error) -> NSError? {
    let nsError = error as NSError
    guard nsError.domain == "com.google.firebase.firebaseai.BackendError" else {
      return nil
    }
    let fields = Mirror(reflecting: error)
    let message = fields.descendant("message") as? String ?? nsError.localizedDescription
    let status = fields.descendant("httpResponseCode") as? Int ?? nsError.code
    let details = (fields.descendant("details") as? [Any] ?? []).map { detail -> [String: Any] in
      let detailFields = Mirror(reflecting: detail)
      var json: [String: Any] = [:]
      if let type = detailFields.descendant("type") as? String {
        json["@type"] = type
      }
      if let reason = detailFields.descendant("reason") as? String {
        json["reason"] = reason
      }
      if let domain = detailFields.descendant("domain") as? String {
        json["domain"] = domain
      }
      if let metadata = detailFields.descendant("metadata") as? [String: String] {
        json["metadata"] = metadata
      }
      return json
    }
    var userInfo: [String: Any] = [statusKey: NSNumber(value: status)]
    if let detailsText = try? ResponseJSON.text(details) {
      userInfo[detailsKey] = detailsText
    }
    return make(.server, message, userInfo: userInfo)
  }

  /// The message of an error: the description of the coding errors (which says what failed where), the localized
  /// description of the others.
  static func describe(_ error: Error) -> String {
    switch error {
    case is DecodingError, is EncodingError:
      return String(describing: error)
    default:
      return (error as NSError).localizedDescription
    }
  }
}
