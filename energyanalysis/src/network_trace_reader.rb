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

class NetworkTraceReader
  attr_accessor :packets

  def initialize(filename)
    warn "* Reading network packets from #{filename}"

    @packets = []
    f = File.open(filename, "r")
    f.each_line do |line|
      if line =~ /([0-9]+) ([0-9]+) ([1-2]+) ([a-z_]+) ([0-9]+) ([0-9]+) ([0-9]+)/
        @packets.push({
            'timestamp' => $1.to_i,
            'conn_id' => $2.to_i,
            'direction' => $3.to_i,
            'event' => $4,
            'size' => $5.to_i,
            'ack_id' => $6.to_i,
            'seq_id' => $7.to_i
        })
      end
    end
    f.close

    warn "* Packets read, count #{@packets.size}"
  end
end
