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
require_src 'dmesg_reader'

class DmesgReaderTest < Test::Unit::TestCase
  def setup
    @reader = DmesgReader.new
  end

  def test_empty_input
    result = @reader.parse_dmesg_input("")
    assert_equal result, []
  end

  def test_invalid_input
    result = @reader.parse_dmesg_input("asdfasdfa\nasdfasdfasdf\nasdfasdfasdf\n")
    assert_equal result, []
  end

  def test_real_input_without_size
    lines = File.readlines(testfile_path('nw_dmesg-without-sizes.nraw'))
    result = @reader.parse_dmesg_input(lines)
    assert_equal result.size, 157

    first = result.first
    assert_equal first['timestamp'], 1305119494611
    assert_equal first['conn_id'], 38412
    assert_equal first['dir'], 2
    assert_equal first['event'], "new_syn"
    assert_equal first['size'], 0
    assert_equal first['ack_id'], 0
    assert_equal first['seq_id'], 0

    last = result.last
    assert_equal last['timestamp'], 1305119534515
    assert_equal last['conn_id'], 38412
    assert_equal last['dir'], 2
    assert_equal last['event'], "ack_only"
    assert_equal last['size'], 0
    assert_equal last['ack_id'], 1216127872
    assert_equal last['seq_id'], 0
  end


  def test_real_input_with_size
    lines = File.readlines(testfile_path('nw_dmesg-with-sizes.nraw'))
    result = @reader.parse_dmesg_input(lines)
    assert_equal result.size, 106

    first = result.first
    assert_equal first['timestamp'], 1306489772078
    assert_equal first['conn_id'], 32994
    assert_equal first['dir'], 2
    assert_equal first['event'], "new_syn"
    assert_equal first['size'], 60
    assert_equal first['ack_id'], 0
    assert_equal first['seq_id'], 1969407115

    last = result.last
    assert_equal last['timestamp'], 1306489822412
    assert_equal last['conn_id'], 58132
    assert_equal last['dir'], 1
    assert_equal last['event'], "burst"
    assert_equal last['size'], 52
    assert_equal last['ack_id'], 3912557241
    assert_equal last['seq_id'], 3402795250
  end
end
