#!/bin/bash
SDK_ROOT=~/android/sdk
P_ROOT=~/android/profiler
BASE=$1

# Process network measurements
"$P_ROOT/analyzer/src/convert-dmesg.rb" < $BASE.nraw > $BASE.nw

# Process method trace
"$SDK_ROOT/tools/dmtracedump" -o $BASE.trace > $BASE.dump
"$P_ROOT/analyzer/src/convert-to-abs-time.rb" $BASE > $BASE.abs
"$P_ROOT/r/grep-network.sh" $BASE.abs > $BASE-network.abs

# Get timestamp
echo "timestamp" > $BASE.timestamp
grep "profiling-start-time" $BASE.trace | cut -c22- >> $BASE.timestamp

