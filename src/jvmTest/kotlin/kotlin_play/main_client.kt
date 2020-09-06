package kotlin_play

import kotlin_play.nswf.rpc.Client
import kotlin_play.nswf.rpc.http_param_serialized_request
import kotlinx.serialization.ImplicitReflectionSerializer
import java.net.URL
import java.net.URLEncoder


@ImplicitReflectionSerializer
suspend fun main() {
    val r = HttpNswfRequest().New(LoginRequest("foo", "bar"))
    println("response is ${r}")

    val sc = HttpNswfRequest().New(QuerySchedule("simo")).list
    println(sc)

}

class HttpNswfRequest : Client() {
    override suspend fun send_remote_for_processing(simpleName: String, serializedRequest: String): String {
        val url = URL(
            "http://localhost:8080/api/$simpleName?$http_param_serialized_request="
                    + URLEncoder.encode(serializedRequest, "UTF-8")
        )
        return url.readText()
    }

}
