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
require_src 'call_tree'

class CallTreeTest < Test::Unit::TestCase

  def setup
    @tree = CallTree.new
  end

  def test_initial_size_is_root_only
    assert_equal 1, @tree.size
  end

  def test_initial_root
    assert_equal "*", @tree.root.content.label
  end

  def test_raw_tree
    populate_tree(@tree)
    assert_equal 10, @tree.size
    assert_equal "Thread.run", @tree.root.children[0].content.label
    assert_equal [0], @tree.root.children[0].content.direct_packets
    assert_equal (1..25).to_a, @tree.root.children[0].content.child_packets
    assert_equal "B", @tree.root.children[0].children[0].content.label
    assert_equal (1..2).to_a, @tree.root.children[0].children[0].content.direct_packets
    assert_equal (3..25).to_a, @tree.root.children[0].children[0].content.child_packets
    assert_equal "C", @tree.root.children[0].children[0].children[0].content.label
    assert_equal ((3..6).to_a + (13..17).to_a), @tree.root.children[0].children[0].children[0].content.direct_packets
    assert_equal (7..12).to_a, @tree.root.children[0].children[0].children[0].content.child_packets
  end

  def test_with_all_reductions
    populate_tree(@tree)
    @tree = @tree.remove_subtrees_without_packets.collapse_single_calls
    assert_equal 4, @tree.size
    assert_equal "Thread.run\\nB", @tree.root.children[0].content.label
    assert_equal (0..2).to_a, @tree.root.children[0].content.direct_packets
    assert_equal (3..25).to_a, @tree.root.children[0].content.child_packets
    assert_equal "C\\nC_1\\nC_2", @tree.root.children[0].children[0].content.label
    assert_equal (3..17).to_a, @tree.root.children[0].children[0].content.direct_packets
    assert_equal [], @tree.root.children[0].children[0].content.child_packets
    assert_equal "D", @tree.root.children[0].children[1].content.label
    assert_equal (18..25).to_a, @tree.root.children[0].children[1].content.direct_packets
    assert_equal [].to_a, @tree.root.children[0].children[1].content.child_packets
  end

  def test_generate_dot_graph
    populate_tree(@tree)
    dot_graph = @tree.as_dot_graph(@packets)
    assert_not_equal 0, dot_graph.length

    f = File.new("test.dot", "w")
    f.write(dot_graph)
    f.close
  end

  def test_serialize_to_json
    populate_tree(@tree)
    serialized_json = @tree.to_json({:max_nesting => 1000})
    parsed_tree = JSON::parse(serialized_json, {:max_nesting => 1000})
    original_as_graph = @tree.as_dot_graph(@packets)
    parsed_as_graph = parsed_tree.as_dot_graph(@packets)
    assert_equal original_as_graph, parsed_as_graph
  end

  private

  def populate_tree(tree)
    @packets = [
        create_packet(0, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(100, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(105, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(109, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(110, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(140, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(1000, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(1010, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(1020, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(1100, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(1150, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3000, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3002, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(3010, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3100, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3400, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(3440, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3441, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3441, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3446, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(3447, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3450, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3455, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT),
        create_packet(3456, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3458, 1, TcpEnergyEstimator::PACKET_DIRECTION_IN),
        create_packet(3500, 1, TcpEnergyEstimator::PACKET_DIRECTION_OUT)
    ]

    trace = [
        create_method("Thread.run", 1, MethodTrace::ENTER_CODE, [0]),

        create_method("B", 2, MethodTrace::ENTER_CODE, [1,2]),

        create_method("C", 1, MethodTrace::ENTER_CODE, [3]),
        create_method("C", 2, MethodTrace::EXIT_CODE, [4,5]),

        create_method("C", 1, MethodTrace::ENTER_CODE, [6]),
        create_method("C_1", 0, MethodTrace::ENTER_CODE),
        create_method("C_2", 6, MethodTrace::ENTER_CODE, (7..12).to_a),
        create_method("C_3", 0, MethodTrace::ENTER_CODE),
        create_method("C_4", 0, MethodTrace::ENTER_CODE),

        create_method("C_4", 0, MethodTrace::EXIT_CODE),
        create_method("C_3", 0, MethodTrace::EXIT_CODE),
        create_method("C_2", 0, MethodTrace::EXIT_CODE),
        create_method("C_1", 0, MethodTrace::EXIT_CODE),

        create_method("C", 2, MethodTrace::EXIT_CODE, [13,14]),

        create_method("C", 1, MethodTrace::ENTER_CODE, [15]),
        create_method("C", 2, MethodTrace::EXIT_CODE, [16, 17]),


        create_method("D", 4, MethodTrace::ENTER_CODE, (18..21).to_a),
        create_method("leaf1", 0, MethodTrace::ENTER_CODE),
        create_method("leaf1", 0, MethodTrace::EXIT_CODE),
        create_method("D", 0, MethodTrace::EXIT_CODE),

        create_method("D", 0, MethodTrace::ENTER_CODE),
        create_method("leaf1", 0, MethodTrace::ENTER_CODE),
        create_method("leaf1", 0, MethodTrace::EXIT_CODE),
        create_method("D", 1, MethodTrace::EXIT_CODE, [22]),
        create_method("D", 0, MethodTrace::ENTER_CODE),
        create_method("D", 3, MethodTrace::EXIT_CODE, [23,24,25]),
    ]

    stack = []
    trace.each { |m|
      if m['code'] == MethodTrace::ENTER_CODE
        tree.add_call(stack.map { |s| s['method'] }, m)
        stack.push(m)
      else
        stack.pop
        tree.add_call(stack.map { |s| s['method'] }, m)
      end
    }
  end

  def create_method(name, packet_count, code, packet_indices = [])
    {
        'method' => name,
        'packet_indices' => packet_indices,
        'code' => code
    }
  end

  def create_packet(timestamp, size, direction)
    {
        'timestamp' => timestamp,
        'size' => size,
        'direction' => direction
    }
  end
end
