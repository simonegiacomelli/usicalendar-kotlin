package db4

import db4common.DataTable
import db4common.DataTableTyped
import java.sql.Connection
import java.sql.PreparedStatement

fun Connection.createQuery(sql: String): Query {
    return Query(this, sql)
}


class Query(val connection: Connection, val sql: String) {
    val params = hashMapOf<String, Any>()

    val parsedSql: SqlParser by lazy { SqlParser(sql) }
    val dataAdapter: DataAdapter by lazy { DataAdapter(connection) }
    lateinit var table: DataTable
    fun addParameter(name: String, value: Any): Query {
        params[name.toUpperCase()] = value
        return this
    }

    //DataTableTyped<out T : DataTable.Row>(val func: () -> T)
    fun <T : DataTable.Row> executeAndFetch(func: () -> T): DataTableTyped<T> {
        val res = DataTableTyped<T>(func)
        executeAndFetch(res)
        return res
    }

    fun <T : DataTable.Row> executeAndFetchQ(func: () -> T): Query {
        executeAndFetch(func)
        return this
    }


    fun executeAndFetch(): DataTable {
        val res = DataTable()
        executeAndFetch(res)
        return res
    }

    private fun executeAndFetch(res: DataTable) {
        val stat = prepStat()
        val rs = stat.executeQuery()

        dataAdapter.fill(rs, res)
        table = res
    }

    private fun prepStat(): PreparedStatement {
        val stat = connection.prepareStatement(parsedSql.parsed)
        parsedSql.ordinal.forEachIndexed { idx, name ->
            stat.setObject(idx + 1, params[name])
        }
        return stat!!
    }

    fun save() {
        dataAdapter.save(table)
    }

    fun executeUpdate()  = prepStat().executeUpdate()

}

