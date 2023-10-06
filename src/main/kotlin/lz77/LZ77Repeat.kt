package lz77

data class LZ77Repeat(val offset: Int, val length: Int): LZ77Token {
    override fun toString(): String {
        return "(${offset},${length})"
    }
}