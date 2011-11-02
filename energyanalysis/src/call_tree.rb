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
require 'tree'
require 'stringio'
require 'json'

require File.dirname(__FILE__) + '/method_trace'
require File.dirname(__FILE__) + '/tcp_energy_estimator'

class CallTree
  attr_accessor :root, :id_counter

  class CallData
    attr_accessor :label, :call_count

    attr_accessor :direct_packets
    attr_accessor :child_packets

    def initialize(label, call_count)
      @label = label
      @call_count = call_count

      @direct_packets = []
      @child_packets = []
    end

    def has_packets
      packets.size > 0
    end

    def add_direct_packets(packets)
      @direct_packets = (@direct_packets + packets).sort.uniq
    end

    def add_child_packets(packets)
      @child_packets = (@child_packets + packets).sort.uniq
    end

    def packets
      (@direct_packets + @child_packets).sort.uniq
    end

    def to_s
      "Label: #{@label}, Count: #{@call_count}, Direct packets: #{@direct_packets}, Child packets: #{@child_packets}"
    end

    def to_json(*a)
      {
          "json_class" => self.class.name,
          "data" => {
              "label" => @label,
              "call_count" => @call_count,
              "direct_packets" => @direct_packets,
              "child_packets" => @child_packets
          }
      }.to_json(*a)
    end

    def self.json_create(o)
      new_o = new(o["data"]["label"], o["data"]["call_count"])
      new_o.direct_packets = o["data"]["direct_packets"]
      new_o.child_packets = o["data"]["child_packets"]
      new_o
    end
  end

  def to_json(*a)
  {
      "json_class" => self.class.name,
      "data" => {
          "root" => @root,
          "id_counter" => @id_counter
      }
  }.to_json(*a)
  end

  def self.json_create(o)
    new_o = new(o["data"]["id_counter"])
    new_o.root = o["data"]["root"]
    new_o
  end

  def initialize(id_counter = 0)
    @root = create_root_node
    @id_counter = id_counter
  end

  def add_call(stack_names, method)
    # Traverse or create new nodes for the current stack
    tree_pos = @root
    label = method['method']
    packet_indices = method['packet_indices']

    stack_names.each { |name|
      matching_child_id = tree_pos.children.index { |c| c.content.label == name }
      if matching_child_id
        tree_pos = tree_pos.children[matching_child_id]
      else
        middle_node = (tree_pos << create_node(name, 1))
        tree_pos = middle_node
      end

      # add packets as child indices to all nodes we are walking by
      tree_pos.content.add_child_packets(packet_indices)
    }

    matching_child_id = tree_pos.children.index { |c| c.content.label == label }
    if matching_child_id
      tree_pos = tree_pos.children[matching_child_id]
      if method['code'] == MethodTrace::ENTER_CODE
        tree_pos.content.call_count += 1

      end
    else
      new_node = tree_pos << create_node(label, 1)
      tree_pos = new_node
    end

    # direct count for the actual node responsible
    tree_pos.content.add_direct_packets(packet_indices)
  end

  def collapse_single_calls
    new_tree = CallTree.new(@id_counter)

    @root.each { |node|
      #puts "Node: #{node.content.label}"

      # Only add non-collapsable nodes, and grind together all collapsed
      # information to these nodes
      if node.parent != nil && !should_collapse(node)
        parent = find_best_collapsable_parent(node.parent)
        nodes = get_nodes_between(node, parent)
        new_node = merge_nodes(nodes)
        #warn "* Collapsing between #{node.content.label} (#{node.content.count}) and #{parent.content.label} (#{parent.content.count}) => #{new_node.content.count}"
        new_parent = new_tree.find_by_id(parent.name)
        new_parent << new_node
      end
    }
    new_tree
  end

  def remove_subtrees_without_packets
    new_tree = CallTree.new(@id_counter)
    @root.each { |node|
      if node.parent != nil && node.content.has_packets
        new_parent = new_tree.find_by_id(node.parent.name)
        new_parent << clone_node(node)
      end
    }
    new_tree
  end

  def size
    @root.size
  end

  def as_string
    @root.each
  end

  def burst_threshold_statistics(packets, bursts_to_try = [3,6,8,10,15,25])
    tee = TcpEnergyEstimator.new
    all_application_packet_indices = @root.children.map {|node| node.content.packets }.flatten.sort.uniq
    all_application_packets = all_application_packet_indices.map {|p| packets[p]}

    per_edge_stats = {}

    bursts_to_try.each { |wlan_burst_threshold_ms|
      total_energy_wlan = tee.wlan_calculate_estimate(all_application_packets, wlan_burst_threshold_ms).energy_joules
      @root.each { |node|
        if node.parent != nil
          all_indices = node.content.packets
          all_packets = all_indices.map { |p| packets[p] }
          all_energy_wlan = tee.wlan_calculate_estimate(all_packets, wlan_burst_threshold_ms).energy_joules

          all_except_this_indices = all_application_packet_indices - all_indices
          all_except_this_packets = all_except_this_indices.map { |p| packets[p] }
          all_except_this_energy_wlan = tee.wlan_calculate_estimate(all_except_this_packets, wlan_burst_threshold_ms).energy_joules

          min_saved_energy_wlan = total_energy_wlan - all_except_this_energy_wlan
          max_saved_energy_wlan = all_energy_wlan

          edge_name = "#{node.parent.name} -> #{node.name}"

          per_edge_stats[edge_name] = [] if !per_edge_stats.has_key?(edge_name)
          per_edge_stats[edge_name].push({
                                             "threshold" => wlan_burst_threshold_ms,
                                             "lower" => min_saved_energy_wlan,
                                             "upper" => max_saved_energy_wlan
                                         })
        end
      }
    }

    lower_sio = StringIO.new
    upper_sio = StringIO.new
    lower_sio.puts bursts_to_try.map {|b| "#{b}ms_lower"}.join(",")
    upper_sio.puts bursts_to_try.map {|b| "#{b}ms_upper"}.join(",")

    per_edge_stats.each { |edge, values|
      lower_sio.puts(values.map {|v| v['lower']}.join(","))
      upper_sio.puts(values.map {|v| v['upper']}.join(","))
    }

    { "lower" => lower_sio.string,
      "upper" => upper_sio.string }
  end

  def as_dot_graph(packets, wlan_burst_threshold_ms = 6)
    sio = StringIO.new

    tee = TcpEnergyEstimator.new
    all_application_packet_indices = @root.children.map {|node| node.content.packets }.flatten.sort.uniq
    all_application_packets = all_application_packet_indices.map {|p| packets[p]}
    total_energy_wlan = tee.wlan_calculate_estimate(all_application_packets, wlan_burst_threshold_ms).energy_joules
    total_energy_umts = tee.umts_calculate_estimate(all_application_packets).energy_joules

    sio.puts "digraph methods {"
    sio.puts "node [shape=note, fontname=\"Helvetica\", fontsize=14, color=grey];"
    sio.puts "edge [fontname=\"Helvetica\", fontsize=16];"
    sio.puts "ranksep=\"0.1\";"
    sio.puts "nodesep=\"0.1\";"

    @root.each { |node|
      if node.parent != nil
        direct_packets = node.content.direct_packets.map { |p| packets[p] }
        direct_size = direct_packets.map { |p| p['size'] }.reduce(0, :+)
        direct_count = direct_packets.size

        all_indices = node.content.packets
        all_packets = all_indices.map { |p| packets[p] }
        all_size = all_packets.map { |p| p['size'] }.reduce(0, :+)
        all_count = all_packets.size
        all_energy_wlan = tee.wlan_calculate_estimate(all_packets, wlan_burst_threshold_ms).energy_joules
        all_energy_umts = tee.umts_calculate_estimate(all_packets).energy_joules

        all_except_this_indices = all_application_packet_indices - all_indices
        all_except_this_packets = all_except_this_indices.map { |p| packets[p] }
        all_except_this_energy_wlan = tee.wlan_calculate_estimate(all_except_this_packets, wlan_burst_threshold_ms).energy_joules
        all_except_this_energy_umts = tee.umts_calculate_estimate(all_except_this_packets).energy_joules

        min_saved_energy_wlan = total_energy_wlan - all_except_this_energy_wlan
        max_saved_energy_wlan = all_energy_wlan

        min_saved_energy_umts = total_energy_umts - all_except_this_energy_umts
        max_saved_energy_umts = all_energy_umts

        sio.puts "\"#{node.parent.name}\" -> \"#{node.name}\" [label=\""+
          "#{node.content.call_count} calls, #{all_count} packets\\n" +
          "#{all_size} bytes\\n" +
          "[#{"%.3f" % min_saved_energy_wlan} J, #{"%.3f" % max_saved_energy_wlan} J]\\n" +
          "\"]"
=begin
        sio.puts "\"#{node.parent.name}\" -> \"#{node.name}\" [label=\""+
          "#{node.content.call_count} calls\\n" +
          "#{all_count} total (#{direct_count} direct) packets\\n"+
          "#{all_size} total (#{direct_size} direct) bytes\\n" +
          "[#{"%.3f" % min_saved_energy_wlan}, #{"%.3f" % max_saved_energy_wlan}] J, WLAN\\n" +
          "[#{"%.3f" % min_saved_energy_umts}, #{"%.3f" % max_saved_energy_umts}] J, 3G" +
          "\"]"
=end
      end
    }

    @root.each { |node|
      sio.puts "\"#{node.name}\" [label=\"#{node.content.label}\"]"
    }
    sio.puts "}"

    sio.string
  end

  def find_by_id(id)
    @root.each { |child|
      if child.name == id
        return child
      end
    }
    return nil
  end

private

  def create_root_node
    Tree::TreeNode.new(0,
                       CallData.new("*", 0))
  end

  def node_id(prefix)
    id = "#{prefix}-#{@id_counter}"
    @id_counter += 1
    id
  end

  def create_node(label, count)
    Tree::TreeNode.new(node_id("n"), CallData.new(label,count))
  end

  def get_stack(node)
    if node.parent == nil
      [node.content.label]
    else
      get_stack(node.parent).concat([node.content.label])
    end
  end


  def find_best_collapsable_parent(node)
    if should_collapse(node)
      find_best_collapsable_parent(node.parent)
    else
      node
    end
  end

  def get_nodes_between(node, parent)
    if node.parent == parent
      [node]
    else
      get_nodes_between(node.parent, parent).concat([node])
    end
  end

  def should_collapse(node)
    node.parent != nil && node.children.size == 1
  end

  def clone_node(node)
    new_node = Tree::TreeNode.new(node.name, node.content.clone)
    new_node
  end

  def merge_nodes(nodes)
    highest_node = nodes.first
    splitting_node = nodes.last

    # special case for dispatch handling
    if highest_node.content.label == "android/os/Handler.dispatchMessage"
        merged_label = nodes.map { |n| n.content.label }.join("\\n")
    # special case for two unique nodes
    elsif nodes.size == 2 && highest_node.content.label == splitting_node.content.label
        merged_label = format_single_call(splitting_node.content.label)
    else
        middle_rows = ["(...)"] if nodes.size < 3
        middle_rows = ["(%d more calls...)" % (nodes.size - 2)] if nodes.size >= 3
        merged_label = ([format_single_call(highest_node.content.label)] + middle_rows + [format_single_call(splitting_node.content.label)]).join("\\n")
    end

    new_node = Tree::TreeNode.new(splitting_node.name, CallData.new(merged_label, highest_node.content.call_count))
    new_node.content.direct_packets = nodes.inject([]) { |all, n| all + n.content.direct_packets }.sort.uniq
    new_node.content.child_packets = splitting_node.content.child_packets

    new_node
  end

  def format_single_call(c)
    max_width=1
    if c.size > max_width
        i = c.rindex("/")
        "\\[%s\\]\\n%s" % [c[0..i], c[(i+1)...c.size]]
        #c[0...max_width] + "...\\n" + "   ..." + format_single_call(c[max_width...c.size])
    else
        c
    end
  end

  def subtrees_equal(a, b)
    labels_equal(a, b) && have_equal_child_trees(a, b)
  end

  def labels_equal(a, b)
    a.content.label == b.content.label
  end

  def have_equal_child_trees(a, b)
    child_count_equal(a, b) && a.children.zip(b.children).map { |ca, cb| subtrees_equal(ca, cb) }.include?(false) == false
  end

  def child_count_equal(a, b)
    a.children.size == b.children.size
  end

end
