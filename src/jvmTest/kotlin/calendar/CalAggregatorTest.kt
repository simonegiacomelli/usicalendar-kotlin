package calendar

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalAggregatorTest {

    @Test
    fun testAggregateAndFilter() {
        runBlocking {
            val target = CalAggregator(AllUrlCache(CalendarResourceManager.icsTestUrls))
            val result = target.aggregateAndFilter("robotics,information sec")
            print(result)
            assertTrue { result.contains("Robotics") }
            assertTrue { result.contains("Information Security") }
            assertFalse { result.contains("quantum") }
        }
    }

    @Test
    fun test_aggregate_distinct_courses() {
        runBlocking {
            val target = CalAggregator(AllUrlCache(CalendarResourceManager.icsTestUrls))
            val result = target.groupCoursesBySummary("robotics,quantum".splitToSequence(",").toSet())
            assertEquals(2, result.size)
            assertEquals(1, result.filter { it.summary.contains("robotics", ignoreCase = true) }.size)
            assertEquals(1, result.filter { it.summary.contains("quantum", ignoreCase = true) }.size)

            result.forEach {
                println(it.dateStart)
            }
        }
    }
}