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

class DmesgReader
  def parse_dmesg_input(lines)
    init_ts = 0
    tm_lines = []

    # First we need to find the initialization timestamp
    lines.map  do |line|
      if line =~ /<[0-9]+>\[([\W0-9.]*)\] TM (.*)/
        ts = ($1.strip.to_f * 1000).to_i
        msg = $2.strip

        if msg =~ /MODULE INITIALIZATION, timestamp ([0-9]+)/
          init_ts = ($1.strip.to_i / 1000) - ts
        else
          tm_lines.push([ts, msg])
        end
      end
    end

    # Then process all lines and correct timestamp
    tm_lines.map { |ts, msg|
      actual_ts = init_ts + ts
      # Size and sequence numbers are optional
      if msg =~ /([<>=]+) ([0-9]+) (.*), ack ([0-9]+)(|, seq ([0-9]+))($|, size ([0-9]+))/
        if $1 == "<="
          dir = 1
        else
          dir = 2
        end
        conn_id = $2.to_i
        event = $3
        ack = $4.to_i
        seq = $6 != "" ? $6.to_i : 0
        size = $8 != nil ? $8.to_i : 0

        {'timestamp' => actual_ts,
         'conn_id' => conn_id,
         'dir' => dir,
         'event' => event,
         'size' => size,
         'ack_id' => ack,
         'seq_id' => seq}
      else
        nil
      end
    }.select { |i| i != nil }
  end
end
