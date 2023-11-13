package zip

import lz77.LZ77Literal
import lz77.LZ77Repeat
import lz77.LZ77Token
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

        // Rp+1, TODO: make this fit in byte boundary
        val p_and_1 = output.size
        for (i in 1..(p_and_1 / 258)) {
            output.add(LZ77Repeat(p_and_1, 258))
        }
        output.add(LZ77Repeat(p_and_1, p_and_1 % 258))

        // Lx

        return output
    }

    fun getLiteralWithSize(size: Int): ByteArray {
        return byteArrayOf(0.toByte()) + getByteArrayOf2Bytes(size) +
                getByteArrayOf2Bytes(size.inv())
    }
}