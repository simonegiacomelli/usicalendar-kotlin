package kotlin_play.nswf.rpc

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.parse
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = DateTimeTz::class)
object DateTimeTzSerializer : KSerializer<DateTimeTz> {
    val df = DateFormat("yyyy-MM-dd HH:mm:ss z")

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: DateTimeTz) {
        encoder.encodeString(df.format(obj))
    }

    override fun deserialize(decoder: Decoder): DateTimeTz {
        return df.parse(decoder.decodeString())
    }
}