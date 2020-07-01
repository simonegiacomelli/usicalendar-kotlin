package sample

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue


internal class CalTokenKtTest {
    @Test
    fun test_generate_token() {
        val token1 = generate_token()
        val token2 = generate_token()
        println("$token1")
        println("$token2")
        assertNotEquals(token1,token2)
        assertTrue {  token1.length > 10 }
        assertTrue {  token2.length > 10 }
    }
}