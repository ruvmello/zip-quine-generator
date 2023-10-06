package lz77

data class LZ77Literal(val char: Byte): LZ77Token {
    override fun toString(): String {
        return char.toInt().toChar().toString()
    }
}