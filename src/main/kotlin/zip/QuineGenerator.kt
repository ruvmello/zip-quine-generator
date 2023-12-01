package zip

import huffman.HuffmanCompressor
import lz77.LZ77Literal
import lz77.LZ77Repeat
import lz77.LZ77Token
import utils.distanceMapStaticHuffman
import utils.getByteArrayOf2Bytes
import utils.lengthMapStaticHuffman
import java.io.File

class QuineGenerator(outputFilePath: String, debug: Boolean) {

    // Split the last repeat so that it ends on a byte boundary
    // Deprecated: Due to the padding in a literal block which follows every repeat
//    fun getRepeatsUntilByteBoundary(totalBitsSet: Int, distance: Int, length: Int): List<LZ77Token> {
//        // 7 bits to indicate end of static block
//        if ((totalBitsSet + getTotalBitsOfRepeat(length, distance) + 7) % 8 == 0) {
//            return listOf(LZ77Repeat(distance, length))
//        } else {
//            // Length is minimum three in deflate
//            for (i in 3..length) {
//                if (length - i >= 3) {
//                    val firstRepeatBits = getTotalBitsOfRepeat(i, distance)
//                    val secondRepeatBits = getTotalBitsOfRepeat(length - i, distance)
//                    if ((totalBitsSet + firstRepeatBits + secondRepeatBits + 7) % 8 == 0) {
//                        return listOf(LZ77Repeat(distance, i), LZ77Repeat(distance, length - i))
//                    }
//                }
//            }
//        }
//        return listOf()
//    }

}