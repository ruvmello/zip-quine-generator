package lz77

import java.io.File

// Look into LZ77 vs LZSS
interface LZ77
data class LZ77Repeat(val offset: Int, val length: Int): LZ77 {
    override fun toString(): String {
        return "(${offset},${length})"
    }
}
data class LZ77Literal(val char: Byte): LZ77 {
    override fun toString(): String {
        return char.toInt().toChar().toString()
    }
}

class LZ77Compressor(private val windowSize: Int = 20, private val lookaheadBufferSize: Int = 15) {

    fun compress(inputFilePath: String): List<LZ77> {
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val compressedTokens = mutableListOf<LZ77>()
        var currentIndex = 0

        while (currentIndex < inputBytes.size) {
            val (maxMatchLength, maxMatchOffset) = findLongestRepeatedOccurenceInWindow(inputBytes, currentIndex)

            if (maxMatchLength > 0) {
                compressedTokens.add(LZ77Repeat(maxMatchOffset, maxMatchLength))
                currentIndex += maxMatchLength
            } else {
                compressedTokens.add(LZ77Literal(inputBytes[currentIndex]))
                currentIndex++
            }
        }

        return compressedTokens
    }

    fun findLongestRepeatedOccurenceInWindow(inputBytes: ByteArray, currentIndex: Int): Pair<Int, Int> {
        var maxMatchLength = 0
        var maxMatchOffset = 0

        val searchWindowStart = maxOf(0, currentIndex - windowSize)
        val lookaheadBufferEnd = minOf(currentIndex + lookaheadBufferSize, inputBytes.size)

        // We use reversed here so we start from distance 0
        for (offset in (searchWindowStart until currentIndex).reversed()) {
            var matchLength = 0

            // Find the longest common prefix inside the buffer
            while (currentIndex + matchLength < lookaheadBufferEnd &&
                inputBytes[offset + matchLength] == inputBytes[currentIndex + matchLength]
            ) {
                matchLength++
            }

            // If match is longer than the match found until now, replace
            if (matchLength > maxMatchLength) {
                maxMatchLength = matchLength
                maxMatchOffset = currentIndex - offset
            }
        }

        return Pair(maxMatchLength, maxMatchOffset)
    }
}