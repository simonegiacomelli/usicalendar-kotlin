package sample

import calendar.CalAggregator
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.system.measureTimeMillis

class IcsCalendarResponse(
    val pipeline: PipelineContext<Unit, ApplicationCall>,
    val agg: CalAggregator
) {
    val context = pipeline.context
    val call = pipeline.call
    val req = context.request
    val txt = req.queryParameters.contains("txt")

    suspend fun getNoFilter(token: String) {
        measureTimeMillis {
            val summaries = if (token.isEmpty()) emptySet() else {
                transaction {
                    val calendar = Calendar.find { Calendars.token eq token }.firstOrNull()
                    if (calendar != null) {
                        counterCalendarIncrement(calendar.id.value)
                        Course.find { Courses.calendar eq calendar.id }.map { it.summary }.toSet()
                    } else emptySet()
                }
            }
            val strCal: String = agg.aggregateAndFilter(summaries)
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondText(
                strCal,
                ContentType.parse(if (txt) "text/plain" else "text/calendar")
            )
        }.also {
            L.info("serving ics msec: ${it.toString().padStart(6)} token:[$token]")
        }
    }

    suspend fun getFilter() {
        val filter = req.queryParameters["filter"].orEmpty()
        measureTimeMillis {
            val strCal: String = agg.aggregateAndFilter(filter)
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondText(
                strCal,
                ContentType.parse(if (txt) "text/plain" else "text/calendar")
            )
        }.also {
            L.info("serving ics msec: ${it.toString().padStart(6)} filter:[$filter]")
        }
    }

}

private fun counterCalendarIncrement(calendar_id: Long) {
    Calendars.update({ Calendars.id eq calendar_id }) {
        with(SqlExpressionBuilder) {
            it.update(counterCalendarRequests, counterCalendarRequests + 1)
            it.update(dtLastCalendarRequests, CurrentDateTime())
        }
    }
}
