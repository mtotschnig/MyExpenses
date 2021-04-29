package org.totschnig.myexpenses.preference

fun shouldStartAutoFill(prefHandler: PrefHandler) = with(prefHandler) {
    getBoolean(PrefKey.AUTO_FILL_SWITCH, true) && oneOfMany(prefHandler)
}

fun oneOfMany(prefHandler: PrefHandler) : Boolean {
    with(prefHandler) {
        if(getBoolean(PrefKey.AUTO_FILL_AMOUNT, true))
            return true
        else if(getBoolean(PrefKey.AUTO_FILL_CATEGORY, true))
            return true
        else if(getBoolean(PrefKey.AUTO_FILL_COMMENT, true))
            return true
        else if(getBoolean(PrefKey.AUTO_FILL_METHOD, true))
            return true
    }
    return false
}

fun enableAutoFill(prefHandler: PrefHandler) {
    with(prefHandler) {
        putBoolean(PrefKey.AUTO_FILL_SWITCH, true)
        putBoolean(PrefKey.AUTO_FILL_AMOUNT, true)
        putBoolean(PrefKey.AUTO_FILL_CATEGORY, true)
        putBoolean(PrefKey.AUTO_FILL_COMMENT, true)
        putBoolean(PrefKey.AUTO_FILL_METHOD, true)
        putString(PrefKey.AUTO_FILL_ACCOUNT, "aggregate")
    }
}

fun disableAutoFill(prefHandler: PrefHandler) {
    with(prefHandler) {
        putBoolean(PrefKey.AUTO_FILL_SWITCH, false)
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
