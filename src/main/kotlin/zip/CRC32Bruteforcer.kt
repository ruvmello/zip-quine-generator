package zip

import utils.findLastSublistOfByteArray
import utils.getByteArrayOf4Bytes
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

/**
 * This class takes care of the brute-forcing of the CRC-32 checksum
 *
 * @param numThreads is the number of threads used for the brute-forcing
 */
class CRC32Bruteforcer(private val numThreads: Int) {
    private val crc32Table = IntArray(256)

    init {
        this.calculateTable()
    }

    /**
     * This method tries to bruteforce the CRC-32 checksum and uses multithreading to do so.
     *
     * @param fullZipFile The zip file for which we want to bruteforce the CRC-32
     * @param quine is the quine itself
     * @param backupSize is the size of the header before the quine itself
     * @param lhQuineSize is the size of the local file header of the quine itself
     * @param cdSize is the size of the central directory
     * @return the ByteArray of the full file that contains the right checksum, if no checksum is found we just use 0
     */
    fun bruteforce(fullZipFile: ByteArray, quine: ByteArray, backupSize: Int, lhQuineSize: Int, cdSize: Int): ByteArray {
        val range = Int.MIN_VALUE..Int.MAX_VALUE
        val segmentSize: Int = ((range.last.toLong() - range.first.toLong() + 1) / numThreads).toInt()

        val firstPartLh = fullZipFile.copyOfRange(0, backupSize + 14)
        val secondPartLh = fullZipFile.copyOfRange(backupSize + 18, backupSize + lhQuineSize + 5 + backupSize + 14)

        val indexOfFooterInQuine = findLastSublistOfByteArray(quine, byteArrayOf((80).toByte(), (75).toByte(), (1).toByte(), (2).toByte()))
        val firstPartCd = fullZipFile.copyOfRange(backupSize + lhQuineSize + 5 + backupSize + 18, backupSize + lhQuineSize + indexOfFooterInQuine + 16)
        val secondPartCd = fullZipFile.copyOfRange(backupSize + lhQuineSize + indexOfFooterInQuine + 20, backupSize + lhQuineSize + quine.size + cdSize + 16)
        val lastPartCd = fullZipFile.copyOfRange(backupSize + lhQuineSize + quine.size + cdSize + 20, fullZipFile.size)
        val prevCalculatedCrc = calculateCRC32Loop(firstPartLh)

        val resultFound = AtomicBoolean(false)
        val doneIterations = AtomicLong(0L)
        val totalIterations = UInt.MAX_VALUE.toLong()
        val result = AtomicReference<ByteArray>()

        val latch = CountDownLatch(numThreads)
        print("Starting brute-forcing the CRC32 using $numThreads threads... (0.00%)\r")
        for (i in 0 until numThreads) {
            val start = range.first + i * segmentSize
            val end = if (i == numThreads - 1) range.last else start + segmentSize - 1

            val thread = Thread {
                var currentTime = System.currentTimeMillis()
                var byteFormOfCrc = getByteArrayOf4Bytes(0)
                var currentCrcFile = byteFormOfCrc + secondPartLh + byteFormOfCrc + firstPartCd + byteFormOfCrc + secondPartCd + byteFormOfCrc + lastPartCd
                var index = 0
                for (crc in start..end) {
                    if (resultFound.get()) {
                        break
                    }

                    byteFormOfCrc = getByteArrayOf4Bytes(crc)
                    currentCrcFile[0] = byteFormOfCrc[0]
                    currentCrcFile[1] = byteFormOfCrc[1]
                    currentCrcFile[2] = byteFormOfCrc[2]
                    currentCrcFile[3] = byteFormOfCrc[3]

                    index += 4 + secondPartLh.size
                    currentCrcFile[index + 0] = byteFormOfCrc[0]
                    currentCrcFile[index + 1] = byteFormOfCrc[1]
                    currentCrcFile[index + 2] = byteFormOfCrc[2]
                    currentCrcFile[index + 3] = byteFormOfCrc[3]

                    index += 4 + firstPartCd.size
                    currentCrcFile[index + 0] = byteFormOfCrc[0]
                    currentCrcFile[index + 1] = byteFormOfCrc[1]
                    currentCrcFile[index + 2] = byteFormOfCrc[2]
                    currentCrcFile[index + 3] = byteFormOfCrc[3]

                    index += 4 + secondPartCd.size
                    currentCrcFile[index + 0] = byteFormOfCrc[0]
                    currentCrcFile[index + 1] = byteFormOfCrc[1]
                    currentCrcFile[index + 2] = byteFormOfCrc[2]
                    currentCrcFile[index + 3] = byteFormOfCrc[3]

                    index = 0
                    if (calculateCRC32(currentCrcFile, prevCalculatedCrc) == crc) {
                        currentCrcFile = firstPartLh + currentCrcFile
                        result.set(currentCrcFile.clone())
                        resultFound.set(true)
                        break
                    }

                    // Every 10 seconds update the progress
                    val iter = doneIterations.incrementAndGet()
                    if (System.currentTimeMillis() - currentTime > 10000) {
                        currentTime = System.currentTimeMillis()
                        print("Starting brute-forcing the CRC32 using $numThreads threads... (${(iter.toDouble() * 100 / totalIterations).toString().take(4)}%)\r")
                    }
                }

                latch.countDown()
            }

            thread.start()
        }

        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        println("Starting brute-forcing the CRC32... Done (${doneIterations.get()} / $totalIterations - ${(doneIterations.get().toDouble() * 100 / totalIterations).toString().take(4)}%)")

        return if (resultFound.get()) {
            result.get()
        } else {
            println("Warning: No CRC32 is found, using 0 instead.")
            val byteFormOfCrc = getByteArrayOf4Bytes(0)
            firstPartLh + byteFormOfCrc + secondPartLh + byteFormOfCrc + firstPartCd + byteFormOfCrc + secondPartCd + byteFormOfCrc + lastPartCd
        }
    }

    fun bruteforceLoop(fullZipFile: ByteArray, secondZip: ByteArray, header: ByteArray, header2: ByteArray, footer: ByteArray, footer2: ByteArray, lhQuineSize: Int, quineSize: Int): ByteArray {
        val indexOfReset = findLastSublistOfByteArray(fullZipFile, byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte()))
        val crcEndofCd = calculateCRC32Loop(fullZipFile.copyOfRange(indexOfReset + 4, fullZipFile.size), 0x00000000)

        val range = Int.MIN_VALUE..Int.MAX_VALUE
        val segmentSize: Int = ((range.last.toLong() - range.first.toLong() + 1) / numThreads).toInt()

        // Find all the places where we need to find the CRC
        val indexOfCdInFooter = findLastSublistOfByteArray(footer, byteArrayOf((80).toByte(), (75).toByte(), (1).toByte(), (2).toByte()))
        val indexOfCdInFooter2 = findLastSublistOfByteArray(footer2, byteArrayOf((80).toByte(), (75).toByte(), (1).toByte(), (2).toByte()))
        val cdAfterHeader = fullZipFile.copyOfRange(0, header.size + indexOfCdInFooter + 16)
        val lhQuineAfterFooter = fullZipFile.copyOfRange(header.size + indexOfCdInFooter + 20, header.size + footer.size + 14)
        val cdOfSecondZip = fullZipFile.copyOfRange(header.size + footer.size + 18, header.size + footer.size + lhQuineSize + 5 + header.size + indexOfCdInFooter2 + 16)
        val lhOfSecondZip = fullZipFile.copyOfRange(header.size + footer.size + lhQuineSize + 5 + header.size + indexOfCdInFooter2 + 20, header.size + footer.size + lhQuineSize + 5 + header.size + footer.size + 14)
        val cdOfFirstZip = fullZipFile.copyOfRange(header.size + footer.size + lhQuineSize + 5 + header.size + footer.size + 18, header.size + footer.size + lhQuineSize + 5 + header.size + footer.size + lhQuineSize + 10 + header2.size + indexOfCdInFooter + 16)
        val lhOfFirstZip = fullZipFile.copyOfRange(header.size + footer.size + lhQuineSize + 5 + header.size + footer.size + lhQuineSize + 10 + header2.size + indexOfCdInFooter + 20, header.size + footer.size + lhQuineSize + 5 + header.size + footer.size + lhQuineSize + 10 + header2.size + footer.size + 14)
        val cdAfterQuine = fullZipFile.copyOfRange(header.size + footer.size + lhQuineSize + 5 + header.size + footer.size + lhQuineSize + 10 + header2.size + footer.size + 18, header.size + footer.size + lhQuineSize + quineSize + indexOfCdInFooter + 16)
        val lastCd = fullZipFile.copyOfRange(header.size + footer.size + lhQuineSize + quineSize + indexOfCdInFooter + 20, fullZipFile.size)

        val resultFound = AtomicBoolean(false)
        val doneIterations = AtomicLong(0L)
        val totalIterations = UInt.MAX_VALUE.toLong()
        val result = AtomicReference<ByteArray>()

        val latch = CountDownLatch(numThreads)
        print("Starting brute-forcing the CRC32 using $numThreads threads... (0 / ${totalIterations})\r")
        for (i in 0 until numThreads) {
            val start = range.first + i * segmentSize
            val end = if (i == numThreads - 1) range.last else start + segmentSize - 1

            val thread = Thread {
                var currentTime = System.currentTimeMillis()
                val crcSecondZip = getByteArrayOf4Bytes(0)
                val crcFirstZip = getByteArrayOf4Bytes(crcEndofCd)
                val prevCalculatedPart = cdAfterHeader + crcSecondZip + lhQuineAfterFooter + crcSecondZip + cdOfSecondZip

		// basically fullZipFile???
                var currentCrcFile = crcFirstZip + lhOfSecondZip + crcFirstZip + cdOfFirstZip + crcSecondZip + lhOfFirstZip + crcSecondZip + cdAfterQuine + crcSecondZip + lastCd


                currentCrcFile = prevCalculatedPart + currentCrcFile

                val bytesBeforeReset = currentCrcFile.copyOfRange(0, indexOfReset)
                val bytesAfterReset = currentCrcFile.copyOfRange(indexOfReset + 4, currentCrcFile.size)
                val indexOfCdQuine = findLastSublistOfByteArray(bytesAfterReset, byteArrayOf((80).toByte(), (75).toByte(), (1).toByte(), (2).toByte()))
                val indexOfCrcInCdQuine = indexOfCdQuine + 16

                val zip2 = secondZip.clone()
                for (crc in start..end) {
                    // This has to go in the quine and the headers of the zip we are making
                    val newCrc = getByteArrayOf4Bytes(crc)

                    bytesAfterReset[indexOfCrcInCdQuine] = newCrc[0]
                    bytesAfterReset[indexOfCrcInCdQuine + 1] = newCrc[1]
                    bytesAfterReset[indexOfCrcInCdQuine + 2] = newCrc[2]
                    bytesAfterReset[indexOfCrcInCdQuine + 3] = newCrc[3]

                    // This has to go in the quine itself
                    val crcInt = calculateCRC32(bytesAfterReset, 0x00000000)
                    val crcOfZip1 = getByteArrayOf4Bytes(crcInt)

                    // Set calculated CRC in quine
                    currentCrcFile[prevCalculatedPart.size] = crcOfZip1[0]
                    currentCrcFile[prevCalculatedPart.size + 1] = crcOfZip1[1]
                    currentCrcFile[prevCalculatedPart.size + 2] = crcOfZip1[2]
                    currentCrcFile[prevCalculatedPart.size + 3] = crcOfZip1[3]

                    bytesBeforeReset[prevCalculatedPart.size] = crcOfZip1[0]
                    bytesBeforeReset[prevCalculatedPart.size + 1] = crcOfZip1[1]
                    bytesBeforeReset[prevCalculatedPart.size + 2] = crcOfZip1[2]
                    bytesBeforeReset[prevCalculatedPart.size + 3] = crcOfZip1[3]

                    zip2[prevCalculatedPart.size] = newCrc[0]
                    zip2[prevCalculatedPart.size + 1] = newCrc[1]
                    zip2[prevCalculatedPart.size + 2] = newCrc[2]
                    zip2[prevCalculatedPart.size + 3] = newCrc[3]

                    currentCrcFile[prevCalculatedPart.size + 4 + lhOfSecondZip.size] = crcOfZip1[0]
                    currentCrcFile[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 1] = crcOfZip1[1]
                    currentCrcFile[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 2] = crcOfZip1[2]
                    currentCrcFile[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 3] = crcOfZip1[3]

                    bytesBeforeReset[prevCalculatedPart.size + 4 + lhOfSecondZip.size] = crcOfZip1[0]
                    bytesBeforeReset[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 1] = crcOfZip1[1]
                    bytesBeforeReset[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 2] = crcOfZip1[2]
                    bytesBeforeReset[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 3] = crcOfZip1[3]

                    zip2[prevCalculatedPart.size + 4 + lhOfSecondZip.size] = newCrc[0]
                    zip2[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 1] = newCrc[1]
                    zip2[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 2] = newCrc[2]
                    zip2[prevCalculatedPart.size + 4 + lhOfSecondZip.size + 3] = newCrc[3]

                    // Set bruteforced CRC in headers and quine
                    var index = cdAfterHeader.size
                    currentCrcFile[index] = newCrc[0]
                    currentCrcFile[index + 1] = newCrc[1]
                    currentCrcFile[index + 2] = newCrc[2]
                    currentCrcFile[index + 3] = newCrc[3]

                    bytesBeforeReset[index] = newCrc[0]
                    bytesBeforeReset[index + 1] = newCrc[1]
                    bytesBeforeReset[index + 2] = newCrc[2]
                    bytesBeforeReset[index + 3] = newCrc[3]

                    zip2[index] = crcOfZip1[0]
                    zip2[index + 1] = crcOfZip1[1]
                    zip2[index + 2] = crcOfZip1[2]
                    zip2[index + 3] = crcOfZip1[3]

                    index += 4 + lhQuineAfterFooter.size
                    currentCrcFile[index] = newCrc[0]
                    currentCrcFile[index + 1] = newCrc[1]
                    currentCrcFile[index + 2] = newCrc[2]
                    currentCrcFile[index + 3] = newCrc[3]

                    bytesBeforeReset[index] = newCrc[0]
                    bytesBeforeReset[index + 1] = newCrc[1]
                    bytesBeforeReset[index + 2] = newCrc[2]
                    bytesBeforeReset[index + 3] = newCrc[3]

                    zip2[index] = crcOfZip1[0]
                    zip2[index + 1] = crcOfZip1[1]
                    zip2[index + 2] = crcOfZip1[2]
                    zip2[index + 3] = crcOfZip1[3]

                    index += 4 + cdOfSecondZip.size + 4 + lhOfSecondZip.size + 4 + cdOfFirstZip.size
                    currentCrcFile[index] = newCrc[0]
                    currentCrcFile[index + 1] = newCrc[1]
                    currentCrcFile[index + 2] = newCrc[2]
                    currentCrcFile[index + 3] = newCrc[3]

                    bytesBeforeReset[index] = newCrc[0]
                    bytesBeforeReset[index + 1] = newCrc[1]
                    bytesBeforeReset[index + 2] = newCrc[2]
                    bytesBeforeReset[index + 3] = newCrc[3]

                    zip2[index] = crcOfZip1[0]
                    zip2[index + 1] = crcOfZip1[1]
                    zip2[index + 2] = crcOfZip1[2]
                    zip2[index + 3] = crcOfZip1[3]

                    index += 4 + lhOfFirstZip.size
                    currentCrcFile[index] = newCrc[0]
                    currentCrcFile[index + 1] = newCrc[1]
                    currentCrcFile[index + 2] = newCrc[2]
                    currentCrcFile[index + 3] = newCrc[3]

                    bytesBeforeReset[index] = newCrc[0]
                    bytesBeforeReset[index + 1] = newCrc[1]
                    bytesBeforeReset[index + 2] = newCrc[2]
                    bytesBeforeReset[index + 3] = newCrc[3]

                    zip2[index] = crcOfZip1[0]
                    zip2[index + 1] = crcOfZip1[1]
                    zip2[index + 2] = crcOfZip1[2]
                    zip2[index + 3] = crcOfZip1[3]

                    index += 4 + cdAfterQuine.size
                    currentCrcFile[index] = newCrc[0]
                    currentCrcFile[index + 1] = newCrc[1]
                    currentCrcFile[index + 2] = newCrc[2]
                    currentCrcFile[index + 3] = newCrc[3]

                    zip2[index] = crcOfZip1[0]
                    zip2[index + 1] = crcOfZip1[1]
                    zip2[index + 2] = crcOfZip1[2]
                    zip2[index + 3] = crcOfZip1[3]

                    // Calculate reset bytes
                    val resetCrc = calculateCRC32(bytesBeforeReset)
                    val resetBytes = getByteArrayOf4Bytes(resetCrc.inv())

                    currentCrcFile[indexOfReset] = resetBytes[0]
                    currentCrcFile[indexOfReset + 1] = resetBytes[1]
                    currentCrcFile[indexOfReset + 2] = resetBytes[2]
                    currentCrcFile[indexOfReset + 3] = resetBytes[3]

                    zip2[indexOfReset] = resetBytes[0]
                    zip2[indexOfReset + 1] = resetBytes[1]
                    zip2[indexOfReset + 2] = resetBytes[2]
                    zip2[indexOfReset + 3] = resetBytes[3]

                    // Change to check CRC of zip2
                    if (calculateCRC32(zip2) == crc) {
                        result.set(currentCrcFile.clone())
                        resultFound.set(true)
                        break
                    }

                    // Every 10 seconds update the progress
                    val iter = doneIterations.incrementAndGet()
                    if (System.currentTimeMillis() - currentTime > 10000) {
                        currentTime = System.currentTimeMillis()
                        print("Starting brute-forcing the CRC32 using $numThreads threads... (${(iter.toDouble() * 100 / totalIterations).toString().take(4)}%)\r")
                    }

                }
                latch.countDown()
            }

            thread.start()
        }

        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        println("Starting brute-forcing the CRC32... Done (${doneIterations.get()} / $totalIterations)")

        return if (resultFound.get()) {
            result.get()
        } else {
            println("Warning: No CRC32 is found, using 0 instead.")
            val byteFormOfCrc = getByteArrayOf4Bytes(0)
            cdAfterHeader + byteFormOfCrc + lhQuineAfterFooter + byteFormOfCrc + cdOfSecondZip + byteFormOfCrc + lhOfSecondZip + byteFormOfCrc + lhOfFirstZip + byteFormOfCrc + cdAfterQuine + byteFormOfCrc + lastCd
        }
    }


    /**
     * Calculate the CRC-32 table, so that it does not need to be recalculated when calling calculateCRC32()
     */
    private fun calculateTable() {
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
    }

    /**
     * Calculate the CRC-32 checksum for a ByteArray
     * More info: https://en.wikipedia.org/wiki/Cyclic_redundancy_check
     *
     * @param byteArray the ByteArray for which we calculate the CRC-32 checksum
     * @return CRC-32 checksum
     */
    fun calculateCRC32(byteArray: ByteArray): Int {
        // Reference implementation that I ported to kotlin
        // https://www.rosettacode.org/wiki/CRC-32#C
        val crc32 = calculateCRC32Loop(byteArray)
        return crc32.inv() and 0xFFFFFFFF.toInt()
    }

    /**
     * This function contains the loop of the CRC32 calculation and allows for the use of a precalculated part,
     * as well as when nothing is precomputed
     *
     * @param byteArray is the ByteArray for which we calculate the checksum
     * @param crc32Start is the crc value from which we work further, the default value is the normal CRC32 without pre-computation
     * return the (almost) crc32 value that is calculated, only some minor logic operations need to be performed for the final CRC32
     */
    fun calculateCRC32Loop(byteArray: ByteArray, crc32Start: Int = 0xFFFFFFFF.toInt()): Int {
        var crc32 = crc32Start
        for (byte in byteArray) {
            val index = (crc32 and 0xFF) xor byte.toUByte().toInt()
            crc32 = (crc32 ushr 8) xor crc32Table[index]
        }
        return crc32
    }

    /**
     * Calculate the CRC-32 checksum for a ByteArray, when there is already a part precalculated
     *
     * @param byteArray the ByteArray for which we calculate the CRC-32 checksum
     * @param prevCalculatedCrc is the precalculated value from calculateCRC32Loop
     * @return CRC-32 checksum
     */
    fun calculateCRC32(byteArray: ByteArray, prevCalculatedCrc: Int): Int {
        val crc32 = calculateCRC32Loop(byteArray, crc32Start = prevCalculatedCrc)
        return crc32.inv() and 0xFFFFFFFF.toInt()
    }
}
