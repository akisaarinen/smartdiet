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
class MethodTrace
  ENTER_CODE='ent'
  EXIT_CODE='xit'
  UNROLL_CODE='unr'

  attr_accessor :events

  def initialize(filename)
    warn "* Reading trace file from #{filename}"

    f = File.open(filename, "r")
    trace_counter = 0
    @events = []

    f.each_line { |line|
      if line =~ /^([0-9]+) ([a-z]+) ([0-9]+) (.*)/
        thread_id=$1
        code=$2
        ts=$3.to_i
        location=$4

        @events.push({'tid' => thread_id, 'code' => code, 'ts' => ts, 'location' => location})
        trace_counter += 1
      end
    }
    f.close
    warn "* Trace read, event count #{@events.size}"
  end
end
