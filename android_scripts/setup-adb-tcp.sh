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

ADB="adb"
WLAN_IFACE="eth0"
IP=`$ADB shell "ifconfig $WLAN_IFACE 2>/dev/null" | awk '{print $3}'`
if [ "$IP" == "" ]; then
    echo "Empty IP detected, exiting"
    exit 1
fi
echo "Detected device IP as '$IP'"

ID_OUTPUT=`$ADB shell "id" | awk '{print $1}'`
ROOT_ID="uid=0(root)"
if [ "$ID_OUTPUT" != "$ROOT_ID" ]; then
    echo "ADB is not running as root (id output '$ID_OUTPUT', expected '$ROOT_ID')"
    echo "Restart adb as root with '$ADB root' and retry"
    exit 1
fi

echo "Setting device to TCP mode"
$ADB tcpip 5555

echo "Waiting a while for adbd to restart on device..."
sleep 2

echo "Connecting over TCP"
$ADB connect $IP

echo "Return to USB by running '$ADB usb' (also in adb root mode)"
