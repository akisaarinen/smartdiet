SmartDiet
=========

SmartDiet is a *proof-of-concept toolkit* for analyzing energy usage of Android applications and
identifying constraints regarding mobile code offloading. This is a research toolkit,
designed to highlight the issues related to mobile offloading, *not a polished set of tools
for everyday use*.

It was developed as part of a research project where our goal was to investigate the feasibility
of mobile offloading as well as look for ways to provide insights for the application developer about the
offloadability of existing programs. More details can be found from our paper (currently in the academic
publishing limbo, will be linked here when it gets published).

SmartDiet consists of two tools:

* Constraint analysis tool to highlight offloadability issues in program source
  code through static code analysis.
* Energy analysis tool to dynamically profile the energy consumption of an
  Android program and to virualize it in a fine-grained per-method level.

Authors and license
-------------------

SmartDiet is Copyright (C) 2011, Aki Saarinen. 

SmartDiet was developed in affiliation with Aalto University School of Science,
Department of Computer Science and Engineering. For more information about the
department, see http://cse.aalto.fi/.

SmartDiet is distributed under the GNU General Public License v3, which should
be distributed with this program, in file COPYING. The license is also
available at http://www.gnu.org/licenses/gpl-3.0.txt.
 
TrafficMonitor kernel module is handled separately, see the
<code>trafficmonitor</code> subdirectory for details.

What does SmartDiet do?
-----------------------

* Static analysis for Android Java applications to highlight issues that might prevent offloading (i.e.
  partial remote execution of application code in somewhere else than in the phone). We look for
  e.g. serializability issues and access to local filesystems, which might cause problems when part of
  the application is executed somewhere else.
* Dynamic energy usage profiling for Android Java Applications. We capture all network traffic as well as
  the program execution flow, and display a graph showing fine-grained analysis which parts of the application
  caused network traffic, along with estimates how much energy that consumed (based on models).
* Tools for processing Monsoon PowerMonitor (http://www.msoon.com) power measurement traces to combine
  actual physical measurements of the device energy consumption to model-based estimates.

Why would I use SmartDiet?
--------------------------

SmartDiet is interesting to researchers doing work related to energy-efficiency
of smartphones. It can provide insights into the energy-usage and offloading
constraints of existing Android applications. We released one paper based on
the insights we gathered using SmartDiet, but it could be used for more.

Similar tools could be established for mainstream developers to enable better
investigation of energy usage properties of any application. However, at its
current state, SmartDiet is probably not ready for mainstream adoption, mainly
because of its quite laboursome installation procedure and customization
requirements (only Google Nexus One with Android 2.2.1-r2 at the moment).

What do I need to use SmartDiet?
--------------------------------

### All features require the following

* Source code for the Android apps you want to profile.
* A Linux or OSX host computer. Tested to work with OSX 10.6 (Snow Leopard) and Ubuntu Linux 11.04.
* Android SDK installed in the host computer.
* Java installed in the host computer. Tested with Java 1.6.0\_26.

### For dynamic energy profiling, you'll also need

* Google Nexus One development phone. To work with other phones, changes are probably required.
* Android NDK installed in the host computer.
* Ruby and R software installed in the host computer, tested with ruby 1.8.7 and R 2.12.1.
* Android 2.2.1-r2 source code (available freely from the Internets). Needs to be patched and
  installed to the Nexus One, so that we can accurately measure the program execution flow.
* This setup has been tested with Ubuntu Linux 11.04 host computer only.

Additionally, if you want to measure physical power usage when doing dynamic
profiling, you will need Monsoon PowerMonitor power measurement device
(http://www.msoon.com). This is strictly not required, though.  You can also
work only with the model-based estimates if you want.

Acknowledgements
----------------
Special thanks to Matti Siekkinen, Yu Xiao, Jukka Nurminen, Matti Kemppainen
and Antti Ylä-Jääski and the whole Department of Computer Science and
Engineering in Aalto University School of Science.

Setting up constraint analysis tool
===================================

Setting up the constraint analysis part of SmartDiet is relatively
straightforward, but involves a bit of work. You'll need a machine with
Java installed and sources codes for one or more Android programs. You need to
compile the SmartDiet analysis program (written in Scala). You also need to 
unzip Android SDK jars in order to track dependencies to the Android SDK
files. This section covers the setup of this part of SmartDiet.

Compiling the SmartDiet toolkit
-------------------------------
* Run <code>./sbt</code> to open up SBT (simple-build-tool) console. SBT documentation at
  https://github.com/harrah/xsbt/wiki, if you want to dig deeper.
* Fetch dependencies by running <code>update</code> in sbt console. This will take a while.
* Assemble a runnable JAR by running <code>assembly:assembly</code> in sbt console.
* You should see <code>target/powerprofiler-{version}.jar</code>, runnable with
  <code>java -jar powerprofiler-{version}.jar</code>.

Getting the bytecode sources for Android SDK
--------------------------------------------

To run the constraint analysis tool, you will need <code>.class</code> files of
all classes the analyzed application is using. All Android apps are referring
to the Android standard library (coming with the SDK), so in practice you'll
need the class files for that library.

Most of the required classes can be fetched from the Android SDK easily by
accessing one of the <code>android.jar</code> files coming with the SDK
under <code>platforms/android-X</code> directory, depending on what android
API version your program is relying on. Just grab the jar and unzip it using
<pre>
$ unzip android.jar
</pre>

...and you'll end up with a series of <code>.class</code> files that can be
used.

If you work with the platform apps, they also require some additional class
files that are not included in the SDK. One way to get these files is to follow
the Android compilation instructions for the energy profiler and then fetch the
intermediate class files from the compiling process and extract them somewhere
where the constraint analysis tool can access them. The licenses don't permit
us to redistribute these intermediate files so you'll have to do it on your own
or invent some other way of getting those files.

Take a look into <code>out/target/common/obj/JAVA_LIBRARIES</code> and
the directories under that which contain <code>classes.jar</code> files.
We used the classes from <code>framework_ intermediates/classes.jar</code>
and <code>sdk_v8_intermediates/classes.jar</code> but you might need something else too,
depending on the application. At least platform apps might need also
<code>android_stubs_current_intermediates/classes.jar</code>.

In any case, you should end up with a directory which contains for example the following
files:

* Set.class (under <code>java/util/</code>)
* URI.class (under <code>java/net/</code>)
* HttpClient.class (under <code>org/apache/http/client/</code>
* Activity.class (under <code>android/app/</code>)
* and so on...

Configuring the constraint analysis tool
------------------------------------

Configure applications in <code>sources.json</code> or a file similar to this
(the name of the configuration file can be specified from the command line).

Remember to point the SDK directory ("sdkClassFilePath") to one where you have
*unzipped* all the SDK <code>.class</code> files, the tool doesn't read the
library files from inside jars.

For each program, you should specify:

* "name": Just something to describe it.
* "appPath": Directory containing compiled <code>.class</code> files for all application classes.
* "appSrcPath": Directory containing <code>.java</code> source files for all application classes.
* "libPath": Directory containing compiled <code>.class</code> files for all libraries that the application depends on (except for the SDK classes, which are under the sdkClassFilepath). These also need to be unzipped, nothing is looked from inside jars.

Running the constraint analysis tool
------------------------------------

Run the analysis with <code>java -jar powerprofiler.jar --java-all</code>, for
more information about flags, run the jar without arguments and check the help.
You can e.g. use <code>--csv</code> to output in CSV format.

Setting up energy analysis tool
===============================

To start using dynamic energy profiling part of SmartDiet, you need a more
complex procedure. You will need to root your Nexus One phone, compile a custom
kernel, the whole Android distribution with minor SmartDiet-specific patches
and the custom kernel. You'll also need to compile the traffic monitor kernel
module against this same custom kernel.  The custom stuff then needs to be
installed into the phone. This section will cover these topics.

Note that I'm assuming you're familiar with the Android platform and know that
there is a risk of bricking your phone, as always when installing custom
firmware. You're doing all of this on your own risk. Shouldn't be too big
a risk if you're careful, but still.

Compiling a custom kernel
-------------------------

You'll need to compile your custom kernel. No guide for this is available ATM. 
I used kernel version 2.6.32.

As a result you will have a compiled kernel which will be referred later as
<code>/path/to/zImage</code>. It will lie under the kernel source tree
at <code>arch/arm/boot/zImage</code>. You should test this kernel as-is, and
then continue applying our patches for it.

Compiling custom kernel with traffic monitor patches
----------------------------------------------------

In order to work with the traffic monitor kernel module, you need to patch the
kernel a bit. Patch for 2.6.32 is available under
<code>patches/kernel-2.6.32-traffic_monitor_netlink_protocol.patch</code>,
which can be applied using <code>git am</code> or (if the hashes don't match)
just by manually doing the same thing, because the addition is rather trivial.

Compiling the traffic monitor kernel module
-------------------------------------------

See the <code>trafficmonitor</code> subdirectory for more information.  This is
a separate kernel module developed at the Aalto University School of Science
and it can be compiled against the patched kernel sources. You should get a
<code>ec.ko</code> file which should be put into the <code>files</code>
subdirectory to be used by the measurement scripts later on (they will upload
and load it into use into the phone). Note that because this is a kernel
module, it has to be compiled against the exact kernel version you are running
or it won't load up correctly.

Compiling Android 2.2.1-r2
-------------------------------------------------------
Official instructions are available at http://source.android.com/source/initializing.html,
these are the steps that worked for me in Ubuntu 11.04. Another useful resource is
http://source.android.com/source/build-numbers.html for the various build numbers and
identifiers for Android.

* Fetch the repo script if you don't already have it. Tested with version (1,13)
  <pre>
  $ curl https://dl-ssl.google.com/dl/googlesource/git-repo/repo > ~/bin/repo
  </pre>
* Make sure your system is configured with Java 1.5 (Android compilation requires this).
  For my Ubuntu 11.04 this can be done with the following command:
  <pre>
  $ sudo update-alternatives --set java /usr/lib/jvm/java-1.5.0-sun/jre/bin/java
  </pre>
* Create a working directory for the platform compilation. Rest of the instructions assume you work under the
  directory created here.
  <pre>
  $ mkdir android-2.2.1-r2
  $ cd android-2.2.1-r2
  </pre>
* Initialize and fetch the repository (last step downloads a *lot* of stuff and takes time)
  <pre>
  $ repo init -u https://android.googlesource.com/platform/manifest -b android-2.2.1_r2
  $ repo sync
  </pre>
* Compile the ADB tool and make it available in PATH
  <pre>
  $ make adb
  $ export PATH=`pwd`/out/host/linux-x86/bin:$PATH
  $ which adb
  </pre>
  You should see <code>/home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/adb</code> or something similar.
* Connect the Nexus One with USB and test connection with adb
  <pre>
  $ adb devices
  List of devices attached
  HT0B2P800954  device
  </pre>
* Fetch required proprietary files from Nexus One (in order to compile the platform). These
  files are not distributed with the platform sources, so you need to have the stock Android
  2.2 in the phone to do this.
  <pre>
  $ cd device/htc/passion
  $ ./extract-files.sh
  </pre>
* Compile Android platform first without any modifications (to make sure everything works in your environment).
  This will also take a while, results should appear in <code>out/target/product/passion/</code>
  <pre>
  $ source build/envsetup.sh
  $ echo 4 | lunch
  $ make -j2
  </pre>
  Results will appear in <code>out/target/product/passion/</code> if everything went well.
* Install the resulting images to your phone to make sure everything works as supposed at this point.

Installing custom Android distribution to Nexus One
---------------------------------------------------
* First compile the distribution, and then go to <code>out/target/product/passion/</code>
  <pre>
  $ cd out/target/product/passion/
  </pre>
* Reboot the phone to bootloader
  <pre>
  $ adb reboot-bootloader
  </pre>
* Find path to fastboot (should appear there if you adjusted the PATH earlier in the compile process).
  <pre>
  $ which fastboot
  /home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/fastboot
  </pre>
  Yours should look similar to this. Adjust paths correctly in the following commands.
* Flash all partitions. Make sure you don't disconnect or boot the phone while flashing.
  <pre>
  $ sudo /home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/fastboot flash boot boot.img
        sending 'boot' (2338 KB)... OKAY [  0.338s]
                  writing 'boot'... OKAY [  0.975s]
  finished. total time: 1.313s
  $ sudo /home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/fastboot flash recovery recovery.img
    sending 'recovery' (2564 KB)... OKAY [  0.369s]
              writing 'recovery'... OKAY [  1.063s]
  finished. total time: 1.433s
  $ sudo /home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/fastboot flash system system.img
     sending 'system' (74841 KB)... OKAY [ 10.323s]
                writing 'system'... OKAY [ 27.256s]
  finished. total time: 37.580s
  $ sudo /home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/fastboot flash userdata userdata.img
       sending 'userdata' (2 KB)... OKAY [  0.014s]
              writing 'userdata'... OKAY [  2.377s]
  finished. total time: 2.391s
  </pre>
* Reboot the phone
  <pre>
  $ sudo /home/amsaarin/android/2.2.1-r2/out/host/linux-x86/bin/fastboot reboot
  </pre>
* Phone should boot up normally, goto Settings -> About phone and check that Build number is something like this:
  <pre>
  full_passion-userdebug 2.2.1 FRG83D eng.
  amsaarin.20111024.152752 test-keys
  </pre>

Compiling Android with custom kernel
------------------------------------

Next you should compile the Android with your custom kernel and check that it works. This can be done
by running:

<pre>
$ make TARGET_PREBUILT_KERNEL=/path/to/zImage
</pre>

Note that after you do this, standard kernel modules distributed with the
Android distribution will be incompatible with the kernel and won't hence load
up. Most important one is the driver dealing with WiFi,
<code>bcm4329.ko</code>, so you'll need to patch that too.  Copy the one from
the compiled kernel under <code>drivers/net/wireless/bcm4329/bcm4329.ko</code>
to <code>device/htc/passion-common/bcm4329.ko</code> under the Android
distribution before compiling, and it'll be shipped to the phone when flashing.

Before continuing, check that everything works by booting up the phone and
checking versions (also kernel version should now change in Settings -> About
phone).

Compiling SmartDiet modifications to Android platform with the custom kernel
----------------------------------------------------------------------------
* Compile stock Android 2.2.1-r2 first as instructed above
* Go to <code>dalvik</code> subdirectory in your <code>android-2.2.1-r2</code> platform source directory.
  <pre>
  $ cd dalvik
  </pre>
* Apply patch using <code>git am</code>
  <pre>
  $ git am /path/to/powerprofiler/patches/android-2.2.1-r2-dalvik-logging.patch
  Applying: Log more clock-related variables and increase buffer size
  </pre>
* Verify that patch got applied by running <code>git log</code>
* Recompile Android, run the following in your <code>android-2.2.1-r2</code> platform source directory.
  <pre>
  $ echo 4 | lunch
  $ make -j2
  </pre>
* Install the modified version to the phone using the same procedure as before. This time you only
  need to re-flash the system partition.

Compiling SmartDiet modifications to Android SDK
------------------------------------------------
* Go to your <code>android-2.2.1-r2</code> platform source directory.
* Go to <code>sdk/ddms</code> subdirectory in your <code>android-2.2.1-r2</code> platform source directory.
  <pre>
  $ cd sdk/ddms
  </pre>
* Apply patch using <code>git am</code>
  <pre>
  $ git am /path/to/powerprofiler/patches/android-2.2.1-r2-ddms-buffer_size_increase.patch
  Applying: Increase default buffer size in ddms java application
  </pre>
* Verify that patch got applied by running <code>git log</code>
* Compile Android SDK, run the following in your <code>android-2.2.1-r2</code> platform source directory.
  <pre>
  $ echo 1 | lunch
  $ make sdk
  </pre>
* Compiled SDK is available under <code>out/host/linux-x86/sdk/</code>, to use DDMS with a bigger
  tracing buffer size limit, run it from there (<code>tools/ddms</code>).

Using the energy profiler tool
==============================

After the phone is bundled with our customized version of Android, you can
start dynamic profiling.

Before doing anything, check the following:
* The phone is rooted.
* You have compiled and installed the patched Android to the phone.
* You have access to the patched and compiled SDK (you'll need DDMS from there).
* Synchronize clocks between machines and phones. This is *important* because
  data is matched based on timestamps. NTP synchronization is preferred.

Profiling an application
------------------------

### 1) Before measurements

* Compile and install the program you are profiling so that it allows debugging,
  i.e. you can connect to it using DDMS (flags in the AndroidManifest.xml file).
* Start the program in the phone.
* Run <code>./run-smartdiet-dynamic-measurements.sh DIR</code> with phone
  connected with either USB or TCP to adb. For more information check the
  sources of the script. 
* Start up Android DDMS from your customized SDK. This customized DDMS has a
  larger buffer size for the program tracing, so longer test runs can be done
  than would be possible with the default settings. From DDMS, select the program
  you're profiling and select 'start method profiling' to start tracing
  the program execution.
* Unplug the USB cable if you're measuring physical power consumption with
  Monsoon. It will try to load the imaginary battery and mess up the
  measurements.

### 2) Execute the test sequence

You can e.g. trigger some network requests from the application by pushing some reload buttons or whatever.

### 3) After you're done

* Re-connect the USB cable and then stop the script by pressing return. This
  will be fetch measurements to <code>DIR</code> from the phone.
* Stop method profiling from the DDMS. Wait a while, and a window pops up which
  shows the graphical UI for inspecting the method trace. You don't need the graphical tool, but
  from the title text you can see where the raw DDMS file is, it should be
  something like <code>/tmp/ddms123123123123.trace</code>. Close the window but
  copy the .trace file into <code>DIR</code> as <code>program.trace</code>.
* If you did physical measurements with Monsoon, stop recording and export data
  from Monsoon Power Monitor to pt4 file format and put resulting file as
  <code>power.pt4</code> under <code>DIR</code> (with the other files).

