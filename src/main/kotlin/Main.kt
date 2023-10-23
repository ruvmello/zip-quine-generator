import lz77.LZ77Compressor
import zip.ZIPArchiver
import java.io.File

fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val inputFilePath = args[0]
    val file = File(inputFilePath)
    val windowSize = 32 * 1024 // Adjust the window size as needed
    val lookaheadBufferSize = 258 // Adjust the lookahead buffer size as needed
    val lz77 = LZ77Compressor(windowSize = windowSize, lookaheadBufferSize = lookaheadBufferSize)
    val compressedTokens = lz77.compress(file)

    // Print the compressed tokens
    for (token in compressedTokens) {
        print(token)
    }

    val zipper = ZIPArchiver("twee.zip")
    val compressedStream = zipper.getDeflateStream(file)
    zipper.getLocalFileHeader(file, compressedStream.size)
    zipper.zip.appendBytes(compressedStream)


    val offset = zipper.zip.length().toInt()
    zipper.getCentralDirectoryFileHeader(file, compressedStream.size)
    zipper.getEndOfCentralDirectoryRecord(1, zipper.zip.length().toInt() - offset, offset)
}