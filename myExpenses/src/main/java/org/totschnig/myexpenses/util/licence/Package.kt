package org.totschnig.myexpenses.util.licence

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import java.util.*

@Keep
sealed class Package(val defaultPrice: Long) : Parcelable {
    open val optionName = "Licence"

    open fun payPalButtonId(isSandBox: Boolean) = if (isSandBox) "TURRUESSCUG8N" else "LBUDF8DSWJAZ8"

    fun getFormattedPrice(context: Context, currencyUnit: CurrencyUnit?, withExtra: Boolean): String {
        val formatted = getFormattedPriceRaw(currencyUnit, context)
        return getFormattedPrice(context, formatted, withExtra)
    }

    open fun getFormattedPrice(context: Context, formatted: String, withExtra: Boolean) = formatted

    fun getFormattedPriceRaw(currencyUnit: CurrencyUnit?, context: Context): String {
        return (context.applicationContext as MyApplication).appComponent.currencyFormatter()
                .formatCurrency(Money(currencyUnit!!, defaultPrice))
    }

    @Parcelize @Keep object Contrib : Package(430)
    @Parcelize@Keep object Upgrade : Package(300)
    @Parcelize@Keep object Extended : Package(670)
}

@Suppress("ClassName")
@Keep
sealed class ProfessionalPackage(defaultPrice: Long, val duration: Int) : Package(defaultPrice) {
    @Parcelize @Keep object Professional_1 : ProfessionalPackage(100, 1)
    @Parcelize @Keep object Professional_6 : ProfessionalPackage(500, 6)
    @Parcelize @Keep object Professional_12 : ProfessionalPackage(800, 12)
    @Parcelize @Keep object Professional_24 : ProfessionalPackage(1500, 24)
    @Parcelize @Keep object Amazon : ProfessionalPackage(900, 0)

    fun getDuration(withExtra: Boolean): Int {
        val base = duration
        return if (withExtra) base + DURATION_EXTRA else base
    }

    override fun getFormattedPrice(context: Context, formatted: String, withExtra: Boolean) =
            formatWithDuration(context, formatted, withExtra)

/*    fun getMonthlyPrice(withExtra: Boolean) =
            ceil(defaultPrice.toDouble() / getDuration(withExtra)).toLong()*/

    private fun formatWithDuration(context: Context, formattedPrice: String?, withExtra: Boolean): String {
        val duration = getDuration(withExtra)
        val formattedDuration: String
        var format = "%s (%s)"
        when (duration) {
            1 -> formattedDuration = context.getString(R.string.monthly_plain)
            12 -> formattedDuration = context.getString(R.string.yearly_plain)
            else -> {
                format = "%s / %s"
                formattedDuration = context.getString(R.string.n_months, duration)
            }
        }
        return String.format(format, formattedPrice, formattedDuration)
    }

    companion object {
        /**
         * Extra months credited for professional licence to holders of extended licence
         */
        private const val DURATION_EXTRA = 3
    }
}

@Keep
sealed class AddOnPackage(defaultPrice: Long, val feature: ContribFeature) : Package(defaultPrice) {
    override val optionName = "AddOn"
    val sku: String
        get() = this::class.simpleName!!.toLowerCase(Locale.ROOT)

    override fun payPalButtonId(isSandBox: Boolean) = if (isSandBox) "9VF4Z9KSLHXZN" else "FNEEWJWU5YJ44"

    @Parcelize @Keep object SplitTemplate : AddOnPackage(430, ContribFeature.SPLIT_TEMPLATE)
    @Parcelize @Keep object History : AddOnPackage(430, ContribFeature.HISTORY)
    @Parcelize @Keep object Budget : AddOnPackage(430, ContribFeature.BUDGET)
    @Parcelize @Keep object Ocr : AddOnPackage(430, ContribFeature.OCR)
    @Parcelize @Keep object WebUi : AddOnPackage(430, ContribFeature.WEB_UI)
}