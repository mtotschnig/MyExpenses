package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.R
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames

class AccountPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreference(context, attrs) {

    override fun findIndexOfValue(value: String?): Int =
        super.findIndexOfValue(value).coerceAtLeast(0)

    fun setData() {
        val accounts = getAccountNames(context)
        entries = arrayOf(context.getString(R.string.not_set), *accounts)
        entryValues = arrayOf(SYNCHRONIZATION_NONE, *accounts)
        notifyChanged()
    }

    companion object {
        const val SYNCHRONIZATION_NONE: String = "_SYNCHRONIZATION_NONE_"
    }
}
