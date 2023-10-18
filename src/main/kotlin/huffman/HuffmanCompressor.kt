package huffman

/**
 * This class handles everything related to the huffman encoding of the deflate algorithm
 */
class HuffmanCompressor {

    /**
     * Calculate the frequency for each byte in [inputData]
     *
     * @param inputData the ByteArray for which we calculate the frequencies
     * @return a map that contains the frequency for each byte that was in [inputData]
     */
    fun computeFrequencies(inputData: ByteArray): MutableMap<Byte, Int> {
        val freq: MutableMap<Byte, Int> = mutableMapOf()
        for (byte in inputData){
            freq[byte] = freq.getOrDefault(byte, 0) + 1
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
        val nodes: MutableList<Node> = mutableListOf()
        for (byte in freq) {
            nodes.add(LeafNode(byte.key, byte.value))
        }

        while (nodes.size != 1) {
            val minNode1 = nodes.minBy { it.weight }
            nodes.remove(minNode1)

            val minNode2 = nodes.minBy { it.weight }
            nodes.remove(minNode2)

            nodes.add(CompositeNode(minNode1, minNode2, minNode1.weight + minNode2.weight))
        }

        return nodes[0] as CompositeNode
    }
}