#!/usr/bin/env ruby
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

require File.dirname(__FILE__) + '/dmesg_reader'

reader = DmesgReader.new
raw_lines = STDIN.read.split("\n")

tm_lines = reader.parse_dmesg_input(raw_lines)

puts "timestamp conn_id direction event size ack_id seq_id"
tm_lines.each do |t|
  puts "#{t['timestamp']} #{t['conn_id']} #{t['dir']} #{t['event']} #{t['size']} #{t['ack_id']} #{t['seq_id']}"
end
