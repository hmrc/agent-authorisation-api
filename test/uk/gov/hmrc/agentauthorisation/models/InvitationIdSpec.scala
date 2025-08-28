/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentauthorisation.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.agentauthorisation.models.InvitationId.{byteToBitsLittleEndian, bytesTo5BitNums, checksumDigits, to5BitAlphaNumeric, to5BitNum}

import java.nio.charset.StandardCharsets
import java.time.LocalDate

class InvitationIdSpec extends AnyFlatSpec with Matchers {
  val invWithoutPrefix = (prefix: Char) =>
    InvitationId.create("myAgency", "clientId", "service", LocalDate.parse("2001-01-01").atStartOfDay())(prefix)

  "create" should "add prefix to start of identifier" in {
    invWithoutPrefix('A').value.head shouldBe 'A'
    invWithoutPrefix('B').value.head shouldBe 'B'
    invWithoutPrefix('C').value.head shouldBe 'C'
  }

  it should "create an identifier 13 characters long" in {
    invWithoutPrefix('A').value.length shouldBe 13
  }

  it should "append two alphanumeric checksum characters using CRC-10" in {
    invWithoutPrefix('A').value.takeRight(2) shouldBe checksumDigits("ABERULMHCKK")
    invWithoutPrefix('B').value.takeRight(2) shouldBe checksumDigits("BBERULMHCKK")
    invWithoutPrefix('C').value.takeRight(2) shouldBe checksumDigits("CBERULMHCKK")
  }

  it should "create a valid identifier" in {
    InvitationId.isValid(invWithoutPrefix('A').value) shouldBe true
  }

  it should "give a different identifier whenever any of the arguments change" in {
    val agency = "agency"
    val clientId = "clientId"
    val service = "service"
    val time = LocalDate.parse("2001-01-01").atStartOfDay()
    implicit val prefix = 'A'

    val invA = InvitationId.create(agency, clientId, service, time).value
    val invB = InvitationId.create("different", clientId, service, time).value
    val invC = InvitationId.create(agency, "different", service, time).value
    val invD = InvitationId.create(agency, clientId, "different", time).value
    val invE = InvitationId.create(agency, clientId, service, LocalDate.parse("1999-01-01").atStartOfDay()).value
    val invF = InvitationId.create(agency, clientId, service, LocalDate.parse("1999-01-01").atStartOfDay())('Z').value

    Set(invA, invB, invC, invD, invE, invF).size shouldBe 6
  }

  "byteToBitsLittleEndian" should "return little endian bit sequences (LSB first) for signed bytes" in {
    byteToBitsLittleEndian(0) shouldBe Seq(false, false, false, false, false, false, false, false)
    byteToBitsLittleEndian(1) shouldBe Seq(true, false, false, false, false, false, false, false)
    byteToBitsLittleEndian(2) shouldBe Seq(false, true, false, false, false, false, false, false)
    byteToBitsLittleEndian(3) shouldBe Seq(true, true, false, false, false, false, false, false)
    byteToBitsLittleEndian(127) shouldBe Seq(true, true, true, true, true, true, true, false)
    byteToBitsLittleEndian(-1) shouldBe Seq(true, true, true, true, true, true, true, true)
    byteToBitsLittleEndian(-2) shouldBe Seq(false, true, true, true, true, true, true, true)
    byteToBitsLittleEndian(-128) shouldBe Seq(false, false, false, false, false, false, false, true)
  }

  it should "return a unique bit sequence for all possible byte values" in {
    (-128 to 127).map(x => byteToBitsLittleEndian(x.toByte)).toSet.size shouldBe 256
  }

  "to5BitNum" should "return a 5 bit number representing a 5-bit bit sequence" in {
    to5BitNum(Seq(false, false, false, false, false)) shouldBe 0
    to5BitNum(Seq(true, false, false, false, false)) shouldBe 1
    to5BitNum(Seq(false, true, false, false, false)) shouldBe 2
    to5BitNum(Seq(true, true, true, true, true)) shouldBe 31
  }

  it should "return a unique number for all possible 5-bit bit sequences" in {
    (-128 to 127).map(x => to5BitNum(byteToBitsLittleEndian(x.toByte).take(5))).toSet.size shouldBe 32
  }

  it should "throw IllegalArgumentException if the passed sequence does not contain 5 bits" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      to5BitNum(Seq.empty)
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      to5BitNum(Seq(true))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      to5BitNum(Seq(true, true, true, true))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      to5BitNum(Seq(true, true, true, true, true, true))
    }
  }

  "bytesTo5BitNums" should "take 8 bytes and return 10 5-bit numbers from of the bytes' bits" in {
    val ff = 0xff.toByte
    bytesTo5BitNums(Seq(0, 0, 0, 0, 0, 0, 0, 0)) shouldBe Seq(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    bytesTo5BitNums(Seq(1, 0, 0, 0, 0, 0, 0, 0)) shouldBe Seq(1, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    bytesTo5BitNums(Seq(0x0ff.toByte, 0, 0, 0, 0, 0, 0, 0)) shouldBe Seq(31, 7, 0, 0, 0, 0, 0, 0, 0, 0)
    bytesTo5BitNums(Seq(ff, ff, ff, ff, ff, ff, ff, ff)) shouldBe Seq(31, 31, 31, 31, 31, 31, 31, 31, 31, 31)
  }

  "to5BitAlphaNumeric" should "return a unique character for each of the 32 values of the 5 bit number" in {
    (0 to 31).map(to5BitAlphaNumeric).mkString shouldBe "ABCDEFGHJKLMNOPRSTUWXYZ123456789"
  }

  it should "throw an IllegalArgumentException if the passed number is not within a 5 bit number's range" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      to5BitAlphaNumeric(32)
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      to5BitAlphaNumeric(-1)
    }
  }

  "CRC10" should "be deterministic" in {
    CRC10.calculate(Array[Byte](1, 2)) shouldBe CRC10.calculate(Array[Byte](1, 2))
  }

  it should "not (usually) give same checksum for different input" in {
    CRC10.calculate(Array[Byte](1, 2)) should not be CRC10.calculate(Array[Byte](2, 1))
    CRC10.calculate(Array[Byte](1, 2, 3)) should not be CRC10.calculate(Array[Byte](1, 3, 2))
    CRC10.calculate(Array[Byte](1, 2, 3)) should not be CRC10.calculate(Array[Byte](3, 2, 3))
    CRC10.calculate(Array[Byte](1, 2, 3)) should not be CRC10.calculate(Array[Byte](3, 2, 1))
    CRC10.calculate(Array[Byte](1, 2, 3)) should not be CRC10.calculate(Array[Byte](3, 1, 2))
  }

  it should "provide checksum for many bytes" in {
    CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)) shouldBe CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8))
  }

  it should "produce a checksum that captures minor errors in large input" in {
    CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)) should not be CRC10.calculate(
      Array[Byte](1, 2, 3, 4, 5, 6, 7, 7)
    )
    CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)) should not be CRC10.calculate(
      Array[Byte](0, 2, 3, 4, 5, 6, 7, 8)
    )
    CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)) should not be CRC10.calculate(
      Array[Byte](1, 2, 3, 3, 5, 6, 7, 8)
    )
    CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)) should not be CRC10.calculate(
      Array[Byte](1, 2, 3, 4, 5, 0, 7, 8)
    )
    CRC10.calculate(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)) should not be CRC10.calculate(
      Array[Byte](0, 0, 0, 4, 5, 0, 7, 8)
    )
  }

  it should "produce checksums from either a String's bytes or bytes directly" in {
    CRC10.calculate("ABC") shouldBe CRC10.calculate("ABC".getBytes(StandardCharsets.UTF_8))
  }

  "isValid" should "be true for a valid InvitationId" in {
    InvitationId.isValid("ABERULMHCKKW3") shouldBe true
  }

  it should "be false when it has more than 13 digits" in {
    InvitationId.isValid("ABERULMHCKKW3A") shouldBe false
  }

  it should "be false when it is empty" in {
    InvitationId.isValid("") shouldBe false
  }

  it should "be false when it has non-alphanumeric characters" in {
    InvitationId.isValid("ABERUL!HCKKW3") shouldBe false
  }

  it should "be false when the checksum digits fail a checksum check" in {
    InvitationId.isValid("ABERULMHCKKW4") shouldBe false
  }
}
