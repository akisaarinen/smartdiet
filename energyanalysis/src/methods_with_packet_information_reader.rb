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
require 'faster_csv'

# Read CSV containing all methods and their related
# network packet counts and sizes
class MethodsWithPacketInformationReader
  attr_reader :all_methods

  def initialize(filename)
    warn "* Reading matched network methods from #{filename}"
    csv = FasterCSV.read(filename)

    cols = csv[0]
    rows = csv.slice(1..csv.size)

    @all_methods = []
    rows.each do |row|
      if row[1] != 'NA'
        @all_methods.push({
                          'thread' => row[1],
                          'code' => row[2],
                          'timestamp' => row[3].to_f,
                          'method' => row[4],
                          'packet_count' => row[5].to_i,
                          'packet_size' => row[6].to_i,
                          'packet_indices' => row[7] ? row[7].split(":").map {|i| i.to_i } : [] # Also handle the case we have not this information available
                      })
      end
    end
    warn "* Matched network methods read, count #{@all_methods.size}"
  end
end
