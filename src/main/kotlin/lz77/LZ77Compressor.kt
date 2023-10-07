package lz77

import java.io.File

// Look into LZ77 vs LZSS
class LZ77Compressor(private val windowSize: Int = 20, private val lookaheadBufferSize: Int = 15) {

    fun compress(inputFilePath: String, minlength: Int = 1): List<LZ77Token> {
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val compressedTokens: MutableList<LZ77Token> = mutableListOf()
        var currentIndex = 0

        while (currentIndex < inputBytes.size) {
            val (maxMatchLength, maxMatchOffset) = findLongestRepeatedOccurenceInWindow(inputBytes, currentIndex)

            if (maxMatchLength >= minlength) {
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