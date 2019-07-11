# DEPRECATED: This variant is no longer maintained. Use common-gingerbread instead
#
# Include this make file to build your application against this module.
#
# Make sure to include it after you've set all your desired LOCAL variables.
# Note that you must explicitly set your LOCAL_RESOURCE_DIR before including this file.
#
# For example:
#
#   LOCAL_RESOURCE_DIR := \
#        $(LOCAL_PATH)/res
#
#   include frameworks/opt/setupwizard/library/common-platform-deprecated.mk
#

# Path to directory of setup wizard lib (e.g. frameworks/opt/setupwizard/library)
suwlib_path := $(dir $(lastword $(MAKEFILE_LIST)))

ifneq ($(LOCAL_USE_AAPT2),true)
  LOCAL_RESOURCE_DIR += \
      $(suwlib_path)/main/res \
      $(suwlib_path)/platform/res
  LOCAL_AAPT_FLAGS += --auto-add-overlay --extra-packages com.android.setupwizardlib
  LOCAL_STATIC_JAVA_LIBRARIES += setup-wizard-lib
else # LOCAL_USE_AAPT2 := true
  LOCAL_STATIC_ANDROID_LIBRARIES += setup-wizard-lib
endif # LOCAL_USE_AAPT2
