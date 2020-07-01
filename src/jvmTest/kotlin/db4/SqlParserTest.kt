package db4

import kotlin.test.*


class SqlParserTest {

    @Test
    fun testSql() {
        val target = SqlParser("select * from clienti where idcliente = :idcliente and dtcreazione <:dtcreazione ")
        assertEquals(target.parsed, "select * from clienti where idcliente = ? and dtcreazione <? ")
    }

    @Test
    fun testSameParams() {
        val target = SqlParser("select * from clienti where idcliente = :p1 and idcliente =:p2 \n and idcliente = :p1 ")
        assertEquals(target.ordinal, listOf("P1", "P2", "P1"))
        assertEquals(target.distinct, listOf("P1", "P2"))
    }
}