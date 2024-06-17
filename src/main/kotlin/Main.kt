import zip.ZIPArchiver
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = args.toMutableList()
    val inputFiles = mutableListOf<String>()
    var outputFilePath: String? = null
    var debug = false
    var noCrc = false
    var enableLoop = false
    var numThreads = Runtime.getRuntime().availableProcessors()
    while(arguments.isNotEmpty()) {
        when(val option = arguments.removeAt(0)) {
            "--help", "-h" -> {
                println("This program aims to create a zip quine.")
                println("The created zip contains the input file, as well as the zip itself.")
                println("Usage: ./zipQuine inputFile [-o outputFile (ignored when using loop)] [-h] [--debug] [--no-crc] [--num-threads number_of_threads] [--loop]")
                exitProcess(0)
            }
            "--output", "-o" -> outputFilePath = arguments.removeAt(0)
            "--debug" -> debug = true
            "--no-crc" -> noCrc = true
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

    val archiver = ZIPArchiver(outputFilePath, debug, noCrc, numThreads)
    if (enableLoop) {
//        archiver.createZipLoop(inputFiles)
        var numberFound = 0
        for (i in 1..100) {
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val randomName = (1..Random.nextInt(1, 20))
                .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
                .joinToString("") + ".zip"
            val archiver2 = ZIPArchiver(randomName, false, false, numThreads)
            val found = archiver2.createZipFile(listOf())
            if (found == true) {
                numberFound += 1
            }
            println("iteration $i, found: $numberFound")
        }
    } else {
        archiver.createZipFile(inputFiles)
    }
}