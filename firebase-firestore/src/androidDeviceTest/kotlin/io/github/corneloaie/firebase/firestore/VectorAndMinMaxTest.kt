/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.firestore

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.memoryEagerGcSettings
import io.github.corneloaie.firebase.initialize
import io.github.corneloaie.firebase.runBlockingTest
import io.github.corneloaie.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/*
 * Vector values and the minimum and maximum transforms of the `com.google.firebase.firestore` layer. firebase-java-sdk
 * has none of them, so these tests cannot compile for the JVM: they are in nonJvmTest for Apple and JS, and the same
 * file is in androidDeviceTest for Android, where VectorAndMinMaxValuesTest checks that this module's classes
 * behave as the Android SDK's own.
 */

/** Vector values and the minimum and maximum field values without a backend: copies and equality, as on Android. */
class VectorAndMinMaxValuesTest {

    @Test
    fun testVectorValue() {
        val values = doubleArrayOf(1.0, -2.5, 3.99)
        val vector = FieldValue.vector(values)
        assertContentEquals(doubleArrayOf(1.0, -2.5, 3.99), vector.toArray())
        // The vector keeps a copy of the values and hands out copies.
        values[0] = 9.0
        vector.toArray()[1] = 9.0
        assertContentEquals(doubleArrayOf(1.0, -2.5, 3.99), vector.toArray())

        assertEquals(FieldValue.vector(doubleArrayOf(1.0, -2.5, 3.99)), vector)
        assertEquals(FieldValue.vector(doubleArrayOf(1.0, -2.5, 3.99)).hashCode(), vector.hashCode())
        assertNotEquals(FieldValue.vector(doubleArrayOf(1.0, -2.5)), vector)
        assertNotEquals(FieldValue.vector(doubleArrayOf(1.0, -2.5, 4.0)), vector)
        assertNotEquals<Any>(doubleArrayOf(1.0, -2.5, 3.99), vector)
        assertContentEquals(doubleArrayOf(), FieldValue.vector(doubleArrayOf()).toArray())
        assertEquals(FieldValue.vector(doubleArrayOf()), FieldValue.vector(doubleArrayOf()))
        // The values are compared as Arrays.equals compares them on Android: NaN equals itself, 0.0 and -0.0 differ.
        assertEquals(FieldValue.vector(doubleArrayOf(Double.NaN)), FieldValue.vector(doubleArrayOf(Double.NaN)))
        assertNotEquals(FieldValue.vector(doubleArrayOf(0.0)), FieldValue.vector(doubleArrayOf(-0.0)))
    }

    @Test
    fun testMinimumAndMaximum() {
        assertEquals(FieldValue.minimum(5L), FieldValue.minimum(5L))
        assertEquals(FieldValue.minimum(5L).hashCode(), FieldValue.minimum(5L).hashCode())
        assertEquals(FieldValue.maximum(5.5), FieldValue.maximum(5.5))
        assertEquals(FieldValue.maximum(5.5).hashCode(), FieldValue.maximum(5.5).hashCode())
        assertNotEquals(FieldValue.minimum(5L), FieldValue.minimum(6L))
        assertNotEquals(FieldValue.minimum(5L), FieldValue.maximum(5L))
        assertNotEquals(FieldValue.minimum(5L), FieldValue.increment(5L))
        // The operand keeps its type, as on Android, where an integer and a double operand are not equal.
        assertNotEquals(FieldValue.maximum(5L), FieldValue.maximum(5.0))
    }
}

/**
 * Round-trips vector values through the Firestore emulator: written with set and update, in a transaction and in a
 * batch, at the top level and in maps, read back with getVectorValue, `get` and `data`, and used in query filters,
 * ordering and cursors. Applies the minimum and maximum transforms.
 */
class VectorAndMinMaxTest {

    companion object {
        private var nextAppId = 0
    }

    private lateinit var app: io.github.corneloaie.firebase.FirebaseApp
    private lateinit var firestore: FirebaseFirestore

    private val embedding = FieldValue.vector(doubleArrayOf(1.0, 2.0, 3.99))

    @BeforeTest
    fun initializeFirebase() {
        app = io.github.corneloaie.firebase.Firebase.initialize(
            context,
            io.github.corneloaie.firebase.FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
                databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk",
                gcmSenderId = "846484016111",
            ),
            "firestoreVectorTest${nextAppId++}",
        )
        firestore = Firebase.firestore(app.compat).apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings { setGcSettings(memoryEagerGcSettings { }) })
            }
            useEmulator(emulatorHost, 8080)
        }
    }

    // Each test gets its own app, so a fresh client: the JS SDK only accepts settings and the emulator before first use.
    @AfterTest
    fun deinitializeFirebase() = runBlockingTest {
        app.delete()
    }

    private fun collection(name: String) = firestore.collection("compat").document("vectors").collection(name)

    @Test
    fun testVectorReadsAndWrites() = runTest {
        val document = collection("documents").document("doc")
        val single = FieldValue.vector(doubleArrayOf(0.0))
        document.set(mapOf("embedding" to embedding, "single" to single, "nested" to mapOf("embedding" to embedding), "name" to "doc")).await()

        val snapshot = document.get(Source.SERVER).await()
        assertEquals(embedding, snapshot.getVectorValue("embedding"))
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.99), snapshot.getVectorValue("embedding")?.toArray())
        assertEquals(single, snapshot.getVectorValue("single"))
        assertEquals(embedding, snapshot.getVectorValue("nested.embedding"))
        assertEquals(embedding, snapshot.get("embedding"))
        assertEquals(embedding, snapshot.get(FieldPath.of("nested", "embedding")))
        assertEquals(mapOf("embedding" to embedding), snapshot.get("nested"))
        assertEquals(embedding, snapshot.data?.get("embedding"))
        assertNull(snapshot.getVectorValue("absent"))
        assertFailsWith<ClassCastException> { snapshot.getVectorValue("name") }

        val negative = FieldValue.vector(doubleArrayOf(-1.0, -200.0, -999.0))
        document.update("embedding", negative, "nested.embedding", single).await()
        document.update(mapOf("single" to FieldValue.vector(doubleArrayOf(0.5)))).await()
        document.update(FieldPath.of("added"), embedding).await()
        val updated = document.get(Source.SERVER).await()
        assertEquals(negative, updated.getVectorValue("embedding"))
        assertEquals(single, updated.getVectorValue("nested.embedding"))
        assertEquals(FieldValue.vector(doubleArrayOf(0.5)), updated.getVectorValue("single"))
        assertEquals(embedding, updated.getVectorValue("added"))
        document.delete().await()
    }

    @Test
    fun testVectorTransactionsAndBatches() = runTest {
        val documents = collection("transactions")
        val first = documents.document("first")
        val second = documents.document("second")
        val other = FieldValue.vector(doubleArrayOf(0.1, 0.3, 0.4))

        // Writes only: the JS SDK reads inside a transaction asynchronously, which the layer's Transaction.get cannot.
        val result = firestore.runTransaction { transaction ->
            transaction.set(first, mapOf("embedding" to embedding))
            transaction.update(first, "other", other)
            transaction.set(second, mapOf("embedding" to other), SetOptions.merge())
            transaction.update(second, FieldPath.of("nested", "embedding"), embedding)
            "done"
        }.await()
        assertEquals("done", result)
        val firstSnapshot = first.get(Source.SERVER).await()
        assertEquals(embedding, firstSnapshot.getVectorValue("embedding"))
        assertEquals(other, firstSnapshot.getVectorValue("other"))
        val secondSnapshot = second.get(Source.SERVER).await()
        assertEquals(other, secondSnapshot.getVectorValue("embedding"))
        assertEquals(embedding, secondSnapshot.getVectorValue("nested.embedding"))

        firestore.batch()
            .set(first, mapOf("batched" to other), SetOptions.merge())
            .update(second, "embedding", embedding)
            .update(second, mapOf("other" to other))
            .commit()
            .await()
        assertEquals(other, first.get(Source.SERVER).await().getVectorValue("batched"))
        val batched = second.get(Source.SERVER).await()
        assertEquals(embedding, batched.getVectorValue("embedding"))
        assertEquals(other, batched.getVectorValue("other"))
        first.delete().await()
        second.delete().await()
    }

    @Test
    fun testVectorQueries() = runTest {
        val documents = collection("queries")
        // In the order of the backend (the iOS SDK's VectorIntegrationTests): arrays, then vectors (by dimension, then by
        // value), then maps.
        documents.document("a").set(mapOf("embedding" to listOf(100L))).await()
        documents.document("b").set(mapOf("embedding" to FieldValue.vector(doubleArrayOf(-100.0)))).await()
        documents.document("c").set(mapOf("embedding" to FieldValue.vector(doubleArrayOf(100.0)))).await()
        documents.document("d").set(mapOf("embedding" to FieldValue.vector(doubleArrayOf(1.0, 2.0)))).await()
        documents.document("e").set(mapOf("embedding" to FieldValue.vector(doubleArrayOf(1.0, 2.0, 3.0)))).await()
        documents.document("f").set(mapOf("embedding" to mapOf("hello" to "world"))).await()
        val pair = FieldValue.vector(doubleArrayOf(1.0, 2.0))

        assertEquals(listOf("a", "b", "c", "d", "e", "f"), documents.orderBy("embedding").get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("d"), documents.whereEqualTo("embedding", pair).get(Source.SERVER).await().map { it.id })
        // Range filters only match values of the same type.
        assertEquals(listOf("b", "c"), documents.whereLessThan("embedding", pair).orderBy("embedding").get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("d", "e"), documents.where(Filter.greaterThanOrEqualTo(FieldPath.of("embedding"), pair)).orderBy("embedding").get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("c", "d", "e", "f"), documents.orderBy("embedding").startAfter(FieldValue.vector(doubleArrayOf(-100.0))).get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("a", "b", "c"), documents.orderBy("embedding").endBefore(pair).get(Source.SERVER).await().map { it.id })
        documents.get(Source.SERVER).await().forEach { it.reference.delete().await() }
    }

    // Non-integral double operands: the JS SDK writes a JS number that is an integer, such as 3.0, as an integer.
    @Test
    fun testMinimumAndMaximum() = runTest {
        val document = collection("transforms").document("doc")
        document.set(mapOf("low" to 10L, "high" to 10L, "lowRatio" to 10.5, "highRatio" to 10.5, "text" to "a")).await()
        document.update(
            mapOf(
                "low" to FieldValue.minimum(5L),
                "high" to FieldValue.maximum(20L),
                "lowRatio" to FieldValue.minimum(5.5),
                "highRatio" to FieldValue.maximum(20.5),
                // A field that is not a number, or is absent, is set to the operand.
                "text" to FieldValue.maximum(1L),
                "absent" to FieldValue.minimum(7.5),
            ),
        ).await()
        val snapshot = document.get(Source.SERVER).await()
        assertEquals(5L, snapshot.getLong("low"))
        assertEquals(20L, snapshot.getLong("high"))
        assertEquals(5.5, snapshot.getDouble("lowRatio"))
        assertEquals(20.5, snapshot.getDouble("highRatio"))
        assertEquals(1L, snapshot.getLong("text"))
        assertEquals(7.5, snapshot.getDouble("absent"))

        // Neither changes the field: 8 is not smaller than 5, 15 not larger than 20.
        document.update("low", FieldValue.minimum(8L), "high", FieldValue.maximum(15L)).await()
        document.update(FieldPath.of("lowRatio"), FieldValue.maximum(6.5)).await()
        document.set(mapOf("created" to FieldValue.maximum(3L)), SetOptions.merge()).await()
        val updated = document.get(Source.SERVER).await()
        assertEquals(5L, updated.getLong("low"))
        assertEquals(20L, updated.getLong("high"))
        assertEquals(6.5, updated.getDouble("lowRatio"))
        assertEquals(3L, updated.getLong("created"))
        document.delete().await()
    }
}
