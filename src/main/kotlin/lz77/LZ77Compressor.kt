package lz77

import java.io.File

data class LZ77Token(val offset: Int, val length: Int, val nextChar: Byte)

class LZ77Compressor(private val windowSize: Int = 20, private val lookaheadBufferSize: Int = 15) {

    fun compress(inputFilePath: String): List<LZ77Token> {
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val compressedTokens = mutableListOf<LZ77Token>()
        var currentIndex = 0

        while (currentIndex < inputBytes.size) {
            val (maxMatchLength, maxMatchOffset) = findLongestRepeatedOccurenceInWindow(inputBytes, currentIndex)

            val nextChar =
                if (currentIndex + maxMatchLength < inputBytes.size) inputBytes[currentIndex + maxMatchLength]
                else 0 // Use a default value if we reach the end of the input (0 is a null byte)

            if (maxMatchLength > 0) {
                compressedTokens.add(LZ77Token(maxMatchOffset, maxMatchLength, nextChar))
                currentIndex += maxMatchLength + 1
            } else {
                compressedTokens.add(LZ77Token(0, 0, inputBytes[currentIndex]))
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