package db4

import java.util.regex.Pattern

class SqlParser(sql: String) {

    val parsed: String
    val ordinal: List<String>
    val distinct: List<String>

    init {

        val regex = Regex(":\\w+")

        ordinal = regex.findAll(sql).map { it.value.substring(1).toUpperCase() }.toList()
        distinct = ordinal.distinct()

        val matcher = regex.replace(sql) {
            "?"
        }

        parsed = matcher
    }
}


