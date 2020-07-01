package widgets.components

import api.PagedRequest
import api.PagedResult
import fragment.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.dom.addClass
import kotlin.dom.hasClass
import kotlin.dom.removeClass

private val init = ResourceManager.reg { PagedTableWidget() }

open class PagedTableWidget : ResourceWidget() {

    val L by logger()

    val tab: HTMLTableElement by docu

    val selectPage: HTMLSelectElement by docu
    val btnGoPage: HTMLInputElement by docu

    val leftInfo: HTMLElement by docu
    val rightInfo: HTMLElement by docu

    val prevPage: HTMLElement by docu
    val nextPage: HTMLElement by docu

    val tableHeader: HTMLTableRowElement? by docuNull

    private val tabLinkSearch: HTMLElement by docu
    private val tabSearch: HTMLElement by docu
    private val btnSearch: HTMLElement by docu
    private val searchHost: HTMLElement by docu
    private val searchGuest: HTMLElement? by docuNull

    private val divSearch: HTMLElement by docu

    var pageNumber = 1
    var totPageNumber = 1

    var loadTable = {}

    private val disabled = "disabled"

    val search = Search()

    val pager: HTMLElement by docu
    val gotoPage: HTMLElement by docu

    var showPagesControls: Boolean = true
        set(value) {
            pager.visible = value
            gotoPage.visible = value
            field = value
        }

    inner class Search {
        var onshow = {}
        var onhide = {}
        var onsearch = {
            pageNumber = 1
            load()
        }
    }

    override fun afterRender() {
        rightInfo.visible = false
        tableHeader?.also {
            clearRows(0)
            tab.appendChild(it)
        }
        searchGuest?.also {
            searchHost.append(it)
            divSearch.style.display = ""
        }

        tabLinkSearch.onclickExt {
            if (tabSearch.hasClass("active")) {
                tabSearch.removeClass("active")
                search.onhide()
            } else {
                search.onshow()
                tabSearch.addClass("active")
            }
        }
        btnSearch.onclickExt { search.onsearch() }

        btnGoPage.onclickExt {
            val x = selectPage.options[selectPage.selectedIndex]!! as HTMLOptionElement
            pageNumber = x.value.toInt()
            load()
        }
        fun changePage(event: Event, x: Int, ele: HTMLElement) {
            event.preventDefault()
            if (ele.hasClass(disabled))
                return
            println("change page $pageNumber + ($x)")
            pageNumber += x
            if (pageNumber < 1) pageNumber = 1
            if (pageNumber > totPageNumber) pageNumber = totPageNumber
            load()
        }
        prevPage.onclick = { changePage(it, -1, prevPage) }
        nextPage.onclick = { changePage(it, 1, nextPage) }
    }

    private fun leftInfoFunDef(it: PagedResult,pageNumber: Int) =
        ("${it.recordCount} record${if (it.recordCount == 1) "" else "s"}") + " found" +
                if (it.pageCount > 1) ", page $pageNumber/${it.pageCount}" else ""

    var leftInfoFun: (it: PagedResult,pageNumber:Int) -> String = ::leftInfoFunDef
    fun updatePages(it: PagedResult) {
        totPageNumber = it.pageCount
        set(prevPage, pageNumber != 1)
        set(nextPage, pageNumber < totPageNumber)
        leftInfo.innerHTML = leftInfoFun(it,pageNumber)
        rightInfo.visible = it.pageCount > 1


        selectPage.optionsRemoveAll()
        (1..it.pageCount).forEach { idx ->
            selectPage.optionsAdd { opt ->
                opt.innerHTML = "$idx"
                opt.value = "$idx"
                if (idx == pageNumber) opt.selected = true
            }
        }
    }

    private fun set(ele: HTMLElement, enable: Boolean) {
        if (enable)
            ele.removeClass(disabled)
        else
            ele.addClass(disabled)

    }

    fun clearRows() {
        clearRows(1)
    }

    private fun clearRows(index: Int) {
        while (tab.rows.length > index) tab.deleteRow(index)
    }

    fun setParam(it: PagedRequest) {
        it.pageNumber = pageNumber
        it.rowsPerPage = 2
    }


    fun load() {
        clearRows()
        leftInfo.innerHTML = "Loading..."
        loadTable()
    }

    fun appendRow(widget: ResourceWidget) {
        tab.append(widget.elementInstance.firstRecursive("TR"))
    }
}
