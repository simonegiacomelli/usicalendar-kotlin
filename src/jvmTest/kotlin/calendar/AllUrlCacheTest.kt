package calendar

import kotlinx.coroutines.runBlocking
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertTrue


class AllUrlCacheTest {

    @Test
    fun `give list of url return string`() {
        val target = AllUrlCache(CalendarResourceManager.icsTestUrls)
        runBlocking {
            check1(target)
            check1(target)
            //fail("test")
        }


    }

    private suspend fun check1(target: AllUrlCache) {
        val contentList = target.collect()
        assertTrue { contentList.isNotEmpty() }
        assertTrue { contentList.all { it.isNotEmpty() } }
    }
}