package db4common

import kotlin.reflect.KProperty


interface NotNullVar {
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

interface NullVar {
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T?
    operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T?)
}

open class TypedRow : DataTable.Row() {

    val notNullVar = object : NotNullVar {
        override operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
            return get<T>(property.name)!!
        }

        override operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            set(property.name, value)
        }

    }

    val nullVar = object : NullVar {
        override operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T? {
            //return "$thisRef, thank you for delegating '${property.name}' to me!"
            return get<T>(property.name)
        }

        override operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            //println("$value has been assigned to '${property.name}' in $thisRef.")
            set(property.name, value)
        }

    }

}

val <T : TypedRow> T.table get() = this.tab as DataTableTyped<T>

class User : TypedRow() {
    var name: String? by nullVar
    var age: Int?     by notNullVar
}

fun testSimpleTable(args: Array<String>) {
    val tab = DataTableTyped { User() }
    tab.Columns.add("name", String::class)
    tab.Columns.add("age", Int::class)
    val u1 = tab.Rows.add("simo", 10)
    u1.name = null
    println(u1.name)
    println(u1.age)
}

class DataTableTyped<out T : DataTable.Row>(override val newInst: () -> T) : DataTable() {
    override val Rows: RowCollection<out T> get() = super.Rows as RowCollection<out T>
    override val DeletedRows: RowCollection<out T> get() = super.DeletedRows as RowCollection<out T>
}

