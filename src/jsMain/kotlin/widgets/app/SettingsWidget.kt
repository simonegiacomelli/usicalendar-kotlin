package widgets.app

import com.soywiz.klock.DateTimeTz
import fragment.LocalStorage
import fragment.ResourceManager
import fragment.ResourceWidget
import nswf_utils.Properties
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val init = ResourceManager.reg { SettingsWidget() }

class SettingsWidget : ResourceWidget() {

    val cbUrl: HTMLInputElement by docu
    val tbUrl: HTMLInputElement by docu
    val cbToken: HTMLInputElement by docu
    val tbToken: HTMLInputElement by docu

    val taConf: HTMLTextAreaElement by docu


    companion object {
        var cbUrlStor: Boolean by LocalStorage("app.debug.url.enabled", false)
        var tbUrlStor: String by LocalStorage("app.debug.url", "")
        var cbTokenStor: Boolean by LocalStorage("app.token.enabled", false)
        var tbTokenStor: String by LocalStorage("app.token", "")
        var tbFriendlyNameStor: String by LocalStorage("app.friendly.name", "")
        var tbDtCreationStor: DateTimeTz by LocalStorage("app.creation.date", DateTimeTz.fromUnixLocal(0))
        var tbDtCreationStrStor: String by LocalStorage("app.creation.date.str", "")

        var taConfStor: String by LocalStorage("app.configuration", "")
        private fun fresh() = Properties().apply({ merge(taConfStor) })


        val readWriteProperty = object : ReadWriteProperty<Any, String> {
            override fun getValue(thisRef: Any, property: KProperty<*>): String {
                val get = fresh().get(property.name)
                if (get == null) setValue(thisRef, property, "")
                return get ?: ""
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
                taConfStor = fresh().apply { set(property.name, value) }.toString()
            }
        }

    }


    override fun afterShow() {
        cbUrl.checked = cbUrlStor
        tbUrl.value = tbUrlStor
        taConf.value = taConfStor

        cbUrl.onclick = {
            cbUrlStor = cbUrl.checked
            0
        }

        tbUrl.onkeydown = {
            tbUrlStor = tbUrl.value
            0
        }

        cbToken.checked = cbTokenStor
        tbToken.value = tbTokenStor
        cbToken.onclick = {
            cbTokenStor = cbToken.checked
            0
        }

        tbToken.onkeydown = {
            tbTokenStor = tbToken.value
            0
        }

        taConf.onkeydown = {
            taConfStor = taConf.value
            0
        }


    }
}
