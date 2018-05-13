package com.github.sereneant.segystream

import java.lang.{Float => JavaFloat}
import java.nio.ByteOrder

import akka.util.ByteIterator

object ConvertUtils {
  /**
    * Converts IMB/370 4-byte floating point value to float.
    */
  def ibm370toFloat(byteIt: ByteIterator): Float = {

    // internal bit conversion
    def bitTransform(ibmBits: Int): Int = {
      var mantissa: Int = 0x00ffffff & ibmBits
      if (mantissa == 0) 0
      else {
        var exponent: Int = ((0x7f000000 & ibmBits) >> 22) - 130
        while ((mantissa & 0x00800000) == 0) {
          mantissa <<= 1
          exponent -= 1
        }

        if (exponent <= 0) 0
        else {
          val sign: Int = 0x80000000 & ibmBits
          if (exponent >= 0xff) sign | 0x7f7fffff
          else sign | (exponent << 23) | (mantissa & 0x007fffff)
        }
      }
    }

    val tmpBits = byteIt.getInt(ByteOrder.BIG_ENDIAN)
    val transBits = bitTransform(tmpBits)
    val float = JavaFloat.intBitsToFloat(transBits)

    // Extra care for IEEE float special values
    if (JavaFloat.isInfinite(float) || JavaFloat.isNaN(float)) 0.0f
    else float
  }

}
