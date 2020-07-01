package widgets.components

import db4common.DataTable
import fragment.LocalStorage
import fragment.ResourceWidget
import fragment.firstRecursive
import nswf_utils.cloneElement
import org.w3c.dom.*
import starter2.Api
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

class TableWidget(var overrideSql: String = "") : ResourceWidget() {

    val thTemplate: HTMLTemplateElement by docu
    val rowTemplate: HTMLTemplateElement by docu
    val taSql: HTMLTextAreaElement by docu
    val btnSql: HTMLButtonElement by docu

    val table: HTMLTableElement by docu

    val selectedRows = mutableListOf<HTMLTableRowElement>()
    var sql: String by LocalStorage("app.tablewidget.sql", "")


    companion object {
        var counter = 0
    }

    override fun afterRender() {
        taSql.value = if (sql.isNullOrBlank()) "select id,calendar,dt_creation,kind, cast(info as varchar(1000)) as info,instance FROM applogs order by id desc" else sql
        btnSql.onclick = {
            runQuery()
            0
        }
        runQuery()
    }

    private fun runQuery() {
        sql = taSql.value
        if (overrideSql.isNotBlank()) {
            taSql.value = overrideSql
            overrideSql = ""
        }
        Api.apiSqlQuery.new {
            it.sql = taSql.value
        }.call {
            val dataTable = it.table

            while (table.rows.length > 1)
                table.deleteRow(table.rows.length - 1)
            val header = table.tHead!!.rows[0]!!
            header.innerHTML = ""
            dataTable.Columns.forEach {
                header.appendChild(document.createElement("th").apply { innerHTML = it.name })
            }


            dataTable.Rows.forEach {
                val row = rowTemplate.cloneElement()
                row.onclick = {
                    row.addClass("table-primary")
//                    while (selectedRows.isNotEmpty())
//                        selectedRows.removeAt(0).removeClass("table-primary")
//                    selectedRows.add(row.firstRecursive("TR"))
                }
                for (value in it.vals) {
                    row.appendChild(document.createElement("td").apply { innerHTML = "$value" })
                }
                table.tBodies[0]!!.appendChild(row)
            }
        }
    }

    private fun testDataTable(): DataTable {
        val dataTable = DataTable()
        dataTable.Columns.add("col1", String::class)
        dataTable.Columns.add("col2", String::class)
        dataTable.Columns.add("col3", String::class)

        (1..10).forEach {
            counter++
            dataTable.Rows.add("val$counter", "xxx$counter", "zzz$counter")
        }
        return dataTable
    }
}