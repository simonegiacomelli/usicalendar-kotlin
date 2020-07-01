package nswf

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime

class Logger(val name: String) {
    val df = DateFormat("yyyy-MM-dd--HH-mm-ss")
    fun debug(msg: () -> String) {
        debug(msg())
    }

    fun debug(msg: Any) {
        Appenders.println("${df.format(DateTime.nowLocal())} [$name] $msg")
    }

    fun info(msg: Any) {
        Appenders.println("${df.format(DateTime.nowLocal())} [$name] $msg")
    }

    fun warn(msg: Any, ex: Throwable) {
        Appenders.println("${df.format(DateTime.nowLocal())} [$name] ${msg} ${ex::class.simpleName} ${ex.message}")
    }

    fun error(msg: Any, ex: Throwable) {
        Appenders.println("${df.format(DateTime.nowLocal())} [$name] ${msg} ${ex::class.simpleName} ${ex.message}")
    }
}


object Appenders {
    fun println(msg: String) {
        list.forEach { it(msg) }
    }

    fun add(appender: (String) -> Unit) = list.add(appender)

    val list = mutableListOf<(String) -> Unit>()
    private val default: (String) -> Unit = {
        kotlin.io.println(it)
    }

    init {
        list.add(default)
    }


}

inline fun <reified T : Any> T.logger(name: String? = null): Lazy<Logger> {
    return lazy { Logger(if (name != null) name else T::class.simpleName.orEmpty()) }
}
