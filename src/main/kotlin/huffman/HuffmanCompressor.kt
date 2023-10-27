package huffman

import lz77.LZ77Literal
import lz77.LZ77Repeat
import lz77.LZ77Token
import utils.*

/**
 * This class handles everything related to the huffman encoding of the deflate algorithm
 */
class HuffmanCompressor {

    private var byte: Int = 0
    private var totalBitsSet: Int = 0

    /**
     * Encode the [tokens] using the Huffman part of the DEFLATE algorithm
     *
     * @param tokens that need to be encoded, they are the output of the LZ77 algorithm
     * @return the ByteArray that is the data for in the ZIP file
     */
    fun encode(tokens: List<LZ77Token>): ByteArray {
        var outputBytes = byteArrayOf()

        val literals = mutableListOf<LZ77Literal>()
        val repeats = mutableListOf<LZ77Repeat>()
        for (index in tokens.indices) {
            val token = tokens[index]
            when (token) {
                is LZ77Literal -> {
                    if (repeats.isNotEmpty()) {
                        outputBytes += encodeRepeatStaticBlock(repeats, false)
                        repeats.clear()
                    }
                    literals.add(token)
                }
                is LZ77Repeat -> {
                    if (literals.isNotEmpty()) {
                        outputBytes += encodeStoredBlock(literals, false)
                        literals.clear()
                    }
                    repeats.add(token)
                }
            }
        }

        if (repeats.isNotEmpty()) {
            outputBytes += encodeRepeatStaticBlock(repeats, true)
            repeats.clear()
        } else if (literals.isNotEmpty()) {
            outputBytes += encodeStoredBlock(literals, true)
            literals.clear()
        }

        if (totalBitsSet != 0) {
            byte = byte shl (8 - totalBitsSet)
            totalBitsSet += (8 - totalBitsSet)
            outputBytes += getBytesAndReset()
        }

        return outputBytes
    }

    fun encodeStoredBlock(literal: List<LZ77Literal>, isLast: Boolean): ByteArray {
        // First bit
        byte = byte shl 1 xor (if (isLast) 1 else 0)

        // Block type, 00 for stored
        byte = byte shl 2

        totalBitsSet += 3

        // Padding first byte
        byte = byte shl (8 - totalBitsSet % 8)
        totalBitsSet += (8 - totalBitsSet % 8)

        val len = literal.size
        val outputBytes = getBytesAndReset().toByteArray()
        byte = 0
        totalBitsSet = 0

        return outputBytes + getByteArrayOf2Bytes(len) +
                getByteArrayOf2Bytes(len.inv()) + literal.map { it.char }.toByteArray()
    }

    fun encodeRepeatStaticBlock(tokens: List<LZ77Repeat>, isLast: Boolean): ByteArray {
        val encoded = mutableListOf<Byte>()
        // First bit
        byte = byte shl 1 xor (if (isLast) 1 else 0)

        // Block type, 01 for static (but it is read from right-to-left, so xor 2)
        byte = byte shl 2 xor 2

        totalBitsSet += 3
        for (token in tokens) {
            // Length
            var base = lengthMapStaticHuffman.keys.findLast { token.length >= it }
            var code = lengthMapStaticHuffman[base]!!.first
            var extraBits = lengthMapStaticHuffman[base]!!.second
            if (code in 256..279) {
                // 7 bits
                byte = (byte shl 7) xor ((code shl 1).toUByte().toInt() shr 1)   // Cut off 25 most significant bits
                byte = (byte shl extraBits) xor reverseBitsByte(((token.length - base!!) shl (8 - extraBits)).toByte()).toUByte().toInt() // Add extra bits, extra bits are in MSB-order (reverseBits)
                totalBitsSet += 7 + extraBits

            } else if (code in 280..287) {
                // 8 bits
                byte = (byte shl 8) xor code.toUByte().toInt()   // Cut off 24 most significant bits
                byte = (byte shl extraBits) xor reverseBitsByte(((token.length - base!!) shl (8 - extraBits)).toByte()).toUByte().toInt() // Add extra bits, extra bits are in MSB-order (reverseBits)
                totalBitsSet += 8 + extraBits
            }

            encoded.addAll(getBytesAndReset())

            // Distance, base is always 5 bits
            base = distanceMapStaticHuffman.keys.findLast { token.distance >= it }
            code = distanceMapStaticHuffman[base]!!.first
            extraBits = distanceMapStaticHuffman[base]!!.second
            byte = (byte shl 5) xor ((code shl 3).toUByte().toInt() shr 3)   // Cut off 27 most significant bits

            // Example: 00000000 00000000 00000001 00000011 ->
            //          (reversed) 11000000 10000000 00000000 00000000 ->
            //          (shiftRight 32 - extraBits) 00000000 00000000 00000001 10000001
            val extraValue = reverseBitsInt(token.distance - base!!).toUInt()
            byte = (byte shl extraBits) xor (extraValue shr (32 - extraBits)).toInt() // Add extra bits, extra bits are in MSB-order (reverseBits)
            totalBitsSet += 5 + extraBits

            encoded.addAll(getBytesAndReset())
        }

        // End of block marker (256 - 7 bits)
        byte = byte shl 7 // First 7 bits of 256 are just 0, so shift 7 is enough
        totalBitsSet += 7

        encoded.addAll(getBytesAndReset())

        return encoded.toByteArray()
    }

    /**
     * Get the full bytes that are set and reset the integer to only have the not full byte at the beginning
     * Example: 00000100 01000100 10101110 00110010 with 27 bits set
     *          -> 00000000 00000000 00000000 00010010
     *
     * @return the bytes that are fully set
     */
    private fun getBytesAndReset(): List<Byte> {
        val output = getListOfNReversedBytes(byte, totalBitsSet)
        val totalFullBytes = totalBitsSet / 8   // Integer division
        totalBitsSet -= totalFullBytes * 8
        byte = (byte shl (8 - totalBitsSet)).toByte().toInt() shr (8 - totalBitsSet)    // Reset to only the set bits
        return output
    }

    /**
     * Calculate the frequency for each byte in [tokens]
     *
     * @param tokens the tokens for which we calculate the frequencies
     * @return a map that contains the frequency for each byte that was in [tokens]
     */
    fun computeFrequencies(tokens: List<LZ77Token>): MutableMap<Byte, Int> {
        val freq: MutableMap<Byte, Int> = mutableMapOf()
        for (token in tokens){
            if (token is LZ77Literal) {
                val byte = token.char
                freq[byte] = freq.getOrDefault(byte, 0) + 1
            }
        }

        return freq
    }

    /**
     * Create a tree based on the frequencies calculated
     *
     * @param freq the frequencies that are calculated for each byte in the input data
     * @return the root of the huffman tree
     */
    fun buildTree(freq: Map<Byte, Int>): CompositeNode {
        val nodes: MutableList<Node> = freq.entries.map { (byte, weight) -> LeafNode(byte, weight) }.toMutableList()

        while (nodes.size != 1) {
            // Sort each time as the composite nodes also need to be considered
            nodes.sortBy { it.weight }
            val minNode1 = nodes.removeAt(0)
            val minNode2 = nodes.removeAt(0)

            nodes.add(CompositeNode(minNode1, minNode2, minNode1.weight + minNode2.weight))
        }

        return nodes[0] as CompositeNode
    }
}