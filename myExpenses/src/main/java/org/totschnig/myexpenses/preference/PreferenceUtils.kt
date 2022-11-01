package org.totschnig.myexpenses.preference

fun shouldStartAutoFill(prefHandler: PrefHandler) = with(prefHandler) {
    getBoolean(PrefKey.AUTO_FILL_SWITCH, false) &&
            (getBoolean(PrefKey.AUTO_FILL_AMOUNT, true)
                    || getBoolean(PrefKey.AUTO_FILL_CATEGORY, true)
                    || getBoolean(PrefKey.AUTO_FILL_COMMENT, true)
                    || getBoolean(PrefKey.AUTO_FILL_METHOD, true)
                    || getString(PrefKey.AUTO_FILL_ACCOUNT, "aggregate") != "never")
}

fun shouldStartAutoFillWithFocus(prefHandler: PrefHandler) = with(prefHandler) {
    shouldStartAutoFill(prefHandler) && getBoolean(PrefKey.AUTO_FILL_FOCUS, true)
}

fun enableAutoFill(prefHandler: PrefHandler) {
    prefHandler.putBoolean(PrefKey.AUTO_FILL_SWITCH, true)
    prefHandler.putBoolean(PrefKey.AUTO_FILL_HINT_SHOWN, true)
}

fun disableAutoFill(prefHandler: PrefHandler) =
        prefHandler.putBoolean(PrefKey.AUTO_FILL_SWITCH, false)
