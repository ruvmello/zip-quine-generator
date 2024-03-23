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

    fun bruteforceLoop(fullZipFile: ByteArray, header: ByteArray, header2: ByteArray, footer: ByteArray, footer2: ByteArray, lhQuineSize: Int, quineSize: Int): ByteArray {
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

        // Calculate the indexes to create the second zip programmatically without actually unzipping
        val p1AndS1AndLh = header.size + footer.size + lhQuineSize
        val indexOfp2AndS2AndLhInQuine = p1AndS1AndLh + 5 + p1AndS1AndLh + 10
        val firstQuineL = fullZipFile.copyOfRange(p1AndS1AndLh, p1AndS1AndLh + 5)
        val twoLtokensBetweenSwap = fullZipFile.copyOfRange(p1AndS1AndLh + 5 + p1AndS1AndLh, p1AndS1AndLh + 5 + p1AndS1AndLh + 10)
        val restOfQuine = fullZipFile.copyOfRange(p1AndS1AndLh + 5 + p1AndS1AndLh + 10 + p1AndS1AndLh, p1AndS1AndLh + quineSize)

        val resultFound = AtomicBoolean(false)
        val doneIterations = AtomicLong(0L)
        val totalIterations = UInt.MAX_VALUE.toULong() * UInt.MAX_VALUE.toULong()
        val result = AtomicReference<ByteArray>()

        val latch = CountDownLatch(numThreads)
        print("Starting brute-forcing the CRC32 using $numThreads threads... (0 / ${totalIterations})\r")
        for (i in 0 until numThreads) {
            val start = range.first + i * segmentSize
            val end = if (i == numThreads - 1) range.last else start + segmentSize - 1

            val thread = Thread {
                var currentTime = System.currentTimeMillis()
                var index = cdAfterHeader.size
                var index2 = 0
                var byteFormOfCrc = getByteArrayOf4Bytes(0)
                val prevCalculatedPart = cdAfterHeader + byteFormOfCrc + lhQuineAfterFooter + byteFormOfCrc + cdOfSecondZip
                var currentCrcFile = byteFormOfCrc + lhOfSecondZip + byteFormOfCrc + cdOfFirstZip + byteFormOfCrc + lhOfFirstZip + byteFormOfCrc + cdAfterQuine + byteFormOfCrc + lastCd
                for (crc in start..end) {
                    byteFormOfCrc = getByteArrayOf4Bytes(crc)

                    prevCalculatedPart[index] = byteFormOfCrc[0]
                    prevCalculatedPart[index + 1] = byteFormOfCrc[1]
                    prevCalculatedPart[index + 2] = byteFormOfCrc[2]
                    prevCalculatedPart[index + 3] = byteFormOfCrc[3]
                    index += lhQuineAfterFooter.size + 4

                    prevCalculatedPart[index] = byteFormOfCrc[0]
                    prevCalculatedPart[index + 1] = byteFormOfCrc[1]
                    prevCalculatedPart[index + 2] = byteFormOfCrc[2]
                    prevCalculatedPart[index + 3] = byteFormOfCrc[3]
                    index = cdAfterHeader.size

                    val prevCalculatedCrc = calculateCRC32Loop(prevCalculatedPart)
                    for (crc2 in start..end) {
                        if (resultFound.get()) {
                            break
                        }

                        val byteFormOfCrc2 = getByteArrayOf4Bytes(crc2)
                        currentCrcFile[index2] = byteFormOfCrc2[0]
                        currentCrcFile[index2 + 1] = byteFormOfCrc2[1]
                        currentCrcFile[index2 + 2] = byteFormOfCrc2[2]
                        currentCrcFile[index2 + 3] = byteFormOfCrc2[3]

                        index2 += 4 + lhOfSecondZip.size
                        currentCrcFile[index2] = byteFormOfCrc2[0]
                        currentCrcFile[index2 + 1] = byteFormOfCrc2[1]
                        currentCrcFile[index2 + 2] = byteFormOfCrc2[2]
                        currentCrcFile[index2 + 3] = byteFormOfCrc2[3]

                        index2 += 4 + cdOfFirstZip.size
                        currentCrcFile[index2] = byteFormOfCrc[0]
                        currentCrcFile[index2 + 1] = byteFormOfCrc[1]
                        currentCrcFile[index2 + 2] = byteFormOfCrc[2]
                        currentCrcFile[index2 + 3] = byteFormOfCrc[3]

                        index2 += 4 + lhOfFirstZip.size
                        currentCrcFile[index2] = byteFormOfCrc[0]
                        currentCrcFile[index2 + 1] = byteFormOfCrc[1]
                        currentCrcFile[index2 + 2] = byteFormOfCrc[2]
                        currentCrcFile[index2 + 3] = byteFormOfCrc[3]

                        index2 += 4 + cdAfterQuine.size
                        currentCrcFile[index2] = byteFormOfCrc[0]
                        currentCrcFile[index2 + 1] = byteFormOfCrc[1]
                        currentCrcFile[index2 + 2] = byteFormOfCrc[2]
                        currentCrcFile[index2 + 3] = byteFormOfCrc[3]

                        index2 = 0
                        if (calculateCRC32(currentCrcFile, prevCalculatedCrc) == crc) {
                            val tempFile = prevCalculatedPart + currentCrcFile
                            val p1AndS1AndLhBytes = tempFile.copyOfRange(0, p1AndS1AndLh)
                            val p2AndS2AndLhBytes = tempFile.copyOfRange(indexOfp2AndS2AndLhInQuine, indexOfp2AndS2AndLhInQuine + p1AndS1AndLh)
                            val secondZipFooter = p2AndS2AndLhBytes.copyOfRange(header.size, header.size + footer.size)
                            val secondZip = p2AndS2AndLhBytes + firstQuineL + p1AndS1AndLhBytes + twoLtokensBetweenSwap + p2AndS2AndLhBytes + restOfQuine + secondZipFooter
                            if (calculateCRC32(secondZip) == crc2) {
                                currentCrcFile = prevCalculatedPart + currentCrcFile
                                result.set(currentCrcFile.clone())
                                resultFound.set(true)
                                break
                            }
                        }

                        // Every 10 seconds update the progress
                        val iter = doneIterations.incrementAndGet()
                        if (System.currentTimeMillis() - currentTime > 10000) {
                            currentTime = System.currentTimeMillis()
                            print(
                                "Starting brute-forcing the CRC32 using $numThreads threads... (${iter} / ${totalIterations})\r"
                            )
                        }
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