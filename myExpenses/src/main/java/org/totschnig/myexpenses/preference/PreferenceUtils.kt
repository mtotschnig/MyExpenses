package org.totschnig.myexpenses.preference

fun shouldStartAutoFill(prefHandler: PrefHandler) = with(prefHandler) {
    getBoolean(PrefKey.AUTO_FILL_AMOUNT, false) || getBoolean(PrefKey.AUTO_FILL_CATEGORY, false) ||
            getBoolean(PrefKey.AUTO_FILL_COMMENT, false) || getBoolean(PrefKey.AUTO_FILL_METHOD, false) ||
            getString(PrefKey.AUTO_FILL_ACCOUNT, "never") != "never"
}

fun enableAutoFill(prefHandler: PrefHandler) {
    with(prefHandler) {
        putBoolean(PrefKey.AUTO_FILL_AMOUNT, true)
        putBoolean(PrefKey.AUTO_FILL_CATEGORY, true)
        putBoolean(PrefKey.AUTO_FILL_COMMENT, true)
        putBoolean(PrefKey.AUTO_FILL_METHOD, true)
        putString(PrefKey.AUTO_FILL_ACCOUNT, "aggregate")

    }
}

fun disableAutoFill(prefHandler: PrefHandler) {
    with(prefHandler) {
        putBoolean(PrefKey.AUTO_FILL_AMOUNT, false)
        putBoolean(PrefKey.AUTO_FILL_CATEGORY, false)
        putBoolean(PrefKey.AUTO_FILL_COMMENT, false)
        putBoolean(PrefKey.AUTO_FILL_METHOD, false)
        putString(PrefKey.AUTO_FILL_ACCOUNT, "never")
    }
}

fun PrefHandler.putLongList(key: String, value: List<Long>) {
    putString(key, value.joinToString(separator = ","))
}

fun PrefHandler.getLongList(key: String) =
        requireString(key, "").takeIf { it.isNotEmpty() }?.split(',')?.map(String::toLong) ?: emptyList()

fun PrefHandler.requireString(key: PrefKey, defaultValue: String) =
        getString(key, defaultValue)!!

fun PrefHandler.requireString(key: String, defaultValue: String) =
        getString(key, defaultValue)!!
