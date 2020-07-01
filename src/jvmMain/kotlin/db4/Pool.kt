package db4

import nswf.logger
import sample.connection
import sample.validationSql
import java.sql.Connection

class Pool() {

    val L by logger()

    private val lock = Any()
    private val connections = mutableListOf<Connection>()

    fun acquire(): Connection {
        synchronized(lock) {
            return if (connections.size == 0) {
                L.debug { "returning new connection" }
                connection()
            } else
                head()
        }
    }

    private fun head() = connections.removeAt(0)
    private fun tail() = connections.removeAt(connections.size - 1)


    fun release(conn: Connection) {
        synchronized(lock) { connections.add(conn) }
    }

    init {
        Thread({
            while (true) {
                Thread.sleep(1000)
                val conn = synchronized(lock) { if (connections.size == 0) null else tail() }
                if (conn != null) {
                    try {
                        if (isValid(conn))
                            synchronized(lock) { connections.add(0, conn) }
                        else
                            L.debug { "Pool: Connection is not valid. It will not return to pool" }
                    } catch (ex: Exception) {
                        L.debug { "Connection is not valid. It will not return to pool" }
                    }
                }
            }
        }).apply { isDaemon = true }.start()
    }

    private fun isValid(conn: Connection): Boolean {
        try {
            val stat = conn.createStatement()
            val rs = stat.executeQuery(validationSql())
            rs.close()
            stat.close()
            return true
        } catch (ex: Exception) {
            return false
        }
    }


}