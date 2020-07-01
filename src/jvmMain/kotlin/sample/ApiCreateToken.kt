package sample

import api.CreateToken
import misc.GetRandomName
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime


private val x = nswf.apply {
    api(::CreateToken, preApi = { true }) { ctx, param, res ->

        transaction {
            sample.Calendars.insert {
                val token = generate_token()
                val friendlyName = GetRandomName(0)
                val now = DateTime()
                it[this.token] = token
                it[this.friendlyName] = friendlyName
                it[counterCalendarRequests] = 0
                it[counterUiRequests] = 0
                it[dtCreation] = now
                res.success = true
                res.token = token
                res.friendlyName = friendlyName
                res.dtCreation = com.soywiz.klock.DateTime(now.toDate().time).local
                L.info("createToken $token $friendlyName")
            }
        }

    }

}