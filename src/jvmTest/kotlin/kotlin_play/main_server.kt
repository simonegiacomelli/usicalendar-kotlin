package kotlin_play

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin_play.nswf.rpc.Server
import kotlin_play.nswf.rpc.http_param_serialized_request


fun main() {
    val sh = Server()

    sh.register_server_handler<LoginRequest, LoginResponse>() {
        LoginResponse(true, "hello ${it.username}")
    }

    sh.register_server_handler<QuerySchedule, QueryScheduleResponse> {
        val ext = it.date.format("EEE")
        QueryScheduleResponse(
            listOf(
                Schedule(name = "${it.spec}1 $ext", counter = 1),
                Schedule(name = "${it.spec}2", counter = 2)
            )
        )
    }
    val server = embeddedServer(Netty, 8080) {
        routing {
            get("/api/{class_name}") {
                val class_name = call.parameters["class_name"]!!
                val serialized_request = call.request.queryParameters[http_param_serialized_request]!!
                val serialized_response = sh.invoke(class_name, serialized_request)
                call.respondText(serialized_response, ContentType.Text.Plain)
            }
        }
    }
    server.start(wait = false)
}

