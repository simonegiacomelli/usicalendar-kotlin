package calendar

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nswf.logger
import org.jetbrains.exposed.sql.transactions.transaction
import sample.Webcal
import java.net.URL

class AllUrlCache() {
    val L by logger()

    private val urls = mutableListOf<URL>()
    constructor(u:List<URL>) : this() {
        urls.addAll(u)
    }
    private val urlMapMutex = Mutex()
    private val urlMap = mutableMapOf<String, UrlCache>()

    private suspend fun get(strUrl: String) = urlMapMutex.withLock {
        urlMap.getOrPut(strUrl) { UrlCache(strUrl) }
    }

    suspend fun collect(): List<String> = urls.map { get(it.toExternalForm()).cached() }

    val startCacheRefresher = lazy {
        L.info("startCacheRefresher called")
        transaction {
            urls.addAll(Webcal.all().map { URL(it.url) })
        }
        Thread {
            runBlocking {
                L.info("starting refresher")
                while (true) {
                    try {
                        urls.forEach { get(it.toExternalForm()).refreshIfStale() }
                    } catch (ex: Exception) {
                        print(ex)
                    }
                    delay(10000)
                }
            }
        }.apply {
            isDaemon = true
            name = "Url-refreshe-thread"
            start()
        }
    }


}


