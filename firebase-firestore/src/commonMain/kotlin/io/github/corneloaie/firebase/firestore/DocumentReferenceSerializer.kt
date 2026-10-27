package io.github.corneloaie.firebase.firestore

import io.github.corneloaie.firebase.FirebaseEncoder
import io.github.corneloaie.firebase.internal.SpecialValueSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/**
 * A serializer for [DocumentReference]. If used with [FirebaseEncoder] performs serialization using native Firebase mechanisms.
 */
public object DocumentReferenceSerializer : KSerializer<DocumentReference> by SpecialValueSerializer(
    serialName = "DocumentReference",
    toNativeValue = { it.nativeValue },
    fromNativeValue = { value ->
        when (value) {
            is NativeDocumentReferenceType -> DocumentReference(value)
            else -> throw SerializationException("Cannot deserialize $value")
        }
    },
)
