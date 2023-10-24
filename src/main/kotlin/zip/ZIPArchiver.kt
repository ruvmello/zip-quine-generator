package zip

import huffman.HuffmanCompressor
import lz77.LZ77Compressor
import utils.getByteArrayOf2Bytes
import utils.getByteArrayOf4Bytes
import java.io.File
import java.time.LocalDateTime

/**
 * Class that constructs the zip archive, this class handles the byte order
 *
 * @param zipName the file name to which we write the bytes
 */
class ZIPArchiver(private val zipName: String = "test.zip") {
    val zip = File(this.zipName)
    private val datetime = LocalDateTime.now()

    /**
     * Write the local file header to the zip archive we are constructing
     *
     * @param file the file for which we write the local file header
     */
    fun getLocalFileHeader(file: File, compressedSize: Int) {
        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        val zipVersion: ByteArray = byteArrayOf(0x14, 0x00)

        zip.writeBytes(zipSignature)
        zip.appendBytes(zipVersion)

        this.writeCommonHeader(file, compressedSize)

        // File name
        zip.appendBytes(file.name.encodeToByteArray())

        // Extra field content
        // zip.appendBytes(getByteArrayOf2Bytes(0))
    }

    fun getDeflateStream(file: File): ByteArray {
        // Get tokens
        val lz77 = LZ77Compressor()
        val tokens = lz77.compress(file)

        // Encode
        val huffman = HuffmanCompressor()
        val output = huffman.encode(tokens)
        return output
    }

    /**
     * Write the central directory file header to the zip archive we are constructing
     *
     * @param file the file for which we write the central directory file header
     */
    fun getCentralDirectoryFileHeader(file: File, compressedSize: Int) {
        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x01, 0x02)   // Other than local file header signature
        val zipVersionMadeBy: ByteArray = byteArrayOf(0x14, 0x00)
        val zipVersionNeededToExtract: ByteArray = byteArrayOf(0x14, 0x00)

        zip.appendBytes(zipSignature)
        zip.appendBytes(zipVersionMadeBy)
        zip.appendBytes(zipVersionNeededToExtract)

        this.writeCommonHeader(file, compressedSize)

        val comment = ""

        // File comment length
        zip.appendBytes(getByteArrayOf2Bytes(comment.length))

        // Number of disk
        zip.appendBytes(getByteArrayOf2Bytes(0))

        // Internal attributes, https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val internalAttributes = byteArrayOf(0x01, 0x00)    // First bit set -> text file, TODO: Extend for other files
        zip.appendBytes(internalAttributes)

        // External attributes
        val externalAttributes = byteArrayOf(0x02, 0x00, 0x00, 0x00)    // Lower byte -> zip spec version, TODO: is the other mapping needed?
        zip.appendBytes(externalAttributes)

        // Offset local header, TODO: is this needed? (windows just sets 0 when compressing)
        zip.appendBytes(getByteArrayOf4Bytes(0))

        // File name
        zip.appendBytes(file.name.encodeToByteArray())

        // Extra field content
        // zip.appendBytes(getByteArrayOf2Bytes(0))

        // File comment
        if (comment.isNotEmpty())
            zip.appendBytes(comment.encodeToByteArray())
    }

    fun getEndOfCentralDirectoryRecord(numberOfFiles: Int, size: Int, offset: Int) {
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x05, 0x06)
        zip.appendBytes(zipSignature)

        // Disk number, TODO
        zip.appendBytes(getByteArrayOf2Bytes(0))

        // Number of disks on which the central directory starts, TODO
        zip.appendBytes(getByteArrayOf2Bytes(0))

        // Disk entries, TODO
        zip.appendBytes(getByteArrayOf2Bytes(numberOfFiles))

        // Total entries, TODO
        zip.appendBytes(getByteArrayOf2Bytes(numberOfFiles))

        // Central Directory size, TODO
        zip.appendBytes(getByteArrayOf4Bytes(size))

        // Offset of the start of the central directory on the disk on which the central directory starts, TODO
        zip.appendBytes(getByteArrayOf4Bytes(offset))

        val comment = ""
        // Comment length
        zip.appendBytes(getByteArrayOf2Bytes(comment.length))

        // Comment
        zip.appendBytes(comment.encodeToByteArray())
    }

    /**
     * This method writes the part of the header that is common for the
     * local file header and the central directory header
     *
     * @param file the file for which we have to write the header
     * */
    private fun writeCommonHeader(file: File, compressedSize: Int) {
        val zipFlags: ByteArray = byteArrayOf(0x00, 0x00)
        val zipCompressionMethod: ByteArray = byteArrayOf(0x08, 0x00)

        zip.appendBytes(zipFlags)
        zip.appendBytes(zipCompressionMethod)

        // File modification time
        var timeAsInt: Int = datetime.hour
        timeAsInt = timeAsInt shl 6 xor datetime.minute
        timeAsInt = timeAsInt shl 5 xor (datetime.second / 2)
        zip.appendBytes(getByteArrayOf2Bytes(timeAsInt))

        // File modification date
        var dateAsInt: Int = datetime.year - 1980
        dateAsInt = dateAsInt shl 4 xor datetime.monthValue
        dateAsInt = dateAsInt shl 5 xor datetime.dayOfMonth
        zip.appendBytes(getByteArrayOf2Bytes(dateAsInt))

        // CRC32-Checksum, TODO: size bigger than 2GB
        zip.appendBytes(getByteArrayOf4Bytes(calculateCRC32(file.readBytes())))

        // TODO: Compressed size
        zip.appendBytes(getByteArrayOf4Bytes(compressedSize))

        // Uncompressed size
        zip.appendBytes(getByteArrayOf4Bytes(file.length().toInt()))

        // File name length
        zip.appendBytes(getByteArrayOf2Bytes(file.name.length))

        // Extra field length
        zip.appendBytes(getByteArrayOf2Bytes(0))
    }

    /**
     * Calculate the CRC-32 checksum for a ByteArray
     * More info: https://en.wikipedia.org/wiki/Cyclic_redundancy_check
     *
     * @param byteArray the ByteArray for which we calculate the CRC-32 checksum
     * @return CRC-32 checksum
     */
    private fun calculateCRC32(byteArray: ByteArray): Int {
        // Reference implementation that I ported to kotlin
        // https://www.rosettacode.org/wiki/CRC-32#C
        val crc32Table = IntArray(256)

        // Populate the CRC32 lookup table
        for (i in 0 until 256) {
            var crc = i
            for (j in 0 until 8) {
                crc = if (crc and 1 == 1) {
                    (crc ushr 1) xor 0xEDB88320.toInt()
                } else {
                    crc ushr 1
                }
            }
            crc32Table[i] = crc
        }

        var crc32 = 0xFFFFFFFF.toInt()
        for (byte in byteArray) {
            val index = (crc32 and 0xFF) xor byte.toUByte().toInt()
            crc32 = (crc32 ushr 8) xor crc32Table[index]
        }

        return crc32.inv() and 0xFFFFFFFF.toInt()
    }
}