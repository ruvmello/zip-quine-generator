import huffman.CompositeNode
import huffman.HuffmanCompressor
import huffman.LeafNode
import lz77.LZ77Literal
import lz77.LZ77Repeat
import lz77.LZ77Token
import org.testng.annotations.Test
import zip.ZIPArchiver
import java.io.File
import kotlin.random.Random

class HuffmanTest {

    private val path = "src/test/resources/"

    @Test
    fun encodeTest() {
        val input = byteArrayOf(0x1f, 0x8b.toByte(), 0x08, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff.toByte(), 0x71, 0x75, 0x69, 0x6e, 0x65, 0x2e,
            0x67, 0x7a, 0x00, 0x00, 0x18, 0x00, 0xe7.toByte(), 0xff.toByte()
        )

        val tokens: MutableList<LZ77Token> = mutableListOf()
        input.forEach { tokens.add(LZ77Literal(it.toUByte())) }
        tokens.add(LZ77Repeat(24, 12))
        tokens.add(LZ77Repeat(24, 12))
        tokens.add(LZ77Literal(0x77.toUByte()))

        val huffman = HuffmanCompressor()
        val output = huffman.encode(tokens)
        println(output.joinToString(separator = " ") { "%02x".format(it) })
    }
    @Test
    fun computeFrequenciesTest() {
        val inputFilePath = path + "zlibExample.txt"
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val tokens: MutableList<LZ77Token> = mutableListOf()
        inputBytes.forEach { tokens.add(LZ77Literal(it.toUByte())) }

        val huffman = HuffmanCompressor()
        val map = huffman.computeFrequencies(tokens)
        for (token in map) {
            println("Key: ${token.key.toInt().toChar()}, Value: ${token.value}")
        }

        assert(map['B'.code.toUByte()] == 1)
        assert(map['l'.code.toUByte()] == 5)
        assert(map['a'.code.toUByte()] == 5)
        assert(map['h'.code.toUByte()] == 5)
        assert(map[' '.code.toUByte()] == 4)
        assert(map['b'.code.toUByte()] == 4)
        assert(map['!'.code.toUByte()] == 1)
    }

    @Test
    fun buildTreeTest() {
        val inputFilePath = path + "hanshqHuffmanExample.txt"
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val tokens: MutableList<LZ77Token> = mutableListOf()
        inputBytes.forEach { tokens.add(LZ77Literal(it.toUByte())) }

        val huffman = HuffmanCompressor()
        val map = huffman.computeFrequencies(tokens)
        val tree = huffman.buildTree(map)

        assert(tree.weight == 15)
        // a
        assert(tree.left.weight == 6)
        assert((tree.left as LeafNode).symbol == 'a'.code.toUByte())

        assert(tree.right.weight == 9)
        // b
        assert((tree.right as CompositeNode).left.weight == 4)
        assert(((tree.right as CompositeNode).left as LeafNode).symbol == 'b'.code.toUByte())

        assert((tree.right as CompositeNode).right.weight == 5)
        // c
        assert(((tree.right as CompositeNode).right as CompositeNode).left.weight == 2)
        assert((((tree.right as CompositeNode).right as CompositeNode).left as LeafNode).symbol == 'c'.code.toUByte())
        // d
        assert(((tree.right as CompositeNode).right as CompositeNode).right.weight == 3)
        assert((((tree.right as CompositeNode).right as CompositeNode).right as LeafNode).symbol == 'd'.code.toUByte())
    }

    @Test
    fun writeTestsForThesis(){
        val h = HuffmanCompressor()
        for(i in 250..258) {
            val repeats = listOf(LZ77Repeat(19, 19))
            val encoded = h.encodeRepeatStaticBlock(repeats, false)
            if (h.totalBitsSet == 0) {
                println("gevonden")
            }
//            encoded.forEach { println(String.format("%8s", Integer.toBinaryString(it.toInt() and 0xFF)).replace(' ', '0')) }
//            val f = File("test.bin")
//            f.writeBytes(encoded)
        }
    }

    @Test
    fun maxRepeat(){
        val zipper = ZIPArchiver("Test", false, true, 1)
        var max = 0
        for (sizeS in 100..256 * 128){
            // lastRepeat and 2 * L0
            val test = zipper.calculateLastQuineRepeat(32347)
            if (32347 + test.size > 256 * 128) {
                println("max: $max")
                break
            }
            max = sizeS
        }
    }

    @Test
    fun chanceOfMatch(){
        var numberFound = 0
        for (i in 1..100) {
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val randomName = (1..Random.nextInt(1, 20))
                .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
                .joinToString("") + ".zip"
            val archiver2 = ZIPArchiver(randomName, false, false, 8)
            val found = archiver2.createZipFile(listOf())
            if (found == true) {
                numberFound += 1
            }
            println("iteration $i, found: $numberFound")
        }
    }
}