package zip

import utils.getByteArrayOf4Bytes

class CRC32Engine {
    companion object {
        val POLYNOMIAL = 0x104c11db7UL
    }
}

/**
 * Calculates a CRC
 *
 * @param data The data to calculate
 * @return The CRC32 checksum
 */
fun CRC32Engine.Companion.calculateCRC(data: ByteArray): ByteArray {
    var ret = 0xffffffffU
    for (byte in data) {
        ret = ret xor (byte.toUInt() and 0xffU)
        for (i in 0 until 8) {
            ret = if (ret and 1U == 1U) {
                (ret shr 1) xor 0xEDB88320U
            } else {
                ret shr 1
            }
        }
    }
    return getByteArrayOf4Bytes(ret.inv().toInt())
}

/**
 * Solves a "rank 1" CRC. This means that there's one file with one CRC value
 * which may have to occur within the file itself. For example, the string
 *
 * "The CRC-32 checksum of this file is \"\x36\x4a\x09\xca\""
 *
 * (sourced from https://natechoe.dev/blog/2025/08/25/0-migrated.html)
 *
 * is a rank-1 CRC, as the bytes of the CRC appear within the message itself.
 *
 * This is a specific case of rank-n CRCs, where we may have multiple files with
 * checksums that appear in each other.
 *
 * This overwrites the bytes in `data`
 *
 * @param data is the data of the quine, with 4 nonce bytes at each of the offsets
 * @param offsets are the locations where the CRC appears within the file.
 */
fun CRC32Engine.Companion.solveRank1CRC(data: ByteArray, offsets: Set<Int>) {
    val map = HashMap<Int, Int>()
    for (offset in offsets) {
        map.put(offset, 0)
    }

    var crcBytesValues = solveCRCSystem(Pair(data, map))
    var crcBytes = crcBytesValues[0]
    for (offset in offsets) {
        data[offset+0] = crcBytes[0]
        data[offset+1] = crcBytes[1]
        data[offset+2] = crcBytes[2]
        data[offset+3] = crcBytes[3]
    }
}

/**
 * Solves a CRC system with multiple files which potentially reference each
 * others's CRCs using Gauss-Jordan elimination.
 *
 * @param files A list of files to solve for CRCs. The associated map goes from
 * offsets to file IDs, where 0 is the first file, 1 is the second, and so on.
 * @return The CRCs of each file
 */
fun CRC32Engine.Companion.solveCRCSystem(vararg files: Pair<ByteArray, Map<Int, Int>>): Array<ByteArray> {
    val n = files.size

    // Augmented matrix of polynomials
    var matrix = Array(n) { Array(n+1) {0UL} }

    // Construct augmented matrix
    for (file in 0 until n) {
        var i = 0
        val data = files[file].first
        val offsets = files[file].second
        matrix[file][n] = 0xffffffffUL

        while (i < data.size) {
            if (offsets.containsKey(i)) {
                val offset = offsets.get(i)!!
                matrix[file][offset] = matrix[file][offset] xor 1UL
                for (j in 0..n) {
                    matrix[file][j] = multiply(matrix[file][j], 0x100000000UL, POLYNOMIAL)
                }
                i += 4
                continue
            }

            for (j in 0 until 8) {
                if (((data[i].toInt() and 0xff) and (1 shl j)) != 0) {
                    matrix[file][n] = matrix[file][n] xor 0x80000000UL
                }
                matrix[file][n] = multiply(matrix[file][n], 2UL, POLYNOMIAL)
            }
            for (j in 0 until n) {
                matrix[file][j] = multiply(matrix[file][j], 0x100UL, POLYNOMIAL)
            }

            i++
        }
    }

    for (file in 0 until n) {
        matrix[file][file] = matrix[file][file] xor 1UL
        matrix[file][n] = matrix[file][n] xor 0xffffffffUL
    }

    // Convert to upper triangular matrix
    for (solvingRow in 0 until n) {
        var exchange = solvingRow
        while (exchange < n) {
            if (matrix[exchange][solvingRow] != 0UL) {
                break
            }
            exchange++
        }

        // The matrix rank is less than n, so there isn't a unique solution.
        // This assertion will almost never fail, although it technically could.
        // If it does fail, that means that there is no solution or more than
        // one solution.
        assert(exchange < n) {"Gauss-Jordan elimination failed while solving CRC"}

        // Swap in a good row
        if (solvingRow != exchange) {
            val backup = matrix[solvingRow]
            matrix[solvingRow] = matrix[exchange]
            matrix[exchange] = backup
        }

        // Eliminate all the other rows

        val inverse = minv(matrix[solvingRow][solvingRow], POLYNOMIAL)
        for (eliminatingRow in solvingRow+1 until n) {
            val anchor = matrix[eliminatingRow][solvingRow]
            val multiplier = multiply(inverse, anchor, POLYNOMIAL)
            for (eliminatingColumn in 0..n) {
                matrix[eliminatingRow][eliminatingColumn] = matrix[eliminatingRow][eliminatingColumn] xor multiply(multiplier, matrix[solvingRow][eliminatingColumn], POLYNOMIAL)
            }
        }
    }

    // Solve the simplified system
    var results = Array(n) { 0UL }
    for (row in n-1 downTo 0) {
        results[row] = matrix[row][n]
        for (column in row+1 until n) {
            results[row] = results[row] xor multiply(results[column], matrix[row][column], POLYNOMIAL)
        }
        results[row] = divide(results[row], matrix[row][row], POLYNOMIAL)
    }

    return Array(n,
        fun(i: Int): ByteArray {
            var implementationValue = 0UL
            val mathematicalValue = results[i]
            for (bit in 0 until 32) {
                if ((mathematicalValue and (1UL shl bit)) != 0UL) {
                    implementationValue = implementationValue or (1UL shl (31 - bit))
                }
            }
            return getByteArrayOf4Bytes(implementationValue.toInt())
        }
    )
}

/**
 * Multiplies two polynomials
 *
 * @param p1 Polynomial 1
 * @param p2 Polynomial 2
 * @return p1*p2 as a polynomial multiplication with no modulus
 * */
private fun CRC32Engine.Companion.multiplyRaw(p1: ULong, p2: ULong): ULong {
    var ret: ULong = 0UL
    val probe = findProbe(p1)

    for (i in 0 until 64) {
        if ((p2 and (1UL shl i)) != 0UL) {
            assert(probe + i < 64) {"Polynomial multiplication had an overflow"}
            ret = ret xor (p1 shl i)
        }
    }

    return ret
}

/**
 * Finds the highest bit of p
 *
 * @param p Some ULong
 * @return The greatest value b such that (1LU shl b) and p, or 0 if p==0
 * */
private fun CRC32Engine.Companion.findProbe(p: ULong): Int {
    var ret = 0

    for (i in 0 until 64) {
        if ((1UL shl i) and p != 0UL) {
            ret = i
        }
    }

    return ret
}

/**
 * Divides two polynomials, and returns the pair (quotient, remainder)
 *
 * @param dividend The numerator
 * @param divisor The denominator
 * @return The pair (quotient, remainder)
 */
private fun CRC32Engine.Companion.divmod(dividend: ULong, divisor: ULong): Pair<ULong, ULong> {
    val probe = findProbe(divisor)
    val probeBit = 1UL shl probe
    var shiftAmount = 63 - probe
    var quot = 0UL
    var rem = dividend

    for (i in shiftAmount downTo 0) {
        if ((rem and (probeBit shl i)) != 0UL) {
            quot = quot or (1UL shl i)
            rem = rem xor (divisor shl i)
        }
    }

    return Pair(quot, rem)
}

/**
 * Multiplies two polynomials with a modulus
 *
 * @param p1 The first polynomial
 * @param p2 The second polynomial
 * @param mod The modulus
 * @return p1*p2%mod as polynomials
 */
private fun CRC32Engine.Companion.multiply(p1: ULong, p2: ULong, mod: ULong): ULong {
    val rawProduct = multiplyRaw(p1, p2)
    return divmod(rawProduct, mod).second

}

/**
 * Calculates Bezout coefficients of two polynomials with the extended Euclidean
 * algorithm
 *
 * @param p1 The first polynomial
 * @param p2 The second polynomial
 * @return (k1, k2, gcd) where p1*k1 + p2*k2 = gcd */
private fun CRC32Engine.Companion.xgcd(p1: ULong, p2: ULong): Triple<ULong, ULong, ULong> {
    if (findProbe(p1) < findProbe(p2)) {
        val r = xgcd(p2, p1)
        return Triple(r.second, r.first, r.third)
    }

    if (p2 == 0UL) {
        return Triple(p1, 0UL, p1)
    }

    val nextArgs = divmod(p1, p2)
    val nextLevel = xgcd(p2, nextArgs.second)

    val c1 = nextLevel.second
    val c2 = nextLevel.first xor multiplyRaw(nextLevel.second, nextArgs.first)
    return Triple(c1, c2, nextLevel.third)
}

/**
 * Calculates the multiplicative inverse of a polynomial over a modulus
 *
 * @param p The polynomial
 * @param mod The modulus
 * @return p^-1 over mod, or 0 if there isn't one
 */
private fun CRC32Engine.Companion.minv(p: ULong, mod: ULong): ULong {
    val r = xgcd(p, mod)
    if (r.third != 1UL) {
        return 0UL
    }

    return divmod(r.first, mod).second
}

/**
 * Divides two polynomials over a modulus
 *
 * @param dividend The numerator
 * @param divisor The denominator
 * @param mod The modulus
 * @return dividend/divisor over mod, or 0 if this isn't possible
 */
private fun CRC32Engine.Companion.divide(dividend: ULong, divisor: ULong, mod: ULong): ULong {
    return multiply(dividend, minv(divisor, mod), mod)
}
