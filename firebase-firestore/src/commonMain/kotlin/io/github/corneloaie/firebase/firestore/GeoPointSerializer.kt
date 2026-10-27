package io.github.corneloaie.firebase.firestore

import io.github.corneloaie.firebase.FirebaseEncoder
import io.github.corneloaie.firebase.internal.SpecialValueSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/** Serializer for [GeoPoint]. If used with [FirebaseEncoder] performs serialization using native Firebase mechanisms. */
public object GeoPointSerializer : KSerializer<GeoPoint> by SpecialValueSerializer(
    serialName = "GeoPoint",
    toNativeValue = GeoPoint::nativeValue,
    fromNativeValue = { value ->
        when (value) {
            is NativeGeoPoint -> GeoPoint(value)
            else -> throw SerializationException("Cannot deserialize $value")
        }
    },
)
