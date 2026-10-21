/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import com.google.firebase.Firebase
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.PipelineSource
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.memoryEagerGcSettings
import com.google.firebase.firestore.pipeline.AggregateFunction
import com.google.firebase.firestore.pipeline.AggregateFunction.Companion.countAll
import com.google.firebase.firestore.pipeline.AggregateHints
import com.google.firebase.firestore.pipeline.AggregateOptions
import com.google.firebase.firestore.pipeline.AggregateStage
import com.google.firebase.firestore.pipeline.AliasedExpression
import com.google.firebase.firestore.pipeline.BooleanExpression
import com.google.firebase.firestore.pipeline.CollectionHints
import com.google.firebase.firestore.pipeline.CollectionSourceOptions
import com.google.firebase.firestore.pipeline.Expression
import com.google.firebase.firestore.pipeline.Expression.Companion.constant
import com.google.firebase.firestore.pipeline.Expression.Companion.field
import com.google.firebase.firestore.pipeline.Expression.Companion.variable
import com.google.firebase.firestore.pipeline.Field
import com.google.firebase.firestore.pipeline.FindNearestOptions
import com.google.firebase.firestore.pipeline.FindNearestStage
import com.google.firebase.firestore.pipeline.FunctionExpression
import com.google.firebase.firestore.pipeline.Ordering
import com.google.firebase.firestore.pipeline.RawOptions
import com.google.firebase.firestore.pipeline.RawStage
import com.google.firebase.firestore.pipeline.SampleStage
import com.google.firebase.firestore.pipeline.SearchStage
import com.google.firebase.firestore.pipeline.Selectable
import com.google.firebase.firestore.pipeline.UnnestOptions
import com.google.firebase.firestore.pipeline.UnnestStage
import com.google.firebase.firestore.pipeline.evaluation.isMicrosecondsInTimestampBounds
import com.google.firebase.firestore.pipeline.evaluation.isMillisecondsInTimestampBounds
import com.google.firebase.firestore.pipeline.evaluation.isSecondsInTimestampBounds
import com.google.firebase.firestore.pipeline.evaluation.isTimestampInBounds
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.runBlockingTest
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * The pipeline API of the `com.google.firebase.firestore` layer. firebase-java-sdk has no pipelines, so these tests
 * cannot compile for the JVM: they are in nonJvmTest for Apple and JS, and the same file is in androidInstrumentedTest
 * for Android, where PipelineExpressionsTest checks that this module's classes behave as the Android SDK's own.
 */

/** Building expressions, stages and options without a backend: equality, types and argument checks, as on Android. */
class PipelineExpressionsTest {

    @Test
    fun testExpressionEquality() {
        assertEquals(field("rating"), field("rating"))
        assertEquals(field("rating").hashCode(), field("rating").hashCode())
        assertNotEquals(field("rating"), field("title"))
        assertEquals(field("nested.n"), field(FieldPath.of("nested", "n")))
        assertEquals(field("__name__"), field(FieldPath.documentId()))
        assertEquals(field("rating").add(1), Expression.add(field("rating"), 1))
        assertEquals(field("rating").add(1), Expression.add("rating", 1))
        assertEquals(field("rating").add(1).hashCode(), Expression.add("rating", 1).hashCode())
        assertNotEquals(field("rating").add(1), field("rating").subtract(1))
        // Integers are equal whatever their Kotlin type, and differ from doubles, as their encoded values do (an
        // integral double is an integer on Kotlin/JS, where both are JS numbers).
        assertEquals(constant(1), constant(1L))
        assertNotEquals(constant(1), constant(1.5))
        assertEquals(constant("x"), constant("x"))
        assertEquals(field("tags").arrayContains("x"), Expression.arrayContains("tags", "x"))
        assertEquals(field("tags").arrayContainsAny(listOf("x", "y")), Expression.arrayContainsAny("tags", listOf("x", "y")))
        assertEquals(
            Expression.and(field("x").equal(1), field("y").equal(2)),
            Expression.and(field("x").equal(1), field("y").equal(2)),
        )
        assertEquals(field("rating").alias("r"), field("rating").alias("r"))
        assertNotEquals(field("rating").alias("r"), field("rating").alias("s"))
        assertEquals(field("rating").ascending(), Ordering.ascending("rating"))
        assertEquals(field("rating").descending(), Ordering.descending(field("rating")))
        assertNotEquals(field("rating").ascending(), field("rating").descending())
        assertEquals(Ordering.Direction.ASCENDING, field("rating").ascending().dir)
        assertEquals(field("rating"), field("rating").ascending().expr)
    }

    @Test
    fun testExpressionTypes() {
        assertIs<Field>(field("rating"))
        assertIs<Selectable>(field("rating"))
        assertIs<AliasedExpression>(field("rating").alias("r"))
        assertIs<Selectable>(field("rating").alias("r"))
        assertIs<FunctionExpression>(field("rating").add(1))
        assertIs<BooleanExpression>(field("rating").greaterThan(3))
        assertIs<BooleanExpression>(field("rating").greaterThan(3).not())
        assertIs<BooleanExpression>(Expression.or(field("x").equal(1), field("y").isAbsent()))
        assertIs<BooleanExpression>(constant(true))
        assertIs<BooleanExpression>(field("flag").asBoolean())
        assertIs<BooleanExpression>(BooleanExpression.rawFunction("custom", field("x")))
        assertIs<FunctionExpression>(Expression.rawFunction("custom", field("x")))
        assertIs<FunctionExpression>(Expression.array(1, "two", null))
        assertIs<FunctionExpression>(Expression.map(mapOf("a" to 1)))
        assertIs<Expression>(Expression.vector(doubleArrayOf(1.0, 2.0)))
        assertIs<Expression>(constant(FieldValue.vector(doubleArrayOf(1.0))))
        assertIs<Expression>(Expression.nullValue())
        assertIs<Expression>(variable("v"))
        assertIs<Expression>(field("location").geoDistance(com.google.firebase.firestore.GeoPoint(1.0, 2.0)))
        assertIs<AggregateFunction>(field("rating").sum())
        assertIs<AggregateFunction>(field("rating").greaterThan(3).countIf())
    }

    @Test
    fun testFieldPaths() {
        assertFailsWith<IllegalArgumentException> { field("a..b") }
        assertFailsWith<IllegalArgumentException> { field(".a") }
        assertFailsWith<IllegalArgumentException> { field("a/b") }
    }

    @Test
    fun testStagesAndOptions() {
        assertEquals(SampleStage.withDocLimit(5), SampleStage.withDocLimit(5))
        assertNotEquals(SampleStage.withDocLimit(5), SampleStage.withPercentage(5.0))
        assertEquals(UnnestStage.withField("tags", "tag"), UnnestStage.withField(field("tags").alias("tag")))
        assertEquals(UnnestStage.withField("tags", "tag").withIndexField("i"), UnnestStage.withField("tags", "tag").withIndexField("i"))
        assertNotEquals(UnnestStage.withField("tags", "tag"), UnnestStage.withField("tags", "tag").withIndexField("i"))
        assertEquals(SampleStage.withDocLimit(5).withOption("seed", 1L), SampleStage.withDocLimit(5).withOption("seed", 1L))
        assertFailsWith<IllegalArgumentException> { AggregateStage.withAccumulators(countAll().alias("n"), AggregateFunction.sum("rating").alias("n")) }
        assertFailsWith<IllegalArgumentException> { AggregateStage.withAccumulators(countAll().alias("n")).withGroups("genre", "genre") }
        // the other builders need no backend either
        RawStage.ofName("limit").withArguments(1).withOption("key", "value")
        SearchStage.withQuery("text").withLimit(10).withOffset(1).withLanguageCode("en").withRetrievalDepth(100).withSort(field("rating").descending())
        FindNearestOptions().withLimit(3).withDistanceField(field("distance"))
        assertNotNull(FindNearestOptions().withDistanceField("distance"))
        CollectionSourceOptions().withHints(CollectionHints().withForceIndex("index").withIgnoreIndexFields("a", "b"))
        AggregateOptions().withHints(AggregateHints().withForceStreamableEnabled())
        UnnestOptions().withIndexField("index")
        Pipeline.ExecuteOptions().withIndexMode(Pipeline.ExecuteOptions.IndexMode.RECOMMENDED).with("raw", RawOptions.DEFAULT.with("nested", 1L))
        assertNotEquals(FindNearestStage.DistanceMeasure.COSINE, FindNearestStage.DistanceMeasure.EUCLIDEAN)
        assertNotEquals(SampleStage.Mode.DOCUMENTS, SampleStage.Mode.PERCENT)
    }

    @Test
    fun testTimestampBounds() {
        assertTrue(isSecondsInTimestampBounds(0))
        assertTrue(isSecondsInTimestampBounds(-62135596800))
        assertFalse(isSecondsInTimestampBounds(-62135596801))
        assertTrue(isSecondsInTimestampBounds(253402300799))
        assertFalse(isSecondsInTimestampBounds(253402300800))
        assertTrue(isMillisecondsInTimestampBounds(253402300799999))
        assertFalse(isMillisecondsInTimestampBounds(253402300800000))
        assertTrue(isMicrosecondsInTimestampBounds(-62135596800000000))
        assertFalse(isMicrosecondsInTimestampBounds(-62135596800000001))
        assertTrue(isTimestampInBounds(0, 999_999_999))
        assertFalse(isTimestampInBounds(0, 1_000_000_000))
        assertFalse(isTimestampInBounds(0, -1))
    }
}

/**
 * Runs pipelines on the Firestore emulator, which supports them in Enterprise edition: sources, stages, functions and
 * aggregates, results with their references, times and fields, and the checks made before the request is sent.
 */
class PipelineTest {

    companion object {
        private var nextAppId = 0
    }

    private lateinit var app: dev.gitlive.firebase.FirebaseApp
    private lateinit var firestore: FirebaseFirestore

    // A collection per test: collection group and database sources read the whole emulator database.
    private val run = "run${Random.nextLong(Long.MAX_VALUE)}"

    @BeforeTest
    fun initializeFirebase() {
        app = dev.gitlive.firebase.Firebase.initialize(
            context,
            dev.gitlive.firebase.FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
                databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk",
                gcmSenderId = "846484016111",
            ),
            "firestorePipelineTest${nextAppId++}",
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

    private val books get() = firestore.collection("pipelines").document(run).collection("books")

    private suspend fun writeBooks() {
        books.document("a").set(mapOf("title" to "A", "rating" to 4.5, "genre" to "sf", "tags" to listOf("x", "y"), "nested" to mapOf("n" to 1L), "vector" to FieldValue.vector(doubleArrayOf(1.0, 0.0)))).await()
        books.document("b").set(mapOf("title" to "B", "rating" to 3L, "genre" to "sf", "tags" to listOf("y"), "nested" to mapOf("n" to 2L), "vector" to FieldValue.vector(doubleArrayOf(0.0, 1.0)))).await()
        books.document("c").set(mapOf("title" to "C", "rating" to 5L, "genre" to "fantasy", "tags" to emptyList<String>(), "nested" to mapOf("n" to 3L), "vector" to FieldValue.vector(doubleArrayOf(1.0, 1.0)))).await()
        books.document("c").collection("reviews").document("r").set(mapOf("stars" to 5L, "run" to run)).await()
    }

    private fun Pipeline.Snapshot.values(field: String) = results.map { it.get(field) }

    private fun Any?.toDouble() = (this as Number).toDouble()

    @Test
    fun testWhereSortLimit() = runTest {
        writeBooks()
        val snapshot = firestore.pipeline().collection(books).where(field("rating").greaterThan(3)).sort(field("rating").descending()).limit(5).execute().await()
        assertEquals(listOf("c", "a"), snapshot.results.map { it.getId() })
        assertEquals(snapshot.results, snapshot.toList())
        assertNotNull(snapshot.executionTime)
        val a = snapshot.results[1]
        assertEquals(books.document("a").path, a.ref?.path)
        assertEquals("A", a.get("title"))
        assertEquals(4.5, a.get("rating"))
        assertEquals(1L, a.get("nested.n"))
        assertEquals(1L, a.get(FieldPath.of("nested", "n")))
        assertEquals(mapOf("n" to 1L), a.get("nested"))
        assertEquals(listOf("x", "y"), a.getData()["tags"])
        assertEquals(FieldValue.vector(doubleArrayOf(1.0, 0.0)), a.get("vector"))
        assertNull(a.get("absent"))
        assertNull(a.get("title.absent"))
        assertNotNull(a.createTime)
        assertNotNull(a.updateTime)
        // The path of a source collection, and the collection reference, are the same source.
        assertEquals(
            listOf("a", "b", "c"),
            firestore.pipeline().collection(books.path).sort(field("title").ascending()).execute().await().results.map { it.getId() },
        )
    }

    @Test
    fun testFunctions() = runTest {
        writeBooks()
        val snapshot = firestore.pipeline().collection(books)
            .select(
                field("title").toLower().alias("lower"),
                field("tags").arrayLength().alias("tagCount"),
                field("rating").greaterThan(4).conditional(constant("high"), constant("low")).alias("level"),
                field("nested.n").multiply(10).alias("times10"),
                Expression.stringConcat(field("title"), "-", field("genre")).alias("label"),
                field("tags").arrayContains("y").alias("hasY"),
            )
            .sort(field("lower").ascending())
            .execute().await()
        assertEquals(listOf("a", "b", "c"), snapshot.values("lower"))
        assertEquals(listOf(2L, 1L, 0L), snapshot.values("tagCount"))
        assertEquals(listOf("high", "low", "high"), snapshot.values("level"))
        assertEquals(listOf(10L, 20L, 30L), snapshot.values("times10"))
        assertEquals(listOf("A-sf", "B-sf", "C-fantasy"), snapshot.values("label"))
        assertEquals(listOf(true, true, false), snapshot.values("hasY"))
        // A selected result is not a stored document.
        assertNull(snapshot.results.first().ref)
        assertNull(snapshot.results.first().getId())
        // A function by its backend name.
        val raw = firestore.pipeline().collection(books).select(Expression.rawFunction("add", field("nested.n"), constant(1)).alias("plus")).sort(field("plus").ascending()).execute().await()
        assertEquals(listOf(2L, 3L, 4L), raw.values("plus"))
    }

    @Test
    fun testAggregate() = runTest {
        writeBooks()
        val grouped = firestore.pipeline().collection(books)
            .aggregate(AggregateStage.withAccumulators(countAll().alias("count"), field("rating").average().alias("average"), AggregateFunction.maximum("rating").alias("max")).withGroups("genre"))
            .sort(field("genre").ascending())
            .execute().await()
        assertEquals(listOf("fantasy", "sf"), grouped.values("genre"))
        assertEquals(listOf(1L, 2L), grouped.values("count"))
        assertEquals(listOf(5.0, 3.75), grouped.values("average").map { it.toDouble() })
        assertEquals(listOf(5.0, 4.5), grouped.values("max").map { it.toDouble() })
        val total = firestore.pipeline().collection(books).aggregate(countAll().alias("count"), field("rating").sum().alias("sum")).execute().await()
        assertEquals(3L, total.results.single().get("count"))
        assertEquals(12.5, total.results.single().get("sum").toDouble())
        val distinct = firestore.pipeline().collection(books).distinct("genre").sort(field("genre").ascending()).execute().await()
        assertEquals(listOf("fantasy", "sf"), distinct.values("genre"))
    }

    @Test
    fun testQueriesAsPipelines() = runTest {
        writeBooks()
        val fromQuery = firestore.pipeline().createFrom(books.whereGreaterThan("rating", 3).orderBy("rating")).execute().await()
        assertEquals(listOf("a", "c"), fromQuery.results.map { it.getId() })
        val fromAggregate = firestore.pipeline().createFrom(books.aggregate(AggregateField.count(), AggregateField.sum("rating"), AggregateField.average("rating"))).execute().await()
        val result = fromAggregate.results.single()
        assertEquals(3L, result.get("count"))
        assertEquals(12.5, result.get("sum_rating").toDouble())
        assertEquals(12.5 / 3, result.get("average_rating").toDouble(), 1e-9)
    }

    @Test
    fun testSources() = runTest {
        writeBooks()
        val documents = firestore.pipeline().documents(books.document("a"), books.document("c")).sort(field("title").ascending()).execute().await()
        assertEquals(listOf("A", "C"), documents.values("title"))
        val paths = firestore.pipeline().documents(books.document("b").path).execute().await()
        assertEquals(listOf("B"), paths.values("title"))
        val group = firestore.pipeline().collectionGroup("reviews").where(field("run").equal(run)).execute().await()
        assertEquals(listOf(5L), group.values("stars"))
        val database = firestore.pipeline().database().where(field("run").equal(run)).execute().await()
        assertEquals(listOf(5L), database.values("stars"))
        // A subquery over a subcollection of each document: an array of the values of its one field.
        val reviews = firestore.pipeline().collection(books).where(field("title").equal("C"))
            .select(PipelineSource.subcollection("reviews").select("stars").toArrayExpression().alias("reviews"))
            .execute().await()
        assertEquals(listOf(5L), reviews.results.single().get("reviews"))
    }

    @Test
    fun testStages() = runTest {
        writeBooks()
        val pipeline = firestore.pipeline().collection(books)
        val unnested = pipeline.unnest(field("tags").alias("tag"), UnnestOptions().withIndexField("index")).sort(field("title").ascending(), field("index").ascending()).select("title", "tag", "index").execute().await()
        assertEquals(listOf("A", "A", "B"), unnested.values("title"))
        assertEquals(listOf("x", "y", "y"), unnested.values("tag"))
        assertEquals(listOf(0L, 1L, 0L), unnested.values("index"))
        val union = pipeline.where(field("rating").equal(3)).union(pipeline.where(field("rating").equal(5))).sort(field("title").ascending()).execute().await()
        assertEquals(listOf("B", "C"), union.values("title"))
        assertEquals(2, pipeline.sample(2).execute().await().results.size)
        assertEquals(1, pipeline.sample(SampleStage.withDocLimit(1)).execute().await().results.size)
        val removed = pipeline.removeFields("tags", "nested", "vector").sort(field("title").ascending()).limit(1).execute().await().results.single()
        assertEquals(setOf("title", "rating", "genre"), removed.getData().keys)
        val replaced = pipeline.replaceWith("nested").sort(field("n").ascending()).execute().await()
        assertEquals(listOf(1L, 2L, 3L), replaced.values("n"))
        val defined = pipeline.define(field("rating").alias("r")).where(variable("r").greaterThan(4)).sort(field("title").ascending()).execute().await()
        assertEquals(listOf("A", "C"), defined.values("title"))
        val added = pipeline.addFields(field("rating").multiply(2).alias("double")).where(field("title").equal("B")).execute().await()
        assertEquals(6L, added.results.single().get("double"))
        val offset = pipeline.sort(field("title").ascending()).offset(1).limit(1).execute().await()
        assertEquals(listOf("B"), offset.values("title"))
        val raw = pipeline.sort(field("title").ascending()).rawStage(RawStage.ofName("limit").withArguments(1)).execute().await()
        assertEquals(listOf("A"), raw.values("title"))
        val nearest = pipeline.findNearest("vector", doubleArrayOf(1.0, 0.0), FindNearestStage.DistanceMeasure.EUCLIDEAN).limit(1).execute().await()
        assertEquals(listOf("A"), nearest.values("title"))
        val withDistance = pipeline.findNearest("vector", Expression.vector(doubleArrayOf(1.0, 0.0)), FindNearestStage.DistanceMeasure.EUCLIDEAN, FindNearestOptions().withLimit(2).withDistanceField(field("distance"))).execute().await()
        assertEquals(listOf("A", "C"), withDistance.values("title"))
        assertEquals(listOf(0.0, 1.0), withDistance.values("distance").map { it.toDouble() })
    }

    @Test
    fun testOptionsAndChecks() = runTest {
        writeBooks()
        val pipeline = firestore.pipeline().collection(books).sort(field("title").ascending()).limit(1)
        val options = Pipeline.ExecuteOptions().withIndexMode(Pipeline.ExecuteOptions.IndexMode.RECOMMENDED)
        if (pipelineExecuteOptionsSupported) {
            assertEquals(listOf("A"), pipeline.execute(options).await().values("title"))
        } else {
            assertFailsWith<UnsupportedOperationException> { pipeline.execute(options) }
        }
        val hinted = firestore.pipeline().collection(books, CollectionSourceOptions().withHints(CollectionHints().withForceIndex("primary"))).limit(1).execute().await()
        assertEquals(1, hinted.results.size)
        // Checked before the request is sent, as the Android SDK checks them.
        assertFailsWith<IllegalArgumentException> { firestore.pipeline().collection(books).select(field("title").alias("x"), field("genre").alias("x")).execute() }
        assertFailsWith<IllegalArgumentException> { firestore.pipeline().collection(books).addFields(field("title").alias("__name__")) }
        // Firestore 27.1.0 for Android lets removeFields remove the reserved fields (firebase/firebase-android-sdk#8524).
        firestore.pipeline().collection(books).removeFields("__name__")
        assertFailsWith<IllegalStateException> { PipelineSource.subcollection("reviews").execute() }
    }
}
