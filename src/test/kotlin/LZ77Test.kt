import lz77.LZ77Compressor
import lz77.LZ77Repeat
import org.testng.annotations.Test
import utils.getByteArrayOf4Bytes
import zip.CRC32Bruteforcer
import java.io.File

class LZ77Test {
    private val path = "src/test/resources/"
    @Test
    fun zlibExampleTest(){
        val inputFilePath = path + "zlibExample.txt"
        val windowSize = 32 * 1024 // Adjust the window size as needed
        val lookaheadBufferSize = 258 // Adjust the lookahead buffer size as needed
        val lz77 = LZ77Compressor(windowSize = windowSize, lookaheadBufferSize = lookaheadBufferSize)
        val compressedTokens = lz77.compress(File(inputFilePath))

        // Print the compressed tokens
        for (token in compressedTokens) {
            print(token)
        }
        assert(compressedTokens.size == 8)
        assert(compressedTokens[6] is LZ77Repeat)
        assert((compressedTokens[6] as LZ77Repeat).distance == 5)
        assert((compressedTokens[6] as LZ77Repeat).length == 18)
    }

    @Test
    fun hanshqExampleTest(){
        val inputFilePath = path + "hanshqLZ77Example.txt"
        val windowSize = 32 * 1024 // Adjust the window size as needed
        val lookaheadBufferSize = 258 // Adjust the lookahead buffer size as needed
        val lz77 = LZ77Compressor(windowSize = windowSize, lookaheadBufferSize = lookaheadBufferSize)
        val compressedTokens = lz77.compress(File(inputFilePath))

        // Print the compressed tokens
        for (token in compressedTokens) {
            print(token)
        }

        val right_offsets = listOf(26, 27, 25, 26, 57, 33, 28, 33, 34, 28, 120, 31, 231, 27, 57, 86, 29, 26, 12, 29, 36, 139, 83, 138)
        val right_lengths = listOf(10, 24, 4, 20, 14, 4, 22, 13, 4, 23, 17, 4, 14, 4, 4, 3, 7, 20, 3, 4, 28, 3, 3, 3)
        var index = 0
        for (i in compressedTokens.indices) {
            if (compressedTokens[i] is LZ77Repeat){
                assert((compressedTokens[i] as LZ77Repeat).distance == right_offsets[index])
                assert((compressedTokens[i] as LZ77Repeat).length == right_lengths[index])
                index++
            }
        }
    }

    @Test
    fun test() {
        val file = File("d.zip")
        val crc = CRC32Bruteforcer(1)
        val value = getByteArrayOf4Bytes(crc.calculateCRC32(file.readBytes()))
        for (b in value) {
            val st = String.format("%02X", b)
            print(st)
        }
    }

    @Test
    fun test2(){
        val file = File("d.zip").readBytes()
        val part1 = file.copyOfRange(0, file.size / 2)
        val part2 = file.copyOfRange(file.size / 2, file.size)
        val crc = CRC32Bruteforcer(1)
        for (b in getByteArrayOf4Bytes(crc.calculateCRC32(file))) {
            val st = String.format("%02X", b)
            print(st)
        }
        println()
        for (b in getByteArrayOf4Bytes(crc.calculateCRC32(part2, crc.calculateCRC32Loop(part1)))) {
            val st = String.format("%02X", b)
            print(st)
        }
    }
}