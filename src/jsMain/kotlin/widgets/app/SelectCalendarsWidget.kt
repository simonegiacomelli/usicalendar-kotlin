package widgets.app

import api.PagedResult
import fragment.*
import nswf.Delegate
import org.w3c.dom.*
import widgets.components.PagedTableWidget

private val init = ResourceManager.reg { SetupOwnCalendarWidget() }

class SelectCalendarsWidget() : ResourceWidget() {
    val L by logger()

    inner class TableRow : TemplateWidget(rowTemplate) {
        val fld_ip: HTMLTableCellElement by docu
        val fld_device_description: HTMLElement by docu


        val edit: HTMLAnchorElement by docu
        val deleteAss: HTMLElement by docu
        val delete: HTMLElement by docu

        val tdDevice: HTMLElement by docu
        val divDevice: HTMLElement by docu
        val editDevice: HTMLElement by docu
    }


    val pagedTable: PagedTableWidget by docu
    val rowTemplate: HTMLTemplateElement by docu
    val btnAdd: HTMLElement by docu

    override fun beforeShow() {
        btnAdd.visible = false
        pagedTable.loadTable = { loadTable() }
        pagedTable.load()
    }

    private fun loadTable() {

        val result = PagedResult(
            Delegate(
                mutableMapOf<String, String>(
                    Pair("pageCount", "4"), Pair("recordCount", "100")
                )
            )
        )
        pagedTable.updatePages(result)
        pagedTable.clearRows()


        val ro = TableRow()
        ro.fld_ip.innerText = "first row fake"

        ro.fld_device_description.innerText = "descriptionoooo "

        //ro.delete.visible = false
        ro.delete.onclickExt { alert("delete ?" + ro.fld_device_description.innerHTML) }

        ro.elementInstance.firstElement.ondblclick = ro.edit.onclick

        pagedTable.appendRow(ro)

    }

    private fun alert(msg: String) {
        app.alert("Rack", msg)
    }


}



