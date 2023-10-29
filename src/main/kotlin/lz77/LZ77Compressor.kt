package lz77

import java.io.File

/**
 * This class handles everything related to the LZ77 encoding part of the deflate algorithm.
 * This is the LZSS version of the LZ77 algorithm where we don't include the next character in a repeat token.
 *
 * @param windowSize is the sliding window in which we search for the longest repeated occurence
 * @param lookaheadBufferSize is how far we look ahead from the current index
 */
class LZ77Compressor(private val windowSize: Int = 32 * 1024, private val lookaheadBufferSize: Int = 258) { // 258 because match is between (3, 258), which can be stored in one byte

    /**
     * Transform the input data to a list of LZ77Token's
     *
     * @param file the file we need to encode
     * @param minlength the minimum length of a match in the longest repeated occurrence (less than three is not really compressing)
     * @return a list of LZ77Token's that encodes the input data
     */
    fun compress(file: File, minlength: Int = 3): List<LZ77Token> {
        // TODO: Also make it possible to handle files bigger than 2GB
        val inputBytes: ByteArray = file.readBytes()

        val compressedTokens: MutableList<LZ77Token> = mutableListOf()
        var currentIndex = 0

        while (currentIndex < inputBytes.size) {
            val (maxMatchLength, maxMatchOffset) = findLongestRepeatedOccurenceInWindow(inputBytes, currentIndex)

            if (maxMatchLength >= minlength) {
                compressedTokens.add(LZ77Repeat(maxMatchOffset, maxMatchLength))
                currentIndex += maxMatchLength
            } else {
                compressedTokens.add(LZ77Literal(inputBytes[currentIndex].toUByte()))
                currentIndex++
            }
        }

        return compressedTokens
    }

    /**
     * Find the longest match inside the sliding window
     *
     * @param inputBytes the input data
     * @param currentIndex the current position inside the input data
     * @return a tuple that contains the length of the longest match
     *         and the offset relative to the current position where the match starts
     */
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