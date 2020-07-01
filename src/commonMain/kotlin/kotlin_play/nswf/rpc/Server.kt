package kotlin_play.nswf.rpc

import kotlinx.serialization.serializer

class Server {
    protected val handlers = mutableMapOf<String, (String) -> String>()
    @kotlinx.serialization.ImplicitReflectionSerializer
    inline fun <reified Req : HasResponse<Res>, reified Res : Any> register_server_handler(crossinline handler: (req: Req) -> Res) {
        val h: (String) -> String = { serialized_request ->
            val request = json.parse(Req::class.serializer(), serialized_request)
            val response = handler(request)
            val serialized_response = json.stringify(Res::class.serializer(), response)
            serialized_response
        }
        handlers[Req::class.simpleName!!] = h
    }

    fun invoke(requestClassName: String, serialized_request: String): String {
        return handlers[requestClassName]!!(serialized_request)
    }
}