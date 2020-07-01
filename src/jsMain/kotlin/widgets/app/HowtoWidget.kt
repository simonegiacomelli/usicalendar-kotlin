package widgets.app

import fragment.*
import nswf_utils.encodeURIComponent
import org.w3c.dom.*
import kotlin.browser.window

private val init = ResourceManager.reg { HowtoWidget() }

class HowtoWidget : ResourceWidget() {

    val linkMailto: HTMLAnchorElement by docu
    val linkWebcal: HTMLAnchorElement by docu
    val taEmail: HTMLTextAreaElement by docu
    val httpxUrl: HTMLInputElement by docu

    var original = ""

    override fun afterRender() {
        original = taEmail.value
        httpxUrl.onfocus = {
            httpxUrl.select()
            true
        }
    }
    init {

        beforeShow {
            val tbTokenStor = SettingsWidget.tbTokenStor
            val ical_url = window.location.host + "/usicalendar/usicalendar?token=" + tbTokenStor
            val webcal_url = "webcal://" + ical_url
            val proto = window.location.protocol
            linkWebcal.href = webcal_url
            httpxUrl.value = "$proto//$ical_url"


            val host = window.location.host
            val edit_url = "$proto//$host/usicalendar?#token=$tbTokenStor"

            val taText = original.replace("\r", "")
                .replace("[webcal_url]", webcal_url)
                .replace("[edit_url]", edit_url)
                .replace("[friendly_name]", SettingsWidget.tbFriendlyNameStor)
                .replace("[creation_date]", SettingsWidget.tbDtCreationStrStor)
            taEmail.value = taText
            val lines = taText.splitToSequence("\n").toList()
            taEmail.rows = lines.size + 3
            val subject = lines[0]
            val body = lines.drop(1).joinToString(separator = "\n")


            linkMailto.href = "mailto:?subject=" + encodeURIComponent(subject) +
                    "&body=" + encodeURIComponent(body)


        }
    }
}
