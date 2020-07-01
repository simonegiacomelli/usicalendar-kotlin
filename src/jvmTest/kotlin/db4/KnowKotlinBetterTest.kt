package db4

import db4common.TypedRow
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.*

class KnowKotlinBetterTest {

    var _conn: Connection? = null

    val conn: Connection
        get() = _conn!!

    @BeforeTest
    fun setUp() {
        _conn = DriverManager.getConnection("jdbc:h2:mem:");
    }


    class h2_log : TypedRow() {
        var id: Int?         by nullVar
        var name: String?    by nullVar
        var counter: Int?    by nullVar
    }

    @Test
    fun testFill() {
        conn.createStatement().executeUpdate("create table log (\n" +
                "  id integer,\n" +
                "  name varchar(40) ,\n" +
                "  counter integer,\n" +
                "primary key (id) )\n")

        conn.createStatement().executeUpdate("insert into log values(1,'uno',10) ")
        conn.createStatement().executeUpdate("insert into log values(2,'due',11) ")

        val rows = conn.createQuery("select * from log").executeAndFetch { h2_log() }.Rows

        val target = rows.groupBy { it.id?:0 }.mapValues { it.value.first().name.orEmpty() }

    }

}