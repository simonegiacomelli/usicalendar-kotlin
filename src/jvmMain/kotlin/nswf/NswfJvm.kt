package nswf

import api.Configuration
import db4common.DataTable
import java.net.URLEncoder

abstract class Context() {
    abstract val uri: String
    abstract val params: Map<String, String>
    abstract val postParams: Map<String, String>
    var exception: Throwable? = null
    val response by lazy { mutableMapOf<String, String>() }
    abstract val responseDone: Boolean

    abstract suspend fun respond(content: String)

    fun responseToString(): String {

        val tab = DataTable()
        tab.Columns.add("key", String::class)
        tab.Columns.add("value", String::class)
        response.entries.forEach { tab.Rows.add(it.key, URLEncoder.encode(it.value, "UTF-8")) }
        tab.acceptChanges()
        return DataTableConv.serialize(tab)
    }

}

class Options(val parsePostParams: Boolean = true)

open class NswfServer<CTX : Context>() {
    val L by logger()

    val NOT_SPECIFIED: suspend (CTX) -> Boolean = { throw Exception("should not be used") }
    var preApi = NOT_SPECIFIED
    var afterServe: (CTX) -> Unit = {}
    val handlers: MutableMap<String, NswfApi<Any, ApiRequest<Any>>> = mutableMapOf()

    inner class NswfApi<Q, T : ApiRequest<Q>>(
        val requestObjCreator: (Delegate) -> T,
        val act: suspend (CTX, T, Q) -> Unit,
        val opt: Options,
        val preApi: suspend ((CTX) -> Boolean)
    )


    inline fun <Q, reified T : ApiRequest<Q>> api(
        noinline requestObjCreator: (Delegate) -> T,
        opt: Options = Options(),
        noinline preApi: suspend ((CTX) -> Boolean) = NOT_SPECIFIED,
        noinline act: suspend (CTX, T, Q) -> Unit
    ) {
        val simpleName = "/" + Configuration.apiPrefixUrl + "/" + T::class.simpleName!!
        L.debug("registering $simpleName")
        handlers[simpleName.toLowerCase()] =
            NswfApi(requestObjCreator, act, opt, preApi) as NswfApi<Any, ApiRequest<Any>>
    }


    suspend fun serve(ctx: CTX): Boolean {
        val key = ctx.uri.toLowerCase()
        val handler = handlers.get(key) ?: return false
        try {
            val selectedPreApi = if (handler.preApi != NOT_SPECIFIED) handler.preApi else preApi
            if (selectedPreApi != NOT_SPECIFIED && !selectedPreApi(ctx)) return true
            val getProp = ctx.params.toMutableMap()
            val postProp = if (handler.opt.parsePostParams) ctx.postParams.toMutableMap() else getProp
            val requestDelegate = Delegate(getProp, postProp)
            val gen = handler.requestObjCreator(requestDelegate)
            val res = gen.res(ctx.response.toSafeNullDelegate())
            handler.act(ctx, gen, res)
            if (!ctx.responseDone) ctx.respond(ctx.responseToString())
        } catch (ex: Throwable) {
            ctx.exception = ex
            L.error("serve", ex)
        } finally {
            afterServe(ctx)
        }
        return true
    }
}

//actual fun debugger() {
//    println("breakpoint?")
//}
