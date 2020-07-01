package sample

import org.h2.jdbcx.JdbcDataSource
import org.h2.tools.Server
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

class Persistence(val databasePath: String) {
    val url = "jdbc:h2:$databasePath;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=19091"

    private val user = "sa"
    private val pass = "sa"

    fun connection(): Connection {
        Class.forName("org.h2.Driver")
        return DriverManager.getConnection(url, user, pass)
    }

    fun dataSource(): DataSource {
        val ds = JdbcDataSource()
        ds.setURL(url)
        ds.user = user
        ds.password = pass

        return ds
    }

    fun setupDatabase() {
        Database.connect(dataSource())
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.createMissingTablesAndColumns(Calendars, Courses, AppLogs, Webcals)
        }
    }

    fun startWebServer() {
        Server.startWebServer(connection())
    }

}

val persistence = Persistence("~/h2/usicalendar/usicalendar")

fun main(args: Array<String>) {
    persistence.startWebServer()
}

