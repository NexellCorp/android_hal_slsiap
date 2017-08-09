LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_PRELINK_MODULE := false

#LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := liblog libcutils libjpeg

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)	\
	external/libjpeg-turbo\
	$(call include-path-for)

LOCAL_CFLAGS += -DANDROID

LOCAL_SRC_FILES:= libnxjpeg.c

LOCAL_MODULE:= libNX_Jpeg

include $(BUILD_SHARED_LIBRARY)
