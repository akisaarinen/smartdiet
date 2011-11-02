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
base=ARGV[0]

dumpfile=base+'.dump'
tracefile=base+'.trace'

thread_base_clocks={}

f = File.open(tracefile, "r")
in_content=false
f.each_line { |line|
  if line =~ /^\*threads/
    break
  end
  if line =~ /thread-clock-base-([0-9]+)=([0-9]+)/
    thread_id=$1
    thread_clock=$2
    thread_base_clocks[thread_id]=thread_clock.to_i
  end
}
f.close

puts "thread code ts method params"

f = File.open(dumpfile, "r")
in_content=false
f.each_line { |line|
  if !in_content && line =~ /^Trace \(/
    in_content=true
  elsif in_content
    if line =~ /^([0-9\W]+)[\W]+([a-z]+)[\W]+([0-9]+)[\W]+(.*)/
      thread_id=$1.strip
      code=$2
      ts=$3.to_i
      method=$4
      full_ts=thread_base_clocks[thread_id] + ts
      puts "#{thread_id} #{code} #{full_ts} #{method}"
    end
  end
}
f.close
