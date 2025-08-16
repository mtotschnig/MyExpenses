package org.totschnig.myexpenses.model

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY

const val PREDEFINED_NAME_FAVORITE = "_FAVORITE_"
const val PREDEFINED_NAME_HIDDEN = "_HIDDEN_"

data class AccountFlag(
    val id: Long = 0,
    val label: String,
    val sortKey: Int,
    val icon: String,
    val isVisible: Boolean = true,
) {

    val asContentValues: ContentValues
        get() = contentValuesOf(
            KEY_LABEL to label,
            KEY_SORT_KEY to sortKey,
            KEY_ICON to icon
        )

    companion object {
        val initialFlags = listOf(
            AccountFlag(
                id = 1,
                label = PREDEFINED_NAME_FAVORITE,
                sortKey = 1,
                icon = "heart",
                isVisible = true
            ),
            AccountFlag(
                id = 2,
                label = PREDEFINED_NAME_HIDDEN,
                sortKey = -1,
                icon = "eye-slash",
                isVisible = false
            )
        )
    }
}