package sample


import api.*
import calendar.AllUrlCache
import calendar.CalAggregator
import calendar.CalendarResourceManager
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
import misc.GetRandomName
import nswf.Logger
import nswf.NswfServer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import starter.NettyContext
import java.io.File
import java.lang.Exception
import java.nio.file.Files
import java.text.SimpleDateFormat
import kotlin.system.measureTimeMillis

val L by lazy { Logger("base-main") }

fun counterUiIncrement(calendar_id: Long) {
    Calendars.update({ Calendars.id eq calendar_id }) {
        with(SqlExpressionBuilder) {
            it.update(counterUiRequests, counterUiRequests + 1)
            it.update(dtLastUiRequests, CurrentDateTime())
        }
    }
}

fun counterCalendarIncrement(calendar_id: Long) {
    Calendars.update({ Calendars.id eq calendar_id }) {
        with(SqlExpressionBuilder) {
            it.update(counterCalendarRequests, counterCalendarRequests + 1)
            it.update(dtLastCalendarRequests, CurrentDateTime())
        }
    }
}

fun dblog(
    ctx: NettyContext,
    instance: String,
    kind: String,
    info: String,
    info2: String
) {
    transaction {
        AppLogs.insert {
            if (ctx.token_ok)
                it[this.calendar] = ctx.calendar.id
            it[this.dtCreation] = DateTime()
            it[this.instance] = instance
            it[this.kind] = kind
            it[this.info] = info
            it[this.info2] = info2
        }
    }
}


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
        val allUrlCache = AllUrlCache()
        allUrlCache.startCacheRefresher.value //start thread
        val agg = CalAggregator(allUrlCache)
        environment.log.info("Web directory: $webDir")
        val nswf = NswfServer<NettyContext>().apply {
            afterServe = {
                it.connClose()
                it.exception?.printStackTrace()
            }
            preApi = { ctx ->
                val token = ctx.params["token"] ?: ""
                val calendar = if (token.isEmpty()) null else transaction {
                    sample.Calendar.find {
                        sample.Calendars.token.eq(token)
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
            api(::CreateToken, preApi = { true }) { ctx, param, res ->

                transaction {
                    sample.Calendars.insert {
                        val token = generate_token()
                        val friendlyName = GetRandomName(0)
                        val now = DateTime()
                        it[this.token] = token
                        it[this.friendlyName] = friendlyName
                        it[counterCalendarRequests] = 0
                        it[counterUiRequests] = 0
                        it[dtCreation] = now
                        res.success = true
                        res.token = token
                        res.friendlyName = friendlyName
                        res.dtCreation = com.soywiz.klock.DateTime(now.toDate().time).local
                        L.info("createToken $token $friendlyName")
                    }
                }

            }
            api(::CourseStatus) { ctx, param, res ->
                if (!ctx.token_ok) {
                    ctx.forbidden()
                    return@api
                }
                transaction {
                    val course =
                        sample.Course.find {
                            (Courses.summary eq param.courseSummary) and
                                    (Courses.calendar eq ctx.calendar.id)
                        }
                            .firstOrNull()
                    if (course == null && param.active) {
                        sample.Courses.insert {
                            it[calendar] = ctx.calendar.id
                            it[summary] = param.courseSummary
                            it[dtCreation] = DateTime()
                        }
                    }
                    if (course != null && !param.active) {
                        course.delete()
                    }
                }
            }
            api(::DbLog) { ctx, param, res ->
                measureTimeMillis {
                    try {
                        dblog(ctx, param.instance, param.kind, param.info, param.info2)
                    } catch (ex: Exception) {
                        //${param.kind}, ${param.info}, ${param.instance}
                        L.error("DbLogError", ex)
                        ex.printStackTrace()
                    }
                }.also {
                    L.info("DbLog msec: ${it.toString().padStart(6)} token:[${if (ctx.token_ok) ctx.calendar.token else "-no token-"}] ")
                }

            }
            api(::SqlQuery) { ctx, param, res ->
                if (ctx.token_ok && ctx.calendar.isAdmin == true) {
                    res.success = "1"
                    res.table = ctx.createQuery(param.sql).executeAndFetch().apply {

                    }
                } else
                    L.info("SqlQuery You are not authorized ${ctx.tokenInfo()}")
            }
            api(::QueryCalendars) { ctx, param, res ->
                measureTimeMillis {

                    val activeCourses = if (ctx.token_ok) {
                        transaction {
                            counterUiIncrement(ctx.calendar.id.value)
                            Course.find { Courses.calendar eq ctx.calendar.id }.map { it.summary }.toSet()
                        }
                    } else emptySet()

                    val filter = param.filter
                    val courses = agg.groupCoursesBySummary(filter.splitToSequence(",").toSet())

                    val tab = QueryCalendars.cl_calendar.new()
                    courses.forEach {
                        val row = tab.Rows.add()
                        val summary = it.summary
                        row.summary = summary
                        row.location = it.location
                        row.dateStart = it.dateStart
                        row.dateEnd = it.dateEnd
                        row.url = it.event?.url?.value ?: ""
                        row.active = activeCourses.contains(summary)
                    }
                    tab.acceptChanges()
                    res.tab = tab
                }.also {
                    L.info("QueryCalendars msec: ${it.toString().padStart(6)} token:[${if (ctx.token_ok) ctx.calendar.token else "-no token-"}]")
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

                val icresp = IcsCalendarResponse(this, agg)
                if (req.queryParameters.contains("filter")) {
                    icresp.getFilter()
                } else {
                    icresp.getNoFilter(req.queryParameters["token"].orEmpty())
                }
            }
            get("/usicalendar/token/{token}/cal.ics") {
                L.info("access with token cal.ics")
                val icresp = IcsCalendarResponse(this, agg)
                icresp.getNoFilter(call.parameters["token"].orEmpty())
            }
        }

        Unit

    }.start(wait = true)
}

