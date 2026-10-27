package io.github.corneloaie.firebase.firestore.internal

import io.github.corneloaie.firebase.firestore.FieldPath

internal sealed class FieldAndValue {

    abstract val value: Any?

    data class WithStringField(val field: String, override val value: Any?) : FieldAndValue()
    data class WithFieldPath(val path: FieldPath, override val value: Any?) : FieldAndValue()
}
