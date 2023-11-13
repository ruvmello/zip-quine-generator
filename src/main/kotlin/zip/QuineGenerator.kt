package zip

import lz77.LZ77Literal
import lz77.LZ77Repeat
import lz77.LZ77Token
import utils.distanceMapStaticHuffman
import utils.getByteArrayOf2Bytes
import java.io.File

class QuineGenerator {

    fun generateQuineAsListOfTokens(file: File): List<LZ77Token> {
        val output = mutableListOf<LZ77Token>()
        val zipPrefix = file.readBytes()

        // [P]
        zipPrefix.forEach { output.add(LZ77Literal(it.toUByte())) }

        // Lp+1
        // Note: Header of a literal block is 5 bytes, so L1 is 5 bytes
        getLiteralWithSize(output.size + 5).forEach { output.add(LZ77Literal(it.toUByte())) }

        // Rp+1
        val pAnd1 = output.size

        // Get number of bits, distance is always 5 + extraBits
        var totalBitsForRepeat = 0
        val base = distanceMapStaticHuffman.keys.findLast { pAnd1 >= it }
        val extraBits = distanceMapStaticHuffman[base]!!.second
        for (i in 1..(pAnd1 / 258)) {
            output.add(LZ77Repeat(pAnd1, 258))
            totalBitsForRepeat += (5 + extraBits) + 8 // (distance) + bits used for 258 as length = (5 + extraBits) + 8
        }
        // TODO: make this fit in byte boundary, maybe use the last repeat as well, because of the last repeat has length 1 ...
        output.add(LZ77Repeat(pAnd1, pAnd1 % 258))

        // Lx

        return output
    }

    fun getLiteralWithSize(size: Int): ByteArray {
        return byteArrayOf(0.toByte()) + getByteArrayOf2Bytes(size) +
                getByteArrayOf2Bytes(size.inv())
    }

    // Split the last repeat so that it ends on a byte boundary
    fun getRepeatsUntilByteBoundary() {

    }
}