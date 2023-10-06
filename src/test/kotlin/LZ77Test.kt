import lz77.LZ77Compressor
import lz77.LZ77Repeat
import org.testng.annotations.Test

class LZ77Test {
    private val path = "src/test/resources/"
    @Test
    fun zlibExampleTest(){
        val inputFilePath = path + "zlibExample.txt"
        val windowSize = 32 * 1024 // Adjust the window size as needed
        val lookaheadBufferSize = 258 // Adjust the lookahead buffer size as needed
        val lz77 = LZ77Compressor(windowSize = windowSize, lookaheadBufferSize = lookaheadBufferSize)
        val compressedTokens = lz77.compress(inputFilePath)

        // Print the compressed tokens
        for (token in compressedTokens) {
            print(token)
        }
        assert(compressedTokens.size == 8)
        assert(compressedTokens[6] is LZ77Repeat)
        assert((compressedTokens[6] as LZ77Repeat).offset == 5)
        assert((compressedTokens[6] as LZ77Repeat).length == 18)
    }
}