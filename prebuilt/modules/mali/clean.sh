#!/bin/bash

#defalut value
#BUILD=debug
BUILD=release


#===================================
# mali environment config for user
#===================================
KDIR=../../../../../../kernel/
CROSS_COMPILE=aarch64-linux-gnu-

USING_UMP=0
USING_PROFILING=0
MALI_SHARED_INTERRUPTS=1

#===================================
# mali device driver build
#===================================
make clean KDIR=$KDIR BUILD=$BUILD \
	USING_UMP=$USING_UMP USING_PROFILING=$USING_PROFILING MALI_SHARED_INTERRUPTS=$MALI_SHARED_INTERRUPTS
