package fragment

import org.w3c.dom.get
import kotlin.browser.localStorage
import kotlin.reflect.KProperty

class LocalStorage<T>(val name: String, val def: T) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val content = localStorage[name]
        return if (content == null) def else {
            try {
                //println("$name=[$content]")
                JSON.parse<T>(content)
            } catch (e: Exception) {
                def
            }
        }
    }

    operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        localStorage.setItem(name, JSON.stringify(value))
    }


}
