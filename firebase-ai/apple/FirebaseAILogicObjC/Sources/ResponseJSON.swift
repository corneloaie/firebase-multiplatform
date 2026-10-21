// Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.

import FirebaseAILogic
import Foundation

/// Writes the SDK's responses as the JSON of the Firebase AI Logic REST API, the form in which the Kotlin side reads
/// them (as the Android SDK reads the server's JSON). `GenerateContentResponse` is only `Decodable`, so responses are
/// written field by field from their public properties; parts are written with their own `Encodable` conformance, the
/// only public way to their thought signatures.
///
/// The SDK fills in some values the server left out; those are left out again where Android reports them as missing
/// (a model version, a safety rating's severity, `blocked` when false). The SDK does not keep the grounding metadata's
/// `retrievalQueries` and the search entry point's `sdkBlob`, the inline data's `displayName`, and the parts it does not
/// know.
enum ResponseJSON {
  /// The JSON text of `value`, a dictionary or an array of strings, numbers, booleans, `NSNull`, arrays and dictionaries.
  static func text(_ value: Any) throws -> String {
    guard JSONSerialization.isValidJSONObject(value) else {
      throw BridgeError(kind: .serialization, message: "The response of the SDK could not be written as JSON.")
    }
    let data = try JSONSerialization.data(withJSONObject: value, options: [.withoutEscapingSlashes])
    return String(decoding: data, as: UTF8.self)
  }

  // MARK: generateContent and countTokens

  /// The JSON text of a `GenerateContentResponse`.
  static func generateContentResponse(_ response: GenerateContentResponse) throws -> String {
    var json: [String: Any] = ["candidates": try response.candidates.map { try candidate($0) }]
    if let promptFeedback = response.promptFeedback {
      json["promptFeedback"] = self.promptFeedback(promptFeedback)
    }
    if let usageMetadata = response.usageMetadata {
      json["usageMetadata"] = self.usageMetadata(usageMetadata)
    }
    // The SDK reports a missing model version as this placeholder.
    if response.modelVersion != "unknown-model-version" {
      json["modelVersion"] = response.modelVersion
    }
    return try text(json)
  }

  /// The JSON text of a `CountTokensResponse`.
  static func countTokensResponse(_ response: CountTokensResponse) throws -> String {
    let json: [String: Any] = [
      "totalTokens": response.totalTokens,
      "promptTokensDetails": response.promptTokensDetails.map { modalityTokenCount($0) },
    ]
    return try text(json)
  }

  static func candidate(_ candidate: Candidate) throws -> [String: Any] {
    var json: [String: Any] = ["safetyRatings": candidate.safetyRatings.compactMap { safetyRating($0) }]
    let contentJSON = try content(candidate.content)
    // The SDK gives a candidate without content an empty one with the role "user"; it is left out, as the server did.
    if let parts = contentJSON["parts"] as? [Any], !parts.isEmpty {
      json["content"] = contentJSON
    }
    if let finishReason = candidate.finishReason {
      json["finishReason"] = finishReason.rawValue
    }
    if let finishMessage = candidate.finishMessage {
      json["finishMessage"] = finishMessage
    }
    if let citationMetadata = candidate.citationMetadata {
      json["citationMetadata"] = ["citations": citationMetadata.citations.map { citation($0) }]
    }
    if let groundingMetadata = candidate.groundingMetadata {
      json["groundingMetadata"] = self.groundingMetadata(groundingMetadata)
    }
    if let urlContextMetadata = candidate.urlContextMetadata {
      json["urlContextMetadata"] = ["urlMetadata": urlContextMetadata.urlMetadata.map { urlMetadata($0) }]
    }
    return json
  }

  /// A content: its role and the parts the SDK knows.
  static func content(_ content: ModelContent) throws -> [String: Any] {
    let encoder = JSONEncoder()
    var json: [String: Any] = ["parts": try content.parts.map { try part($0, encoder: encoder) }]
    if let role = content.role {
      json["role"] = role
    }
    return json
  }

  /// A part, as its `Encodable` conformance writes it: with the REST API's keys (`text`, `inlineData`, `functionCall`,
  /// ..., and `thoughtSignature`), except for the thought flag, which it writes under the name of its stored property.
  static func part(_ part: any Part, encoder: JSONEncoder) throws -> [String: Any] {
    let data = try encoder.encode(part)
    guard var json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
      throw BridgeError(kind: .serialization, message: "A part of the response could not be written as JSON.")
    }
    if let thought = json.removeValue(forKey: "_isThought") {
      json["thought"] = thought
    }
    return json
  }

  /// A safety rating, or nil for one without a category or probability (the SDK filters those out of the candidates'
  /// ratings but not of the prompt feedback's, the Kotlin side out of both).
  static func safetyRating(_ rating: SafetyRating) -> [String: Any]? {
    guard rating.category.rawValue != "HARM_CATEGORY_UNSPECIFIED",
          rating.probability.rawValue != "HARM_PROBABILITY_UNSPECIFIED" else {
      return nil
    }
    var json: [String: Any] = [
      "category": rating.category.rawValue,
      "probability": rating.probability.rawValue,
      "probabilityScore": Double(rating.probabilityScore),
    ]
    // The severity and its score come only from the Agent Platform backend.
    if rating.severity.rawValue != "HARM_SEVERITY_UNSPECIFIED" {
      json["severity"] = rating.severity.rawValue
      json["severityScore"] = Double(rating.severityScore)
    }
    if rating.blocked {
      json["blocked"] = true
    }
    return json
  }

  /// A citation; its indices are the server's (UTF-8 byte offsets, which the Kotlin side converts).
  static func citation(_ citation: Citation) -> [String: Any] {
    var json: [String: Any] = ["startIndex": citation.startIndex, "endIndex": citation.endIndex]
    if let uri = citation.uri {
      json["uri"] = uri
    }
    if let title = citation.title {
      json["title"] = title
    }
    if let license = citation.license {
      json["license"] = license
    }
    if let date = citation.publicationDate {
      json["publicationDate"] = compact(["year": date.year, "month": date.month, "day": date.day])
    }
    return json
  }

  static func groundingMetadata(_ metadata: GroundingMetadata) -> [String: Any] {
    var json: [String: Any] = [
      "webSearchQueries": metadata.webSearchQueries,
      "groundingChunks": metadata.groundingChunks.map { chunk -> [String: Any] in
        var chunkJSON: [String: Any] = [:]
        if let web = chunk.web {
          chunkJSON["web"] = compact(["uri": web.uri, "title": web.title, "domain": web.domain])
        }
        if let maps = chunk.maps {
          chunkJSON["maps"] = compact(["uri": maps.url?.absoluteString, "title": maps.title, "placeId": maps.placeID])
        }
        return chunkJSON
      },
      "groundingSupports": metadata.groundingSupports.map { support -> [String: Any] in
        [
          "segment": [
            "partIndex": support.segment.partIndex,
            "startIndex": support.segment.startIndex,
            "endIndex": support.segment.endIndex,
            "text": support.segment.text,
          ] as [String: Any],
          "groundingChunkIndices": support.groundingChunkIndices,
        ]
      },
    ]
    if let searchEntryPoint = metadata.searchEntryPoint {
      json["searchEntryPoint"] = ["renderedContent": searchEntryPoint.renderedContent]
    }
    return json
  }

  static func urlMetadata(_ metadata: URLMetadata) -> [String: Any] {
    compact([
      "retrievedUrl": metadata.retrievedURL?.absoluteString,
      "urlRetrievalStatus": metadata.retrievalStatus.rawValue,
    ])
  }

  static func promptFeedback(_ feedback: PromptFeedback) -> [String: Any] {
    var json: [String: Any] = ["safetyRatings": feedback.safetyRatings.compactMap { safetyRating($0) }]
    if let blockReason = feedback.blockReason {
      json["blockReason"] = blockReason.rawValue
    }
    if let blockReasonMessage = feedback.blockReasonMessage {
      json["blockReasonMessage"] = blockReasonMessage
    }
    return json
  }

  static func usageMetadata(_ usage: GenerateContentResponse.UsageMetadata) -> [String: Any] {
    [
      "promptTokenCount": usage.promptTokenCount,
      "candidatesTokenCount": usage.candidatesTokenCount,
      "totalTokenCount": usage.totalTokenCount,
      "cachedContentTokenCount": usage.cachedContentTokenCount,
      "thoughtsTokenCount": usage.thoughtsTokenCount,
      "toolUsePromptTokenCount": usage.toolUsePromptTokenCount,
      "promptTokensDetails": usage.promptTokensDetails.map { modalityTokenCount($0) },
      "candidatesTokensDetails": usage.candidatesTokensDetails.map { modalityTokenCount($0) },
      "cacheTokensDetails": usage.cacheTokensDetails.map { modalityTokenCount($0) },
      "toolUsePromptTokensDetails": usage.toolUsePromptTokensDetails.map { modalityTokenCount($0) },
    ]
  }

  static func modalityTokenCount(_ count: ModalityTokenCount) -> [String: Any] {
    ["modality": count.modality.rawValue, "tokenCount": count.tokenCount]
  }

  // MARK: Live API

  /// The JSON text of a server message of the Live API: an object with one key, `serverContent`, `toolCall`,
  /// `toolCallCancellation`, `goAway` (with `timeLeft` in seconds) or `sessionResumptionUpdate`; an empty object for a
  /// message a newer SDK may add. The SDK leaves out `setupComplete` and the messages' usage metadata.
  static func liveServerMessage(_ message: LiveServerMessage) throws -> String {
    let json: [String: Any]
    switch message.payload {
    case let .content(content):
      json = ["serverContent": try serverContent(content)]
    case let .toolCall(toolCall):
      json = ["toolCall": ["functionCalls": (toolCall.functionCalls ?? []).map { functionCall($0) }]]
    case let .toolCallCancellation(cancellation):
      json = ["toolCallCancellation": ["ids": cancellation.ids ?? []]]
    case let .goingAwayNotice(notice):
      json = ["goAway": compact(["timeLeft": notice.timeLeft])]
    case let .sessionResumptionUpdate(update):
      json = ["sessionResumptionUpdate": compact([
        "newHandle": update.newHandle,
        "resumable": update.resumable,
        "lastConsumedClientMessageIndex": update.lastConsumedClientMessageIndex,
      ])]
    @unknown default:
      json = [:]
    }
    return try text(json)
  }

  static func serverContent(_ content: LiveServerContent) throws -> [String: Any] {
    var json: [String: Any] = [:]
    if let modelTurn = content.modelTurn {
      json["modelTurn"] = try self.content(modelTurn)
    }
    if content.isTurnComplete {
      json["turnComplete"] = true
    }
    if content.wasInterrupted {
      json["interrupted"] = true
    }
    if content.isGenerationComplete {
      json["generationComplete"] = true
    }
    if let transcription = content.inputAudioTranscription {
      json["inputTranscription"] = compact(["text": transcription.text])
    }
    if let transcription = content.outputAudioTranscription {
      json["outputTranscription"] = compact(["text": transcription.text])
    }
    return json
  }

  static func functionCall(_ call: FunctionCallPart) -> [String: Any] {
    compact(["name": call.name, "args": object(call.args), "id": call.functionId])
  }

  // MARK: Helpers

  /// A `JSONObject` of the SDK as a dictionary for `JSONSerialization`.
  static func object(_ object: JSONObject) -> [String: Any] {
    object.mapValues { value($0) }
  }

  static func value(_ value: JSONValue) -> Any {
    switch value {
    case .null:
      return NSNull()
    case let .number(number):
      return number
    case let .string(string):
      return string
    case let .bool(bool):
      return bool
    case let .object(object):
      return self.object(object)
    case let .array(array):
      return array.map { self.value($0) }
    }
  }

  /// The entries of `values` that are not nil.
  static func compact(_ values: [String: Any?]) -> [String: Any] {
    values.compactMapValues { $0 }
  }
}
