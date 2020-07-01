package starter

import api.AuthToken
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import db4.Converter
import db4.Pool
import db4.Query
import db4.createQuery
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import nswf.logger
import java.sql.Date
import java.sql.Timestamp


class NettyContext(val pc: PipelineContext<Unit, ApplicationCall>) : nswf.Context() {
    val L by logger()

    var nettyResponse: String? = null
    override val responseDone: Boolean get() = nettyResponse != null

    override suspend fun respond(content: String) {
//        L.debug(content)
        nettyResponse = content
        pc.call.respondText(content)

    }

    override val postParams: Map<String, String>
        get() = params //paraculo
    override val params: Map<String, String> get() = pc.context.request.queryParameters.toMap().mapValues { it.value.first() }
    override val uri: String get() = pc.call.request.path()

    suspend fun forbidden() {
        nettyResponse = ""
        pc.call.respondText("", status = HttpStatusCode.Forbidden)
    }

    val pool by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Pool() }
    val connLazy = lazy {
        val conn = pool.acquire()
        conn.autoCommit = false
        conn
    }
    val conn by connLazy
    fun connClose() {
        if (connLazy.isInitialized()) {
            if (exception == null) conn.commit() else conn.rollback()
            conn.autoCommit = true
            pool.release(conn)
        }
    }


    companion object {
        private val timestampConverter = Converter(
            Timestamp::class,
            DateTimeTz::class,
            { com.soywiz.klock.DateTime((it as Timestamp).time).local },
            { Timestamp((it as DateTimeTz).utc.unixMillisLong) })
        private val dateConverter = Converter(
            Date::class,
            DateTimeTz::class,
            { com.soywiz.klock.DateTime((it as Date).time).local },
            { Date((it as DateTimeTz).utc.unixMillisLong) })


    }

    fun createQuery(sql: String): Query {
        return conn.createQuery(sql).apply {
            dataAdapter.set(timestampConverter)
            dataAdapter.set(dateConverter)
        }
    }


    var token_ok = false
    lateinit var calendar: sample.Calendar

    fun tokenInfo(): String {
        return if (token_ok) calendar.token + " " + calendar.friendlyName else "- no token -"
    }
}