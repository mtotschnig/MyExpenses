package org.totschnig.myexpenses.model

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.TextUtils.joinEnum
import java.util.Locale

enum class CrStatus(@ColorRes val color: Int, val symbol: Char?) {
    UNRECONCILED(R.color.UNRECONCILED, null),
    CLEARED(R.color.CLEARED, '*'),
    RECONCILED(R.color.RECONCILED, 'X'),
    VOID(R.color.VOID, 'V');

    @StringRes
    fun toStringRes() = when (this) {
        CLEARED -> R.string.status_cleared
        RECONCILED -> R.string.status_reconciled
        UNRECONCILED -> R.string.status_unreconciled
        VOID -> R.string.status_void
    }

    fun toColorRoles(context: Context) = MaterialColors.getColorRoles(
        context,
        ContextCompat.getColor(context, color)
    )

    companion object {
        @JvmField
        val JOIN: String = joinEnum(CrStatus::class.java)

        @JvmStatic
        fun fromQifName(qifName: String?) =
            when(qifName?.uppercase(Locale.getDefault())) {
                "*", "C" -> CLEARED
                "X", "R" -> RECONCILED
                "V" -> VOID
                else -> UNRECONCILED
            }

        val editableStatuses = arrayOf(UNRECONCILED, CLEARED, VOID)
    }
}