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

require 'rubygems'
require 'json'

require File.dirname(__FILE__) + '/method_trace'
require File.dirname(__FILE__) + '/methods_with_packet_information_reader'
require File.dirname(__FILE__) + '/network_trace_reader'
require File.dirname(__FILE__) + '/call_tree'

class GraphAnalyzer

  def initialize(base_name)
    @base_name = base_name
    graph_json_filename = "#{@base_name}-call_graph.json"
    f = File.new(graph_json_filename)
    s = f.readlines.join("\n")
    f.close

    @call_tree = JSON::parse(s, { :max_nesting => 1000})

    packet_file = "#{@base_name}.nw"
    @network_trace_reader = NetworkTraceReader.new(packet_file)
    @packets = @network_trace_reader.packets

    burst_stats = @call_tree.burst_threshold_statistics(@packets)
    f = File.new("#{@base_name}-burst_lower.csv", "w")
    f.write(burst_stats["lower"])
    f.close

    f = File.new("#{@base_name}-burst_upper.csv", "w")
    f.write(burst_stats["upper"])
    f.close
  end
end
