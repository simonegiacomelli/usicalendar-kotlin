package nswf

import fragment.HttpRequestDebug
import kotlinx.coroutines.*
import nswf_utils.logException

class NswfClient(val makeUrl: (api: String) -> String) {
    val L by logger()

    var completionHandler: CompletionHandler? = null

    inline fun <Q, reified T : ApiRequest<Q>> api(noinline gen: (Delegate) -> T): NswfApi<Q, T> {
        val apiName = T::class.simpleName!!
        L.debug("registering $apiName")
        return NswfApi(gen, apiName)
    }

    inner class NswfApi<Q, out T : ApiRequest<Q>>(private val gen: (Delegate) -> T, private val apiName: String) {

        fun new(debug: Boolean = false, fill: (T) -> Unit = {}): Callback<Q, T> {
            val getProp = mutableMapOf<String, String>()
            val postProp = mutableMapOf<String, String>()
            val request: T = gen(Delegate(getProp, postProp))
            fill(request)
            return Callback(debug, request, getProp, postProp, apiName)
        }

    }

    inner class Callback<Q, out T : ApiRequest<Q>>(
        val debug: Boolean, val request: T
        , val getProp: MutableMap<String, String>
        , val postProp: MutableMap<String, String>
        , val apiName: String
    ) {

        val url by lazy { appendParams(getProp, StringBuilder(makeUrl(apiName))).toString() }

        private fun appendParams(
            values: MutableMap<String, String>,
            sb: StringBuilder = StringBuilder()
        ): StringBuilder {
            values.entries.forEach {
                if (debug) L.debug("${it.key}=${it.value}")
                sb.appendParams(it.key, it.value)
            }
            return sb
        }


        val defaultRequest: suspend (Callback<*, *>) -> String = {
            val string = if (postProp.isEmpty())
                HttpRequestDebug.getString(url).await()
            else {
                val postString = appendParams(postProp).toString()
                HttpRequestDebug.post(url, postString).await()
            }
            string
        }

        fun call(doRequest: suspend (Callback<Q, out T>) -> String = defaultRequest, result: (Q) -> Unit): Deferred<Q> {
            if (debug) L.debug("calling $url")
            val deferred = GlobalScope.async {
                val string = doRequest(this@Callback)
                if (debug) L.debug("response [$string]")
                val response = dataTableToMap(string)
                val del = response.toSafeNullDelegate()
                val res = request.res(del)
                result(res)
                res
            }.logException("url was=[${url}]")

            if (completionHandler != null)
                deferred.invokeOnCompletion { completionHandler?.invoke(it) }
            return deferred
        }
    }

}

/*
PHP FORWARDER

https://developer.explorerservizi.it/es4dmin/addonmodules.php?module=addonmodule&action=ajax&proc=fw&url=/api2/RackList?rowsPerPage=10&pageNumber=1

 function fw($vars) {

        $qs = $_SERVER['QUERY_STRING'];
        $pos = strpos($qs, "url=") + 4;
        $urlamp = substr($qs, $pos);
        $url = str_replace("&amp;", "&", $urlamp);

        $full_url = "http://localhost:8084" . $url;

        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            $entityBody = file_get_contents('php://input');
            $opts = array('http' =>
                array(
                    'method' => 'POST',
                    'header' => 'Content-type: application/x-www-form-urlencoded',
                    'content' => $entityBody
                )
            );

            $context = stream_context_create($opts);
            $response = file_get_contents($full_url, false, $context);
        } else {
            $response = file_get_contents($full_url);
        }

        return $response;
    }


 */


private fun dataTableToMap(it: String): MutableMap<String, String> {
    val tab = DataTableConv.deserialize(it)
    val response = mutableMapOf<String, String>()
    tab.Rows.forEach {
        val get: String = it.get<String>(0)!!
        val encodedURI = it.get<String>(1)!!.replace("+", "%20")
        val value = decodeURIComponent(encodedURI)
        //println("row print, encoded = [$encodedURI] decoded = [$value]")
        response.set(get, value)
    }
    return response
}

fun StringBuilder.appendParams(key: String, value: String) {
    val n = encodeURIComponent(key)
    val v = encodeURIComponent(value)
    append("&$n=$v")
}

//actual fun debugger() {
//    js("debugger;")
//}
