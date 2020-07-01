package nswf

import db4common.DataTable
import db4common.DataTableTyped
import db4common.RowState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DataTableSer(val name: String?
                        , val primaryKey: List<String>
                        , val cols: List<Column>
                        , val rows: List<Row>
                        , val deletedRows: List<Row>
) {
    @Serializable
    data class Column(val name: String, val kClassName: String, val autoIncrement: Boolean, val isDummy: Boolean)

    @Serializable
    data class Row(val state: String, val vals: Array<String?>, val oldVals: Array<OldValue?>)

    @Serializable
    data class OldValue(val value: String?)
}

object DataTableConv {

    fun deserialize(content: String): DataTable {
        val tab = DataTable()
        deserialize(content, tab)
        return tab
    }

    private fun deserialize(content: String, table: DataTable) {
        val dts = Json.parse(DataTableSer.serializer(), content)
        table.name = dts.name
        table.primaryKey = dts.primaryKey
        val deserCols = dts.cols.map { Converters.getSerializerFor(it.kClassName) }
        dts.cols.forEachIndexed { idx, it -> table.Columns.add(DataTable.Column(it.name, deserCols[idx].kclass, it.autoIncrement, it.isDummy)) }
        dts.rows.forEach { deserializeRow(table.Rows, it, deserCols) }
        dts.deletedRows.forEach { deserializeRow(table.DeletedRows, it, deserCols) }
    }


    fun deserializeRow(rowCollection: DataTable.RowCollection<out DataTable.Row>, row: DataTableSer.Row, types: List<TypeConverter>) {
        val values = row.vals.mapIndexed { idx, it -> if (it == null) null else types[idx].deser(it) }.toTypedArray()
        val nr = rowCollection.add(*values).apply { state = RowState.valueOf(row.state) }
        row.oldVals.forEachIndexed { idx, it -> nr.oldVals.add(if (it == null) null else DataTable.OldValue(if (it.value == null) null else types[idx].deser(it.value))) }
    }

    fun serialize(table: DataTable, full: Boolean = true): String {
        val serCols = table.Columns.map { Converters.getSerializerFor(it.kClass!!.simpleName!!) }
        val dts = DataTableSer(
                name = table.name
                , primaryKey = table.primaryKey
                , cols = table.Columns.map { DataTableSer.Column(it.name, it.kClass?.simpleName.orEmpty(), it.autoIncrement, it.isDummy) }
                , rows = table.Rows.filter { if (full) true else it.state != RowState.DEFAULT }.map { serializeRow(it, serCols) }
                , deletedRows = table.DeletedRows.map { serializeRow(it, serCols) }
        )
        val result = Json.indented.stringify(DataTableSer.serializer(), dts)
        //println(result)
        return result
    }

    private fun serializeRow(row: DataTable.Row, types: List<TypeConverter>): DataTableSer.Row {
        return DataTableSer.Row(state = row.state.name
                , vals = row.vals.mapIndexed { idx, it -> if (it == null) null else types[idx].ser(it) }.toTypedArray()
                , oldVals = row.oldVals.mapIndexed { idx, it -> if (it == null) null else DataTableSer.OldValue(if (it.value == null) null else types[idx].ser(it.value)) }.toTypedArray())
    }


    fun <R : DataTable.Row> deserializeTyped(content: String, rowGen: () -> R): DataTableTyped<R> {
        val tab = DataTableTyped(rowGen)
        deserialize(content, tab)
        return tab
    }


}