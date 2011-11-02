=begin
This file is part of SmartDiet.

Copyright (C) 2011, Aki Saarinen.

SmartDiet was developed in affiliation with Aalto University School 
of Science, Department of Computer Science and Engineering. For
more information about the department, see <http://cse.aalto.fi/>.

SmartDiet is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
 
SmartDiet is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SmartDiet.  If not, see <http://www.gnu.org/licenses/>.
=end
require File.dirname(__FILE__) + '/helper.rb'
require_src 'tcp_energy_estimator'
require_src 'network_trace_reader'

class TcpEnergyEstimatorTest < Test::Unit::TestCase

  DELTA = 0.0001

  def setup
    @tee = TcpEnergyEstimator.new
  end

  def test_empty_packets
    estimate = @tee.wlan_calculate_estimate([])
    assert_equal estimate.energy_joules, 0.0
  end

  def test_umts_empty_packets
    estimate = @tee.umts_calculate_estimate([])
    assert_equal estimate.energy_joules, 0.0
  end

  def test_single_incoming_packet
    packets = [
        {'timestamp' => 0, 'size' => 103, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN}
    ]
    assert_in_delta 0.088842875, @tee.wlan_calculate_estimate(packets).energy_joules, DELTA
    assert_in_delta 7.95985, @tee.umts_calculate_estimate(packets).energy_joules, DELTA
  end

  def test_single_incoming_burst
    packets = [
        {'timestamp' => 10, 'size' => 103, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN},
        {'timestamp' => 20, 'size' => 123, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN},
        {'timestamp' => 30, 'size' => 432, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_OUT},
        {'timestamp' => 60, 'size' => 423, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN},
        {'timestamp' => 110, 'size' => 423, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN}
    ]
    assert_in_delta 0.17904325, @tee.wlan_calculate_estimate(packets).energy_joules, DELTA
    assert_in_delta 8.057115, @tee.umts_calculate_estimate(packets).energy_joules, DELTA
  end

  def test_multiple_incoming_and_outgoing_bursts
    packets = [
        {'timestamp' => 0, 'size' => 103, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN},
        {'timestamp' => 100, 'size' => 123, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN},
        {'timestamp' => 1200, 'size' => 432, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_OUT},
        {'timestamp' => 1240, 'size' => 423, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_OUT},
        {'timestamp' => 8240, 'size' => 423, 'direction' => TcpEnergyEstimator::PACKET_DIRECTION_IN}
    ]
    assert_in_delta 0.723032125, @tee.wlan_calculate_estimate(packets).energy_joules, DELTA
    assert_in_delta 14.446036, @tee.umts_calculate_estimate(packets).energy_joules, DELTA
  end

  def test_real_data_single_connection
    reader = NetworkTraceReader.new(testfile_path("test-network-data.nw"))
    conn_packets = reader.packets.select { |p| p['conn_id'] == 32994 }
    assert_equal 11, conn_packets.size
    assert_in_delta 0.4645375, @tee.wlan_calculate_estimate(conn_packets).energy_joules, DELTA
    assert_in_delta 8.6037443, @tee.umts_calculate_estimate(conn_packets).energy_joules, DELTA
  end
end
