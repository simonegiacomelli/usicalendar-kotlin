package db4common

import kotlin.reflect.KClass
/*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON

@Serializable
class DataTableSer {
    var name: String? = null
    lateinit var primaryKey: List<String>
    lateinit var cols: Array<Column>

    @Serializable
    class Column(val name: String, val javaClassStr: String?)

    lateinit var rows: Array<Array<Any?>>
}

fun DataTable.save(): String {
    val table = this
    val dts = DataTableSer()
    dts.name = table.name
    dts.primaryKey = table.primaryKey
    //dts.cols = table.Columns.map({ DataTableSer.Column(it.name, it.kClass?.qualifiedName) }).toTypedArray()
    dts.cols = table.Columns.map({ DataTableSer.Column(it.name, DebugKClass(it).toString()) }).toTypedArray()
    dts.rows = table.Rows.map { it.vals.toTypedArray() }.toTypedArray()
    //val result = ObjectSerializer.serialize(dts)
    val result = JSON.indented.stringify(dts)
    //print(result)
    return result
}

*/
//expect fun DebugKClass(it: DataTable.Column): KClass<*>?

//{
//    return it.kClass
//}

//
//fun DataTable.load(string: String): DataTable {
//    val table = this
//    //val dts = ObjectSerializer.deserialize<DataTableSer>(string)!!
//    val dts = JSON.parse<DataTableSer>(string)
//    table.name = dts.name
//    table.primaryKey = dts.primaryKey
//    dts.cols.forEach {
//        table.Columns.add(it.name, if (it.javaClassStr != null) Class.forName(it.javaClassStr).kotlin else null)
//    }
//    dts.rows.forEach {
//        table.Rows.add(*it)
//    }
//    return table
//}
//

