import lz77.LZ77Compressor
import zip.ZIPArchiver
import java.io.File

fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    assert(args.size == 3) { "The arguments must have the format [inputFile] -o [outputFile]" }

    val inputFilePath = args[0]

    var outputFilePath: String? = null
    if (args[1] == "-o")
        outputFilePath = args[2]

    val file = File(inputFilePath)
    val lz77 = LZ77Compressor()
    val compressedTokens = lz77.compress(file)

    // Print the compressed tokens
    for (token in compressedTokens) {
        print(token)
    }

    val zipper = ZIPArchiver(outputFilePath!!)
    val compressedStream = zipper.getDeflateStream(file)
    zipper.getLocalFileHeader(file, compressedStream.size)
    zipper.zip.appendBytes(compressedStream)


    val offset = zipper.zip.length().toInt()
    zipper.getCentralDirectoryFileHeader(file, compressedStream.size)
    zipper.getEndOfCentralDirectoryRecord(1, zipper.zip.length().toInt() - offset, offset)
}