TrafficMonitor
==============

TrafficMonitor can be used to capture TCP packets under Android using netfilter
hooks. A modified version has also been used with N900.

Compiling
---------

This version has been directly tested with Android kernel 2.6.32 for Google
Nexus One.

Before compiling you will need to add a line to the kernel under
include/linux/netlink.h (in 2.6.32, file might vary in newer versions), by
adding 
<pre>#define NETLINK_TRAFFICMONITOR 20</pre>
after the other netlink protocols. Pick whichever is the next available number,
in 2.6.32 it is 20.

Then configure paths to Kernel and Android NDK (if compiling for Android) to file
named <code>config</code> and run <code>make</code> under the directory
<code>sdk</code>.  You'll end up with <code>ec.ko</code> which can be loaded to
the phone with 
<pre>
$ insmod ec.ko
</pre>

Authors and licensing
---------------------
TrafficMonitor Copyright (C) 2011, Ahmad Nazir, Mohammad Hoque, Wei Li and Aki Saarinen.

TrafficMonitor was developed in affiliation with Aalto University School of Science,
Department of Computer Science and Engineering. For more information about the
department, see http://cse.aalto.fi/.

TrafficMonitor is distributed under the GNU General Public License v3, which should
be distributed with this program, in file COPYING. The license is also
available at http://www.gnu.org/licenses/gpl-3.0.txt.
