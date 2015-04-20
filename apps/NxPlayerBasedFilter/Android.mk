LOCAL_PATH  := $(call my-dir)

####################################################################################################
#
#	Build Package
#
include $(CLEAR_VARS)

LOCAL_SRC_FILES		:=	\
	$(call all-java-files-under, src)

LOCAL_PACKAGE_NAME	:=	\
	NxPlayerBasedFilter

LOCAL_RESOURCE_DIR :=	\
	$(LOCAL_PATH)/res

$(shell cp $(wildcard $(LOCAL_PATH)/jni/libs/*.so) $(TARGET_OUT_INTERMEDIATE_LIBRARIES))

LOCAL_JNI_SHARED_LIBRARIES :=	\
	libtheoraparser_and		\
	libavcodec-2.1.4		\
	libavdevice-2.1.4		\
	libavfilter-2.1.4		\
	libavformat-2.1.4		\
	libavresample-2.1.4		\
	libavutil-2.1.4			\
	libswresample-2.1.4		\
	libswscale-2.1.4		\
	libNX_FILTER			\
	libNX_FILTERHELPER		\
	libNX_MPMANAGER			\
	libnxmovieplayer

include $(BUILD_PACKAGE)

include $(call all-makefiles-under, $(LOCAL_PATH))
