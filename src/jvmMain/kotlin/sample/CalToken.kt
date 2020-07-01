package sample

import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*
import java.util.Base64

val alphabet = listOf(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
)

fun generate_token(): String {
    var uuid = UUID.randomUUID()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.getMostSignificantBits())
    bb.putLong(uuid.getLeastSignificantBits())
    val bytes = bb.array()
    val bi = BigInteger(1, bytes)
    val result = number_to_base(bi, alphabet)
    return result
}

fun number_to_base(n: BigInteger, alphabet: List<Char>): String {
    val b = alphabet.size.toBigInteger()
    if (n.compareTo(BigInteger.ZERO) == 0)
        return alphabet[0].toString()
    val result = StringBuilder()
    var x = n
    while (x.compareTo(BigInteger.ZERO) > 0) {
        val divrem = x.divideAndRemainder(b)
        val div = divrem[0]
        x = div
        val rem = divrem[1].toInt()
        val letter = alphabet[rem]
        result.append(letter)
    }
    return result.toString()
}

fun generate_token_uuid(): String {
    var uuid = UUID.randomUUID()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.getMostSignificantBits())
    bb.putLong(uuid.getLeastSignificantBits())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array())!!
}

