package nswf_utils

import kotlinx.coroutines.Deferred
import nswf.Logger

fun <T> Deferred<T>.logException(info: String = ""): Deferred<T> {
    invokeOnCompletion {
        if (it != null) logException(it, info)

    }
    return this
}

private val L = Logger("AsyncException")

fun logException(it: Throwable, info: String) {
    console.error(it)
    if (info.isNotEmpty())
        println(info)
    fun accessStack(ex: dynamic) {
        L.debug("$info ${ex.message} STAMPA DELLO STACK PER POTENZIALE errata CORRISPONDENZA COL SOURCEMAP\n${ex.stack}")
    }
    accessStack(it)
}
