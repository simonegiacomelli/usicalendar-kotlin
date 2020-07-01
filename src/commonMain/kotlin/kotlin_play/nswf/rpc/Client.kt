package kotlin_play.nswf.rpc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.serializer


val http_param_serialized_request = "serialized_request"

interface HasResponse<Res>

val json = Json(JsonConfiguration.Stable)

open abstract class Client {
    @kotlinx.serialization.ImplicitReflectionSerializer
    suspend inline fun <reified Req : HasResponse<Res>, reified Res : Any> New(request: Req): Res {
        val serialized_request = json.stringify(Req::class.serializer(), request)
        val responseStr = send_remote_for_processing(Req::class.simpleName!!, serialized_request)
        return json.parse(Res::class.serializer(), responseStr)
    }

    abstract suspend fun send_remote_for_processing(simpleName: String, serializedRequest: String): String
}