package lz77

data class LZ77Literal(val char: UByte): LZ77Token {
    override fun toString(): String {
        return char.toInt().toChar().toString()
//        return ""
    }
}