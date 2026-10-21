// Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.

import CoreLocation
import FirebaseAILogic
import Foundation

/// Reads the requests of the Kotlin side, the JSON of the Firebase AI Logic REST API, into the SDK's types.
///
/// Contents are decoded with the SDK's own `Decodable` conformance of `ModelContent`, which keeps the thought flags
/// and signatures of the parts. The configuration types are only `Encodable`, so they are built with their public
/// initializers and factories, which cannot take everything Android sends:
/// - `responseJsonSchema` (JSON Schema) has no field in the SDK; it is sent as the OpenAPI `responseSchema`, and so is
///   an automatic function's `parametersJsonSchema` (as `parameters`); a function's `responseJsonSchema` is left out.
/// - A function response's `parts` and an inline data's `displayName` are left out.
/// - The values of the SDK's closed sets (thresholds, modalities, thinking levels, aspect ratios, image sizes) are
///   those it has constants for; harm categories it has no constant for are decoded with its own decoder.
enum RequestJSON {
  /// The keys of the data of the parts the SDK knows. The SDK keeps a part with none of them without data, and then
  /// fails to encode it; such parts are left out.
  static let partKeys = [
    "text", "inlineData", "fileData", "functionCall", "functionResponse", "executableCode", "codeExecutionResult",
  ]

  /// The keys of the parts the SDK can rebuild a content from (it cannot rebuild one with code execution parts, and
  /// does so with the system instruction).
  static let rebuildablePartKeys = ["text", "inlineData", "fileData", "functionCall", "functionResponse"]

  static func invalid(_ message: String) -> BridgeError {
    BridgeError(kind: .invalidRequest, message: message)
  }

  // MARK: JSON text

  /// The JSON object of `text`.
  static func object(_ text: String) throws -> [String: Any] {
    guard let object = try parse(text) as? [String: Any] else {
      throw invalid("The request is not a JSON object.")
    }
    return object
  }

  /// The JSON array of `text`.
  static func array(_ text: String) throws -> [Any] {
    guard let array = try parse(text) as? [Any] else {
      throw invalid("The request is not a JSON array.")
    }
    return array
  }

  private static func parse(_ text: String) throws -> Any {
    do {
      return try JSONSerialization.jsonObject(with: Data(text.utf8))
    } catch {
      throw invalid("The request is not valid JSON: \(error.localizedDescription)")
    }
  }

  /// `value` (of `JSONSerialization`) decoded as `type` by the SDK's `Decodable` conformance.
  static func decode<T: Decodable>(_ type: T.Type, from value: Any) throws -> T {
    guard JSONSerialization.isValidJSONObject(value) else {
      throw invalid("The request could not be read as \(T.self).")
    }
    let data = try JSONSerialization.data(withJSONObject: value)
    do {
      return try JSONDecoder().decode(type, from: data)
    } catch {
      throw invalid("The request could not be read as \(T.self): \(error)")
    }
  }

  // MARK: Contents

  /// `contents` (or a template's `history`, a Live API turn) as the SDK's contents.
  static func contents(_ value: Any?) throws -> [ModelContent] {
    guard let value, !(value is NSNull) else {
      return []
    }
    guard let array = value as? [Any] else {
      throw invalid("The contents are not a JSON array.")
    }
    let contents = array.compactMap { $0 as? [String: Any] }.map { sanitized($0, partKeys: partKeys) }
    return try decode([ModelContent].self, from: contents)
  }

  /// A content (a Live API session's system instruction).
  static func content(_ json: [String: Any]) throws -> ModelContent {
    try decode(ModelContent.self, from: sanitized(json, partKeys: partKeys))
  }

  /// The system instruction of a model. The SDK rebuilds it from its parts, which it cannot do with code execution
  /// parts (a system instruction is text anyway), so only the other parts are kept.
  static func systemInstruction(_ json: [String: Any]) throws -> ModelContent {
    try decode(ModelContent.self, from: sanitized(json, partKeys: rebuildablePartKeys))
  }

  /// The contents of `countTokens` with the Gemini Developer API, where the SDK rebuilds each content from its parts,
  /// which it cannot do with code execution parts: those are counted as text parts of their code and output.
  static func countTokensContents(_ value: Any?) throws -> [ModelContent] {
    guard let array = value as? [Any] else {
      return try contents(value)
    }
    return try contents(array.map { item -> Any in
      guard var content = item as? [String: Any], let parts = content.objects("parts") else {
        return item
      }
      content["parts"] = parts.map { part -> [String: Any] in
        if let code = part.object("executableCode") {
          return ["text": code.string("code") ?? ""]
        }
        if let result = part.object("codeExecutionResult") {
          return ["text": result.string("output") ?? ""]
        }
        return part
      }
      return content
    })
  }

  /// `content` with the role "user" when it has none (the SDK's template chat would otherwise rebuild it from its parts,
  /// see `rebuildablePartKeys`), and only the parts with one of `partKeys`.
  private static func sanitized(_ content: [String: Any], partKeys: [String]) -> [String: Any] {
    var json = content
    if content.string("role") == nil {
      json["role"] = "user"
    }
    json["parts"] = (content.objects("parts") ?? []).filter { part in partKeys.contains { part.has($0) } }
    return json
  }

  // MARK: Schemas

  /// A schema as the SDK's `Schema`, from the OpenAPI form of `responseSchema` and a function's `parameters`
  /// (`"type": "STRING"`, `nullable`) or, with `jsonSchema`, from the JSON Schema form of `responseJsonSchema`,
  /// `parametersJsonSchema` and a template function's `inputSchema` (`"type": "string"` or `["string", "null"]`, `null`
  /// among the `enum` values, no type for an enumeration). The SDK's schemas have the OpenAPI form only.
  static func schema(_ json: [String: Any], jsonSchema: Bool) throws -> Schema {
    var nullable = json.bool("nullable") ?? false
    var type: String?
    if let name = json["type"] as? String {
      type = name.uppercased()
    } else if let names = json["type"] as? [Any] {
      let names = names.compactMap { $0 as? String }
      nullable = nullable || names.contains { $0.lowercased() == "null" }
      type = names.first { $0.lowercased() != "null" }?.uppercased()
    }
    var enumValues = json["enum"] as? [Any]
    if jsonSchema, let values = enumValues, values.contains(where: isNull) {
      nullable = true
      enumValues = values.filter { !isNull($0) }
    }
    let description = json.string("description")
    let title = json.string("title")
    let format = json.string("format")

    if let anyOf = json.objects("anyOf"), !anyOf.isEmpty {
      return .anyOf(schemas: try anyOf.map { try schema($0, jsonSchema: jsonSchema) })
    }
    if let values = enumValues?.compactMap({ $0 as? String }), type == nil || type == "STRING" {
      return .enumeration(values: values, description: description, title: title, nullable: nullable)
    }
    switch type {
    case "STRING":
      return .string(description: description, title: title, nullable: nullable, format: format.map { .custom($0) })
    case "INTEGER":
      return .integer(
        description: description,
        title: title,
        nullable: nullable,
        format: format.map { .custom($0) },
        minimum: json.int("minimum"),
        maximum: json.int("maximum")
      )
    case "NUMBER" where format == "float":
      return .float(
        description: description,
        title: title,
        nullable: nullable,
        minimum: json.float("minimum"),
        maximum: json.float("maximum")
      )
    case "NUMBER":
      return .double(
        description: description,
        title: title,
        nullable: nullable,
        minimum: json.number("minimum"),
        maximum: json.number("maximum")
      )
    case "BOOLEAN":
      return .boolean(description: description, title: title, nullable: nullable)
    case "ARRAY":
      guard let items = json.object("items") else {
        throw invalid("An array schema has no items.")
      }
      return .array(
        items: try schema(items, jsonSchema: jsonSchema),
        description: description,
        title: title,
        nullable: nullable,
        minItems: json.int("minItems"),
        maxItems: json.int("maxItems")
      )
    case "OBJECT":
      let (properties, optionalProperties) = try self.properties(json, jsonSchema: jsonSchema)
      return .object(
        properties: properties,
        optionalProperties: optionalProperties,
        propertyOrdering: json.strings("propertyOrdering"),
        description: description,
        title: title,
        nullable: nullable
      )
    default:
      throw invalid("The schema type \(type ?? "(none)") is not supported.")
    }
  }

  /// The properties of an object schema, and those of them that are not required (the SDK takes the optional ones).
  static func properties(_ json: [String: Any]?, jsonSchema: Bool) throws -> ([String: Schema], [String]) {
    var properties: [String: Schema] = [:]
    for (name, value) in json?.object("properties") ?? [:] {
      if let property = value as? [String: Any] {
        properties[name] = try schema(property, jsonSchema: jsonSchema)
      }
    }
    let required = Set(json?.strings("required") ?? [])
    return (properties, properties.keys.filter { !required.contains($0) }.sorted())
  }

  private static func isNull(_ value: Any) -> Bool {
    value is NSNull || (value as? String) == "null"
  }

  // MARK: Model configuration

  static func generationConfig(_ json: [String: Any]?) throws -> GenerationConfig? {
    guard let json else {
      return nil
    }
    let responseSchema: Schema?
    if let schemaJSON = json.object("responseSchema") {
      responseSchema = try schema(schemaJSON, jsonSchema: false)
    } else if let schemaJSON = json.object("responseJsonSchema") {
      responseSchema = try schema(schemaJSON, jsonSchema: true)
    } else {
      responseSchema = nil
    }
    // The initializer that takes the sampling parameters is deprecated (Gemini 3 ignores them); Android sends them.
    return GenerationConfig(
      temperature: json.float("temperature"),
      topP: json.float("topP"),
      topK: json.int("topK"),
      candidateCount: json.int("candidateCount"),
      maxOutputTokens: json.int("maxOutputTokens"),
      presencePenalty: json.float("presencePenalty"),
      frequencyPenalty: json.float("frequencyPenalty"),
      stopSequences: json.strings("stopSequences"),
      responseMIMEType: json.string("responseMimeType"),
      responseSchema: responseSchema,
      responseModalities: json.strings("responseModalities")?.compactMap { responseModality($0) },
      thinkingConfig: thinkingConfig(json.object("thinkingConfig")),
      imageConfig: imageConfig(json.object("imageConfig")),
      speechConfig: speechConfig(json.object("speechConfig"))
    )
  }

  static func responseModality(_ value: String) -> ResponseModality? {
    switch value {
    case "TEXT": return .text
    case "IMAGE": return .image
    case "AUDIO": return .audio
    default: return nil
    }
  }

  /// A thinking configuration: a thinking level or a thinking budget (the SDK takes one of them, as Android's builder).
  static func thinkingConfig(_ json: [String: Any]?) -> ThinkingConfig? {
    guard let json else {
      return nil
    }
    let includeThoughts = json.bool("includeThoughts")
    let level: ThinkingConfig.ThinkingLevel?
    switch json.string("thinkingLevel") {
    case "MINIMAL": level = .minimal
    case "LOW": level = .low
    case "MEDIUM": level = .medium
    case "HIGH": level = .high
    default: level = nil
    }
    if let level {
      return ThinkingConfig(thinkingLevel: level, includeThoughts: includeThoughts)
    }
    return ThinkingConfig(thinkingBudget: json.int("thinkingBudget"), includeThoughts: includeThoughts)
  }

  static func imageConfig(_ json: [String: Any]?) -> ImageConfig? {
    guard let json else {
      return nil
    }
    let aspectRatio: ImageConfig.AspectRatio?
    switch json.string("aspectRatio") {
    case "1:1": aspectRatio = .square1x1
    case "9:16": aspectRatio = .portrait9x16
    case "16:9": aspectRatio = .landscape16x9
    case "3:4": aspectRatio = .portrait3x4
    case "4:3": aspectRatio = .landscape4x3
    case "2:3": aspectRatio = .portrait2x3
    case "3:2": aspectRatio = .landscape3x2
    case "4:5": aspectRatio = .portrait4x5
    case "5:4": aspectRatio = .landscape5x4
    case "1:4": aspectRatio = .portrait1x4
    case "4:1": aspectRatio = .landscape4x1
    case "1:8": aspectRatio = .portrait1x8
    case "8:1": aspectRatio = .landscape8x1
    case "21:9": aspectRatio = .ultrawide21x9
    default: aspectRatio = nil
    }
    let imageSize: ImageConfig.ImageSize?
    switch json.string("imageSize") {
    case "512": imageSize = .size512
    case "1K": imageSize = .size1K
    case "2K": imageSize = .size2K
    case "4K": imageSize = .size4K
    default: imageSize = nil
    }
    return ImageConfig(aspectRatio: aspectRatio, imageSize: imageSize)
  }

  /// A speech configuration: a prebuilt voice or the voices of several speakers (the SDK takes one of them).
  static func speechConfig(_ json: [String: Any]?) -> SpeechConfig? {
    guard let json else {
      return nil
    }
    let languageCode = json.string("languageCode")
    if let voiceName = json.object("voiceConfig")?.object("prebuiltVoiceConfig")?.string("voiceName") {
      return SpeechConfig(voiceName: voiceName, languageCode: languageCode)
    }
    if let speakers = json.object("multiSpeakerVoiceConfig")?.objects("speakerVoiceConfigs") {
      let speakerVoiceConfigs = speakers.compactMap { speaker -> SpeakerVoiceConfig? in
        guard let voiceName = speaker.object("voiceConfig")?.object("prebuiltVoiceConfig")?.string("voiceName") else {
          return nil
        }
        return SpeakerVoiceConfig(speaker: speaker.string("speaker") ?? "", voiceName: voiceName)
      }
      return SpeechConfig(
        multiSpeakerVoiceConfig: MultiSpeakerVoiceConfig(speakerVoiceConfigs: speakerVoiceConfigs),
        languageCode: languageCode
      )
    }
    return nil
  }

  static func safetySettings(_ json: [[String: Any]]?) throws -> [SafetySetting]? {
    try json?.map { setting -> SafetySetting in
      let category = try harmCategory(setting.string("category") ?? "")
      let blockThreshold = try threshold(setting.string("threshold") ?? "")
      return SafetySetting(harmCategory: category, threshold: blockThreshold, method: method(setting.string("method")))
    }
  }

  static func harmCategory(_ value: String) throws -> HarmCategory {
    switch value {
    case "HARM_CATEGORY_HARASSMENT": return .harassment
    case "HARM_CATEGORY_HATE_SPEECH": return .hateSpeech
    case "HARM_CATEGORY_SEXUALLY_EXPLICIT": return .sexuallyExplicit
    case "HARM_CATEGORY_DANGEROUS_CONTENT": return .dangerousContent
    case "HARM_CATEGORY_CIVIC_INTEGRITY": return .civicIntegrity
    default:
      // The image categories have no constants; the SDK's decoder keeps any value.
      return try decode([HarmCategory].self, from: [value])[0]
    }
  }

  static func threshold(_ value: String) throws -> SafetySetting.HarmBlockThreshold {
    switch value {
    case "BLOCK_LOW_AND_ABOVE": return .blockLowAndAbove
    case "BLOCK_MEDIUM_AND_ABOVE": return .blockMediumAndAbove
    case "BLOCK_ONLY_HIGH": return .blockOnlyHigh
    case "BLOCK_NONE": return .blockNone
    case "OFF": return .off
    default: throw invalid("The harm block threshold \(value) is not supported.")
    }
  }

  static func method(_ value: String?) -> SafetySetting.HarmBlockMethod? {
    switch value {
    case "SEVERITY": return .severity
    case "PROBABILITY": return .probability
    default: return nil
    }
  }

  // MARK: Tools

  /// The tools of a model. The SDK's tools have one kind each, so one tool of the request (which always has
  /// `functionDeclarations`, empty for the other kinds) can make several.
  static func tools(_ json: [[String: Any]]?) throws -> [GenerativeModel.Tool]? {
    var tools: [GenerativeModel.Tool] = []
    for tool in json ?? [] {
      let declarations = try (tool.objects("functionDeclarations") ?? []).map { try functionDeclaration($0) }
      if !declarations.isEmpty {
        tools.append(.functionDeclarations(declarations))
      }
      if tool.has("googleSearch") {
        tools.append(.googleSearch())
      }
      if tool.has("codeExecution") {
        tools.append(.codeExecution())
      }
      if tool.has("urlContext") {
        tools.append(.urlContext())
      }
      if tool.has("googleMaps") {
        tools.append(.googleMaps())
      }
    }
    return tools.isEmpty ? nil : tools
  }

  /// A function declaration: its `parameters`, or an automatic function's `parametersJsonSchema`, become the SDK's
  /// parameters; an automatic function's `responseJsonSchema` has no place in the SDK's declarations.
  static func functionDeclaration(_ json: [String: Any]) throws -> FunctionDeclaration {
    let openAPISchema = json.object("parameters")
    let (parameters, optionalParameters) = try properties(
      openAPISchema ?? json.object("parametersJsonSchema"),
      jsonSchema: openAPISchema == nil
    )
    return FunctionDeclaration(
      name: json.string("name") ?? "",
      description: json.string("description") ?? "",
      parameters: parameters,
      optionalParameters: optionalParameters
    )
  }

  static func toolConfig(_ json: [String: Any]?) -> ToolConfig? {
    guard let json else {
      return nil
    }
    var functionCallingConfig: FunctionCallingConfig?
    if let config = json.object("functionCallingConfig") {
      switch config.string("mode") {
      case "ANY":
        functionCallingConfig = FunctionCallingConfig.any(allowedFunctionNames: config.strings("allowedFunctionNames"))
      case "NONE":
        functionCallingConfig = FunctionCallingConfig.none()
      default:
        functionCallingConfig = FunctionCallingConfig.auto()
      }
    }
    return ToolConfig(
      functionCallingConfig: functionCallingConfig,
      retrievalConfig: retrievalConfig(json.object("retrievalConfig"))
    )
  }

  static func retrievalConfig(_ json: [String: Any]?) -> RetrievalConfig? {
    guard let json else {
      return nil
    }
    var location: CLLocationCoordinate2D?
    if let latLng = json.object("latLng"), let latitude = latLng.number("latitude"),
       let longitude = latLng.number("longitude") {
      location = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
    return RetrievalConfig(location: location, languageCode: json.string("languageCode"))
  }

  // MARK: Templates

  /// The template's inputs, as `JSONSerialization` read them (the SDK tells its booleans from its numbers; it rejects
  /// a `null`, which then fails the request).
  static func templateInputs(_ request: [String: Any]) -> [String: Any] {
    request.object("inputs") ?? [:]
  }

  /// The tools of a template: its functions (`templateFunctions`) and Google Maps.
  ///
  /// The SDK sends each function declaration with an input schema, which overrides the template's. On Android, a
  /// declaration without an input schema only names a function of the template, whose own schema then applies; such
  /// declarations are therefore left out here. The SDK has no output schemas for template functions.
  static func templateTools(_ json: [[String: Any]]?) throws -> [TemplateTool]? {
    var tools: [TemplateTool] = []
    for tool in json ?? [] {
      let declarations = try (tool.objects("templateFunctions") ?? []).compactMap { function -> FunctionDeclaration? in
        guard let inputSchema = function.object("inputSchema") else {
          return nil
        }
        let (parameters, optionalParameters) = try properties(inputSchema, jsonSchema: true)
        return FunctionDeclaration(
          name: function.string("name") ?? "",
          description: "",
          parameters: parameters,
          optionalParameters: optionalParameters
        )
      }
      if !declarations.isEmpty {
        tools.append(.functionDeclarations(declarations))
      }
      if tool.has("googleMaps") {
        tools.append(.googleMaps())
      }
    }
    return tools.isEmpty ? nil : tools
  }

  static func templateToolConfig(_ json: [String: Any]?) -> TemplateToolConfig? {
    guard let json else {
      return nil
    }
    return TemplateToolConfig(retrievalConfig: retrievalConfig(json.object("retrievalConfig")))
  }

  // MARK: Live API

  /// The configuration of a Live API session, from its `setup`: the `generationConfig` and the transcription, context
  /// window compression and realtime input settings.
  static func liveGenerationConfig(_ setup: [String: Any]) -> LiveGenerationConfig? {
    let config = setup.object("generationConfig")
    let inputAudioTranscription = setup.has("inputAudioTranscription") ? AudioTranscriptionConfig() : nil
    let outputAudioTranscription = setup.has("outputAudioTranscription") ? AudioTranscriptionConfig() : nil
    let contextWindowCompression = setup.object("contextWindowCompression").map { json in
      ContextWindowCompressionConfig(
        triggerTokens: json.int("triggerTokens"),
        slidingWindow: json.object("slidingWindow").map { SlidingWindow(targetTokens: $0.int("targetTokens")) }
      )
    }
    let realtimeInputConfig = setup.object("realtimeInputConfig").map { realtimeInput($0) }
    guard config != nil || inputAudioTranscription != nil || outputAudioTranscription != nil
      || contextWindowCompression != nil || realtimeInputConfig != nil else {
      return nil
    }
    // The initializer that takes the sampling parameters is deprecated (Gemini 3 ignores them); Android sends them.
    return LiveGenerationConfig(
      temperature: config?.float("temperature"),
      topP: config?.float("topP"),
      topK: config?.int("topK"),
      candidateCount: config?.int("candidateCount"),
      maxOutputTokens: config?.int("maxOutputTokens"),
      presencePenalty: config?.float("presencePenalty"),
      frequencyPenalty: config?.float("frequencyPenalty"),
      responseModalities: config?.strings("responseModalities")?.compactMap { responseModality($0) },
      speech: speechConfig(config?.object("speechConfig")),
      inputAudioTranscription: inputAudioTranscription,
      outputAudioTranscription: outputAudioTranscription,
      contextWindowCompression: contextWindowCompression,
      realtimeInputConfig: realtimeInputConfig
    )
  }

  static func realtimeInput(_ json: [String: Any]) -> RealtimeInputConfig {
    let activityHandling: RealtimeInputConfig.ActivityHandling?
    switch json.string("activityHandling") {
    case "START_OF_ACTIVITY_INTERRUPTS": activityHandling = .interrupt
    case "NO_INTERRUPTION": activityHandling = .noInterrupt
    default: activityHandling = nil
    }
    let turnCoverage: RealtimeInputConfig.TurnCoverage?
    switch json.string("turnCoverage") {
    case "TURN_INCLUDES_ONLY_ACTIVITY": turnCoverage = .onlyActivity
    case "TURN_INCLUDES_ALL_INPUT": turnCoverage = .allInput
    case "TURN_INCLUDES_AUDIO_ACTIVITY_AND_ALL_VIDEO": turnCoverage = .audioActivityAndAllVideo
    default: turnCoverage = nil
    }
    return RealtimeInputConfig(
      automaticActivityDetection: json.object("automaticActivityDetection").map { activityDetection($0) },
      activityHandling: activityHandling,
      turnCoverage: turnCoverage
    )
  }

  static func activityDetection(_ json: [String: Any]) -> ActivityDetectionConfig {
    if json.bool("disabled") == true {
      return .disabled()
    }
    func sensitivity(_ key: String) -> ActivityDetectionConfig.Sensitivity? {
      switch json.string(key) {
      case let value? where value.hasSuffix("_LOW"): return .low
      case let value? where value.hasSuffix("_HIGH"): return .high
      default: return nil
      }
    }
    let prefixPadding: TimeInterval? = json.number("prefixPaddingMs").map { $0 / 1000 }
    let silenceDuration: TimeInterval? = json.number("silenceDurationMs").map { $0 / 1000 }
    return ActivityDetectionConfig(
      startSensitivity: sensitivity("startOfSpeechSensitivity"),
      endSensitivity: sensitivity("endOfSpeechSensitivity"),
      prefixPadding: prefixPadding,
      silenceDuration: silenceDuration
    )
  }

  static func sessionResumption(_ json: [String: Any]?) -> SessionResumptionConfig? {
    guard let json else {
      return nil
    }
    if let handle = json.string("handle") {
      return SessionResumptionConfig(handle: handle)
    }
    return SessionResumptionConfig()
  }

  /// The data and MIME type of a Live API realtime input's inline data (`{"mimeType": ..., "data": base64}`).
  static func inlineData(_ json: [String: Any]) throws -> (data: Data, mimeType: String) {
    guard let base64 = json.string("data"), let data = Data(base64Encoded: base64) else {
      throw invalid("The inline data is not base64.")
    }
    return (data, json.string("mimeType") ?? "")
  }

  /// The function responses of a Live API tool response (`[{"name": ..., "response": {...}, "id": ...}]`).
  static func functionResponses(_ array: [Any]) -> [FunctionResponsePart] {
    array.compactMap { $0 as? [String: Any] }.map { json in
      FunctionResponsePart(
        name: json.string("name") ?? "",
        response: jsonObject(json.object("response") ?? [:]),
        functionId: json.string("id")
      )
    }
  }

  /// A dictionary of `JSONSerialization` as the SDK's `JSONObject`.
  static func jsonObject(_ object: [String: Any]) -> JSONObject {
    object.mapValues { jsonValue($0) }
  }

  static func jsonValue(_ value: Any) -> JSONValue {
    switch value {
    case let string as String:
      return .string(string)
    case let number as NSNumber:
      return CFGetTypeID(number) == CFBooleanGetTypeID() ? .bool(number.boolValue) : .number(number.doubleValue)
    case let array as [Any]:
      return .array(array.map { jsonValue($0) })
    case let object as [String: Any]:
      return .object(jsonObject(object))
    default:
      return .null
    }
  }
}

/// Reading the values of the dictionaries of `JSONSerialization`.
extension Dictionary where Key == String, Value == Any {
  func string(_ key: String) -> String? {
    self[key] as? String
  }

  /// A number (not a boolean).
  func number(_ key: String) -> Double? {
    guard let number = self[key] as? NSNumber, CFGetTypeID(number) != CFBooleanGetTypeID() else {
      return nil
    }
    return number.doubleValue
  }

  func float(_ key: String) -> Float? {
    number(key).map { Float($0) }
  }

  /// An integral number (a number out of the range of `Int` is none).
  func int(_ key: String) -> Int? {
    number(key).flatMap { Int(exactly: $0.rounded()) }
  }

  func bool(_ key: String) -> Bool? {
    (self[key] as? NSNumber)?.boolValue
  }

  func object(_ key: String) -> [String: Any]? {
    self[key] as? [String: Any]
  }

  func objects(_ key: String) -> [[String: Any]]? {
    (self[key] as? [Any])?.compactMap { $0 as? [String: Any] }
  }

  func strings(_ key: String) -> [String]? {
    (self[key] as? [Any])?.compactMap { $0 as? String }
  }

  /// Whether the key has a value other than `null`.
  func has(_ key: String) -> Bool {
    guard let value = self[key] else {
      return false
    }
    return !(value is NSNull)
  }
}
