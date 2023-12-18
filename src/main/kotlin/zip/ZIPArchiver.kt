package zip

import huffman.HuffmanCompressor
import lz77.LZ77Compressor
import lz77.LZ77Literal
import lz77.LZ77Repeat
import utils.findLastSublistOfByteArray
import utils.getByteArrayOf2Bytes
import utils.getByteArrayOf4Bytes
import java.io.File
import java.time.LocalDateTime

/**
 * Class that constructs the zip archive, this class handles the byte order
 *
 * @param zipName the file name to which we write the bytes
 */
class ZIPArchiver(private val zipName: String = "test.zip", private val debug: Boolean = false, private val noCrc: Boolean = false) {
    private val zip = File(this.zipName)
    private val datetime = LocalDateTime.now()

    /**
     * Create a zip archive of the [inputFilePath] file.
     *
     * @param inputFilePath the file path that needs to be encoded in the zip
     */
    fun createZipFile(inputFilePath: String) {
        // Clear zip file
        this.zip.writeBytes(byteArrayOf())
        val crc32Bruteforcer = CRC32Bruteforcer()

        // Compress the file
        val file = File(inputFilePath)

        print("Compressing the given file...\r")
        val compressedStream = this.getDeflateStream(file)
        val lh = this.getLocalFileHeader(file.name, compressedStream.size, file.length().toInt(), getByteArrayOf4Bytes(crc32Bruteforcer.calculateCRC32(file.readBytes())))
        val cd = this.getCentralDirectoryFileHeader(file.name, compressedStream.size, 0, file.length().toInt(), getByteArrayOf4Bytes(crc32Bruteforcer.calculateCRC32(file.readBytes())))
        this.zip.appendBytes(lh)
        this.zip.appendBytes(compressedStream)

        println("Compressing the given file... Done")

        // ### Quine ###
        val backup = this.zip.readBytes()
        print("Generating the quine...\r")

        // Generate quine of the right size, but the local file header will still be wrong
        // Create right size header
        var offset = this.zip.length().toInt()
        var lh_quine = this.getLocalFileHeader(this.zipName, 0, 0)
        this.zip.appendBytes(lh_quine)

        // Create right size footer
        var cd_quine = this.getCentralDirectoryFileHeader(this.zipName, 0, 0, 0)
        var endCd = this.getEndOfCentralDirectoryRecord(1, this.zip.length().toInt() - offset, offset)
        var footer = cd + cd_quine + endCd

        var quine = this.generateQuine(this.zip.readBytes(), footer)
        this.zip.appendBytes(quine)
        offset = this.zip.length().toInt()

        // Now that we know the compressed size, make quine with the right local file header and calculate right crc
        var fullZipFile = backup.copyOf()
        val totalSize = backup.size + lh_quine.size + quine.size + footer.size

        lh_quine = this.getLocalFileHeader(this.zipName, quine.size, totalSize)
        fullZipFile += lh_quine

        cd_quine = this.getCentralDirectoryFileHeader(this.zipName, quine.size, backup.size, totalSize)
        endCd = this.getEndOfCentralDirectoryRecord(
            2,
            fullZipFile.size + quine.size + cd.size + cd_quine.size - offset,
            offset
        )
        footer = cd + cd_quine + endCd

        quine = this.generateQuine(fullZipFile, footer)
        fullZipFile += quine

        // Zip tail
        fullZipFile += footer
        println("Generating the quine... Done")

        // Bruteforce zip without recalculating the quine each time
        if (!noCrc) {
            val finalFile = crc32Bruteforcer.bruteforce(fullZipFile, quine, backup.size, lh_quine.size, cd.size)
            this.zip.writeBytes(finalFile)
        } else {
            this.zip.writeBytes(fullZipFile)
        }

        println("ZIP written to ${this.zipName}")
    }

    fun generateQuine(zipPrefix: ByteArray, footer: ByteArray): ByteArray {
        var quineData = byteArrayOf()

        val huffman = HuffmanCompressor()

        // Lp+1
        val firstLiteral = mutableListOf<LZ77Literal>()
        zipPrefix.forEach { firstLiteral.add(LZ77Literal(it.toUByte())) }   // [P]

        // Note: Header of a literal block is 5 bytes, so L1 is 5 bytes
        getLiteralWithSize(firstLiteral.size + 5).forEach { firstLiteral.add(LZ77Literal(it.toUByte())) }   // Lp+1

        // Add to zip
        var bytesToAdd = huffman.encodeStoredBlock(firstLiteral, false)
        quineData += bytesToAdd

        // Rp+1
        val pAnd1 = firstLiteral.size
        var repeats = mutableListOf<LZ77Repeat>()
        for (i in 1..(pAnd1 / 258)) {
            repeats.add(LZ77Repeat(pAnd1, 258))
        }
        repeats.add(LZ77Repeat(pAnd1, pAnd1 % 258))

        // Add to zip
        bytesToAdd = huffman.encodeRepeatStaticBlock(repeats, false)
        quineData += bytesToAdd

        // Do it once and if we can't fit the last repeat in under one unit, keep repeating until we can
        do {
            // Keep track of the encoding for the literal
            var lXAndThree = byteArrayOf()

            // Lx
            val rPAndOne = mutableListOf<LZ77Literal>()
            bytesToAdd.forEach { rPAndOne.add(LZ77Literal(it.toUByte())) }

            // Add to zip
            bytesToAdd = huffman.encodeStoredBlock(rPAndOne, false)
            lXAndThree += bytesToAdd.copyOfRange(5, bytesToAdd.size)
            quineData += bytesToAdd

            // L1
            val lX = bytesToAdd.copyOfRange(0, 5)
            var literals = mutableListOf<LZ77Literal>()
            lX.forEach { literals.add(LZ77Literal(it.toUByte())) }

            // Add to zip
            bytesToAdd = huffman.encodeStoredBlock(literals, false)
            lXAndThree += bytesToAdd
            quineData += bytesToAdd

            // Lx+3
            lXAndThree += getLiteralWithSize(lXAndThree.size + 5)
            literals = mutableListOf()
            lXAndThree.forEach { literals.add(LZ77Literal(it.toUByte())) }

            // Add to zip
            bytesToAdd = huffman.encodeStoredBlock(literals, false)
            quineData += bytesToAdd

            // Rx+3
            val x = bytesToAdd.copyOfRange(5, bytesToAdd.size)  // Header of L is 5 bytes
            repeats = mutableListOf()
            for (i in 1..(x.size / 258)) {
                repeats.add(LZ77Repeat(x.size, 258))
            }
            repeats.add(LZ77Repeat(x.size, x.size % 258))

            // Add to zip
            bytesToAdd = huffman.encodeRepeatStaticBlock(repeats, false)

            if (repeats.size == 1 && bytesToAdd.size != 5) {
                val repeat: LZ77Repeat = repeats.get(0)
                bytesToAdd = getFiveByteRepeat(repeat)
            }

            quineData += bytesToAdd
        } while (bytesToAdd.size < 4)
        // Keep track of the encoding for the literal
        var lZAndThree = byteArrayOf()

        // Lz
        val rXAndThree = mutableListOf<LZ77Literal>()
        bytesToAdd.forEach { rXAndThree.add(LZ77Literal(it.toUByte())) }

        // Add to zip
        bytesToAdd = huffman.encodeStoredBlock(rXAndThree, false)
        lZAndThree += bytesToAdd.copyOfRange(5, bytesToAdd.size)
        quineData += bytesToAdd

        // L1
        val lX = bytesToAdd.copyOfRange(0, 5)
        var literals = mutableListOf<LZ77Literal>()
        lX.forEach { literals.add(LZ77Literal(it.toUByte())) }

        // Add to zip
        bytesToAdd = huffman.encodeStoredBlock(literals, false)
        lZAndThree += bytesToAdd
        quineData += bytesToAdd

        // Lz+3
        lZAndThree += getLiteralWithSize(lZAndThree.size + 5)
        literals = mutableListOf()
        lZAndThree.forEach { literals.add(LZ77Literal(it.toUByte())) }

        // Add to zip
        bytesToAdd = huffman.encodeStoredBlock(literals, false)
        quineData += bytesToAdd

        // Rz+3
        val bytesR4 = byteArrayOf(0x42, 0x88.toByte(), 0x21, 0xc4.toByte(), 0x00)
        bytesToAdd = bytesR4

        quineData += bytesToAdd

        // L4
        // Encoding of R4 is constant
        var literal = mutableListOf<LZ77Literal>()
        bytesR4.forEach { literal.add(LZ77Literal(it.toUByte())) }
        getLiteralWithSize(20).forEach { literal.add(LZ77Literal(it.toUByte())) }
        bytesR4.forEach { literal.add(LZ77Literal(it.toUByte())) }
        getLiteralWithSize(20).forEach { literal.add(LZ77Literal(it.toUByte())) }

        // Add to zip
        bytesToAdd = huffman.encodeStoredBlock(literal, false)
        quineData += bytesToAdd

        // R4
        quineData += bytesR4

        // L4
        literal = mutableListOf()
        bytesR4.forEach { literal.add(LZ77Literal(it.toUByte())) }
        getLiteralWithSize(0).forEach { literal.add(LZ77Literal(it.toUByte())) }
        getLiteralWithSize(0).forEach { literal.add(LZ77Literal(it.toUByte())) }
        val repeatAndTwoL0 = calculateLastQuineRepeat(footer.size)
        getLiteralWithSize(repeatAndTwoL0.size + footer.size).forEach { literal.add(LZ77Literal(it.toUByte())) }

        // Add to zip
        bytesToAdd = huffman.encodeStoredBlock(literal, false)
        quineData += bytesToAdd

        // R4
        quineData += bytesR4

        // L0
        quineData += getLiteralWithSize(0)

        // L0
        quineData += getLiteralWithSize(0)

        // Ls+y+2
        quineData += getLiteralWithSize(repeatAndTwoL0.size + footer.size) + repeatAndTwoL0 + footer

        // Rs+y+2 L0 L0
        quineData += repeatAndTwoL0

        return quineData
    }

    fun getFiveByteRepeat(token: LZ77Repeat): ByteArray {
        val huffman = HuffmanCompressor()
        val length = token.length
        val distance = token.distance

        for (i in 3 until length) {
            val repeat1 = LZ77Repeat(distance, i)
            val repeat2 = LZ77Repeat(distance, length - i)
            val repeats = listOf(repeat1, repeat2)
            val bytes = huffman.encodeRepeatStaticBlock(repeats, false)
            if (bytes.size == 5) {
                return bytes
            }
        }

        return byteArrayOf()
    }

    fun calculateLastQuineRepeat(footerSize: Int): ByteArray {
        val huffman = HuffmanCompressor()
        var totalLiteralSize = footerSize + 2 * 5   // footer size + 2 * size L0
        val totalRepeats = totalLiteralSize / 258
        var tokens = mutableListOf<LZ77Repeat>()
        for (i in 1..totalRepeats) {
            tokens.add(LZ77Repeat(totalLiteralSize, 258))
        }
        tokens.add(LZ77Repeat(totalLiteralSize, totalLiteralSize % 258))

        var bytesToAdd = huffman.encode(tokens)
        bytesToAdd += huffman.encodeStoredBlock(listOf(), isLast = false, zeroLiteral = true)
        bytesToAdd += huffman.encodeStoredBlock(listOf(), isLast = true, zeroLiteral = true)

        totalLiteralSize = footerSize + bytesToAdd.size
        tokens = mutableListOf()
        for (i in 1..totalRepeats) {
            tokens.add(LZ77Repeat(totalLiteralSize, 258))
        }
        tokens.add(LZ77Repeat(totalLiteralSize, totalLiteralSize % 258))

        bytesToAdd = huffman.encode(tokens)
        bytesToAdd += huffman.encodeStoredBlock(listOf(), isLast = false, zeroLiteral = true)
        bytesToAdd += huffman.encodeStoredBlock(listOf(), isLast = true, zeroLiteral = true)

        return bytesToAdd
    }

    fun getLiteralWithSize(size: Int, isLast: Boolean = false): ByteArray {
        return if(!isLast) byteArrayOf(0.toByte()) + getByteArrayOf2Bytes(size) +
                getByteArrayOf2Bytes(size.inv()) else byteArrayOf(128.toByte()) + getByteArrayOf2Bytes(size) +
                getByteArrayOf2Bytes(size.inv())
    }

    /**
     * Write the local file header to the zip archive we are constructing
     *
     * @param file the file for which we write the local file header
     */
    private fun getLocalFileHeader(fileName: String, compressedSize: Int, uncompressedSize: Int, crc32: ByteArray = byteArrayOf(0x0, 0x0, 0x0, 0x0)): ByteArray {
        var data = byteArrayOf()

        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        val zipVersion: ByteArray = byteArrayOf(0x14, 0x00)

        data += zipSignature
        data += zipVersion

        val commonHeader = this.writeCommonHeader(fileName.length, compressedSize, uncompressedSize, crc32)
        data += commonHeader

        // File name
        data += fileName.encodeToByteArray()

        // Extra field content

        return data
    }

    /**
     * Get the encoded stream for the file.
     *
     * @param file the file for which we want the deflate stream
     * @return the bytes that need to be written
     */
    private fun getDeflateStream(file: File): ByteArray {
        // Get tokens
        val lz77 = LZ77Compressor()
        val tokens = lz77.compress(file)

        if (this.debug) {
            for (token in tokens) {
                print(token)
            }
        }

        // Encode
        val huffman = HuffmanCompressor()
        return huffman.encode(tokens)
    }

    /**
     * Write the central directory file header to the zip archive we are constructing
     *
     * @param file the file for which we write the central directory file header
     * @param compressedSize the compressed size
     */
    private fun getCentralDirectoryFileHeader(fileName: String, compressedSize: Int, localHeaderOffset: Int, uncompressedSize: Int, crc32: ByteArray = byteArrayOf(0x0, 0x0, 0x0, 0x0)): ByteArray {
        var data = byteArrayOf()

        // https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x01, 0x02)   // Other than local file header signature
        val zipVersionMadeBy: ByteArray = byteArrayOf(0x14, 0x00)
        val zipVersionNeededToExtract: ByteArray = byteArrayOf(0x14, 0x00)

        data += zipSignature
        data += zipVersionMadeBy
        data += zipVersionNeededToExtract

        val commonHeader = this.writeCommonHeader(fileName.length, compressedSize, uncompressedSize, crc32)
        data += commonHeader

        val comment = ""

        // File comment length
        data += getByteArrayOf2Bytes(comment.length)

        // Number of disk
        data += getByteArrayOf2Bytes(0)

        // Internal attributes, https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        val internalAttributes = byteArrayOf(0x01, 0x00)    // First bit set -> text file, TODO: Extend for other files
        data += internalAttributes

        // External attributes
        val externalAttributes = byteArrayOf(0x02, 0x00, 0x00, 0x00)    // Lower byte -> zip spec version, TODO: is the other mapping needed?
        data += externalAttributes

        // Offset local header
        data += getByteArrayOf4Bytes(localHeaderOffset)

        // File name
        data += fileName.encodeToByteArray()

        // Extra field content
        // zip.appendBytes(getByteArrayOf2Bytes(0))

        // File comment
        if (comment.isNotEmpty())
            data += comment.encodeToByteArray()

        return data
    }

    /**
     * This method writes the part of the tail of a zip file.
     * This part is called end of central directory.
     *
     * @param numberOfFiles the number of files that are in the zip
     * @param size the size of the central directory
     * @param offset of the start of the central directory on the disk on which the central directory starts
     * */
    private fun getEndOfCentralDirectoryRecord(numberOfFiles: Int, size: Int, offset: Int): ByteArray {
        var data = byteArrayOf()

        val zipSignature: ByteArray = byteArrayOf(0x50, 0x4b, 0x05, 0x06)
        data += zipSignature

        // Disk number
        data += getByteArrayOf2Bytes(0)

        // Number of disks on which the central directory starts
        data += getByteArrayOf2Bytes(0)

        // Disk entries
        data += getByteArrayOf2Bytes(numberOfFiles)

        // Total entries
        data += getByteArrayOf2Bytes(numberOfFiles)

        // Central Directory size
        data += getByteArrayOf4Bytes(size)

        // Offset of the start of the central directory on the disk on which the central directory starts
        data += getByteArrayOf4Bytes(offset)

        val comment = ""
        // Comment length
        data += getByteArrayOf2Bytes(comment.length)

        // Comment
        data += comment.encodeToByteArray()
        return data
    }

    /**
     * This method writes the part of the header that is common for the
     * local file header and the central directory header
     *
     * @param file the file for which we have to write the header
     * */
    private fun writeCommonHeader(fileLength: Int, compressedSize: Int, uncompressedSize: Int, crc32: ByteArray): ByteArray {
        var data = byteArrayOf()

        val zipFlags: ByteArray = byteArrayOf(0x00, 0x00)
        val zipCompressionMethod: ByteArray = byteArrayOf(0x08, 0x00)

        data += zipFlags
        data += zipCompressionMethod

        // File modification time
        var timeAsInt: Int = datetime.hour
        timeAsInt = timeAsInt shl 6 xor datetime.minute
        timeAsInt = timeAsInt shl 5 xor (datetime.second / 2)
        data += getByteArrayOf2Bytes(timeAsInt)

        // File modification date
        var dateAsInt: Int = datetime.year - 1980
        dateAsInt = dateAsInt shl 4 xor datetime.monthValue
        dateAsInt = dateAsInt shl 5 xor datetime.dayOfMonth
        data += getByteArrayOf2Bytes(dateAsInt)

        // CRC32-Checksum
        data += crc32

        // Compressed size
        data += getByteArrayOf4Bytes(compressedSize)

        // Uncompressed size
        data += getByteArrayOf4Bytes(uncompressedSize)

        // File name length
        data += getByteArrayOf2Bytes(fileLength)

        // Extra field length
        data += getByteArrayOf2Bytes(0)

        return data
    }
}