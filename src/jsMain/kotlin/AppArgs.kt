import nswf.logger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AppArgs(content: String) : ReadOnlyProperty<AppArgs, String> {
    val L by logger()
    val map = mutableMapOf<String, String>()

    init {
        content.split("&").filter { it.isNotBlank() }
            .forEach {
                L.debug("line = $it")
                val parts = it.split("=", limit = 2)
                map[parts[0]] = if (parts.size > 1) parts[1] else "1"
            }
    }

    override fun getValue(thisRef: AppArgs, property: KProperty<*>): String = map[property.name].orEmpty()

    val token by this
    val isAdmin get() = !map["isAdmin"].isNullOrEmpty()
}