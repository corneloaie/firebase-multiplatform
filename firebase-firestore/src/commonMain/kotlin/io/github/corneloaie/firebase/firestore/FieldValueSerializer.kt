package io.github.corneloaie.firebase.firestore

import io.github.corneloaie.firebase.FirebaseEncoder
import io.github.corneloaie.firebase.internal.SpecialValueSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/** A serializer for [FieldValue]. Must be used in conjunction with [FirebaseEncoder]. */
public object FieldValueSerializer : KSerializer<FieldValue> by SpecialValueSerializer(
    serialName = "FieldValue",
    toNativeValue = FieldValue::nativeValue,
    fromNativeValue = { raw ->
        raw?.let(::FieldValue) ?: throw SerializationException("Cannot deserialize $raw")
    },
)
