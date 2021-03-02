package org.totschnig.myexpenses.util.licence

import android.content.Context
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import java.io.Serializable
import java.util.*
import kotlin.math.ceil

sealed class Package(val defaultPrice: Long) : Serializable {
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

    object Contrib : Package(350)
    object Upgrade : Package(300)
    object Extended : Package(500)
}

@Suppress("ClassName")
sealed class ProfessionalPackage(defaultPrice: Long, val duration: Int) : Package(defaultPrice) {
    object Professional_1 : ProfessionalPackage(100, 1)
    object Professional_6 : ProfessionalPackage(500, 6)
    object Professional_12 : ProfessionalPackage(800, 12)
    object Professional_24 : ProfessionalPackage(1500, 24)
    object Amazon : ProfessionalPackage(900, 0)

    fun getDuration(withExtra: Boolean): Int {
        val base = duration
        return if (withExtra) base + DURATION_EXTRA else base
    }

    override fun getFormattedPrice(context: Context, formatted: String, withExtra: Boolean) =
            formatWithDuration(context, formatted, withExtra)

    fun getMonthlyPrice(withExtra: Boolean) =
            ceil(defaultPrice.toDouble() / getDuration(withExtra)).toLong()

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

sealed class AddOnPackage(defaultPrice: Long, val feature: ContribFeature) : Package(defaultPrice) {
    override val optionName = "AddOn"
    val sku: String
        get() = this::class.simpleName!!.toLowerCase(Locale.ROOT)

    override fun payPalButtonId(isSandBox: Boolean) = if (isSandBox) "9VF4Z9KSLHXZN" else TODO()

    object SplitTemplate : AddOnPackage(500, ContribFeature.SPLIT_TEMPLATE)
    object History : AddOnPackage(500, ContribFeature.HISTORY)
    object Budget : AddOnPackage(500, ContribFeature.BUDGET)
    object Ocr : AddOnPackage(500, ContribFeature.OCR)
    object WebUi : AddOnPackage(500, ContribFeature.WEB_UI)
}