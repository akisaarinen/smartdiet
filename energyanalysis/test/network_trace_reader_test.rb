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
require_src 'network_trace_reader'

class NetworkTraceReaderTest < Test::Unit::TestCase
  def setup
    @reader = NetworkTraceReader.new(testfile_path("test-network-data.nw"))
  end

  def test_count
    assert_equal 106, @reader.packets.size
  end

  def test_first_packet
    p = @reader.packets.first
    assert_equal 1306489772078, p['timestamp']
    assert_equal 32994, p['conn_id']
    assert_equal 2, p['direction']
    assert_equal 'new_syn', p['event']
    assert_equal 60, p['size']
    assert_equal 0, p['ack_id']
    assert_equal 1969407115, p['seq_id']
  end

  def test_last_packet
    p = @reader.packets.last
    assert_equal 1306489822412, p['timestamp']
    assert_equal 58132, p['conn_id']
    assert_equal 1, p['direction']
    assert_equal 'burst', p['event']
    assert_equal 52, p['size']
    assert_equal 3912557241, p['ack_id']
    assert_equal 3402795250, p['seq_id']
  end
end
