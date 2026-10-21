/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified in firebase-multiplatform for Kotlin Multiplatform: the public API of the Firebase AI Logic
 * Android SDK without its Android implementation (see firebase-ai/documentation.md).
 */
package com.google.firebase.ai.type

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** Interface representing data sent to and received from requests. */
public interface Part {
  public val isThought: Boolean
}

/** Represents text or string based data sent to and received from requests. */
public class TextPart
internal constructor(
  public val text: String,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  public constructor(text: String) : this(text, false, null)

  public companion object {
    /**
     * Creates a [TextPart] with advanced thinking metadata.
     *
     * @param text The text content of the part.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      text: String,
      isThought: Boolean,
      thoughtSignature: String?
    ): TextPart = TextPart(text, isThought, thoughtSignature)
  }
}

/**
 * Represents the code execution result from the model.
 * @property outcome The result of the execution.
 * @property output The stdout from the code execution, or an error message if it failed.
 * @property isThought Indicates whether the response is a thought.
 */
public class CodeExecutionResultPart
internal constructor(
  public val outcome: String,
  public val output: String,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  /** Indicates if the code execution was successful */
  public fun executionSucceeded(): Boolean = (outcome.lowercase() == "outcome_ok")

  public companion object {
    /**
     * Creates a [CodeExecutionResultPart] with advanced thinking metadata.
     *
     * @param outcome The result of the execution.
     * @param output The stdout from the code execution, or an error message if it failed.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      outcome: String,
      output: String,
      isThought: Boolean,
      thoughtSignature: String?
    ): CodeExecutionResultPart =
      CodeExecutionResultPart(outcome, output, isThought, thoughtSignature)
  }
}

/**
 * Represents the code that was executed by the model.
 * @property language The programming language of the code.
 * @property code The source code to be executed.
 * @property isThought Indicates whether the response is a thought.
 */
public class ExecutableCodePart
internal constructor(
  public val language: String,
  public val code: String,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  public companion object {
    /**
     * Creates an [ExecutableCodePart] with advanced thinking metadata.
     *
     * @param language The programming language of the code.
     * @param code The source code to be executed.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      language: String,
      code: String,
      isThought: Boolean,
      thoughtSignature: String?
    ): ExecutableCodePart = ExecutableCodePart(language, code, isThought, thoughtSignature)
  }
}

/**
 * Represents image data sent to and received from requests. The image is converted client-side to
 * JPEG encoding at 80% quality before being sent to the server.
 *
 * On Android this is the SDK's part over an `android.graphics.Bitmap`. Its constructors,
 * `createWithThinking` and `image` take the Android-only `Bitmap`, so they are not available in
 * common code (see `api/android-sdk/exclusions.txt`): send images as [InlineDataPart]s there, which
 * is also what responses carry on the other platforms.
 */
public class ImagePart
private constructor(
  public val displayName: String?,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part

/** Represents binary data with an associated MIME type sent to and received from requests. */
public class InlineDataPart
internal constructor(
  public val inlineData: ByteArray,
  public val mimeType: String,
  public val displayName: String?,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  /**
   * @param inlineData the binary data as a [ByteArray]
   * @param mimeType an IANA standard MIME type. For supported values, see the
   * [Firebase documentation](https://firebase.google.com/docs/ai-logic/input-file-requirements).
   */
  public constructor(
    inlineData: ByteArray,
    mimeType: String,
  ) : this(inlineData, mimeType, null, false, null)

  /**
   * @param inlineData the binary data as a [ByteArray]
   * @param mimeType an IANA standard MIME type. For supported values, see the
   * [Firebase documentation](https://firebase.google.com/docs/ai-logic/input-file-requirements).
   * @param displayName the name of the file, including the extension
   */
  public constructor(
    inlineData: ByteArray,
    mimeType: String,
    displayName: String
  ) : this(inlineData, mimeType, displayName, false, null)

  public companion object {
    /**
     * Creates an [InlineDataPart] with advanced thinking metadata.
     *
     * @param inlineData The binary data as a [ByteArray].
     * @param mimeType An IANA standard MIME type.
     * @param displayName The name of the file, including the extension.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      inlineData: ByteArray,
      mimeType: String,
      displayName: String?,
      isThought: Boolean,
      thoughtSignature: String?
    ): InlineDataPart =
      InlineDataPart(inlineData, mimeType, displayName, isThought, thoughtSignature)
  }
}

/**
 * Represents binary data with an associated MIME type.
 * @property data the binary data as a [ByteArray]
 * @property mimeType an IANA standard MIME type.
 * @property displayName the file name
 */
public class InlineData(
  public val data: ByteArray,
  public val mimeType: String,
  public val displayName: String?
) {
  public constructor(data: ByteArray, mimeType: String) : this(data, mimeType, null)
}

/** Represents function call name and params received from requests. */
public class FunctionCallPart
internal constructor(
  public val name: String,
  public val args: Map<String, JsonElement>,
  public val id: String? = null,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  /**
   * @param name the name of the function to call
   * @param args the function parameters and values as a [Map]
   * @param id Unique id of the function call. If present, the returned [FunctionResponsePart]
   * should have a matching `id` field.
   */
  @JvmOverloads
  public constructor(
    name: String,
    args: Map<String, JsonElement>,
    id: String? = null,
  ) : this(name, args, id, false, null)

  public companion object {
    /**
     * Creates a [FunctionCallPart] with advanced thinking metadata.
     *
     * @param name The name of the function to call.
     * @param args The function parameters and values as a [Map].
     * @param id Unique id of the function call.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      name: String,
      args: Map<String, JsonElement>,
      id: String?,
      isThought: Boolean,
      thoughtSignature: String?
    ): FunctionCallPart = FunctionCallPart(name, args, id, isThought, thoughtSignature)
  }
}

/** Represents function call output to be returned to the model when it requests a function call. */
public class FunctionResponsePart
internal constructor(
  public val name: String,
  public val response: JsonObject,
  public val id: String? = null,
  public val parts: List<Part> = emptyList(),
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  /**
   * @param name The name of the called function.
   * @param response The response produced by the function as a [JsonObject].
   * @param id Matching `id` for a [FunctionCallPart], if one was provided.
   */
  @JvmOverloads
  public constructor(
    name: String,
    response: JsonObject,
    id: String? = null,
    parts: List<Part> = emptyList()
  ) : this(name, response, id, parts, false, null)

  internal fun normalizeAgainstCall(call: FunctionCallPart): FunctionResponsePart {
    return FunctionResponsePart(call.name, this.response, call.id, this.parts)
  }

  public companion object {
    public fun from(jsonObject: JsonObject, parts: List<Part> = emptyList()): FunctionResponsePart {
      return FunctionResponsePart("", jsonObject, null, parts)
    }

    /**
     * Creates a [FunctionResponsePart] with advanced thinking metadata.
     *
     * @param name The name of the called function.
     * @param response The response produced by the function as a [JsonObject].
     * @param id Matching `id` for a [FunctionCallPart], if one was provided.
     * @param parts Additional response parts, if any.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      name: String,
      response: JsonObject,
      id: String?,
      parts: List<Part>,
      isThought: Boolean,
      thoughtSignature: String?
    ): FunctionResponsePart =
      FunctionResponsePart(name, response, id, parts, isThought, thoughtSignature)
  }
}

/** Represents file data stored in Cloud Storage for Firebase, referenced by URI. */
public class FileDataPart
internal constructor(
  public val uri: String,
  public val mimeType: String,
  public override val isThought: Boolean,
  public val thoughtSignature: String?
) : Part {
  /**
   * @param uri The `"gs://"`-prefixed URI of the file in Cloud Storage for Firebase, for example,
   * `"gs://bucket-name/path/image.jpg"`
   * @param mimeType an IANA standard MIME type. For supported MIME type values see the
   * [Firebase documentation](https://firebase.google.com/docs/ai-logic/input-file-requirements).
   */
  public constructor(uri: String, mimeType: String) : this(uri, mimeType, false, null)

  public companion object {
    /**
     * Creates a [FileDataPart] with advanced thinking metadata.
     *
     * @param uri The `"gs://"`-prefixed URI of the file in Cloud Storage for Firebase.
     * @param mimeType An IANA standard MIME type.
     * @param isThought Indicates whether the response is a thought.
     * @param thoughtSignature The signature associated with the thought, if any.
     */
    @JvmStatic
    @PublicPreviewAPI
    public fun createWithThinking(
      uri: String,
      mimeType: String,
      isThought: Boolean,
      thoughtSignature: String?
    ): FileDataPart = FileDataPart(uri, mimeType, isThought, thoughtSignature)
  }
}

/** Returns the part as a [String] if it represents text, and null otherwise */
public fun Part.asTextOrNull(): String? = (this as? TextPart)?.text

/** Returns the part as a [InlineDataPart] if it represents inline data, and null otherwise */
public fun Part.asInlineDataPartOrNull(): InlineDataPart? = this as? InlineDataPart

/** Returns the part as a [FileDataPart] if it represents a file, and null otherwise */
public fun Part.asFileDataOrNull(): FileDataPart? = this as? FileDataPart
