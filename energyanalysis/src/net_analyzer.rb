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

require File.dirname(__FILE__) + '/method_trace'
require File.dirname(__FILE__) + '/methods_with_packet_information_reader'
require File.dirname(__FILE__) + '/network_trace_reader'
require File.dirname(__FILE__) + '/call_tree'

class NetAnalyzer
  attr_reader :all_methods
  attr_reader :packets

  def initialize(base_name)
    @base_name = base_name
    warn "* Analyzing network traffic and method data for dataset '#{@base_name}'"

    # Read method data
    network_match_file = "#{@base_name}-all_methods.data"
    @network_matcher = MethodsWithPacketInformationReader.new(network_match_file)
    @all_methods = @network_matcher.all_methods

    # Read packet data
    packet_file = "#{@base_name}.nw"
    @network_trace_reader = NetworkTraceReader.new(packet_file)
    @packets = @network_trace_reader.packets
  end

  def get_ui_related_indices
    get_methods_encapsulated_in_call_of(
        ["android/app/Activity.dispatchKeyEvent",
         "android/app/Activity.dispatchMouseEvent"])
  end

  def write_method_statistics(indices, filename)
    prev = -2
    level = 0
    f = File.open(filename, 'w')
    indices.each do |i, stack_size|
      m = @all_methods[i]
      f.puts "[#{i}] #{"." * stack_size} #{m['code']} #{m['method']}"
      prev = i
    end
    f.close
  end

  def get_methods_encapsulated_in_call_of(method_names)
    list = []
    each_method_with_stack { |i, m, stack|
      #if m['code'] == MethodTrace::ENTER_CODE
        has_name_in_stack = stack.select { |s| method_names.include?(@all_methods[s]['method']) }.size > 0
        list.push([i, stack.size]) if has_name_in_stack
      #end
    }
    list
  end

  def write_accumulated_data(accumulated_counts)
    method_filename = "#{@base_name}-accumulated.data"
    warn "* Outputting aggregated method level statistics to '#{method_filename}'"

    f = File.open(method_filename, 'w')
    f.puts("thread code timestamp method packet_count accumulated_count accumulated_size")

    for i in 0...@all_methods.size do
      m = @all_methods[i]
      a = accumulated_counts[i]
      f.puts("#{m['thread']} #{m['code']} #{m['timestamp']} #{m['method']} #{m['packet_count']} #{a['accumulated_count']} #{a['accumulated_size']}")
    end
    f.close
  end

  def get_accumulated_counts
    warn "* Calculating aggregated method statistics..."
    accumulated_counts = Array.new(@all_methods.size) { |i| {
        'accumulated_count' => 0,
        'accumulated_size' => 0
    } }

    each_method_with_stack { |i, m, stack|
      # Accumulate all method calls in stack if there's packets directly assigned
      # to this method.
      if (m['packet_count'] > 0)
        #warn "  + [#{i}] #{m['method']}, packet count #{m['packet_count']}, stack size #{stack.size}"

        stack.each { |parent_id|
          #warn "    + [#{parent_id}] #{@methods[parent_id]['method']} got accumulated"

          accumulated_counts[parent_id]['accumulated_count'] += m['packet_count']
          accumulated_counts[parent_id]['accumulated_size'] += m['packet_size']
        }
      end
    }
    accumulated_counts
  end

  def get_uniq_methods(accumulated_counts)
    methods = {}

    each_method_with_stack { |i, m, stack|
      a = accumulated_counts[i]

      if !methods.has_key?(m['method'])
        methods[m['method']] = {
            'calls' => 0,
            'packets' => 0,
            'bytes' => 0
        }
      end
      methods[m['method']]['calls'] += 1
      methods[m['method']]['packets'] += a['accumulated_count']
      methods[m['method']]['bytes'] += a['accumulated_size']
    }

    methods.to_a.sort { |u1, u2| u1[1]['packets'] <=> u2[1]['packets'] }.reverse
  end

  def write_uniq_methods(uniq_methods)
    methods_containing_packets = uniq_methods.select { |name, u| u['packets'] > 0 }

    all_methods_filename = "#{@base_name}-uniq-methods.data"
    packet_methods_filename = "#{@base_name}-packet-methods.data"

    warn "* Outputting all methods to '#{all_methods_filename}' (#{uniq_methods.size} methods, #{methods_containing_packets.size} contain packets)"
    f = File.open(all_methods_filename, 'w')
    uniq_methods.each { |name, u|
      f.puts("#{u['packets']} packets (#{u['calls']} calls, #{u['bytes']} bytes) #{name}")
    }
    f.close

    warn "* Outputting methods containing packets to '#{packet_methods_filename}' (#{uniq_methods.size} methods, #{methods_containing_packets.size} contain packets)"
    f = File.open(packet_methods_filename, 'w')
    methods_containing_packets.each { |name, u|
      f.puts("#{u['packets']} packets (#{u['calls']} calls, #{u['bytes']} bytes) #{name}")
    }
    f.close
  end

  def write_separated_full_dot_graph(uniq_methods)
    warn "* Calculating full graph for dataset '#{@base_name}'"
    packetful_method_names = uniq_methods.select { |name,u| u['packets'] > 0 }.map { |name,u| name }
    warn "* Packetful method count: #{packetful_method_names.size}"

    call_tree = CallTree.new
    each_method_with_stack { |i, m, stack|
      if packetful_method_names.include?(m['method'])
        stack_names = stack.take(stack.size - 1).map { |i| @all_methods[i]['method'] }
        call_tree.add_call(stack_names, m)
      end
    }

    warn "* Removing empty subtrees"
    call_tree = call_tree.remove_subtrees_without_packets

    warn "* Collapsing single calls"
    call_tree = call_tree.collapse_single_calls

    graph_json_filename = "#{@base_name}-call_graph.json"
    warn "* Writing graph as JSON to #{graph_json_filename}"
    f = File.open(graph_json_filename, "w")
    f.write(call_tree.to_json({:max_nesting => 1000}))
    f.close

    graph_filename = "#{@base_name}-graph-grouped-network-calls.dot"
    warn "* Calculating TCP energy estimates and outputting graph data to '#{graph_filename}'"
    f = File.open(graph_filename, 'w')
    f.write(call_tree.as_dot_graph(@packets))
    f.close
  end

  private

  def each_method_with_stack
    thread_stacks = {}

    for i in 0...@all_methods.size do
      m = @all_methods[i]

      thread_stacks[m['thread']] = [] if !thread_stacks.has_key?(m['thread'])
      stack = thread_stacks[m['thread']]

      if m['code'] == MethodTrace::ENTER_CODE
        stack.push(i)
        yield(i, m, stack)
      elsif stack.size > 0 && m['code'] != MethodTrace::ENTER_CODE
        yield(i, m, stack)
        last = @all_methods[stack.last]
        if m['method'] != last['method']
          warn "* [#{i}] NON-LINEAR STACK!, '#{m['method']}'@#{m['timestamp']} != '#{last['method']}'@#{m['timestamp']}, code #{m['code']}"
        end
        stack.pop
      else
        #warn "* [#{i}] Unhandled stack operation '#{m['code']}' for '#{m['method']} with stack size #{stack.size}"
      end
    end
  end

end
