@file:UseSerializers(DateTimeTzSerializer::class)

package kotlin_play

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import kotlin_play.nswf.rpc.DateTimeTzSerializer
import kotlin_play.nswf.rpc.HasResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class LoginRequest(val username: String, val password: String) : HasResponse<LoginResponse>

@Serializable
data class LoginResponse(val success: Boolean, val message: String)


@Serializable
data class QuerySchedule(val spec: String, val date: DateTimeTz = DateTime.now().local) :
    HasResponse<QueryScheduleResponse>

@Serializable
data class Schedule(
    val name: String,
    var counter: Int
)

@Serializable
class QueryScheduleResponse(val list: List<Schedule>)
