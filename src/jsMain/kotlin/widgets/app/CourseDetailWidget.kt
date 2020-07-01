package widgets.app


import api.QueryCalendars
import fragment.ResourceManager
import fragment.ResourceWidget
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLSpanElement
import starter2.Api

private val init = ResourceManager.reg(CourseDetailWidget::class)

class CourseDetailWidget(val cal: QueryCalendars.cl_calendar) : ResourceWidget() {
    val fldsummary: HTMLSpanElement by docu
    val fldwhen: HTMLSpanElement by docu
    val fldlink: HTMLAnchorElement by docu

    override fun afterRender() {
        fldsummary.innerText = cal.summary
        fldwhen.innerText = "Starts on " + cal.dateStart.toString("dd MMM HH:mm") + " ends at " +
                cal.dateEnd.toString("HH:mm")
        fldlink.href = cal.url
        fldlink.innerText = cal.url
        Api.apiDbLogAddOnClickInstrument(this, fldlink, cal.url)
    }
}
