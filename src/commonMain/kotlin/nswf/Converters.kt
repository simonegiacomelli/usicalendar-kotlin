package nswf

import com.soywiz.klock.*
import kotlin.reflect.KClass

object Converters {
    val sdf = DateFormat("yyyy-MM-dd HH:mm:ss z")

    val klockDateTime = Converter(ser = { sdf.format(it as DateTime) }, deser = { sdf.parse(it).local })
    val klockDateTimeTz = Converter(ser = { sdf.format(it as DateTimeTz) }, deser = { sdf.parse(it) })
    val types = listOf(
        TypeConverter(String::class, Converter(ser = { "$it" }, deser = { it }))
        , TypeConverter(Int::class, Converter(ser = { "$it" }, deser = { it.toInt() }))
        , TypeConverter(Long::class, Converter(ser = { "$it" }, deser = { it.toLong() }))
        , TypeConverter(Boolean::class, Converter(ser = { "$it" }, deser = { it.toBoolean() }))
        , TypeConverter(DateTime::class, klockDateTime) /* 2018-07-29T07:41:06Z */
        , TypeConverter(DateTimeTz::class, klockDateTimeTz)
    )
    val supportedTypes =
        types.groupBy { it }.mapKeys { it.key.classSimpleName }.mapValues { it.value.first() }

    fun getSerializerFor(kClassName: String): TypeConverter = this.supportedTypes[kClassName]
        ?: throw Exception("Serializer not found for kClassName: ${kClassName}")//Types(String::class, ser = { "$it" }, deser = { it })

    fun getSerializerFor(kClass: KClass<*>): TypeConverter = getSerializerFor(kClass.simpleName!!)
}

class Converter(val ser: (Any) -> String, val deser: (String) -> Any)

class TypeConverter(val kclass: KClass<*>, val converter: Converter) {
    val classSimpleName = kclass.simpleName!!
    val ser = converter.ser
    val deser = converter.deser
}

