##
# Build the platform version of setup wizard library.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_MANIFEST_FILE := main/AndroidManifest.xml
LOCAL_MODULE := setup-wizard-lib
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/main/res \
    $(LOCAL_PATH)/platform/res
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, main/src platform/src)

include $(BUILD_STATIC_JAVA_LIBRARY)

##
# Build eclair-mr1-compat library, which uses AppCompat support library to provide backwards
# compatibility back to SDK v7.
#

include $(CLEAR_VARS)

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.appcompat
LOCAL_MANIFEST_FILE := main/AndroidManifest.xml
LOCAL_MODULE := setup-wizard-lib-eclair-mr1-compat
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/main/res \
    $(LOCAL_PATH)/eclair-mr1/res \
    frameworks/support/v7/appcompat/res
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, main/src eclair-mr1/src)
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v7-appcompat

include $(BUILD_STATIC_JAVA_LIBRARY)


##
# Build the full-support library, which includes RecyclerView and any other support libraries as
# they are integrated.
#
include $(CLEAR_VARS)

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.recyclerview
LOCAL_MANIFEST_FILE := main/AndroidManifest.xml
LOCAL_MODULE := setup-wizard-lib-full-support
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/main/res \
    $(LOCAL_PATH)/eclair-mr1/res \
    $(LOCAL_PATH)/full-support/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/recyclerview/res
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, main/src eclair-mr1/src full-support/src)
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-recyclerview

include $(BUILD_STATIC_JAVA_LIBRARY)
