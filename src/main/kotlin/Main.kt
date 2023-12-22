import zip.ZIPArchiver
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = args.toMutableList()
    val inputFiles = mutableListOf<String>()
    var outputFilePath: String? = null
    var debug = false
    var noCrc = false
    var numThreads = Runtime.getRuntime().availableProcessors()
    while(arguments.isNotEmpty()) {
        val option = arguments.removeAt(0)
        when(option) {
            "--help", "-h" -> {
                println("This program aims to create a zip quine.")
                println("The created zip contains the input file, as well as the zip itself.")
                println("Usage: ./zipQuine inputFile [-o outputFile] [-h] [--debug] [--no-crc] [--num-threads number_of_threads]")
                exitProcess(0)
            }
            "--output", "-o" -> outputFilePath = arguments.removeAt(0)
            "--debug" -> debug = true
            "--no-crc" -> noCrc = true
            "--num-threads" -> numThreads = arguments.removeAt(0).toInt()
            else -> inputFiles.add(option)
        }
    }

    if (outputFilePath == null) {
        println("Please specify where to output the file by using the -o option. For more information use --help.")
        exitProcess(0)
    }


    val archiver = ZIPArchiver(outputFilePath, debug, noCrc, numThreads)
    archiver.createZipFile(inputFiles)
}