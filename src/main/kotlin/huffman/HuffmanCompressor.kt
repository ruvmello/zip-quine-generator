package huffman

import lz77.LZ77Literal
import lz77.LZ77Repeat
import lz77.LZ77Token

/**
 * This class handles everything related to the huffman encoding of the deflate algorithm
 */
class HuffmanCompressor {

    /**
     * Encode the [tokens] using the Huffman part of the DEFLATE algorithm
     *
     * @param tokens that need to be encoded, they are the output of the LZ77 algorithm
     * @return the ByteArray that is the data for in the ZIP file
     */
    fun encode(tokens: List<LZ77Token>): ByteArray {
        val outputBytes = byteArrayOf()

        // Compute the frequency of the literals
        val freq = computeFrequencies(tokens)

        // Build the Huffman tree
        val tree = buildTree(freq)

        for (token in tokens) {
            when (token) {
                is LZ77Literal -> {
                    // TODO: Encode literal
                    // outputBytes +=
                }
                is LZ77Repeat -> {
                    // TODO: Encode repeat
                    // outputBytes +=
                }
            }
        }
        return outputBytes
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