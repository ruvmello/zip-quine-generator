import org.testng.annotations.Test
import utils.getByteArrayOf4Bytes
import zip.CRC32Bruteforcer
import java.io.File

class CRC32Test {

    private val path = "src/test/resources/"

    @Test
    fun addCRCtoEndTest() {
        val inputFilePath = path + "hanshqHuffmanExample.txt"
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val crc = CRC32Bruteforcer(1)
        val crcVal = crc.calculateCRC32(inputBytes)
        val crcBytes = getByteArrayOf4Bytes(crcVal.inv())
        val newBytes = crc.calculateCRC32(inputBytes + crcBytes)
        println(getByteArrayOf4Bytes(newBytes).joinToString(separator = " ") { "%02x".format(it) })
    }
}