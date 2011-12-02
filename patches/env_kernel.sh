#!/bin/bash
ANDROID_PATH="/home/amsaarin/android/android-2.2.1-r2"
NDK_PATH="/home/amsaarin/android/ndk"

. $ANDROID_PATH/build/envsetup.sh
export PATH="$NDK_PATH/toolchains/arm-eabi-4.4.0/prebuilt/linux-x86/bin":$ANDROID_PATH/platform/out/host/linux-x86/bin:$PATH
export ARCH=arm
export CROSS_COMPILE=arm-eabi-
echo "*!* REMEMBER TO SOURCE *!*"
