package dev.gitlive.firebase.perf.metrics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.perf.FirebasePerformance
import dev.gitlive.firebase.perf.context
import dev.gitlive.firebase.perf.performance
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidTraceTest {

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
    fun testGetAttributes() = runTest {
        val trace = performance.newTrace("testGetAttributes")
        trace.start()
        val values = listOf(1, 2, 3)

        values.forEach {
            trace.putAttribute("Test_Get_Attributes_$it", "Test Get Attributes Value $it")
        }

        val attributes = trace.getAttributes()

        assertEquals(3, attributes.size)

        attributes.onEachIndexed { _, entry ->

            assertEquals(entry.key.last(), entry.value.last())
        }

        trace.stop()
    }

    @Test
    fun testGetAttribute() = runTest {
        val trace = performance.newTrace("testGetAttribute")
        trace.start()
        trace.putAttribute("Test_Get_Attribute", "Test Get Attribute Value")

        assertEquals("Test Get Attribute Value", trace.getAttribute("Test_Get_Attribute"))
        trace.stop()
    }

    @Test
    fun testPutAttribute() = runTest {
        val trace = performance.newTrace("testPutAttribute")
        trace.start()
        trace.putAttribute("Test_Put_Attribute", "Test Put Attribute Value")

        assertEquals("Test Put Attribute Value", trace.getAttribute("Test_Put_Attribute"))
        trace.stop()
    }
}
