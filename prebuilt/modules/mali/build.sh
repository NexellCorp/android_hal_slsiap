#!/bin/bash

#defalut value
BUILD=debug
#BUILD=release

#===================================
# mali environment config for user
#===================================
#KDIR=../../../../../../kernel
#CROSS_COMPILE=arm-eabi-
#KDIR=~/devel/build/s5p6818/linux_ln
#CROSS_COMPILE=arm-cortex_a9-linux-gnueabi-
#CROSS_COMPILE=aarch64-linux-gnu-

KDIR=../../../../../../kernel/
#CROSS_COMPILE=aarch64-linux-gnu-
CROSS_COMPILE=aarch64-linux-gnu-

USING_UMP=0
USING_PROFILING=0
MALI_SHARED_INTERRUPTS=1
CONFIG_ARCH_S5P4418=0
CONFIG_ARCH_S5P6818=1

#===================================
# mali device driver build
#===================================
make clean KDIR=$KDIR BUILD=$BUILD \
	USING_UMP=$USING_UMP USING_PROFILING=$USING_PROFILING MALI_SHARED_INTERRUPTS=$MALI_SHARED_INTERRUPTS
#        CONFIG_ARCH_S5P4418=$CONFIG_ARCH_S5P4418 CONFIG_ARCH_S5P6818=$CONFIG_ARCH_S5P6818

make -j7 KDIR=$KDIR BUILD=$BUILD \
	USING_UMP=$USING_UMP USING_PROFILING=$USING_PROFILING MALI_SHARED_INTERRUPTS=$MALI_SHARED_INTERRUPTS
#        CONFIG_ARCH_S5P4418=$CONFIG_ARCH_S5P4418 CONFIG_ARCH_S5P6818=$CONFIG_ARCH_S5P6818

	
cp mali.ko ../

