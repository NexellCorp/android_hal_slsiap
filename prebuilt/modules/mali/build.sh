#!/bin/bash

#defalut value
BUILD=debug
#BUILD=release


#===================================
# mali environment config for user
#===================================
KDIR=../../../../../../kernel
#KDIR=~/devel/build/s5p6818/linux_ln
#KDIR=~/project/zynq/petalinux/build/linux/kernel/xlnx-3.19
#KDIR=~/project/zynq_nexell/kernel/kernel-3.18

CROSS_COMPILE=arm-eabi-
#CROSS_COMPILE=arm-cortex_a9-linux-gnueabi-
#CROSS_COMPILE=aarch64-linux-gnu-
#CROSS_COMPILE=arm-xilinx-linux-gnueabi-

USING_UMP=0
USING_PROFILING=0
MALI_SHARED_INTERRUPTS=1
CONFIG_ARCH_S5P6818=0
CONFIG_ARCH_ZYNQ=1


#===================================
# mali device driver build
#===================================
make clean KDIR=$KDIR BUILD=$BUILD \
	USING_UMP=$USING_UMP USING_PROFILING=$USING_PROFILING MALI_SHARED_INTERRUPTS=$MALI_SHARED_INTERRUPTS CONFIG_ARCH_S5P6818=$CONFIG_ARCH_S5P6818 CONFIG_ARCH_ZYNQ=$CONFIG_ARCH_ZYNQ

make -j7 KDIR=$KDIR BUILD=$BUILD \
	USING_UMP=$USING_UMP USING_PROFILING=$USING_PROFILING MALI_SHARED_INTERRUPTS=$MALI_SHARED_INTERRUPTS CONFIG_ARCH_S5P6818=$CONFIG_ARCH_S5P6818 CONFIG_ARCH_ZYNQ=$CONFIG_ARCH_ZYNQ

	
cp mali.ko ../
sudo cp -a mali.ko ~/devel/nfs/kernel_rootfs-zynq/test/


