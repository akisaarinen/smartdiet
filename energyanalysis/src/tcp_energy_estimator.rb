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
require 'rubygems'
require 'tree'
require 'stringio'

require File.dirname(__FILE__) + '/method_trace'

class TcpEnergyEstimator
  PACKET_DIRECTION_IN = 1
  PACKET_DIRECTION_OUT = 2

  class EnergyEstimate
    attr_accessor :energy_joules, :avg_power_watts

    def initialize(energy_joules, avg_power_watts)
      @energy_joules = energy_joules
      @avg_power_watts = avg_power_watts
    end
  end

  def wlan_calculate_estimate(packets, burst_threshold_ms = 6, psm_enabled = true)
    #puts "Calculating for #{packets.size} packets with burst threshold #{burst_threshold_ms}"
    packet_pairs = packets.take([packets.size - 1, 0].max).zip(packets.drop(1))
    energy_joules = 0.0
    avg_power_watts = 0.0

    if !packets.empty?
      current_burst = [packets.first]

      packet_pairs.each { |p1, p2|
        packet_interval = (p2['timestamp'] - p1['timestamp'])
        is_burst_ongoing = packet_interval <= burst_threshold_ms
        if is_burst_ongoing
          current_burst.push(p2)
        else
          energy_joules += wlan_energy_for_burst(current_burst, p2, psm_enabled)
          current_burst = [p2]
        end
      }

      energy_joules += wlan_energy_for_burst(current_burst, nil, psm_enabled)
    end

    EnergyEstimate.new(energy_joules, avg_power_watts)
  end

  def umts_calculate_estimate(packets)
    packet_pairs = packets.take([packets.size - 1, 0].max).zip(packets.drop(1))
    energy_joules = 0.0
    avg_power_watts = 0.0

    if !packets.empty?
      packet_pairs.each { |p1, p2|
        packet_interval = (p2['timestamp'] - p1['timestamp'])
        energy_joules += umts_energy_for_packet(packet_interval)
      }
      last_packet_tail_time = UMTS_STATE_DCH[:timeout_ms] + UMTS_STATE_FACH[:timeout_ms]
      energy_joules += umts_energy_for_packet(last_packet_tail_time)
    end

    EnergyEstimate.new(energy_joules, avg_power_watts)
  end

  private
  # Timeout
  WLAN_TIMEOUT_MS = 100.0

  # Packet processing time
  WLAN_MAX_PACKET_SIZE_BYTES = 1500.0
  WLAN_MAX_THROUGHPUT_BYTES_PER_SECOND = 4000000.0
  WLAN_PACKET_PROCESSING_TIME_MS = (WLAN_MAX_PACKET_SIZE_BYTES / WLAN_MAX_THROUGHPUT_BYTES_PER_SECOND) * 1000.0

  # Power values
  WLAN_P_TRANSMIT = 1.258
  WLAN_P_RECEIVE = 1.181
  WLAN_P_SLEEP = 0.042
  WLAN_P_IDLE = 0.884

  def wlan_energy_for_burst(burst, next_packet, psm_enabled)
    burst_length = (burst.last['timestamp'] - burst.first['timestamp'])
    interval_to_next_packet = next_packet ? (next_packet['timestamp'] - burst.last['timestamp']) : WLAN_TIMEOUT_MS

    t_burst = burst_length + WLAN_PACKET_PROCESSING_TIME_MS
    t_idle = (psm_enabled && interval_to_next_packet > WLAN_TIMEOUT_MS) ? WLAN_TIMEOUT_MS : interval_to_next_packet
    t_sleep = (psm_enabled && interval_to_next_packet > WLAN_TIMEOUT_MS) ? interval_to_next_packet - WLAN_TIMEOUT_MS : 0.0

    #puts "t_burst = #{t_burst}, t_idle = #{t_idle}, t_sleep = #{t_sleep} (first: #{burst.first['timestamp']}, last #{burst.last['timestamp']}"

    ((t_burst * wlan_power_constant_for_burst(burst)) + (t_idle * WLAN_P_IDLE) + (t_sleep * WLAN_P_SLEEP)) / 1000.0
  end

  def wlan_power_constant_for_burst(burst)
    received_bytes = burst.select { |p| p['direction'] == PACKET_DIRECTION_IN }.map { |p| p['size'] }.reduce(0, :+)
    transmit_bytes = burst.select { |p| p['direction'] == PACKET_DIRECTION_OUT }.map { |p| p['size'] }.reduce(0, :+)
    is_receive_burst = (received_bytes >= transmit_bytes)
    is_receive_burst ? WLAN_P_RECEIVE : WLAN_P_TRANSMIT
  end

  INFINITY = 1.0/0

  UMTS_VOLTAGE = 3.97
  UMTS_STATE_DCH = {
      :name => "DCH",
      :timeout_ms => 3500.0,
      :power => UMTS_VOLTAGE * 0.245
  }
  UMTS_STATE_FACH = {
      :name => "FACH",
      :timeout_ms => 8500.0,
      :power => UMTS_VOLTAGE * 0.135
  }
  UMTS_STATE_PCH_OR_IDLE = {
      :name => "PCH_OR_IDLE",
      :timeout_ms => INFINITY,
      :power => UMTS_VOLTAGE * 0.016
  }
  UMTS_STATES = [UMTS_STATE_DCH, UMTS_STATE_FACH, UMTS_STATE_PCH_OR_IDLE]

  def umts_energy_for_packet(packet_interval)
    result = UMTS_STATES.reduce({:time_left => packet_interval, :energy_joules => 0.0}) { |memo, state|
      time_in_state = state[:timeout_ms] if memo[:time_left] >= state[:timeout_ms]
      time_in_state = memo[:time_left] if memo[:time_left] < state[:timeout_ms]

      energy_in_state = time_in_state / 1000.0 * state[:power]
      {
          :time_left => memo[:time_left] - time_in_state,
          :energy_joules => memo[:energy_joules] + energy_in_state
      }
    }
    result[:energy_joules]
  end
end
