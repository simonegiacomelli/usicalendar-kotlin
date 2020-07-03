package sample

import api.QueryCalendars
import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.system.measureTimeMillis


fun registerQueryCalendarsApi() = nswf.apply {
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
            L.info(
                "QueryCalendars msec: ${it.toString()
                    .padStart(6)} token:[${if (ctx.token_ok) ctx.calendar.token else "-no token-"}]"
            )
        }
    }

}

private fun counterUiIncrement(calendar_id: Long) {
    Calendars.update({ Calendars.id eq calendar_id }) {
        with(SqlExpressionBuilder) {
            it.update(counterUiRequests, counterUiRequests + 1)
            it.update(dtLastUiRequests, CurrentDateTime())
        }
    }
}
