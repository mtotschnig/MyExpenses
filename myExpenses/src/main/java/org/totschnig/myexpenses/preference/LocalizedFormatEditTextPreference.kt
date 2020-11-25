package org.totschnig.myexpenses.preference

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.preference.EditTextPreference

/**
 * an edit text preference that only stores values if they are different than a defined default value
 * This is meant for preferences with localized defaults, so that when the locale changes, we do not
 * end up with the persisted default from the previous locale
 */
abstract class LocalizedFormatEditTextPreference constructor(context: Context, attrs: AttributeSet) :
        EditTextPreference(context, attrs) {
    var onValidationErrorListener: OnValidationErrorListener? = null

    interface OnValidationErrorListener: OnPreferenceChangeListener {
        fun onValidationError(messageResId: Int)
    }

    override fun getText(): String? {
        return super.getText().takeIf { !TextUtils.isEmpty(it) } ?: getDefaultValue()
    }

    abstract fun getDefaultValue(): String?

    abstract fun validate(newValue: String): Int?

    override fun persistString(value: String?) = when {
        !shouldPersist() -> false
        value == getDefaultValue() || TextUtils.isEmpty(value) -> {
            voidValue()
            false
        }
        else -> super.persistString(value)
    }

    private fun voidValue() {
        preferenceManager.sharedPreferences.edit()
                .remove(key)
                .apply()
    }

    override fun callChangeListener(newValue: Any?) = (newValue as? String)
            ?.takeIf { !TextUtils.isEmpty(it) }
            ?.let { validate(it) }
            ?.let {
                onValidationErrorListener?.onValidationError(it)
                false
            } ?: super.callChangeListener(newValue)
}