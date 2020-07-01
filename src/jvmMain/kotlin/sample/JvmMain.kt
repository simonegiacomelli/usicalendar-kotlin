package sample


import calendar.AllUrlCache
import calendar.CalAggregator
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ConditionalHeaders
import io.ktor.features.DefaultHeaders
import io.ktor.http.content.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import nswf.Logger
import nswf.NswfServer
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime
import starter.NettyContext
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat

val L by lazy { Logger("base-main") }



class nswfCl : NswfServer<NettyContext>() {
    val allUrlCache = AllUrlCache()

    init {
        allUrlCache.startCacheRefresher.value //start thread
    }

    val agg = CalAggregator(allUrlCache)
}

val nswf by lazy { nswfCl() }

fun main() {

    setupDatabase()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        val sdf = SimpleDateFormat("yyyy-MM-dd--HH-mm-ss")
        install(DefaultHeaders)
        install(ConditionalHeaders) {
            version { content ->
                if (content is LocalFileContent) {
                    val mod = Files.getLastModifiedTime(content.file.toPath())

                    listOf(
                        LastModifiedVersion(mod),
                        EntityTagVersion("last-mod-" + sdf.format(mod.toMillis()))
                    )
                } else
                    listOf()
            }
        }

        val currentDir = File(".").absoluteFile.normalize()
        environment.log.info("Current directory: $currentDir")

        val webDir = listOf(
            "build/distributions",
            "src/jsMain/web",
            "web",
            "../src/jsMain/web"
        ).map {
            File(currentDir, it)
        }.firstOrNull { it.isDirectory }?.absoluteFile ?: error("Can't find 'web' folder for this sample")

        environment.log.info("Web directory: $webDir")
        nswf.apply {
            afterServe = {
                it.connClose()
                it.exception?.printStackTrace()
            }
            preApi = { ctx ->
                val token = ctx.params["token"] ?: ""
                val calendar = if (token.isEmpty()) null else transaction {
                    Calendar.find {
                        Calendars.token.eq(token)
                    }.firstOrNull()
                }
                if (calendar != null) {
                    ctx.token_ok = true
                    ctx.calendar = calendar
                    transaction {
                        Calendars.update({ Calendars.id eq calendar.id }) {
                            it[dtLastUse] = DateTime()
                        }
                    }


                    true
                } else {
                    if (!token.isEmpty())
                        L.info("token not found $token")
                    true
                }
            }

        }
        routing {
            static("usicalendar") {
                files(webDir)
                default(webDir.resolve("index.html"))
            }
            static("/src") {
                files("./src")
            }
            get("/usicalendar/api/{...}") {
                log.info("calling ${this.context.request}")
                nswf.serve(NettyContext(this))
            }
            get("/usicalendar/usicalendar") {
                val req = context.request

                val icresp = IcsCalendarResponse(this, nswf.agg)
                if (req.queryParameters.contains("filter")) {
                    icresp.getFilter()
                } else {
                    icresp.getNoFilter(req.queryParameters["token"].orEmpty())
                }
            }
            get("/usicalendar/token/{token}/cal.ics") {
                L.info("access with token cal.ics")
                val icresp = IcsCalendarResponse(this, nswf.agg)
                icresp.getNoFilter(call.parameters["token"].orEmpty())
            }
        }

        Unit

    }.start(wait = true)
}

