package sample

import api.DbLog
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import starter.NettyContext
import java.lang.Exception
import kotlin.system.measureTimeMillis

private val x = nswf.apply {
    api(::DbLog) { ctx, param, res ->
        measureTimeMillis {
            try {
                dblog(ctx, param.instance, param.kind, param.info, param.info2)
            } catch (ex: Exception) {
                //${param.kind}, ${param.info}, ${param.instance}
                L.error("DbLogError", ex)
                ex.printStackTrace()
            }
        }.also {
            L.info(
                "DbLog msec: ${it.toString()
                    .padStart(6)} token:[${if (ctx.token_ok) ctx.calendar.token else "-no token-"}] "
            )
        }

    }
}

private fun dblog(
    ctx: NettyContext,
    instance: String,
    kind: String,
    info: String,
    info2: String
) {
    transaction {
        AppLogs.insert {
            if (ctx.token_ok)
                it[this.calendar] = ctx.calendar.id
            it[this.dtCreation] = DateTime()
            it[this.instance] = instance
            it[this.kind] = kind
            it[this.info] = info
            it[this.info2] = info2
        }
    }
}
