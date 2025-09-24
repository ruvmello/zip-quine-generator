import zip.ZIPArchiver
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = args.toMutableList()
    val inputFiles = mutableListOf<String>()
    var outputFilePath: String? = null
    var debug = false
    var enableLoop = false
    var numThreads = Runtime.getRuntime().availableProcessors()
    while(arguments.isNotEmpty()) {
        when(val option = arguments.removeAt(0)) {
            "--help", "-h" -> {
                println("This program aims to create a zip quine.")
                println("The created zip contains the input file, as well as the zip itself.")
                println("Usage: ./zipQuine inputFile [-o outputFile (ignored when using loop)] [-h] [--debug] [--num-threads number_of_threads] [--loop]")
                exitProcess(0)
            }
            "--output", "-o" -> outputFilePath = arguments.removeAt(0)
            "--debug" -> debug = true
            "--loop" -> enableLoop = true
            "--num-threads" -> numThreads = arguments.removeAt(0).toInt()
            else -> inputFiles.add(option)
        }
    }

    if (outputFilePath == null && !enableLoop) {
        println("Please specify where to output the file by using the -o option. For more information use --help.")
        exitProcess(0)
    } else if (outputFilePath == null) {
        outputFilePath = "test.zip"
    }

    val archiver = ZIPArchiver(outputFilePath, debug, numThreads)
    if (enableLoop) {
        archiver.createZipLoop(inputFiles)
    } else {
        archiver.createZipFile(inputFiles)
    }
}
