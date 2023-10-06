import lz77.LZ77Compressor

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
}