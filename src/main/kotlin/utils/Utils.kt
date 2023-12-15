package utils

/**
 * An integer has a size of 32 bits, get a UByteArray of the two least significant bytes
 *
 * @param input the integer for which we construct a UByteArray of size two
 * @return the bytearray of size two
 */
fun getByteArrayOf2Bytes(input: Int): ByteArray {
    return byteArrayOf((input shr 0).toByte(), (input shr 8).toByte())
}

/**
 * An integer has a size of 32 bits, get a UByteArray of the size four with the least significant byte first
 *
 * @param input the integer for which we construct a ByteArray of size four
 * @return the bytearray of size four
 */
fun getByteArrayOf4Bytes(input: Int): ByteArray {
    return byteArrayOf((input shr 0).toByte(), (input shr 8).toByte(), (input shr 16).toByte(), (input shr 24).toByte())
}

/**
 * Get the bytes inside the [input] and reverse the bit order.
 * So change 1000 0000 to 0000 0001.
 *
 * @param input the integer for which we construct a UByteArray of size four
 * @param n the total bits that are set
 * @return list of the reversed bytes
 */
fun getListOfNReversedBytes(input: UInt, n: Int): List<UByte> {

    val totalUBytes = n / 8
    val bytes = mutableListOf<UByte>()
    for (i in 1 ..  totalUBytes) {
        bytes.add(reverseBitsByte((input shr (n - 8 * i)).toUByte()))
    }
    return bytes
}

/**
 * Reverse the bits of a byte, so change from LSB to MSB.
 *
 * @param byte the byte that needs to be reversed
 * @return the reversed byte
 */
fun reverseBitsByte(byte: UByte): UByte {
    var result = 0
    var input = byte.toInt()

    for (i in 0 until 8) {
        result = (result shl 1) or (input and 1)
        input = input ushr 1
    }

    return result.toUByte()
}

/**
 * Reverse the bits of an integer, so change from LSB to MSB.
 *
 * @param int the integer that needs to be reversed
 * @return the reversed integer
 */
fun reverseBitsInt(int: Int): Int {
    var result = 0
    var input = int

    for (i in 0 until 32) {
        result = (result shl 1) or (input and 1)
        input = input ushr 1
    }

    return result
}

// Mapping of length base to code
// https://calmarius.net/?lang=en&page=programming%2Fzlib_deflate_quick_reference
val lengthMapStaticHuffman: Map<Int, Pair<Int, Int>> = mapOf(
    3 to Pair(257, 0),
    4 to Pair(258, 0),
    5 to Pair(259, 0),
    6 to Pair(260, 0),
    7 to Pair(261, 0),
    8 to Pair(262, 0),
    9 to Pair(263, 0),
    10 to Pair(264, 0),
    11 to Pair(265, 1),
    13 to Pair(266, 1),
    15 to Pair(267, 1),
    17 to Pair(268, 1),
    19 to Pair(269, 2),
    23 to Pair(270, 2),
    27 to Pair(271, 2),
    31 to Pair(272, 2),
    35 to Pair(273, 3),
    43 to Pair(274, 3),
    51 to Pair(275, 3),
    59 to Pair(276, 3),
    67 to Pair(277, 4),
    83 to Pair(278, 4),
    99 to Pair(279, 4),
    115 to Pair(280, 4),
    131 to Pair(281, 5),
    163 to Pair(282, 5),
    195 to Pair(283, 5),
    227 to Pair(284, 5),
    258 to Pair(285, 0)
)

// Mapping of distance base to code
// https://calmarius.net/?lang=en&page=programming%2Fzlib_deflate_quick_reference
val distanceMapStaticHuffman = mapOf(
    1 to Pair(0, 0),
    2 to Pair(1, 0),
    3 to Pair(2, 0),
    4 to Pair(3, 0),
    5 to Pair(4, 1),
    7 to Pair(5, 1),
    9 to Pair(6, 2),
    13 to Pair(7, 2),
    17 to Pair(8, 3),
    25 to Pair(9, 3),
    33 to Pair(10, 4),
    49 to Pair(11, 4),
    65 to Pair(12, 5),
    97 to Pair(13, 5),
    129 to Pair(14, 6),
    193 to Pair(15, 6),
    257 to Pair(16, 7),
    385 to Pair(17, 7),
    513 to Pair(18, 8),
    769 to Pair(19, 8),
    1025 to Pair(20, 9),
    1537 to Pair(21, 9),
    2049 to Pair(22, 10),
    3073 to Pair(23, 10),
    4097 to Pair(24, 11),
    6145 to Pair(25, 11),
    8193 to Pair(26, 12),
    12289 to Pair(27, 12),
    16385 to Pair(28, 13),
    24577 to Pair(29, 13))