package api

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import db4common.*

import nswf.ApiRequest
import nswf.Delegate

object Configuration {
    val port = 8080
    val apiPrefixUrl = "usicalendar/api"
}

open class PagedResult(del: Delegate) {
    var pageCount: Int by del.int
    var recordCount: Int by del.int
}

open class PagedRequest(del: Delegate) {
    var pageNumber: Int by del.int
    var rowsPerPage: Int by del.int
    var order: String by del.string

    fun allRows() {
        pageNumber = 1
        rowsPerPage = 10000
    }

    fun noRows() {
        pageNumber = 1
        rowsPerPage = 0
    }
}

open class rm_base : TypedRow() {
    //var created_at: java.sql.Timestamp by notNullVar
    //var updated_at: java.sql.Timestamp by notNullVar
}

class rm_rack : rm_base() {
    var id: Int by notNullVar
    var description: String? by nullVar
    var unit_height: Int? by nullVar
}

open class BbxToken : TypedRow() {
    var id: String by notNullVar
    val token: String
        get() = id
    var idSocieta: String? by nullVar
    var idCollaboratore: Int? by nullVar
    var idUtente: Int? by nullVar
    var dsData: String? by nullVar
}

class CreateToken(del: Delegate) : ApiRequest<CreateToken.Result> {
    var info: String by del.string
    override val res = CreateToken::Result

    class Result(del: Delegate) {
        var success: Boolean by del.boolean
        var token: String by del.string
        var friendlyName: String by del.string
        var dtCreation: DateTimeTz by del.datetimeTz
    }
}

class Login(del: Delegate) : ApiRequest<Login.Result> {
    var username: String by del.string
    var password: String by del.string
    override val res = ::Result

    class Result(del: Delegate) {

        var success: Boolean by del.boolean
        var tempResult: String by del.string
    }
}

class CourseStatus(del: Delegate) : ApiRequest<CourseStatus.Result> {
    var courseSummary: String by del.string
    var active: Boolean by del.boolean
    override val res = ::Result

    class Result(del: Delegate)
}

class EmptyResult(del: Delegate) {
    var success: String by del.string
}

class DbLog(del: Delegate) : ApiRequest<EmptyResult> {
    class Result(del: Delegate)

    var kind: String by del.string
    var info: String by del.string
    var info2: String by del.string
    var instance: String by del.string
    override val res = ::EmptyResult
}

class AppLogInspect(del: Delegate) : ApiRequest<EmptyResult> {
    override val res = ::EmptyResult
    var tab: DataTable by del.dataTable
}

class QueryCalendars(del: Delegate) : ApiRequest<QueryCalendars.Result> {
    var filter: String by del.string
    override val res = ::Result

    class Result(del: Delegate) {
        var tab: DataTableTyped<cl_calendar> by del.table { cl_calendar() }
    }

    open class cl_calendar : TypedRow() {
        var summary: String by notNullVar
        var location: String by notNullVar
        var dateStart: DateTimeTz by notNullVar
        var dateEnd: DateTimeTz by notNullVar
        var url: String by notNullVar
        var active: Boolean by notNullVar

        companion object {
            fun new(): DataTableTyped<cl_calendar> {
                val tab = DataTableTyped { cl_calendar() }
                tab.Columns.add("summary", String::class)
                tab.Columns.add("location", String::class)
                tab.Columns.add("dateStart", DateTimeTz::class)
                tab.Columns.add("dateEnd", DateTimeTz::class)
                tab.Columns.add("url", String::class)
                tab.Columns.add("active", Boolean::class)
                return tab
            }
        }
    }
}

class RackAdd(del: Delegate) : ApiRequest<RackAdd.Result> {
    var description: String by del.string
    var unit_height: String by del.string

    var table: DataTableTyped<rm_rack> by del.table { rm_rack() }

    override val res = RackAdd::Result

    class Result(del: Delegate) {
        var success: String by del.string
    }
}


class RackEdit(del: Delegate) : ApiRequest<RackEdit.Result> {
    var rackid: String by del.string
    var description: String by del.string
    var unit_height: String by del.string

    override val res = RackEdit::Result

    class Result(del: Delegate) {
        var success: String by del.string
    }
}

class RackList(del: Delegate) : ApiRequest<RackList.Result> {
    var limit: Int by del.int

    override val res = RackList::Result

    class Result(del: Delegate) {
        var success: String by del.string
        //var table by del.del.dataTableTyped { rm_rack() }
        var table: DataTableTyped<rm_rack> by del.table { rm_rack() }
        //var table by del.DataTableDel
    }
}

class SqlQuery(del: Delegate) : ApiRequest<SqlQuery.Result> {
    var sql: String by del.string

    override val res = ::Result

    class Result(del: Delegate) {
        var success: String by del.string
        var table: DataTable by del.dataTable
    }
}

class ListaFatture(del: Delegate) : ApiRequest<ListaFatture.Result> {

    override val res = ::Result

    class Result(del: Delegate) {
        //var table by del.del.dataTableTyped { ListaFatturaRow() }
        var table: DataTableTyped<ListaFatturaRow> by del.table { ListaFatturaRow() }
    }

    class ListaFatturaRow : TypedRow() {
        var nr: Int by notNullVar
        var dt: DateTime by notNullVar
    }
}

class DocsUtente(del: Delegate) : ApiRequest<DocsUtente.Result> {

    override val res = ::Result

    class Result(del: Delegate) {
        var table: DataTable by del.dataTable
    }
}

class DocsUtenteDownload(del: Delegate) : ApiRequest<DocsUtenteDownload.Result> {
    var path: String by del.string
    override val res = ::Result

    class Result(del: Delegate)
}

class PdfFattura(del: Delegate) : ApiRequest<PdfFattura.Result> {

    var nr: Int by del.int
    var dt: DateTime by del.datetime

    override val res = ::Result

    class Result(del: Delegate)

}

open class AuthToken : TypedRow() {
    var id: String by notNullVar
    var dt: DateTimeTz by notNullVar
}