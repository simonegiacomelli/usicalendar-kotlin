package nswf_utils

class Uri(uri: String) {
    var protocol: String = ""
    var username: String = ""
    var password: String = ""
    var hostname: String = ""
    var port: Int = 0
    var queryString: String = ""
    val params: MutableMap<String, String> = mutableMapOf()

    init {
        var leftOver = uri
        fun s(delimiters: String): String {
            val split = leftOver.split(delimiters, limit = 2)
            leftOver = if (split.size > 1) split.component2() else ""
            return split.first()
        }

        protocol = s("://")
        username = s(":")
        password = s("@")
        hostname = s(":")
        port = s("?").toInt()
        queryString = leftOver
        queryString.split("&")
                .forEach {
                    val keyvalue = it.split("=")
                    when (keyvalue.size) {
                        1 -> params.set(keyvalue[0], "")
                        2 -> params.set(keyvalue[0], keyvalue[1])
                    }
                }
        println("p=$protocol u=$username p=$password h=$hostname port=$port q=$queryString")
    }
}