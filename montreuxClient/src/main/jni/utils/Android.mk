LOCAL_PATH:= $(call my-dir)

# The purpose of this part of make is to create wrapper around libfastcv.a library
# Build module will be also called 'libfastcv'
LOCAL_MODULE    := libfastcv
LOCAL_SRC_FILES := ../../libs/libfastcv.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE:= false

# This variable determines the OpenGL ES API version to use:
# If set to true, OpenGL ES 1.1 is used, otherwise OpenGL ES 2.0.

USE_OPENGL_ES_1_1 := false

# Set OpenGL ES version-specific settings.

ifeq ($(USE_OPENGL_ES_1_1), true)
    OPENGLES_LIB  := -lGLESv1_CM
	OPENGLES_DEF  := -DUSE_OPENGL_ES_1_1
else
    OPENGLES_LIB  := -lGLESv2
	OPENGLES_DEF  := -DUSE_OPENGL_ES_2_0
endif

# An optional set of compiler flags that will be passed when building
# C ***AND*** C++ source files.
#
# NOTE: flag "-Wno-write-strings" removes warning about deprecated conversion
#       from string constant to 'char*'

LOCAL_CFLAGS := -Wno-write-strings $(OPENGLES_DEF)
LOCAL_LDFLAGS:= -Wl,--no-fix-cortex-a8

LOCAL_MODULE    := libfastcvUtils
LOCAL_CFLAGS    := -Werror
LOCAL_C_INCLUDES += $(JNI_DIR)
LOCAL_SRC_FILES := \
    FPSCounter.cpp \
    CameraRendererRGB565GL2.cpp \
    CameraUtil.cpp \
    FastCVSampleRenderer.cpp \
    FastCVUtil.cpp

LOCAL_LDLIBS := -llog $(OPENGLES_LIB)
LOCAL_SHARED_LIBRARIES := liblog libGLESv2
LOCAL_STATIC_LIBRARIES := libfastcv

include $(BUILD_SHARED_LIBRARY)