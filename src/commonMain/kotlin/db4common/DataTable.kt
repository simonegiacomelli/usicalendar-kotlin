package db4common

import kotlin.reflect.KClass

enum class RowState {
    DEFAULT,
    UPDATED,
    INSERTED,
}

open class DataTable {
    data class Column(
            var name: String,
            var kClass: KClass<*>? = null,
            var autoIncrement: Boolean = false
            , var isDummy: Boolean = false
    )

    class ColumnCollection : AbstractList<Column>() {
        override fun get(index: Int): Column = cols[index]
        operator fun get(name: String): Column = cols[lookup(name)]

        override val size: Int
            get() = cols.size
        val cols: MutableList<Column> = mutableListOf()
        private val names: MutableMap<String, Int> = mutableMapOf()

        fun add(fieldName: String, kClass: KClass<*>? = null, autoIncrement: Boolean = false) {
            add(Column(fieldName, kClass, autoIncrement))
        }

        fun add(column: Column) {
            names[column.name.toUpperCase()] = cols.size
            cols.add(column)
        }

        fun lookup(fieldName: String): Int = names.getOrElse(fieldName.toUpperCase()) { throw Exception("field [$fieldName] not found") }
        fun exists(fieldName: String): Boolean = names.containsKey(fieldName.toUpperCase())

    }

    open val Rows: RowCollection<out Row> = RowCollection()

    open val newInst = { Row() }

    inner class RowCollection<T : Row> : AbstractList<T>() {
        override fun get(index: Int): T = rows[index]

        override val size: Int
            get() = rows.size

        private val rows: MutableList<T> = mutableListOf()

        fun add(row: Row) {
            row.tab = this@DataTable
            rows.add(row as T)
        }

        fun add(vararg values: Any?): T {
            val row: T = newInst() as T
            add(row)
            row.init()
            row.assign(values)
            row.oldVals.clear()
            return row
        }


        fun removeNoTracking(row: Row) = rows.remove(row)

        fun clear() = rows.clear()

    }

    class OldValue(val value: Any?) {}

    open class Row {

        open lateinit var tab: DataTable
        var state: RowState = RowState.INSERTED

        val vals: MutableList<Any?> = mutableListOf()

        fun init() {
            fillEmpty(vals)
        }

        fun assign(values: Array<out Any?>) {
            if (values.isEmpty())
                return
            if (values.size > tab.Columns.cols.size)
                throw Exception("too many fields specified. Table have less columns")

            for (idx in values.indices)
                this[idx] = values[idx]

        }

        internal val oldVals: MutableList<OldValue?> = mutableListOf()

        internal val old = Old()

        inner class Old {

            operator fun <T> get(fieldName: String): T? {
                return this[tab.Columns.lookup(fieldName)]
            }

            operator fun <T> get(idx: Int): T? {
                if (!hasOld(idx)) return this@Row[idx]
                return oldVals[idx]!!.value as T?;
            }

            operator fun set(idx: Int, value: OldValue) {
                fillEmpty(oldVals)
                oldVals[idx] = value
            }

        }

        fun hasOld(fieldName: String): Boolean = hasOld(tab.Columns.lookup(fieldName))
        fun hasOld(idx: Int): Boolean = idx < oldVals.size && oldVals[idx] != null


        private fun <T> fillEmpty(mutableList: MutableList<T?>) {
            while (mutableList.size < tab.Columns.cols.size)
                mutableList.add(null)
        }

        operator fun <T> get(idx: Int): T? = if (idx >= vals.size) null else vals[idx] as T?


        operator fun set(idx: Int, value: Any?) {
            val column = tab.Columns.cols[idx]
            if (column.kClass != null) {
                if (value != null && !column.kClass!!.isInstance(value))
                //if (value != null && value::class != column.kClass)
                    throw Exception("bad type column ${column.name} expected:${column.kClass} actual:${value::class}")
            } else {
                if (value != null)
                    column.kClass = value::class
            }
            fillEmpty(vals)
            if (vals[idx] == value) return
            if (state != RowState.INSERTED && !hasOld(idx))
                old[idx] = OldValue(get(idx))
            if (state == RowState.DEFAULT)
                state = RowState.UPDATED
            vals[idx] = value
        }

        operator fun set(fieldName: String, value: Any?) {
            this[tab.Columns.lookup(fieldName)] = value
        }

        operator fun <T> get(fieldName: String): T? = get(tab.Columns.lookup(fieldName)) as T?

        fun acceptChanges() {
            oldVals.clear()
            state = RowState.DEFAULT
        }

        fun acceptChanges(fieldName: String) {
            val colIdx = tab.Columns.lookup(fieldName)
            if (colIdx < oldVals.size)
                oldVals[colIdx] = null
            if (state == RowState.UPDATED && oldVals.all { it == null })
                state = RowState.DEFAULT
        }

        fun remove() {
            tab.Rows.removeNoTracking(this)
            tab.DeletedRows.add(this)
        }

    }


    val Columns = ColumnCollection()

    open val DeletedRows: RowCollection<out Row> = RowCollection()

    fun acceptChanges() {
        DeletedRows.clear()
        Rows.forEach { row -> row.acceptChanges() }
    }

    var name: String? = null
    var primaryKey: List<String> = listOf()

}

fun DataTable.ColumnCollection.print() {
    for (idx in cols.indices) {
        print(cols[idx].name)
        print("\t")
    }
    println()
}

fun DataTable.ColumnCollection.printMeta() {
    for (idx in cols.indices) {
        print(cols[idx].kClass?.simpleName)
        if (cols[idx].autoIncrement) print(" (ai)")
        print("\t")
    }
    println()
    this.print()
}

fun DataTable.Row.print() {

    vals.forEach({
        print(it)
        print("\t")
    })
    println()


}

fun DataTable.printInfo(): DataTable {
    val table = this
    println("table: ${table.name} primaryKey: ${table.primaryKey} record count: ${table.Rows.size}")
    table.Columns.print()
    return table
}

fun DataTable.print(): DataTable {
    val table = this
    table.printInfo();
    table.Rows.forEach({ it.print() })
    return table
}

fun DataTable.printHeaderAndData(): DataTable {
    this.Columns.print()
    this.Rows.forEach({ it.print() })
    return this
}
