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
require_src 'methods_with_packet_information_reader'

class MethodsWithPacketInformationReaderTest < Test::Unit::TestCase
  def setup
    @reader = MethodsWithPacketInformationReader.new(testfile_path("5k-all_methods.data"))
  end

  def test_count
    assert_equal 5000, @reader.all_methods.size
  end

  def test_first_line
    first = @reader.all_methods.first
    assert_equal "4", first['thread']
    assert_equal "xit", first['code']
    assert_equal 0.1650390625, first['timestamp']
    assert_equal "dalvik/system/VMDebug.startMethodTracingDdms", first['method']
    assert_equal 0, first['packet_count']
    assert_equal 0, first['packet_size']
    assert_equal [], first['packet_indices']
  end

  def test_last_line
    last = @reader.all_methods.last
    assert_equal "1", last['thread']
    assert_equal "xit", last['code']
    assert_equal 946.2099609375, last['timestamp']
    assert_equal "java/lang/String._getChars", last['method']
    assert_equal 0, last['packet_count']
    assert_equal 0, last['packet_size']
    assert_equal [], last['packet_indices']
  end
end
