# This file is part of TrafficMonitor.
# 
# Copyright (C) 2011, Ahmad Nazir, Mohammad Hoque, Wei Li and Aki Saarinen.
#
# TrafficMonitor is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# TrafficMonitor is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with TrafficMonitor.  If not, see <http://www.gnu.org/licenses/>.

include $(PWD)/../config

PATH:=$(NDK_PATH)/toolchains/arm-eabi-4.4.0/prebuilt/linux-x86/bin:$(PATH)
ARCH := arm
CROSS_COMPILE := arm-eabi-

obj-m := ec.o

all:
	test -d $(KERNEL_PATH) || exit 1
	test -d $(NDK_PATH) || exit 1
	ARCH=$(ARCH) CROSS_COMPILE=$(CROSS_COMPILE) make -C $(KERNEL_PATH) M=$(PWD) modules
clean:
	ARCH=$(ARCH) CROSS_COMPILE=$(CROSS_COMPILE) make -C $(KERNEL_PATH) M=$(PWD) clean

