package zip

import java.io.File
import java.time.LocalDateTime

class ZIPArchiver(val zipName: String = "test.zip") {
    private val zip = File(this.zipName)

    // File name of the file that is compressed
    fun getLocalFileHeader(fileName: String, size: Int) {
        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        val zipVersion: ByteArray = byteArrayOf(0x14, 0x00)
        val zipFlags: ByteArray = byteArrayOf(0x00, 0x00)
        val zipCompressionMethod: ByteArray = byteArrayOf(0x08, 0x00)

        zip.writeBytes(zipSignature)
        zip.appendBytes(zipVersion)
        zip.appendBytes(zipFlags)
        zip.appendBytes(zipCompressionMethod)

        val datetime = LocalDateTime.now()

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
        zip.appendBytes(getByteArrayOf4Bytes(size))

        // File name length
        zip.appendBytes(getByteArrayOf2Bytes(fileName.length))

        // TODO: Extra field length
        zip.appendBytes(getByteArrayOf2Bytes(0))

        // File name
        zip.appendBytes(fileName.encodeToByteArray())

        // TODO: Extra fields, is this needed?
        // zip.appendBytes(getByteArrayOf2Bytes(0))
    }

    private fun getByteArrayOf2Bytes(input: Int): ByteArray {
        return byteArrayOf((input shr 0).toByte(), (input shr 8).toByte())
    }

    private fun getByteArrayOf4Bytes(input: Int): ByteArray {
        return byteArrayOf((input shr 0).toByte(), (input shr 8).toByte(), (input shr 16).toByte(), (input shr 24).toByte())
    }
}