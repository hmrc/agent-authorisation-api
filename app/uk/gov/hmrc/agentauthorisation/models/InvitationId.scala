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

import play.api.libs.json.{Format, __}

import java.security.MessageDigest
import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.collection.immutable.ArraySeq.unsafeWrapArray

case class InvitationId(value: String)

object InvitationId {
  private def idWrites =
    (__ \ "value")
      .write[String]
      .contramap((id: InvitationId) => id.value)

  private def idReads =
    (__ \ "value")
      .read[String]
      .map(x => InvitationId(x))

  implicit val idFormats: Format[InvitationId] = Format(idReads, idWrites)

  private val pattern = "^[ABCDEFGHJKLMNOPRSTUWXYZ123456789]{13}$".r

  def isValid(identifier: String): Boolean = identifier match {
    case pattern(_*) => checksumDigits(identifier.take(11)) == identifier.takeRight(2)
    case _           => false
  }

  def create(
    arn: String,
    clientId: String,
    serviceName: String,
    timestamp: LocalDateTime = Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime
  )(implicit prefix: Char): InvitationId = {
    val idUnhashed = s"$arn.$clientId,$serviceName-${timestamp.toInstant(ZoneOffset.UTC).toEpochMilli}"
    val idBytes = MessageDigest.getInstance("SHA-256").digest(idUnhashed.getBytes("UTF-8")).take(7)
    val idChars = bytesTo5BitNums(unsafeWrapArray(idBytes)).map(to5BitAlphaNumeric).mkString
    val idWithPrefix = s"$prefix$idChars"

    InvitationId(s"$idWithPrefix${checksumDigits(idWithPrefix)}")
  }

  def checksumDigits(toChecksum: String): String = {
    val checksum10Bits = CRC10.calculate(toChecksum)
    val lsb5BitsChecksum = to5BitAlphaNumeric(checksum10Bits & 0x1f)
    val msb5BitsChecksum = to5BitAlphaNumeric((checksum10Bits & 0x3e0) >> 5)

    s"$lsb5BitsChecksum$msb5BitsChecksum"
  }

  def byteToBitsLittleEndian(byte: Byte): Seq[Boolean] = {
    def isBitOn(bitPos: Int): Boolean = {
      val maskSingleBit = 0x01 << bitPos
      (byte & maskSingleBit) != 0
    }

    (0 to 7).map(isBitOn)
  }

  def to5BitNum(bitsLittleEndian: Seq[Boolean]): Int = {
    require(bitsLittleEndian.size == 5)

    bitsLittleEndian
      .take(5)
      .zipWithIndex
      .map { case (bit, power) => if (bit) 1 << power else 0 }
      .sum
  }

  def bytesTo5BitNums(bytes: Seq[Byte]): Seq[Int] = {
    require(bytes.nonEmpty)

    bytes
      .flatMap(byteToBitsLittleEndian)
      .grouped(5) // Group into chunks of 5 bits
      .collect { case x if x.size == 5 => to5BitNum(x) }
      .take(10) // Take only 10 5-bit numbers
      .toSeq
  }

  def to5BitAlphaNumeric(fiveBitNum: Int): Char = {
    require(fiveBitNum >= 0 && fiveBitNum <= 31)

    "ABCDEFGHJKLMNOPRSTUWXYZ123456789" (fiveBitNum)
  }
}

object CRC10 {

  /* Params for CRC-10 */
  val bitWidth = 10
  val poly = 0x233
  val initial = 0
  val xorOut = 0
  val widthMask: Int = (1 << bitWidth) - 1

  val table: Seq[Int] = {

    val top = 1 << (bitWidth - 1)

    for (i <- 0 until 256) yield {
      var crc = i << (bitWidth - 8)
      for (_ <- 0 until 8)
        crc = if ((crc & top) != 0) (crc << 1) ^ poly else crc << 1

      crc & widthMask
    }
  }

  def calculate(string: String): Int = calculate(string.getBytes())

  def calculate(input: Array[Byte]): Int = {
    val length = input.length
    var crc = initial ^ xorOut

    for (i <- 0 until length)
      crc = table(((crc >>> (bitWidth - 8)) ^ input(i)) & 0xff) ^ (crc << 8)
    crc & widthMask
  }
}
