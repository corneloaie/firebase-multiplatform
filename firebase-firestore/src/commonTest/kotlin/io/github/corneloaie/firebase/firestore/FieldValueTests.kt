package io.github.corneloaie.firebase.firestore

import io.github.corneloaie.firebase.internal.firebaseSerializer
import io.github.corneloaie.firebase.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FieldValueTests {
    @Test
    fun equalityChecks() = runTest {
        assertEquals(FieldValue.delete, FieldValue.delete)
        assertEquals(FieldValue.serverTimestamp, FieldValue.serverTimestamp)
        assertNotEquals(FieldValue.delete, FieldValue.serverTimestamp)
        // Note: arrayUnion and arrayRemove can't be checked due to vararg to array conversion
    }

    @Test
    @IgnoreJs
    fun serializers() = runTest {
        assertEquals(FieldValueSerializer, FieldValue.delete.firebaseSerializer())
    }
}
