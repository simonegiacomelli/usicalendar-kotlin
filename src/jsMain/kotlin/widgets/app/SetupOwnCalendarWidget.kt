package widgets.app

import api.PagedResult
import api.QueryCalendars
import api.kind_ux
import com.soywiz.klock.DateTime
import db4common.DataTableTyped
import fragment.*
import nswf.Delegate
import org.w3c.dom.*
import starter2.Api
import widgets.components.PagedTableWidget
import kotlin.browser.window

private val init = ResourceManager.reg { SetupOwnCalendarWidget() }

class SetupOwnCalendarWidget() : ResourceWidget() {
    private lateinit var table: DataTableTyped<QueryCalendars.cl_calendar>
    val L by logger()

    val pagedTable: PagedTableWidget by docu
    val rowTemplate: HTMLTemplateElement by docu

    inner class TableRow : TemplateWidget(rowTemplate) {
        val fldsummary: HTMLElement by docu
        val fldwhen: HTMLElement by docu
        val fldstartdate: HTMLSpanElement by docu
        val fldstarttime: HTMLSpanElement by docu
        val fldlocation: HTMLSpanElement by docu
        val cbenable: HTMLInputElement by docu
        val cblabel: HTMLLabelElement by docu
        val fldswitch: HTMLDivElement by docu
        val linkMore: HTMLAnchorElement by docu
    }

    val tbFilter: HTMLInputElement by docu
    val btnFilter: HTMLElement by docu
    val linkFirst: HTMLAnchorElement by docu
    val linkRequest: HTMLAnchorElement by docu
    val linkHowto: HTMLAnchorElement by docu

    init {
        afterRender {
            Hotkey(tbFilter).add("ENTER", ::send_request)
            tbFilter.addEventListener("keyup", { refreshTable() }, true)
            btnFilter.onclickExt { tbFilter.value = ""; refreshTable() }
            pagedTable.leftInfoFun = { it, pn ->
                ("${it.recordCount} course${if (it.recordCount == 1) "" else "s"}") + " found" +
                        if (it.pageCount > 1) ", page $pn/${it.pageCount}" else ""
            }
            no_pagination()
            gui_can_create_token()
            send_request()
            linkFirst.onclickExt { ask_create_calendar() }
            linkHowto.onclickExt { app.calendarCodesWidget.show() }
            Api.apiDbLogAddOnClickInstrument(this, linkHowto)
        }
        beforeShow {

        }
        afterShow {
            if (!SettingsWidget.cbTokenStor)
                gui_can_create_token()
            else
                token_is_present()
        }
    }

    val token_exists get() = SettingsWidget.cbTokenStor
    private fun gui_can_create_token() {

        linkHowto.visible = token_exists
        linkFirst.visible = !token_exists
        linkRequest.visible = false

    }

    private fun ask_create_calendar() {
        app.confirm(
            "New calendar",
            "This is the first time this application is used on this device\n" +
                    "If you already have saved a calendar you should stop and recover it\n\n" +
                    "Otherwise, do you want me to create a new calendar?"
        )
            .apply {
                btnSave.innerText = "Yes"
                btnClose.innerText = "Cancel"
                onSave = { create_new_calendar() }
            }.show()
    }

    private fun create_new_calendar() {
        linkFirst.visible = false
        linkRequest.visible = true
        Api.apiCreateToken.new {
            val n = window.navigator
            it.info = arrayOf(n.platform, n.vendor, n.product, n.userAgent).joinToString("||")
        }.call {
            SettingsWidget.tbTokenStor = it.token
            SettingsWidget.tbFriendlyNameStor = it.friendlyName
            SettingsWidget.tbDtCreationStor = it.dtCreation
            SettingsWidget.tbDtCreationStrStor = it.dtCreation.toString("dd MMM HH:mm")
            SettingsWidget.cbTokenStor = true
            token_is_present()
            app.alert("Success!", "The new calendar is ready\n\nYou can now select the courses you want")
        }
    }

    private fun token_is_present() {
        linkRequest.visible = false
        linkHowto.visible = true
        app.headerWidget.howto.visible = true
    }

    private fun send_request() {
        no_pagination()
        addLoadingRow()
        Api.apiQueryCalendars.new {
            it.filter = ""
        }.call {
            this.table = it.tab
            refreshTable()
        }
    }

    private fun refreshTable() {

        val result = PagedResult(
            Delegate(
                mutableMapOf(Pair("pageCount", "1"), Pair("recordCount", "${table.Rows.size}"))
            )
        )

        pagedTable.updatePages(result)
        no_pagination()

        val rows = table.Rows
        rows.filter {
            it.summary.contains(tbFilter.value, ignoreCase = true)
                    || it.location.contains(tbFilter.value, ignoreCase = true)
        }.filter {
            it.dateEnd > DateTime.now().local
        }
            .sortedWith(compareBy({ !it.active }, { it.dateStart }))
            .forEach { row ->


                val htmlrow = TableRow()
                val summary = row.summary
                htmlrow.fldsummary.innerText = summary
                htmlrow.fldsummary.onclickExt { details(row) }
                htmlrow.fldwhen.onclickExt { details(row) }
//                htmlrow.linkMore.onclickExt { details(row) }
                htmlrow.linkMore.visible = false
                Api.apiDbLogAddOnClickInstrument(this, htmlrow.linkMore, row.summary)
                htmlrow.fldstartdate.innerText = row.dateStart.toString("EEE dd")
                htmlrow.fldstarttime.innerText = row.dateStart.toString("HH:mm")
                htmlrow.fldlocation.innerText = row.location.take(7)
                htmlrow.cblabel.htmlFor = htmlrow.cbenable.id
                htmlrow.cbenable.checked = row.active
                htmlrow.cbenable.addEventListener("click", {
                    if (!token_exists) {
                        htmlrow.cbenable.checked = false
                        ask_create_calendar()
                        return@addEventListener
                    }
                    val active = htmlrow.cbenable.checked
                    row.active = active
                    app.headerWidget.saving_increment()
                    Api.apiCourseStatus.new {
                        it.courseSummary = summary
                        it.active = active
                    }.call {
                        app.headerWidget.saving_decrement()
                        LogWidget.log("course [$summary] $active")
                    }
                }, true)

                pagedTable.appendRow(htmlrow)
            }
    }

    private fun details(row: QueryCalendars.cl_calendar) {
        app.create(CourseDetailWidget(row)).show()
    }

    private fun no_pagination() {
        pagedTable.leftInfo.visible = false
        pagedTable.gotoPage.visible = false
        pagedTable.pager.visible = false
        pagedTable.clearRows()
    }

    private fun addLoadingRow() {
        val htmlrow = TableRow()
        htmlrow.fldsummary.innerText = "loading courses list..."
        htmlrow.fldswitch.visible = false
        htmlrow.linkMore.visible = false
        pagedTable.appendRow(htmlrow)
    }


}



