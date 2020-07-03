package sample

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.sql.Connection
import java.sql.DriverManager

fun validationSql() = "CALL SESSION_ID()"

fun connection(): Connection {
    Class.forName("org.h2.Driver")
    return DriverManager.getConnection(
        "jdbc:h2:~/h2/usicalendar/usicalendar;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=19090",
        "sa",
        "sa"
    )
}


fun setupDatabase() {
    persistence.setupDatabase()
}

object Calendars : LongIdTable() {
    val token = varchar("token", 38)
    val friendlyName = varchar("friendly_name", 100)
    val counterCalendarRequests = long("counter_calendar_requests")
    val counterUiRequests = long("counter_ui_requests")
    val dtCreation = datetime("dt_creation")
    val dtLastUse = datetime("dt_last_use").nullable()
    val dtLastCalendarRequests = datetime("dt_last_calendar_request").nullable()
    val dtLastUiRequests = datetime("dt_last_ui_request").nullable()
    val isAdmin = bool("is_admin").nullable()
}

class Calendar(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Calendar>(Calendars)

    var token by Calendars.token
    var friendlyName by Calendars.friendlyName
    var dtLastUse by Calendars.dtLastUse
    var isAdmin by Calendars.isAdmin
}

object Courses : LongIdTable() {
    val calendar = reference("calendar", Calendars)
    val dtCreation = datetime("dt_creation")
    val summary = varchar("summary", 1000)
}

class Course(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Course>(Courses)

    var calendar by Calendar referencedOn Courses.calendar
    val summary by Courses.summary

}

object AppLogs : LongIdTable() {
    val calendar = reference("calendar", Calendars).nullable()
    val instance = varchar("instance", 30)
    val dtCreation = datetime("dt_creation")
    val kind = varchar("kind", 30)
    val info = text("info")
    val info2 = text("info2")
}

class AppLog(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AppLog>(AppLogs)
}

object Webcals : LongIdTable() {
    val url = varchar("url", 1000)
    val dtCreation = datetime("dt_creation")

    fun addTestCalendar(cal: String) {
        transaction {
            if (select { url eq cal }.count() == 0)
                insert {
                    it[url] = cal
                    it[dtCreation] = DateTime()
                }
        }
    }
}

class Webcal(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Webcal>(Webcals)

    var url by Webcals.url
}