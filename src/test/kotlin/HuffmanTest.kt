import huffman.CompositeNode
import huffman.HuffmanCompressor
import huffman.LeafNode
import org.testng.annotations.Test
import java.io.File

class HuffmanTest {

    private val path = "src/test/resources/"

    @Test
    fun computeFrequenciesTest() {
        val inputFilePath = path + "zlibExample.txt"
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val huffman = HuffmanCompressor()
        val map = huffman.computeFrequencies(inputBytes)
        for (token in map) {
            println("Key: ${token.key.toInt().toChar()}, Value: ${token.value}")
        }

        assert(map['B'.code.toByte()] == 1)
        assert(map['l'.code.toByte()] == 5)
        assert(map['a'.code.toByte()] == 5)
        assert(map['h'.code.toByte()] == 5)
        assert(map[' '.code.toByte()] == 4)
        assert(map['b'.code.toByte()] == 4)
        assert(map['!'.code.toByte()] == 1)
    }

    @Test
    fun buildTreeTest() {
        val inputFilePath = path + "hanshqHuffmanExample.txt"
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val huffman = HuffmanCompressor()
        val map = huffman.computeFrequencies(inputBytes)
        val tree = huffman.buildTree(map)

        assert(tree.weight == 15)
        // a
        assert(tree.left.weight == 6)
        assert((tree.left as LeafNode).symbol == 'a'.code.toByte())

        assert(tree.right.weight == 9)
        // b
        assert((tree.right as CompositeNode).left.weight == 4)
        assert(((tree.right as CompositeNode).left as LeafNode).symbol == 'b'.code.toByte())

        assert((tree.right as CompositeNode).right.weight == 5)
        // c
        assert(((tree.right as CompositeNode).right as CompositeNode).left.weight == 2)
        assert((((tree.right as CompositeNode).right as CompositeNode).left as LeafNode).symbol == 'c'.code.toByte())
        // d
        assert(((tree.right as CompositeNode).right as CompositeNode).right.weight == 3)
        assert((((tree.right as CompositeNode).right as CompositeNode).right as LeafNode).symbol == 'd'.code.toByte())
    }
}