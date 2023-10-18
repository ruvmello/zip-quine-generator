package zip

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
    fun getLocalFileHeader(file: File) {
        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        val zipVersion: ByteArray = byteArrayOf(0x14, 0x00)

        zip.writeBytes(zipSignature)
        zip.appendBytes(zipVersion)

        this.writeCommonHeader(file)

        // File name
        zip.appendBytes(file.name.encodeToByteArray())

        // Extra field content
        // zip.appendBytes(getByteArrayOf2Bytes(0))
    }

    /**
     * This method writes the part of the header that is common for the
     * local file header and the central directory header
     *
     * @param file the file for which we have to write the header
     * */
    private fun writeCommonHeader(file: File) {
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

        // TODO: CRC32-Checksum
        zip.appendBytes(getByteArrayOf4Bytes(0))

        // TODO: Compressed size
        zip.appendBytes(getByteArrayOf4Bytes(0))

        // Uncompressed size
        zip.appendBytes(getByteArrayOf4Bytes(file.length().toInt()))

        // File name length
        zip.appendBytes(getByteArrayOf2Bytes(file.name.length))

        // Extra field length
        zip.appendBytes(getByteArrayOf2Bytes(0))
    }

    /**
     * Write the central directory file header to the zip archive we are constructing
     *
     * @param file the file for which we write the central directory file header
     */
    fun getCentralDirectoryFileHeader(file: File) {
        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x01, 0x02)   // Other than local file header signature
        val zipVersionMadeBy: ByteArray = byteArrayOf(0x14, 0x00)
        val zipVersionNeededToExtract: ByteArray = byteArrayOf(0x14, 0x00)

        zip.appendBytes(zipSignature)
        zip.appendBytes(zipVersionMadeBy)
        zip.appendBytes(zipVersionNeededToExtract)

        this.writeCommonHeader(file)

        val comment = ""

        // File comment length
        zip.appendBytes(getByteArrayOf2Bytes(comment.length))

        // Number of disk TODO: How?
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
     * An integer has a size of 32 bits, get a ByteArray of the two least significant bytes
     *
     * @param input the integer for which we construct a ByteArray of size two
     */
    private fun getByteArrayOf2Bytes(input: Int): ByteArray {
        return byteArrayOf((input shr 0).toByte(), (input shr 8).toByte())
    }

    /**
     * An integer has a size of 32 bits, get a ByteArray of the size four with the least significant byte first
     *
     * @param input the integer for which we construct a ByteArray of size four
     */
    private fun getByteArrayOf4Bytes(input: Int): ByteArray {
        return byteArrayOf((input shr 0).toByte(), (input shr 8).toByte(), (input shr 16).toByte(), (input shr 24).toByte())
    }
}