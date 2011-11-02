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

require File.dirname(__FILE__) + '/cpu_analyzer'
require File.dirname(__FILE__) + '/net_analyzer'
require File.dirname(__FILE__) + '/graph_analyzer'

def print_usage_and_exit
  warn "Usage: "
  warn "1) -cpu <basefilename>"
  warn "2) -net <basefilename>"
  exit 1
end

if ARGV.size != 2
  print_usage_and_exit
end

param = ARGV[0]
basename = ARGV[1]
start_time = Time.new

if param == '-cpu'
  CpuAnalyzer.new.doshit("#{basename}.abs", "#{basename}.cpu")
elsif param == '-loadgraph'
  ga = GraphAnalyzer.new(basename)
elsif param == '-net'
  na = NetAnalyzer.new(basename)

  accumulated_counts = na.get_accumulated_counts
  uniq_methods = na.get_uniq_methods(accumulated_counts)

  na.write_accumulated_data(accumulated_counts)
  na.write_uniq_methods(uniq_methods)
  na.write_separated_full_dot_graph(uniq_methods)
else
  print_usage_and_exit
end

end_time = Time.new
warn "* Execution took #{end_time - start_time} seconds"
