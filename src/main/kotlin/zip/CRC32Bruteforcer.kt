package zip

import utils.findLastSublistOfByteArray
import utils.getByteArrayOf4Bytes
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch

class CRC32Bruteforcer(private val numThreads: Int) {
    private val crc32Table = IntArray(256)
    
    init {
        this.calculateTable()
    }

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
        val result = AtomicReference<ByteArray>()

        val latch = CountDownLatch(numThreads)
        print("Starting bruteforcing the CRC32 using ${numThreads} threads... (this might take a while)\r")
        for (i in 0 until numThreads) {
            val start = range.first + i * segmentSize
            val end = if (i == numThreads - 1) range.last else start + segmentSize - 1

            val thread = Thread {
                for (crc in start..end) {
                    if (resultFound.get()) {
                        break
                    }

                    val byteFormOfCrc = getByteArrayOf4Bytes(crc)
                    var currentCrcFile = byteFormOfCrc + secondPartLh + byteFormOfCrc + firstPartCd + byteFormOfCrc + secondPartCd + byteFormOfCrc + lastPartCd

                    if (calculateCRC32(currentCrcFile, prevCalculatedCrc) == crc) {
                        currentCrcFile = firstPartLh + currentCrcFile
                        result.set(currentCrcFile.clone())
                        resultFound.set(true)
                        break
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

        println("Starting bruteforcing the CRC32... Done")

        return if (resultFound.get()) {
            result.get()
        } else {
            println("Warning: No CRC32 is found, using 0 instead.")
            val byteFormOfCrc = getByteArrayOf4Bytes(0)
            firstPartLh + byteFormOfCrc + secondPartLh + byteFormOfCrc + firstPartCd + byteFormOfCrc + secondPartCd + byteFormOfCrc + lastPartCd
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