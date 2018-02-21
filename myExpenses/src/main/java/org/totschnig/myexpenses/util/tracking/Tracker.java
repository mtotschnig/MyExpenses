package org.totschnig.myexpenses.util.tracking;

import android.content.Context;
import android.os.Bundle;

public interface Tracker {
  String EVENT_SELECT_MENU = "select_menu";
  String EVENT_SELECT_OPERATION_TYPE = "select_operation_type";
  String EVENT_CONTRIB_DIALOG_NEGATIVE = "contrib_dialog_negative";
  String EVENT_CONTRIB_DIALOG_CANCEL = "contrib_dialog_cancel";
  String EVENT_CONTRIB_DIALOG_BUY = "contrib_dialog_buy";
  String EVENT_AD_REQUEST = "ad_request";
  String EVENT_AD_LOADED = "ad_loaded";
  String EVENT_AD_FAILED = "ad_failed";
  //only used for interstitial
  String EVENT_AD_SHOWN = "ad_shown";
  String EVENT_PARAM_AD_PROVIDER = "provider";
  String EVENT_PARAM_AD_TYPE = "type";
  String EVENT_PARAM_AD_ERROR_CODE = "error_code";
  String EVENT_PARAM_PACKAGE = "package";
  String EVENT_PARAM_OPERATION_TYPE = "operation_type";
  String EVENT_PARAM_ITEM_ID = "item_id";


  void init(Context context);

  void logEvent(String eventName, Bundle params);

  void setEnabled(boolean enabled);
}
