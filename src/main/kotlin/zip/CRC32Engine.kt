package zip

import utils.getByteArrayOf4Bytes

class CRC32Engine {
    companion object {
        val POLYNOMIAL = 0x104c11db7UL
    }
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
 * checksums that appear in each other. Generally rank-n CRCs can be solved with
 * Gaussian elimination over GF(2^32), but that seems a bit excessive for this
 * use case.
 *
 * This overwrites the bytes in `data`
 *
 * @param data is the data of the quine, with 4 nonce bytes at each of the offsets
 * @param offsets are the locations where the CRC appears within the file.
 */
fun CRC32Engine.Companion.solveRank1CRC(data: ByteArray, offsets: Set<Int>) {
    var i = 0
    var basePolynomial = 0xffffffffUL
    var offsetsPolynomial = 0UL
    while (i < data.size) {
        if (offsets.contains(i)) {
            basePolynomial = multiply(basePolynomial, 0x100000000UL, POLYNOMIAL)
            offsetsPolynomial = offsetsPolynomial xor 1UL
            offsetsPolynomial = multiply(offsetsPolynomial, 0x100000000UL, POLYNOMIAL)
            i += 4
            continue
        }

        for (j in 0..<8) {
            if (((data[i].toInt() and 0xff) and (1 shl j)) != 0) {
                basePolynomial = basePolynomial xor 0x80000000UL
            }
            basePolynomial = multiply(basePolynomial, 2UL, POLYNOMIAL)
        }
        offsetsPolynomial = multiply(offsetsPolynomial, 0x100UL, POLYNOMIAL)

        i++
    }
    val mathematicalValue = divide(basePolynomial xor 0xffffffffUL, offsetsPolynomial xor 1UL, POLYNOMIAL)
    var implementationValue = 0UL
    for (i in 0..<32) {
        if ((mathematicalValue and (1UL shl i)) != 0UL) {
            implementationValue = implementationValue or (1UL shl (31 - i))
        }
    }
    var crcBytes = getByteArrayOf4Bytes(implementationValue.toInt())
    for (offset in offsets) {
        data[offset+0] = crcBytes[0]
        data[offset+1] = crcBytes[1]
        data[offset+2] = crcBytes[2]
        data[offset+3] = crcBytes[3]
    }
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

    for (i in 0..<64) {
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

    for (i in 0..<64) {
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
