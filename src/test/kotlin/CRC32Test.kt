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
        val newBytes = crc.calculateCRC32Loop(inputBytes + crcBytes)
        println("Bytes + CRC: " + getByteArrayOf4Bytes(newBytes).joinToString(separator = " ") { "%02x".format(it) })

        val extra = byteArrayOf(0x09.toByte(), 0xde.toByte())
        val crcExtra = crc.calculateCRC32(extra, 0x00000000)
        println("CRC extra bytes: " + getByteArrayOf4Bytes(crcExtra).joinToString(separator = " ") { "%02x".format(it) })

        val dataAndExtraBytes = crc.calculateCRC32(inputBytes + crcBytes + extra)
        println("Bytes + CRC: " + getByteArrayOf4Bytes(dataAndExtraBytes).joinToString(separator = " ") { "%02x".format(it) })
    }

    @Test
    fun addCRCtoEndTest2() {
        val inputFilePath = "test2.bin"
        val inputFile = File(inputFilePath)
        val inputBytes = inputFile.readBytes()

        val inputFilePath2 = "test3.bin"
        val inputFile2 = File(inputFilePath2)
        val inputBytes2 = inputFile2.readBytes()

        val crc = CRC32Bruteforcer(1)
        val crcVal = crc.calculateCRC32(inputBytes2)
        val crcBytes = getByteArrayOf4Bytes(crcVal.inv())
        val newBytes = crc.calculateCRC32Loop(inputBytes2 + crcBytes)
        println("Bytes + CRC: " + getByteArrayOf4Bytes(newBytes).joinToString(separator = " ") { "%02x".format(it) })
        println("CRC: " + crcBytes.joinToString(separator = " ") { "%02x".format(it) })

        val crcExtra = crc.calculateCRC32(inputBytes, 0x00000000)
        println("CRC extra bytes: " + getByteArrayOf4Bytes(crcExtra).joinToString(separator = " ") { "%02x".format(it) })

        val dataAndExtraBytes = crc.calculateCRC32(inputBytes2 + crcBytes + inputBytes)
        println("Bytes + CRC: " + getByteArrayOf4Bytes(dataAndExtraBytes).joinToString(separator = " ") { "%02x".format(it) })

//        val fileToWrite = File("test4.bin")
//        fileToWrite.writeBytes(inputBytes2 + crcBytes + inputBytes)
    }
}