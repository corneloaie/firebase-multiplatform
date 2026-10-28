/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.internal

import io.github.corneloaie.firebase.externals.jsObject
import io.github.corneloaie.firebase.externals.jsSet
import io.github.corneloaie.firebase.externals.jsonStringify
import io.github.corneloaie.firebase.externals.toJs
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

internal actual fun FirebaseEncoderImpl.structureEncoder(descriptor: SerialDescriptor): FirebaseCompositeEncoder = when (descriptor.kind) {
    StructureKind.LIST -> encodeAsList(descriptor)

    StructureKind.MAP -> {
        val map = jsObject()
        var lastKey = ""
        value = map
        FirebaseCompositeEncoder(settings) { _, index, value ->
            if (index % 2 == 0) {
                lastKey = (value as? String) ?: jsonStringify(value.toJs())
            } else {
                jsSet(map, lastKey, value.toJs())
            }
        }
    }

    StructureKind.CLASS, StructureKind.OBJECT -> encodeAsMap(descriptor)

    is PolymorphicKind -> encodeAsMap(descriptor)

    else -> TODO("The firebase-kotlin-sdk does not support $descriptor for serialization yet")
}

private fun FirebaseEncoderImpl.encodeAsList(@Suppress("UNUSED_PARAMETER") descriptor: SerialDescriptor): FirebaseCompositeEncoder {
    val array = JsArray<JsAny?>()
    value = array
    return FirebaseCompositeEncoder(settings) { _, index, value -> array[index] = value.toJs() }
}

private fun FirebaseEncoderImpl.encodeAsMap(descriptor: SerialDescriptor): FirebaseCompositeEncoder {
    val map = jsObject()
    value = map
    return FirebaseCompositeEncoder(
        settings,
        setPolymorphicType = { discriminator, type ->
            jsSet(map, discriminator, type.toJs())
        },
        set = { _, index, value -> jsSet(map, descriptor.getElementName(index), value.toJs()) },
    )
}
