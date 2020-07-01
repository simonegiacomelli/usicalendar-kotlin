package calendar

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nswf.logger
import java.net.URL
import kotlin.system.measureTimeMillis

data class UrlCache(val urlstr: String) {
    val url = URL(urlstr)
    val L by logger()
    var cache: CacheContent? = null

    val urlMutex = Mutex()
    suspend fun cached() = urlMutex.withLock {
        if (cache == null)
            refreshIfStale()
        cache?.content ?: throw Exception("")
    }

    internal fun refreshIfStale() {
        if (cache?.stale != false)
            measureTimeMillis {
                cache = CacheContent(url.readText())
            }.also {
                L.info("msec: ${it.toString().padStart(6)} refresh for ${urlstr}")
            }
    }

}


data class CacheContent(val content: String) {
    val millisec: Long = System.currentTimeMillis()
    val stale get() = (System.currentTimeMillis() - millisec) > (1000 * 60 * 5)
}