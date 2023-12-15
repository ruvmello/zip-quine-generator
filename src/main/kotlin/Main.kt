import zip.ZIPArchiver
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = args.toMutableList()
    var inputFilePath: String? = null
    var outputFilePath: String? = null
    var debug = false
    var noCrc = false
    while(arguments.isNotEmpty()) {
        val option = arguments.removeAt(0)
        when(option) {
            "--help", "-h" -> {
                println("This program aims to create a zip quine.")
                println("The created zip contains the input file, as well as the zip itself.")
                println("Usage: ./zipQuine inputFile [-o outputFile] [-h] [--debug]")
                exitProcess(0)
            }
            "--output", "-o" -> outputFilePath = arguments.removeAt(0)
            "--debug" -> debug = true
            "--no-crc" -> noCrc = true
            else -> inputFilePath = option
        }
    }

    if (inputFilePath == null) {
        println("There is no file given as input.")
        println("Usage: ./zipQuine inputFile [-o outputFile] [-h] [--debug] [--no-crc]")
        exitProcess(0)
    }

    if (outputFilePath == null)
        outputFilePath = inputFilePath.substringBeforeLast('.') + ".zip"

    val archiver = ZIPArchiver(outputFilePath, debug, noCrc)
    archiver.createZipFile(inputFilePath)
}