package org.totschnig.myexpenses.util.tracking

import android.content.Context
import android.os.Bundle

interface Tracker {
    fun init(context: Context)
    fun logEvent(eventName: String, params: Bundle?)
    fun setEnabled(enabled: Boolean)

    companion object {
        const val EVENT_DISPATCH_COMMAND = "dispatch_command"
        const val EVENT_SELECT_OPERATION_TYPE = "select_operation_type"
        const val EVENT_CONTRIB_DIALOG_NEGATIVE = "contrib_dialog_negative"
        const val EVENT_CONTRIB_DIALOG_CANCEL = "contrib_dialog_cancel"
        const val EVENT_CONTRIB_DIALOG_BUY = "contrib_dialog_buy"
        const val EVENT_AD_REQUEST = "ad_request"
        const val EVENT_AD_LOADED = "ad_loaded"
        const val EVENT_AD_FAILED = "ad_failed"
        const val EVENT_AD_CUSTOM = "ad_custom"
        const val EVENT_PREFERENCE_CLICK = "preference_click"
        const val EVENT_RATING_DIALOG = "rating_dialog"

        //only used for interstitial
        const val EVENT_AD_SHOWN = "ad_shown"
        const val EVENT_PARAM_AD_PROVIDER = "provider"
        const val EVENT_PARAM_AD_TYPE = "type"
        const val EVENT_PARAM_AD_ERROR_CODE = "error_code"
        const val EVENT_PARAM_PACKAGE = "package"
        const val EVENT_PARAM_OPERATION_TYPE = "operation_type"
        const val EVENT_PARAM_ITEM_ID = "item_id"
        const val EVENT_PARAM_BUTTON_ID = "button_id"
    }
}