# Module firebase-ai
This module provides the Firebase AI Logic API, `com.google.firebase.ai`, on Android, iOS, macOS, tvOS, JS and Wasm: the
Android SDK on Android, the Firebase JS SDK's `firebase/ai` on JS and Wasm (wasmJs) and the Firebase AI Logic iOS SDK
(`FirebaseAILogic`, through an Objective-C wrapper) on Apple. The Gemini models (`generateContent`, streaming, chats,
structured output with `generateObject`, automatic function calling), server prompt templates and the Live API are
available from common code with the Android SDK's classes and signatures.

The Android SDK's API is already built on coroutines, `Flow` and kotlinx.serialization, so it is used as is from common
code. On the platforms other than Android requests and responses cross to the platform SDK as the JSON of the AI Logic
REST API, and they are checked, and their errors mapped to `FirebaseAIException`s, the way the Android SDK does.

Not available in common code, as their signatures need Android/JVM-only types: images as `android.graphics.Bitmap`
(`ImagePart`'s constructors, `Content.Builder.image`, the `Bitmap` overloads of `generateContent`, `countTokens` and
`sendMessage`; send images as `InlineDataPart`s), `Citation.publicationDate` (`java.util.Calendar`), the
`AudioRecord`/`AudioTrack` initialization handler of `LiveAudioConversationConfig`, and the Java futures of
`com.google.firebase.ai.java`. The platforms other than Android have no on-device model, as Android without the
`firebase-ai-ondevice` library: `GenerativeModel.onDeviceExtension` is null there, the hybrid inference modes use the
cloud model and `ONLY_ON_DEVICE` fails with `FirebaseAIOnDeviceNotAvailableException`. `LiveSession.startAudioConversation`
records and plays audio on Android only; elsewhere it does nothing (stream audio with `sendAudioRealtime` instead). The
JS SDK has no realtime input configuration for the Live API, so `LiveGenerationConfig.realtimeInputConfig` is not
applied on JS and Wasm.

On Apple platforms the iOS SDK has no JSON Schema in its configuration, so the JSON schemas of `generateObject` and of
automatic functions are sent as the equivalent OpenAPI schemas; it also has no `displayName` for inline data, no `parts`
in function responses and no total request timeout (`RequestOptions.timeout` limits the time without data), and its
Live API sends realtime audio as `audio/pcm`.
