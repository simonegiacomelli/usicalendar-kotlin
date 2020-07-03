package calendar

import biweekly.Biweekly
import biweekly.ICalVersion
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.io.text.ICalWriter
import biweekly.property.DateOrDateTimeProperty
import biweekly.property.ProductId
import biweekly.property.RefreshInterval
import biweekly.util.Duration
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.jvm.toDateTime
import java.io.StringWriter
import java.net.URL

class CalAggregator(val urlCache: AllUrlCache) {


    suspend fun aggregateAndFilter(filter: String): String {
        return aggregateAndFilter(filter.splitToSequence(",").toSet())
    }

    suspend fun aggregateAndFilter(filters: Set<String>): String {
        val cval = aggregateAndFilterCal(filters)

        val res = StringWriter()
        val iCalWriter = ICalWriter(res, ICalVersion.V2_0)
        iCalWriter.write(cval)
        iCalWriter.flush()


        return res.toString()
    }

    suspend fun aggregateAndFilterCal(filters: Set<String>): ICalendar {
        val eventSet: MutableSet<CEvent> = eventSet(filters)
        val cval = ICalendar()
        cval.productId = ProductId("//Simone Giacomelli//ical-aggregator2")
        cval.refreshInterval = RefreshInterval(Duration.builder().hours(1).build())
        cval.addDescription("USI Live Calendar")
        eventSet.forEach { cval.addEvent(it.event) }
        return cval
    }

    private suspend fun eventSet(filters: Set<String>): MutableSet<CEvent> {
        if (filters.isEmpty())
            return mutableSetOf()

        val contents = urlCache.collect()
        val filteredCalendars = contents.map { Biweekly.parse(it).first() }.map {
            it.events.map { it.encapsulate() }
                .filter { evt ->
                    filters.isEmpty() || filters.any { str -> evt.summary.contains(str, ignoreCase = true) }
                }
        }


        val eventSet: MutableSet<CEvent> = mutableSetOf()
        filteredCalendars.forEach { eventSet.addAll(it) }
        return eventSet
    }

    suspend fun groupCoursesBySummary(
        filters: Set<String>,
        dateTimeTz: DateTimeTz = DateTime.now().local
    ): List<CEvent> {
        val set = eventSet(filters).groupBy { it.summary }
        val courses = set.map { entry ->
            val sorted = entry.value.sortedBy { it.dateStart }
            val res = sorted.find { it.dateEnd > dateTimeTz } ?: sorted.last()
            res
        }
        return courses.toList()
    }

    data class CEvent(
        val summary: String,
        val dateStart: DateTimeTz,
        val dateEnd: DateTimeTz,
        val location: String
    ) {
        var event: VEvent? = null
    }

    companion object {
        val dtDef = DateTimeTz.fromUnixLocal(0)
    }

    private fun VEvent.encapsulate(): CEvent {

        return CEvent(
            summary.value,
            dateStart.toDateTimeTz(dtDef),
            dateEnd.toDateTimeTz(dtDef),
            location.value
        ).also { it.event = this }
    }

}

fun main() {
    println(DateTime.now().local)
}

private fun DateOrDateTimeProperty?.toDateTimeTz(defaultIfNull: DateTimeTz): DateTimeTz {
    return this?.value?.toDateTime()?.local ?: defaultIfNull
}
