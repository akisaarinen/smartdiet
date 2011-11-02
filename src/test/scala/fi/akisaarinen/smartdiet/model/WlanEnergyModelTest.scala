package fi.akisaarinen.smartdiet.model

import org.scalatest.FunSuite
import fi.akisaarinen.smartdiet.measurement.networkpacket._

class WlanEnergyModelTest extends FunSuite {
  val model = new WlanEnergyModel

  test("without packets") {
    expect(0.0) { model.calculate(List()).energyJoules }
  }
  test("single incoming packet") {
    val packets = packet(0,103,In) :: Nil
    expect(0.088842875) { model.calculate(packets).energyJoules }
  }
  test("single incoming burst") {
    val packets =
      packet(10,103,In) ::
      packet(20,123,In) ::
      packet(30,432,Out) ::
      packet(60,432,In) ::
      packet(110,423,In) ::
      Nil
    expect(0.17904325) { model.calculate(packets).energyJoules }
  }
  test("three separate bursts") {
    val packets =
      packet(0,103,In) ::
      packet(100,123,In) ::
      packet(1200,432,Out) ::
      packet(1240,432, Out) ::
      packet(8240,432,In) ::
      Nil
    expect(0.723032125) { model.calculate(packets).energyJoules }
  }

  def packet(timestamp: Long, size: Int, direction: Direction) = {
    NetworkPacket(timestamp, 1, direction, UnknownEvent, size, 0, 0)
  }
}