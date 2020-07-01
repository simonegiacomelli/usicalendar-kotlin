package nswf

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import db4common.DataTable
import db4common.DataTableTyped
import db4common.TypedRow
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


interface ApiRequest<T> {
    val res: (Delegate) -> T
}

fun MutableMap<String, String>.toSafeNullDelegate() = Delegate(this)

class Delegate(
    private val getProp: MutableMap<String, String>,
    private val postProp: MutableMap<String, String> = getProp
) {

    companion object {
        val dataTableConv =
            Converter(ser = { DataTableConv.serialize(it as DataTable) }, deser = { DataTableConv.deserialize(it) })

    }

    class GenDelegate(val converter: Converter, val prop: MutableMap<String, String>) {
        operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
            val value = prop[property.name] ?: throw Exception("No value for ${property.name} map=$prop")
            return converter.deser(value) as T
        }

        operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val ser: String = converter.ser(value as Any)
            prop[property.name] = ser
        }
    }

    class GenDelegate3(val ser: Converter, val prop: MutableMap<String, String>) {
        inline operator fun <reified T : Any> getValue(thisRef: Any?, property: KProperty<*>): T {
            val value = prop[property.name]
            return ser.deser(value!!) as T
        }

        inline operator fun <reified T : Any> setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            prop[property.name] = ser.ser(value)
        }

    }

    val post = Fast(postProp)
    val get = Fast(getProp)

    val int = get.int
    val boolean = get.boolean
    val string = get.string
    val long = get.long
    val datetime = get.datetime
    val datetimeTz = get.datetimeTz
    val dataTable = get.dataTable


    class Fast(val prop: MutableMap<String, String>) {
        private fun conv(kClass: KClass<*>): GenDelegate3 {
            val ser = Converters.getSerializerFor(kClass)
            return GenDelegate3(ser.converter, prop)
        }

        val int = conv(Int::class)
        val boolean = conv(Boolean::class)
        val string = conv(String::class)
        val long = conv(Long::class)
        val datetime = conv(DateTime::class)
        val datetimeTz = conv(DateTimeTz::class)
        val dataTable = GenDelegate(dataTableConv, prop)

    }

    fun <R : TypedRow, T : DataTableTyped<R>> table(gen: () -> R): GenDelegate {
        val dtConv = Converter(ser = { DataTableConv.serialize(it as T) },
            deser = { DataTableConv.deserializeTyped(it, gen) as T })
        return GenDelegate(dtConv, postProp)
    }


    fun <R : TypedRow, T : DataTableTyped<R>> tableOnlyChanges(gen: () -> R): GenDelegate {
        val dtConv = Converter(ser = { DataTableConv.serialize(it as T, full = false) },
            deser = { DataTableConv.deserializeTyped(it, gen) as T })
        return GenDelegate(dtConv, postProp)
    }


}

//expect fun debugger()