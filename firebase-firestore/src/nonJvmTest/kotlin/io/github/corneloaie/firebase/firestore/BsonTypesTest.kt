/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.firestore

import com.google.firebase.Firebase
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.BsonObjectId
import com.google.firebase.firestore.BsonTimestamp
import com.google.firebase.firestore.Decimal128Value
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Int32Value
import com.google.firebase.firestore.MaxKey
import com.google.firebase.firestore.MinKey
import com.google.firebase.firestore.RegexValue
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
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/*
 * The BSON types of the `com.google.firebase.firestore` layer. firebase-java-sdk has no BSON types, so these tests cannot
 * compile for the JVM: they are in nonJvmTest for Apple and JS, and the same file is in androidInstrumentedTest for
 * Android, where BsonValuesTest checks that this module's common classes behave as the Android SDK's own.
 */

/** The BSON value classes without a backend: construction, validation, equality and ordering, as on Android. */
class BsonValuesTest {

    @Test
    fun testValues() {
        val objectId = BsonObjectId("507f191e810c19729de860ea")
        assertEquals("507f191e810c19729de860ea", objectId.value)
        assertEquals(objectId, BsonObjectId("507f191e810c19729de860ea"))
        assertNotEquals(objectId, BsonObjectId("507f191e810c19729de860eb"))

        val timestamp = BsonTimestamp(4_294_967_295L, 0L)
        assertEquals(4_294_967_295L, timestamp.seconds)
        assertEquals(0L, timestamp.increment)
        assertEquals(timestamp, BsonTimestamp(4_294_967_295L, 0L))
        assertNotEquals(timestamp, BsonTimestamp(4_294_967_295L, 1L))
        assertFailsWith<IllegalArgumentException> { BsonTimestamp(-1L, 0L) }
        assertFailsWith<IllegalArgumentException> { BsonTimestamp(0L, 4_294_967_296L) }

        assertEquals("1.50", Decimal128Value("1.50").stringValue)
        assertEquals(Decimal128Value("1.5"), Decimal128Value("1.50"))
        assertEquals(Decimal128Value("1.5"), Decimal128Value("15e-1"))
        assertEquals(Decimal128Value("1.5").hashCode(), Decimal128Value("15e-1").hashCode())
        assertEquals(Decimal128Value("0"), Decimal128Value("-0.0"))
        assertEquals(Decimal128Value("NaN"), Decimal128Value("NaN"))
        assertEquals(Decimal128Value("Infinity"), Decimal128Value("+Infinity"))
        assertNotEquals(Decimal128Value("Infinity"), Decimal128Value("-Infinity"))
        assertNotEquals(Decimal128Value("1.5"), Decimal128Value("-1.5"))
        assertFailsWith<NumberFormatException> { Decimal128Value("one") }
        assertFailsWith<NumberFormatException> { Decimal128Value("1e") }

        assertEquals(42, Int32Value(42).value)
        assertEquals(Int32Value(42), Int32Value(42))
        assertNotEquals(Int32Value(42), Int32Value(43))

        assertSame(MaxKey.instance(), MaxKey.instance())
        assertSame(MinKey.instance(), MinKey.instance())
        assertNotEquals<Any>(MaxKey.instance(), MinKey.instance())

        val regex = RegexValue("^a.*z$", "i")
        assertEquals("^a.*z$", regex.pattern)
        assertEquals("i", regex.options)
        assertEquals(regex, RegexValue("^a.*z$", "i"))
        assertNotEquals(regex, RegexValue("^a.*z$", "m"))

        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(0, Blob.fromBytes(bytes).subType)
        assertEquals(Blob.fromBytes(bytes), Blob.createBsonBinary(bytes))
        val binary = Blob.createBsonBinary(128, bytes)
        assertEquals(128, binary.subType)
        assertContentEquals(bytes, binary.toBytes())
        assertNotEquals(Blob.fromBytes(bytes), binary)
        assertTrue(Blob.createBsonBinary(1, byteArrayOf(0)) > Blob.fromBytes(byteArrayOf(9)))
        assertFailsWith<IllegalArgumentException> { Blob.createBsonBinary(256, bytes) }
        assertFailsWith<IllegalArgumentException> { Blob.createBsonBinary(-1, bytes) }
    }
}

/**
 * Round-trips every BSON type through the Firestore emulator: written with set and update, in a transaction and in a
 * batch, at the top level, in lists and in maps, read back with the typed getters, `get` and `data`, and used in query
 * filters, ordering and cursors.
 */
@Ignore // BSON types need a Firestore Enterprise edition database; the emulator (1.21.0) rejects them even when started as one.
class BsonTypesTest {

    companion object {
        private var nextAppId = 0
    }

    private lateinit var app: io.github.corneloaie.firebase.FirebaseApp
    private lateinit var firestore: FirebaseFirestore

    private val objectId = BsonObjectId("507f191e810c19729de860ea")
    private val timestamp = BsonTimestamp(1_700_000_000L, 7L)
    private val decimal = Decimal128Value("1.5e-3")
    private val int32 = Int32Value(42)
    private val regex = RegexValue("^a.*z$", "i")
    private val binary = Blob.createBsonBinary(128, byteArrayOf(1, 2, 3))

    /** A value of every BSON type by field name. */
    private fun bsonFields(): Map<String, Any> = mapOf(
        "objectId" to objectId,
        "timestamp" to timestamp,
        "decimal" to decimal,
        "int32" to int32,
        "maxKey" to MaxKey.instance(),
        "minKey" to MinKey.instance(),
        "regex" to regex,
        "binary" to binary,
    )

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
            "firestoreBsonTest${nextAppId++}",
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

    private fun collection(name: String) = firestore.collection("compat").document("bson").collection(name)

    @Test
    fun testDocumentReadsAndWrites() = runTest {
        val document = collection("documents").document("doc")
        val fields = bsonFields()
        document.set(fields + mapOf("list" to fields.values.toList(), "nested" to fields, "generic" to Blob.createBsonBinary(byteArrayOf(4, 5)))).await()

        val snapshot = document.get(Source.SERVER).await()
        assertEquals(objectId, snapshot.getBsonObjectId("objectId"))
        assertEquals(timestamp, snapshot.getBsonTimestamp("timestamp"))
        assertEquals(decimal, snapshot.getDecimal128Value("decimal"))
        assertEquals(int32, snapshot.getInt32Value("int32"))
        assertSame(MaxKey.instance(), snapshot.getMaxKey("maxKey"))
        assertSame(MinKey.instance(), snapshot.getMinKey("minKey"))
        assertEquals(regex, snapshot.getRegexValue("regex"))
        assertEquals(binary, snapshot.getBlob("binary"))
        assertEquals(128, snapshot.getBlob("binary")?.subType)
        assertEquals(Blob.fromBytes(byteArrayOf(4, 5)), snapshot.getBlob("generic"))
        assertEquals(fields.values.toList(), snapshot.get("list"))
        assertEquals(fields, snapshot.get("nested"))
        assertEquals(int32, snapshot.get(FieldPath.of("nested", "int32")))
        assertEquals(regex, snapshot.data?.get("regex"))
        assertNull(snapshot.getBsonObjectId("absent"))
        assertFailsWith<RuntimeException> { snapshot.getInt32Value("objectId") }
        assertFailsWith<RuntimeException> { snapshot.getMaxKey("minKey") }

        document.update("int32", Int32Value(43), "nested.regex", RegexValue("b", "")).await()
        document.update(mapOf("decimal" to Decimal128Value("-2"))).await()
        document.update(FieldPath.of("timestamp"), BsonTimestamp(1L, 2L)).await()
        val updated = document.get(Source.SERVER).await()
        assertEquals(Int32Value(43), updated.getInt32Value("int32"))
        assertEquals(RegexValue("b", ""), updated.get("nested.regex"))
        assertEquals(Decimal128Value("-2.0"), updated.getDecimal128Value("decimal"))
        assertEquals(BsonTimestamp(1L, 2L), updated.getBsonTimestamp("timestamp"))
        document.delete().await()
    }

    @Test
    fun testTransactionsAndBatches() = runTest {
        val documents = collection("transactions")
        val first = documents.document("first")
        val second = documents.document("second")

        // Writes only: the JS SDK reads inside a transaction asynchronously, which the layer's Transaction.get cannot.
        val result = firestore.runTransaction { transaction ->
            transaction.set(first, mapOf("int32" to int32, "objectId" to objectId))
            transaction.update(first, "int32", Int32Value(43))
            transaction.set(second, mapOf("regex" to regex))
            transaction.set(second, mapOf("timestamp" to timestamp), SetOptions.merge())
            transaction.update(second, mapOf("decimal" to decimal))
            transaction.update(second, FieldPath.of("minKey"), MinKey.instance())
            "done"
        }.await()
        assertEquals("done", result)
        val firstSnapshot = first.get(Source.SERVER).await()
        assertEquals(Int32Value(43), firstSnapshot.getInt32Value("int32"))
        assertEquals(objectId, firstSnapshot.getBsonObjectId("objectId"))
        val secondSnapshot = second.get(Source.SERVER).await()
        assertEquals(regex, secondSnapshot.getRegexValue("regex"))
        assertEquals(timestamp, secondSnapshot.getBsonTimestamp("timestamp"))
        assertEquals(decimal, secondSnapshot.getDecimal128Value("decimal"))
        assertSame(MinKey.instance(), secondSnapshot.getMinKey("minKey"))

        firestore.batch()
            .set(first, mapOf("binary" to binary), SetOptions.merge())
            .update(second, "maxKey", MaxKey.instance())
            .update(second, FieldPath.of("int32"), int32)
            .commit()
            .await()
        assertEquals(binary, first.get(Source.SERVER).await().getBlob("binary"))
        val batched = second.get(Source.SERVER).await()
        assertSame(MaxKey.instance(), batched.getMaxKey("maxKey"))
        assertEquals(int32, batched.getInt32Value("int32"))
        first.delete().await()
        second.delete().await()
    }

    @Test
    fun testQueries() = runTest {
        val documents = collection("queries")
        documents.document("a").set(mapOf("value" to MinKey.instance(), "tag" to int32)).await()
        documents.document("b").set(mapOf("value" to Int32Value(1), "tag" to regex)).await()
        documents.document("c").set(mapOf("value" to MaxKey.instance(), "tag" to objectId)).await()

        assertEquals(listOf("a", "b", "c"), documents.orderBy("value").get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("b"), documents.whereEqualTo("tag", regex).get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("a", "c"), documents.whereIn("tag", listOf(int32, objectId)).get(Source.SERVER).await().map { it.id }.sorted())
        assertEquals(listOf("c"), documents.where(Filter.equalTo(FieldPath.of("value"), MaxKey.instance())).get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("b", "c"), documents.orderBy("value").startAfter(MinKey.instance()).get(Source.SERVER).await().map { it.id })
        assertEquals(listOf("a", "b"), documents.orderBy("value").endBefore(MaxKey.instance()).get(Source.SERVER).await().map { it.id })
        documents.get(Source.SERVER).await().forEach { it.reference.delete().await() }
    }
}
