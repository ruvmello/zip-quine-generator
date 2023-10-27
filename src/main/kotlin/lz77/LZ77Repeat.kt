package lz77

data class LZ77Repeat(val distance: Int, val length: Int): LZ77Token {
    override fun toString(): String {
        return "(${distance},${length})"
    }
}