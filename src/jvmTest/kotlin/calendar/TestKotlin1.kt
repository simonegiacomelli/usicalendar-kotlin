package calendar

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.assertTrue

import kotlin.test.Test
import kotlin.test.assertTrue

class TestKotlin1 {

    @Test
    fun testHello2() {
        val (a,_,b) = ciao()
        print("$a $b")
    }

    fun ciao():Aaa{
        return Aaa(12,42,43)
    }

    @Test
    fun testHello3() {

    }

    @Test
    fun main() {
        runBlocking {
            //sampleStart
            val channel = Channel<Int>()
            launch {
                for (x in 1..5) channel.send(x * x)
                channel.close() // we're done sending
            }
            // here we print received values using `for` loop (until the channel is closed)
            for (y in channel) println(y)
            println("Done!")
//sampleEnd
        }
    }
}

data class Aaa(val ele1:Int,val ele2:Int, val ele3:Int)