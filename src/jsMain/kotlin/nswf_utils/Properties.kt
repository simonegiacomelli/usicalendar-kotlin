package nswf_utils

class Properties : LinkedHashMap<String, String>() {
    fun merge(content: String) {
        content.split("\n").filter { it.isNotBlank() }
            .forEach {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2)
                    set(parts[0], parts[1])
            }
    }

    override fun toString(): String {
        val res = StringBuilder()
        forEach { res.append("${it.key}=${it.value}\n") }
        return res.toString()
    }
}