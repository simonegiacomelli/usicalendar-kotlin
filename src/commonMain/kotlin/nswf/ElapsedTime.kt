package nswf

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz

class ElapsedTime {
    val L by logger()

    private var st: Long = 0

    fun start(): ElapsedTime {
        L.debug { "Start measure" }
        st = DateTime.nowUnixLong()
        return this
    }

    fun stop() {
        val elap =  DateTime.nowUnixLong() - st
        L.debug { "elapsed $elap msec" }
    }

}

fun DateTime.toStr() = toString("yyyy-MM-dd HH:mm:ss")
fun DateTimeTz.toStr() = toString("yyyy-MM-dd HH:mm:ss")