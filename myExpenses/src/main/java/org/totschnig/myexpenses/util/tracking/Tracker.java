package org.totschnig.myexpenses.util.tracking;

import android.content.Context;
import android.os.Bundle;

public interface Tracker {
  String EVENT_DISPATCH_COMMAND = "dispatch_command";
  String EVENT_SELECT_OPERATION_TYPE = "select_operation_type";
  String EVENT_CONTRIB_DIALOG_NEGATIVE = "contrib_dialog_negative";
  String EVENT_CONTRIB_DIALOG_CANCEL = "contrib_dialog_cancel";
  String EVENT_CONTRIB_DIALOG_BUY = "contrib_dialog_buy";
  String EVENT_AD_REQUEST = "ad_request";
  String EVENT_AD_LOADED = "ad_loaded";
  String EVENT_AD_FAILED = "ad_failed";
  String EVENT_AD_CUSTOM = "ad_custom";
  String EVENT_PREFERENCE_CLICK = "preference_click";
  String EVENT_RATING_DIALOG = "rating_dialog";
  //only used for interstitial
  String EVENT_AD_SHOWN = "ad_shown";
  String EVENT_PARAM_AD_PROVIDER = "provider";
  String EVENT_PARAM_AD_TYPE = "type";
  String EVENT_PARAM_AD_ERROR_CODE = "error_code";
  String EVENT_PARAM_PACKAGE = "package";
  String EVENT_PARAM_OPERATION_TYPE = "operation_type";
  String EVENT_PARAM_ITEM_ID = "item_id";
  String EVENT_PARAM_BUTTON_ID = "button_id";


  void init(Context context);

  void logEvent(String eventName, Bundle params);

  void setEnabled(boolean enabled);
}
