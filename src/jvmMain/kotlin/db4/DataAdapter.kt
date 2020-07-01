package db4

import db4common.DataTable
import db4common.RowState
import nswf.logger
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import kotlin.reflect.KClass


class DataAdapter(val connection: Connection) {
    companion object {
        val L by logger(DataAdapter::class.simpleName)
    }

    var debug = false
    var sql: String = ""

    constructor(connection: Connection, sql: String) : this(connection) {
        this.sql = sql
    }

    fun fill(table: DataTable): DataTable {
        val rs = connection.createStatement().executeQuery(sql)
        return fill(rs, table)
    }

    fun fill(rs: ResultSet, table: DataTable): DataTable {

        table.name = rs.metaData.getTableName(1)

        val meta = fillNoMeta(connection.metaData.getPrimaryKeys(null, null, table.name), DataTable())
        //meta.print()
        val colNames = arrayOf("ORDINAL_POSITION", "key_seq")
        val pkOrder = colNames.first { meta.Columns.exists(it) }
        val list = meta.Rows
                .sortedBy { it.get<Number>(pkOrder)!!.toInt() }
                .map { it.get<String>("COLUMN_NAME")!! }

        table.primaryKey = list

        return fillNoMeta(rs, table)
    }

    private fun fillNoMeta(rs: ResultSet, table: DataTable): DataTable {
        for (idx in 1..rs.metaData.columnCount) {
            val cn = rs.metaData.getColumnLabel(idx)
            val ccn = rs.metaData.getColumnClassName(idx)
            val ai = rs.metaData.isAutoIncrement(idx)
            //val ctn = rs.metaData.getColumnTypeName(idx)
            //val ct = rs.metaData.getColumnType(idx)
            //println("$cn $ccn $ctn $ct")
            //val type: KClass<*> = Class.forName(ccn).kotlin

            table.Columns.add(cn, autoIncrement = ai)
        }

        val noConv: (Any?) -> Any? = { it }

        class MetaColumn(val column: db4common.DataTable.Column, val dbColClassName: kotlin.String) {

            var processValue: (Any?) -> Any? = { value ->
                if (value == null)
                    null
                else {
                    val conv = convToCom[value::class.qualifiedName]
                    if (conv != null) {
                        processValue = conv.toCom
                        column.kClass = conv.com
                    } else
                        processValue = noConv
                    processValue(value)
                }
            }

            fun kClass(): KClass<out Any> = convToCom[dbColClassName]?.com ?: Class.forName(dbColClassName).kotlin
        }

        val metaCols = (1..rs.metaData.columnCount).map {
            val column = table.Columns[it - 1]
            val dbColClassName = rs.metaData.getColumnClassName(it)!!
            MetaColumn(column, dbColClassName)
        }

        while (rs.next()) {
            val row = table.Rows.add()
            metaCols.forEachIndexed { index, metaColumn ->
                val value: Any? = rs.getObject(index + 1)
                row[index] = metaColumn.processValue(value)
            }
        }

        metaCols.forEachIndexed { index, metaColumn ->
            if (metaColumn.column.kClass == null)
                metaColumn.column.kClass = metaColumn.kClass()
        }

        table.acceptChanges()
        return table
    }

    private val convToCom = mutableMapOf<String, Converter>()
    private val convToJvm = mutableMapOf<String, Converter>()
    fun set(conv: Converter) {
        convToCom[conv.jvm.qualifiedName!!] = conv
        convToJvm[conv.com.qualifiedName!!] = conv
    }

    fun save(table: DataTable) {

        val convs = mutableListOf<(Any?) -> Any?>()
        val noConv: (Any?) -> Any? = { it }

        for (idx in 0 until table.Columns.size) {
            val column = table.Columns[idx]
            val ccn = column.kClass!!.qualifiedName
            val conv = convToJvm[ccn]
            if (conv == null) {
                convs.add(noConv)
            } else {
                convs.add(conv.toJvm)
            }
        }

        fun conv(row: DataTable.Row, idx: Int): Any? = convs[idx](row[idx])
        fun conv(row: DataTable.Row, fieldName: String): Any? {
            val idx = table.Columns.lookup(fieldName)
            return conv(row, idx)
        }

        fun convOld(row: DataTable.Row, fieldName: String): Any? {
            val idx = table.Columns.lookup(fieldName)
            return convs[idx](row.old[idx])
        }


        class AutoInc {
            val ai = table.Columns.any { it.autoIncrement }
            val first by lazy { table.Columns.first { it.autoIncrement } }
        }

        class InsStuff {
            val params = StringBuilder()


            init {
                params.append(table.Columns.filter { !it.isDummy }.map { "?" }.joinToString(separator = ","))
            }

            val sql = "insert into ${table.name} values(${params})"
            val autoInc = AutoInc()
            val statement =
                    if (autoInc.ai)
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                    else connection.prepareStatement(sql)

            fun doRow(row: DataTable.Row) {
                debug(sql)
                var parIdx = 0
                table.Columns.forEachIndexed { idx, column ->
                    if (!column.isDummy) parIdx++
                    statement.setObject(parIdx, conv(row, idx))
                }

                statement.execute()
                if (autoInc.ai) {
                    val rs = statement.generatedKeys
                    rs.next()
                    val number = rs.getObject(1) as Number
                    row[autoInc.first.name] = when (autoInc.first.kClass) {
                        Int::class -> number.toInt()
                        Long::class -> number.toLong()
                        Short::class -> number.toShort()
                        else -> number
                    }

                }
                row.acceptChanges()
            }
        }

        open class CommonStuff {
            val par = mutableListOf<Any?>()
            val sql = StringBuilder()
            fun clear() {
                par.clear(); sql.setLength(0)
            }

            fun appendWherePk(row: DataTable.Row) {
                sql.append(" where ")
                for (fieldName in table.primaryKey) {
                    sql.append("$fieldName=? and ")
                    if (row.hasOld(fieldName))
                        par.add(convOld(row, fieldName))
                    else
                        par.add(conv(row, fieldName))
                }
                sql.setLength(sql.length - 5)
            }

        }

        class UpdStuff : CommonStuff() {
            fun doRow(row: DataTable.Row) {

                clear()
                sql.append("update ${table.name} set ")

                table.Columns.forEachIndexed { idx, col ->
                    if (row.hasOld(idx) && !col.isDummy) {
                        sql.append("${col.name}=?,")
                        par.add(conv(row, idx))
                    }
                }
                sql.setLength(sql.length - 1)
                if (par.size > 0) {
                    appendWherePk(row)
                    debug(sql)
                    val stat = connection.prepareStatement(sql.toString())
                    for (idx in par.indices)
                        stat.setObject(idx + 1, par[idx])
                    stat.executeUpdate()
                }
                row.acceptChanges()
            }

        }

        class DelStuff : CommonStuff() {
            fun doRow(row: DataTable.Row) {

                clear()
                sql.append("delete from ${table.name} ")

                appendWherePk(row)
                debug(sql)
                val stat = connection.prepareStatement(sql.toString())
                for (idx in par.indices)
                    stat.setObject(idx + 1, par[idx])
                stat.execute()
            }
        }


        val ins = InsStuff()
        val upd = UpdStuff()
        val del = DelStuff()
        table.Rows.forEach {
            when (it.state) {
                RowState.INSERTED -> ins.doRow(it)
                RowState.UPDATED -> upd.doRow(it)
            }
        }
        table.DeletedRows
                .filter { it.state != RowState.INSERTED }
                .forEach { del.doRow(it) }
        table.DeletedRows.clear()
    }

    fun debug(msg: Any) {
        if (debug) L.debug(msg)
    }

}

class Converter(val jvm: KClass<*>, val com: KClass<*>, val toCom: (Any?) -> Any?, val toJvm: (Any?) -> Any?)