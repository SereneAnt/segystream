package com.github.sereneant.segystream

import akka.util.ByteIterator
import akka.util.ByteIterator.ByteArrayIterator
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class ConvertUtilsTest extends WordSpec with Matchers with PropertyChecks {

  def genByteIterator(sizeFrom: Int, sizeTo: Int): Gen[ByteIterator] = Gen.listOfN(sizeTo, arbitrary[Byte])
    .suchThat(bytes => bytes.size >= sizeFrom)
    .map(bytes => ByteArrayIterator.empty ++ bytes)

  "ConvertUtils.ibm370toFloat" should {
    "fail when less then 4 bytes are given" in {
      forAll(genByteIterator(0, 3)) { it =>
        assertThrows[NoSuchElementException] {
          ConvertUtils.ibm370toFloat(it)
        }
      }
    }
    "succeed for any 4 bytes" in {
      forAll(genByteIterator(4, 4)) {
        ConvertUtils.ibm370toFloat
      }
    }
  }

}
