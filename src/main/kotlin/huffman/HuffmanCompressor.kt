package huffman

class HuffmanCompressor {

    fun computeFrequencies(inputDate: ByteArray): MutableMap<Byte, Int> {
        val freq: MutableMap<Byte, Int> = mutableMapOf()
        for (byte in inputDate){
            freq[byte] = freq.getOrDefault(byte, 0) + 1
        }

        return freq
    }

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