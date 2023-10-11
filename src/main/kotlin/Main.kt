import lz77.LZ77Compressor
import zip.ZIPArchiver
import java.io.File

fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val inputFilePath = args[0]
    val windowSize = 32 * 1024 // Adjust the window size as needed
    val lookaheadBufferSize = 258 // Adjust the lookahead buffer size as needed
    val lz77 = LZ77Compressor(windowSize = windowSize, lookaheadBufferSize = lookaheadBufferSize)
    val compressedTokens = lz77.compress(inputFilePath)

    // Print the compressed tokens
    for (token in compressedTokens) {
        print(token)
    }

    val file = File("droste.jpg")
    val zipper = ZIPArchiver("test.zip")
    zipper.getLocalFileHeader(file.name, file.length().toInt())
}