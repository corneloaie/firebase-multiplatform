package io.github.corneloaie.firebase.perf.metrics

import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseOptions
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize
import io.github.corneloaie.firebase.perf.FirebasePerformance
import io.github.corneloaie.firebase.perf.IgnoreForAndroidUnitTest
import io.github.corneloaie.firebase.perf.context
import io.github.corneloaie.firebase.perf.performance
import io.github.corneloaie.firebase.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@IgnoreForAndroidUnitTest
class TraceTest {

    private lateinit var performance: FirebasePerformance

    @BeforeTest
    fun initializeFirebase() {
        val app = Firebase.apps(context).firstOrNull() ?: Firebase.initialize(
            context,
            FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
                databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk",
                gcmSenderId = "846484016111",
            ),
        )

        performance = Firebase.performance(app)
    }

    // The default app is kept for the whole test process, as in FirebasePerformanceTest: with BoM 35, re-creating it
    // makes Firebase Sessions open a second DataStore on the same file, which crashes the process.

    @Test
    fun testGetLongMetric() = runTest {
        val trace = performance.newTrace("testGetLongMetric")
        trace.start()
        trace.putMetric("Get Long Metric Test", 1L)

        assertEquals(1L, trace.getLongMetric("Get Long Metric Test"))
        trace.stop()
    }

    @Test
    fun testIncrementMetric() = runTest {
        val trace = performance.newTrace("testIncrementMetric")
        trace.start()
        trace.putMetric("Get Increment Metric Test", 1L)

        trace.incrementMetric("Get Increment Metric Test", 1L)

        assertEquals(2L, trace.getLongMetric("Get Increment Metric Test"))
        trace.stop()
    }

    @Test
    fun testPutMetric() = runTest {
        val trace = performance.newTrace("testPutMetric")
        trace.start()
        trace.putMetric("Get Put Metric Test", 1L)

        assertEquals(1L, trace.getLongMetric("Get Put Metric Test"))
        trace.stop()
    }

    @Test
    fun testAttributes() = runTest {
        val trace = performance.newTrace("testAttributes")
        trace.start()

        trace.putAttribute("first_attribute", "first_value")
        trace.putAttribute("second_attribute", "second_value")

        assertEquals("first_value", trace.getAttribute("first_attribute"))
        assertEquals(
            mapOf(
                "first_attribute" to "first_value",
                "second_attribute" to "second_value",
            ),
            trace.getAttributes(),
        )

        trace.removeAttribute("first_attribute")

        assertEquals(null, trace.getAttribute("first_attribute"))
        assertEquals(mapOf("second_attribute" to "second_value"), trace.getAttributes())
        trace.stop()
    }
}
