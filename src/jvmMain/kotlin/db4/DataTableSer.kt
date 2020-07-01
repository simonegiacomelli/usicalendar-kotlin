package db4

import db4common.DataTable
import java.io.*

import java.io.ByteArrayOutputStream

class DataTableSer : Serializable {
    var name: String? = null
    lateinit var primaryKey: List<String>
    lateinit var cols: Array<Column>

    class Column(val name: String, val javaClassStr: String?) : Serializable

    lateinit var rows: Array<Array<Any?>>
}

fun DataTable.save(): String {
    val table = this
    val dts = DataTableSer()
    dts.name = table.name
    dts.primaryKey = table.primaryKey
    dts.cols = table.Columns.map({ DataTableSer.Column(it.name, it.kClass?.javaObjectType?.name) }).toTypedArray()
    dts.rows = table.Rows.map { it.vals.toTypedArray() }.toTypedArray()
    val result = ObjectSerializer.serialize(dts)
    //print(result)
    return result
}

fun DataTable.load(string: String): DataTable {
    val table = this
    val dts = ObjectSerializer.deserialize<DataTableSer>(string)!!
    table.name = dts.name
    table.primaryKey = dts.primaryKey
    dts.cols.forEach {
        table.Columns.add(it.name, if (it.javaClassStr != null) Class.forName(it.javaClassStr).kotlin else null)
    }
    dts.rows.forEach {
        table.Rows.add(*it)
    }
    return table
}


object ObjectSerializer {

    /**
     * Serialize given object into [String] using [ObjectOutputStream].
     * @param obj object to serialize
     * @see ObjectInputStream
     * @return the serialization result, empty string for _null_ input
     */
    fun <T : Serializable> serialize(obj: T?): String {
        if (obj == null) {
            return ""
        }

        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(obj)
        oos.close()

        return baos.toString("ISO-8859-1")
    }

    /**
     * Deserialize given [String] using [ObjectInputStream].
     * @param string the string to deserialize
     * @return deserialized object, null, in case of error.
     */
    fun <T : Serializable> deserialize(string: String): T? {
        if (string.isEmpty()) {
            return null
        }

        var bais = ByteArrayInputStream(string.toByteArray(charset("ISO-8859-1")))
        var ois = ObjectInputStream(bais)

        return ois.readObject() as T
    }

    fun <T : Serializable> deserialize(string: String, clazz: Class<T>): T? = deserialize<T>(string)

}