package sample

import api.SqlQuery

private val x = nswf.apply {
    api(::SqlQuery) { ctx, param, res ->
        if (ctx.token_ok && ctx.calendar.isAdmin == true) {
            res.success = "1"
            res.table = ctx.createQuery(param.sql).executeAndFetch().apply {

            }
        } else
            L.info("SqlQuery You are not authorized ${ctx.tokenInfo()}")
    }

}