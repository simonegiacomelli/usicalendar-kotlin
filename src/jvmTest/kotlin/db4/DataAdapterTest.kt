package db4

import com.soywiz.klock.DateTime
import db4common.DataTable
import db4common.RowState
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import kotlin.test.*


class DataAdapterTest {

    var _conn: Connection? = null

    val conn: Connection
        get() = _conn!!

    @BeforeTest
    fun setUp() {
        _conn = DriverManager.getConnection("jdbc:h2:mem:");
    }

    @Test
    fun testFill() {
        conn.createStatement().executeUpdate(
            "create table log (\n" +
                    "  name varchar(40) ,\n" +
                    "  id integer,\n" +
                    "  counter integer,\n" +
                    "primary key (name,id) )\n"
        )

        conn.createStatement().executeUpdate("insert into log values('ciao',10,123) ")
        conn.createStatement().executeUpdate("insert into log values('ciao',11,456) ")

        val target = DataAdapter(conn, "select * from log")
        var table = target.fill(DataTable())

        assertEquals(table.name!!.toUpperCase(), "LOG")
        assertEquals(listOf("NAME", "ID").toSet(), table.primaryKey.map { it.toUpperCase() }.toSet())

        assertEquals(table.Rows.size, 2)
        assertEquals(table.Rows[0]["name"]!!, "ciao")
        assertEquals(table.Rows[0]["id"]!!, 10)
        assertEquals(table.Rows[0]["counter"]!!, 123)
        table.Rows.forEach({ assertEquals(it.state, RowState.DEFAULT) })

    }

    @Test(expected = Exception::class)
    fun testFill_all_nulls() {
        conn.createStatement().executeUpdate(
            "create table log (\n" +
                    "  id integer ,\n" +
                    "  counter integer,\n" +
                    "primary key (id) )\n"
        )


        conn.createStatement().executeUpdate("insert into log values(1,null) ")
        val target = DataAdapter(conn, "select * from log")
        var table = target.fill(DataTable())
        table.Rows.add(2, "should fail")
    }

    @Test
    fun testNull() {
        createSimpleTable()

        conn.createStatement().executeUpdate("insert into log values(10,'ten') ")
        conn.createStatement().executeUpdate("insert into log values(11,null) ")

        val target = DataAdapter(conn, "select * from log order by id")
        var table = target.fill(DataTable())
        assertEquals(table.Rows.size, 2)
        assertNull(table.Rows[1]["ds"])
    }

    @Test
    fun test_timestamp() {
        createTimestampTable()

        val target = DataAdapter(conn, "select * from log order by id")
        var table = target.fill(DataTable())
        assertEquals(Timestamp::class, table.Columns["dt"].kClass)

        val currentTimeMillis = System.currentTimeMillis()
        table.Rows.add(1, Timestamp(currentTimeMillis))
        target.save(table)
        table = target.fill(DataTable())
        assertEquals(1, table.Rows.size)
        val row = table.Rows[0]
        assertEquals(1, row.get<Int>(0))
        assertEquals(currentTimeMillis, row.get<Timestamp>(1)!!.time)
    }

    @Test
    fun test_timestamp_to_klock() {
        createTimestampTable()

        val target = DataAdapter(conn, "select * from log order by id")
        target.set(Converter(Timestamp::class, DateTime::class
            , { DateTime((it as Timestamp).time) }, { Timestamp((it as DateTime).unixMillisLong) })
        )
        var table = target.fill(DataTable())
        assertEquals(table.Columns["dt"].kClass, com.soywiz.klock.DateTime::class)

        val currentTimeMillis = System.currentTimeMillis()
        table.Rows.add(1, com.soywiz.klock.DateTime(currentTimeMillis))
        target.save(table)
        table = target.fill(DataTable())
        assertEquals(1, table.Rows.size)
        val row = table.Rows[0]
        assertEquals(row.get<Int>(0), 1)
        assertEquals(currentTimeMillis, row.get<com.soywiz.klock.DateTime>(1)!!.unixMillisLong)
        assertEquals(table.Columns["dt"].kClass, com.soywiz.klock.DateTime::class)
    }

    private fun createTimestampTable() {
        conn.createStatement().executeUpdate(
            "create table log (\n" +
                    "  id integer ,\n" +
                    "  dt timestamp,\n" +
                    "primary key (id) )\n"
        )
    }

    @Test
    fun insert() {
        createSimpleTable()

        val target = DataAdapter(conn, "select * from log order by id")
        var table = target.fill(DataTable())
        val row = table.Rows.add()
        row["id"] = 10
        row["ds"] = "xyz"
        target.save(table)

        val count = executeScalar<Number>("select count(*) from log where id=10 and ds ='xyz' ")!!.toInt()
        assertEquals(count, 1)
        table.Rows.forEach({ assertEquals(it.state, RowState.DEFAULT) })
    }

    @Test
    fun dummy_fields() {
        createSimpleTable()

        val target = DataAdapter(conn, "select log.* , 'hi' as dummy from log order by id")
        var table = target.fill(DataTable())
        table.Columns["dummy"].isDummy = true

        val row = table.Rows.add()
        row["id"] = 10
        row["ds"] = "xyz"
        row["dummy"] = "y"
        target.save(table) //insert

        row["dummy"] = "x"
        target.save(table) //update

    }

    @Test
    fun insert_autoincrement() {
        insert_autoincrement_fun("smallint")
        insert_autoincrement_fun("int")
        insert_autoincrement_fun("bigint")
    }

    private fun insert_autoincrement_fun(s: String) {
        try {
            conn.createStatement().executeUpdate("drop table log")
        } catch (ex: Exception) {
        }
        conn.createStatement().executeUpdate(
            "create table log (\n" +
                    "  id $s auto_increment,\n" +
                    "  ds varchar(50),\n" +
                    "primary key (id) )\n"
        )

        val target = DataAdapter(conn, "select * from log order by id")
        var table = target.fill(DataTable())
        val row = table.Rows.add()
        //row["id"] = 10
        row["ds"] = "xyz"
        target.save(table)

        assertNotNull(row.get<Number>("id"))
        assertEquals(row.get<Number>("id")!!.toInt(), 1)
        val count = executeScalar<Number>("select count(*) from log where id = 1 ")!!.toInt()
        assertEquals(count, 1)
    }


    @Test
    fun update() {
        createSimpleTable()
        conn.createStatement().executeUpdate("insert into log values(10,'ten') ")

        val target = DataAdapter(conn, "select * from log order by id")
        var table = target.fill(DataTable())
        val row = table.Rows[0]
        row["id"] = 11
        row["ds"] = "xyz"

        target.save(table)

        val rowCount = executeScalar<Number>("select count(*)")!!.toInt()
        assertEquals(rowCount, 1)
        val countNew = executeScalar<Number>("select count(*) from log where id=11 and ds ='xyz' ")!!.toInt()
        assertEquals(countNew, 1)
        table.Rows.forEach({ assertEquals(it.state, RowState.DEFAULT) })
    }

    @Test
    fun delete() {
        createSimpleTable()
        conn.createStatement().executeUpdate("insert into log values(10,'ten') ")
        conn.createStatement().executeUpdate("insert into log values(11,'eleven') ")

        val target = DataAdapter(conn, "select * from log order by id")
        var table = target.fill(DataTable())
        table.Rows[0].remove()

        target.save(table)

        val rowCount = executeScalar<Number>("select count(*) from log")!!.toInt()
        assertEquals(rowCount, 1)
        val countNew = executeScalar<Number>("select count(*) from log where id=11 ")!!.toInt()
        assertEquals(countNew, 1)
        assertEquals(table.DeletedRows.size, 0)
    }

    @Test
    fun test_column_alias() {
        createSimpleTable()
        conn.createStatement().executeUpdate("insert into log values(10,'ten') ")
        val target = DataAdapter(conn, "select id as idalias from log order by id")
        var table = target.fill(DataTable())
        assertEquals("idalias", table.Columns[0].name.toLowerCase())
    }

    private fun <T> executeScalar(sql: String): T? = DataAdapter(conn, sql).fill(DataTable()).Rows[0][0]!!

    private fun createSimpleTable() {
        conn.createStatement().executeUpdate(
            "create table log (\n" +
                    "  id integer ,\n" +
                    "  ds varchar(50),\n" +
                    "primary key (id) )\n"
        )
    }

}