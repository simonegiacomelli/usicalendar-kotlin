package sample

import api.CourseStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

private val x = nswf.apply {
    api(::CourseStatus) { ctx, param, res ->
        if (!ctx.token_ok) {
            ctx.forbidden()
            return@api
        }
        transaction {
            val course =
                Course.find {
                    (Courses.summary eq param.courseSummary) and
                            (Courses.calendar eq ctx.calendar.id)
                }
                    .firstOrNull()
            if (course == null && param.active) {
                Courses.insert {
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

}