#!/bin/bash

# This file is part of SmartDiet.
# 
# Copyright (C) 2011, Aki Saarinen.
# 
# SmartDiet was developed in affiliation with Aalto University School 
# of Science, Department of Computer Science and Engineering. For
# more information about the department, see <http://cse.aalto.fi/>.
# 
# SmartDiet is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# SmartDiet is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with SmartDiet.  If not, see <http://www.gnu.org/licenses/>.

if [ $# = 0 ]; then
    echo "Usage: $0 <dir> [device]"
    echo "* if device not given, will use first device from 'adb devices' list"
    exit 1
fi

DIR=$1

if [ -d "$DIR" ]; then
    echo "Directory '$DIR' already exists, unable to continue!"
    exit 1
fi

mkdir $DIR

if [ ! -d "$DIR" ]; then
    echo "Unable to create '$DIR'"
    exit 1
fi

echo "STARTING to record to '`pwd`/$DIR'"
date

STATIC_FILES_DIR=./files
DEVICE_DIR=/data/modules
DMESG_OUTPUT_FILE=$DIR/dmesg.out
LOGCAT_BEFORE_FILE=$DIR/logcat.before.out
LOGCAT_OUTPUT_FILE=$DIR/logcat.out
INSMOD_OUTPUT_FILE=$DIR/insmod.out

ADB_BIN="adb"

if [ $# -le 2 ]; then
    ADB_DEVICE=`$ADB_BIN devices | head -n2 | tail -n1 | awk '{print $1}'`
else
    ADB_DEVICE=$2
fi

echo "Using device '$ADB_DEVICE'"

ADB_ARGS="-s $ADB_DEVICE"
ADB="$ADB_BIN $ADB_ARGS"

TEST_CMD=`$ADB shell "echo -n 'testing'"`

if [ "$TEST_CMD" != "testing" ]; then
    echo "Unable to run test command for '$ADB_DEVICE', probably invalid device"
    echo "Test command output: '$TEST_CMD'"
    exit 1
fi

if [ "`echo $ADB_DEVICE | grep ":"`" != "" ]; then
    IS_NETWORK_DEVICE=1
    echo "* Using a network device!"
else 
    IS_NETWORK_DEVICE=0
    echo "* Using an USB device!"
fi

ID_OUTPUT=`$ADB shell "id" | awk '{print $1}'`
ROOT_ID="uid=0(root)"
if [ "$ID_OUTPUT" != "$ROOT_ID" ]; then
    echo "ADB is not running as root (id output '$ID_OUTPUT', expected '$ROOT_ID')"
    echo "Restart adb as root with '$ADB root' and retry"
    exit 1
fi

echo "* Removing old crap from device"
$ADB shell "rmmod ec.ko 2&> /dev/null" 
$ADB shell "dmesg -c 2&> /dev/null"
$ADB shell "mkdir $DEVICE_DIR 2&> /dev/null"

echo "* Uploading kernel module, might take a while (especially over TCP)..."
$ADB push $STATIC_FILES_DIR/ec.ko "$DEVICE_DIR" || exit 1
echo "* Starting traffic monitor. You might need to trigger a screen event (e.g. push the lock button) to get one event into the buffer to continue."
$ADB shell "logcat -d -b main" > $LOGCAT_BEFORE_FILE
$ADB shell "logcat -c"
$ADB shell "insmod $DEVICE_DIR/ec.ko" > $INSMOD_OUTPUT_FILE

insmod_errors=`grep "error" "$INSMOD_OUTPUT_FILE"`
if [ "$insmod_errors" != "" ]; then
    insmod_error_count=${#insmod_errors[@]}
    echo "Insmod failed, $insmod_error_count errors:"
    echo "${insmod_errors}"
    echo "Maybe kernel module is for wrong kernel version?"
    exit 1
fi

if [ $IS_NETWORK_DEVICE = 1 ]; then
    echo "* Disconnecting network device for measurements..."
    $ADB disconnect
fi

echo "* Started, waiting for user input"
read

if [ $IS_NETWORK_DEVICE = 1 ]; then
    echo "* Reconnecting network device after measurements..."
    $ADB_BIN connect $ADB_DEVICE
    echo "* Should be connected"
fi

$ADB shell "rmmod ec"
$ADB shell "dmesg -c" > $DMESG_OUTPUT_FILE
$ADB shell "logcat -d -b main" > $LOGCAT_OUTPUT_FILE
echo "* Disabled, results on '$DMESG_OUTPUT_FILE' and '$LOGCAT_OUTPUT_FILE'"
wc -l $DMESG_OUTPUT_FILE
wc -l $LOGCAT_OUTPUT_FILE

echo "Done"
date
