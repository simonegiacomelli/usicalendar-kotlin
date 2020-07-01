package fragment

import nswf.logger
import org.w3c.dom.events.Event
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Promise

object HttpRequestDebug {
    val L by logger()


    fun getString(url: String): Promise<String> =
            Promise { resolve, reject ->
                val xhr = XMLHttpRequest()
                xhr.open("GET", url)
                xhr.addEventListener("load", {
                    //    L.debug("resolving $url")
                    resolve(xhr.responseText)
                })
                xhr.addEventListener("error", {
                    L.debug("error happened for url $url")
                    reject(EventException(xhr))
                })
                //L.debug("sending new request $url")
                xhr.send()
            }

    fun get(url: String): Promise<XMLHttpRequest> =
            Promise { resolve, reject ->
                val xhr = XMLHttpRequest()
                xhr.open("GET", url)
                xhr.addEventListener("load", { resolve(xhr) })
                xhr.addEventListener("error", {
                    L.debug("error happened for url $url")
                    reject(EventException(xhr))
                })
                xhr.send()
            }

    fun post(url: String, params: String): Promise<String> =
            Promise { resolve, reject ->
                val xhr = XMLHttpRequest()
                xhr.open("POST", url, true)
                xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded")
                xhr.addEventListener("load", {
                    //L.debug("resolving $url")
                    resolve(xhr.responseText)
                })
                xhr.addEventListener("error", {
                    L.debug("error happened for url $url")
                    reject(EventException(xhr))
                })
                //L.debug("sending new request $url")
                xhr.send(params)
            }
}

class EventException(val xhr: XMLHttpRequest) : Throwable() {
    override fun toString(): String {
        return "${xhr.status} ${xhr.statusText}"
    }
}
