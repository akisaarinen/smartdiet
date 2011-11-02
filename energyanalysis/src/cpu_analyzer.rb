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

class CpuAnalyzer
  ENTER_CODE='ent'
  EXIT_CODE='xit'
  UNROLL_CODE='unr'
  TOLERANCE=1000

  def find_cpu_for_period(time_start, time_end)
    first_before_start=0
    first_after_end=$cpu_stats.size-1

    (0 ... $cpu_stats.size).each { |i|
      s=$cpu_stats[i]
      if s['ts'] < time_start
        first_before_start=i
      elsif s['ts'] > time_end
        first_after_end=i
        break
      end
    }

    sum=0
    # Partial start
    s_before0 = $cpu_stats[first_before_start]
    s_before1 = $cpu_stats[first_before_start+1]
    before_time_total = s_before1['ts'] - s_before0['ts']
    before_time_sample = time_start - s_before0['ts']
    before_percentage = 1.0 - (before_time_sample / before_time_total.to_f)
    sum += (s_before0['cpu_u'] +
        s_before0['cpu_n'] +
        s_before0['cpu_s']) * before_percentage


    #puts "----------------------"
    #puts "time_start: #{time_start} => #{time_end}"
    #puts "first time after: #{$cpu_stats[first_after_end]['ts']}"
    #puts "sum before #{sum}"


    # Certainly in
    (first_before_start+1 .. first_after_end-2).each { |i|
    #puts "Certain: #{$cpu_stats[i]['ts']}"
      sum+= $cpu_stats[i]['cpu_u'] +
          $cpu_stats[i]['cpu_n'] +
          $cpu_stats[i]['cpu_s']
    }

    #puts "sum cert #{sum}"

    # Partial end if not already done on partial start
    if first_after_end > first_before_start+1
      s_after0 = $cpu_stats[first_after_end-1]
      s_after1 = $cpu_stats[first_after_end]

      #puts "after0: #{s_after0['ts']}"
      #puts "after1: #{s_after1['ts']}"

      after_time_sample = time_end - s_after0['ts']
      after_time_total = s_after1['ts'] - s_after0['ts']
      #puts "after_time_sample: #{after_time_sample}"
      after_percentage = after_time_sample / after_time_total.to_f
      #puts "after perc: #{after_percentage}"
      sum += (s_after0['cpu_u'] +
          s_after0['cpu_n'] +
          s_after0['cpu_s']) * after_percentage
      #puts "sum after #{sum}"
    end
    sum
  end

  def doshit(absfile, cpufile)

    thread_events={}
    thread_stacks={}
    events=[]

    warn "* Reading CPU stats"
    $cpu_stats=[]
    f = File.open(cpufile, "r")
    f.each_line { |line|
      if line =~ /^([0-9]+) [0-9]+ ([0-9]+) ([0-9]+) ([0-9]+)/
        ts=$1.to_i
        cpu_u=$2.to_i
        cpu_n=$3.to_i
        cpu_s=$4.to_i

        $cpu_stats.push({'ts' => ts, 'cpu_u' => cpu_u, 'cpu_n' => cpu_n, 'cpu_s' => cpu_s})
      end
    }
    f.close
    warn "* CPU stats read, #{$cpu_stats.size} events"

    # 317080111700 0101 8 0 1 = 9
    # 317080111801 0101 4 0 3 = 7
    # 317080111902 0101 3 0 0 = 3
    # 317080112003 0101 3 0 4 = 7
=begin
        a= find_cpu_for_period(317080111700, 317080111801)
        puts "a = #{a}"
        b= find_cpu_for_period(317080111801, 317080111901)
        puts "b = #{b}"
        c= find_cpu_for_period(317080111901, 317080112003)
        puts "c = #{c}"
        puts "========================="
        t= find_cpu_for_period(317080111700, 317080112003)
        puts "========================="
        puts "#{a+b+c} == #{t}"

        exit
=end

    trace = MethodTrace.new(absfile)

    events = trace.events
    total_events=events.size
=begin
        thread_events.each { |thread_id, events|
            total_events += events.size
        }
=end
    warn "* Total dalvikvm events: #{total_events}"


    thread_stacks = {}

    total_usages={}
    counter=0
    events.each { |e|
      tid = e['tid']

=begin
            weight = events.size / total_events.to_f
            warn "* Processing thread ##{thread_id}, weight: #{weight}"
=end


      code = e['code']
      ts = e['ts']
      location = e['location']

      active_threads = thread_stacks.map { |t, s|
        if !s.empty?
          last_item = s.last
          diff = ts - s.last['ts'].to_i
          diff >= TOLERANCE
        else
          false
        end
      }.select { |s| s }

      weight = 1000 / [1, active_threads.size].max

      if counter % 5000 == 0
        warn "#{counter / events.size.to_f * 100.0} %, active threads: #{active_threads.size}"
      end

      thread_stacks[tid] = [] if !thread_stacks.key?(tid)


      if location =~ /(.*)\.(.*) (.*)/
        l_class=$1
        l_method=$2
        l_args=$3
        id=l_class+"."+l_method

        if code == ENTER_CODE
          thread_stacks[tid].push({'ts' => ts,
                                   'id' => id})

          # Init total usage
          if not total_usages.key?(id)
            total_usages[id] = {
                'total_cpu' => 0,
                'total_time' => 0,
                'child_cpu' => 0,
                'child_time' => 0
            }
          end
        elsif code == EXIT_CODE || code == UNROLL_CODE
          if thread_stacks[tid].size > 0
            item = thread_stacks[tid].pop
            enter_ts = item['ts'] / 1000
            exit_ts = ts / 1000
            cpu_usage = find_cpu_for_period(enter_ts, exit_ts)

            weighted_cpu = cpu_usage * weight
            time_used = exit_ts - enter_ts

            # Add total usage of exited method
            total_usages[id]['total_cpu'] += weighted_cpu
            total_usages[id]['total_time'] += time_used

            # Update parent info
            thread_stacks[tid].each { |parent|
              parent_id = parent['id']
              total_usages[parent_id]['child_cpu'] += weighted_cpu
              total_usages[parent_id]['child_time'] += time_used
            }
          end
        elsif warn "Unknown code #{code}"
          exit
        end
      end
      counter+=1
    }

    total_usages.each { |id, f|
      puts "CPU #{f['total_cpu'].to_i} #{id} #{f['total_time']}"
    }

    total_usages.each { |id, f|
      puts "TIME #{f['total_time']} #{id} #{f['total_cpu'].to_i}"
    }

    total_usages.each { |id, f|
      puts "CHILDCPU #{f['total_cpu'].to_i - f['child_cpu'].to_i} #{id} #{f['total_time'] - f['child_time']}"
    }

    total_usages.each { |id, f|
      puts "CHILDTIME #{f['total_time'] - f['child_time']} #{id} #{f['total_cpu'].to_i - f['child_cpu'].to_i}"
    }
  end
end

