package org.totschnig.myexpenses.util.licence

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature

enum class LicenceStatus(val resId: Int, val isUpgradeable: Boolean = true) {
    CONTRIB(R.string.contrib_key),
    EXTENDED(R.string.extended_key),
    EXTENDED_FALLBACK(R.string.extended_key),
    PROFESSIONAL(R.string.professional_key, false);

    val color: Int
        @ColorRes get() = when (this) {
            CONTRIB -> R.color.premium_licence
            EXTENDED, EXTENDED_FALLBACK -> R.color.extended_licence
            PROFESSIONAL -> R.color.professional_licence
        }

    fun greaterOrEqual(other: LicenceStatus?): Boolean {
        return other == null || this >= other
    }

    fun covers(contribFeature: ContribFeature?): Boolean {
        return if (contribFeature == null) true else this >= contribFeature.licenceStatus
    }

    /**
     * for historical reasons, skus for Contrib used "premium"
     */
    fun toSkuType(): String {
        return if (this === CONTRIB) {
            "premium"
        } else name.lowercase()
    }
}