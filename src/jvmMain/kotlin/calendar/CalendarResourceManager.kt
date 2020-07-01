package calendar

import java.io.File
import java.net.URL

class CalendarResourceManager {
    companion object {
        val icsTest = "./ics-test"
        val icsTestUrls by lazy { File(icsTest).listFiles().map { it.toURI().toURL() } }
        val calendarURLs: List<URL> by lazy {
            listOf(
                URL("https://search.usi.ch/en/educations/54/master-of-science-in-artificial-intelligence/schedules/47/1/ics"),
                URL("https://search.usi.ch/en/educations/54/master-of-science-in-artificial-intelligence/schedules/47/2/ics"),
                URL("https://search.usi.ch/en/educations/59/master-of-science-in-software-data-engineering/schedules/47/1/ics"),
                URL("https://search.usi.ch/en/educations/59/master-of-science-in-software-data-engineering/schedules/47/2/ics"),
                URL("https://search.usi.ch/en/educations/22/master-of-science-in-informatics/schedules/47/1/ics"),
                URL("https://search.usi.ch/en/educations/22/master-of-science-in-informatics/schedules/47/2/ics"),
                URL("https://search.usi.ch/en/educations/13/master-of-science-in-computational-science/schedules/47/1/ics"),
                URL("https://search.usi.ch/en/educations/13/master-of-science-in-computational-science/schedules/47/2/ics"),

                URL("https://search.usi.ch/en/educations/55/master-of-science-in-financial-technology-and-computing/schedules/47/1/ics"),
                URL("https://search.usi.ch/en/educations/55/master-of-science-in-financial-technology-and-computing/schedules/47/2/ics"),

                URL("https://search.usi.ch/en/educations/17/master-of-science-in-communication-and-economics-in-corporate-communication/schedules/47/2/ics"),

                URL("https://search.usi.ch/en/educations/30/bachelor-of-science-in-informatics/schedules/47/1/ics"),
                URL("https://search.usi.ch/en/educations/30/bachelor-of-science-in-informatics/schedules/47/2/ics"),
                URL("https://search.usi.ch/en/educations/30/bachelor-of-science-in-informatics/schedules/47/3/ics")
//                URL("https://search.usi.ch/en/educations/54/master-of-science-in-artificial-intelligence/schedules/46/1/ics"),
//                URL("https://search.usi.ch/en/educations/22/master-of-science-in-informatics/schedules/46/1/ics"),
//                URL("https://search.usi.ch/en/educations/59/master-of-science-in-software-data-engineering/schedules/46/1/ics"),
//                URL("https://search.usi.ch/en/educations/54/master-of-science-in-artificial-intelligence/schedules/46/2/ics"),
//                URL("https://search.usi.ch/en/educations/13/master-of-science-in-computational-science/schedules/46/1/ics")
            )

        }
    }

}