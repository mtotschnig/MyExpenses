package org.totschnig.myexpenses.util.licence

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.formatMoney
import java.util.Locale

@Keep
sealed class Package(val defaultPrice: Long) : Parcelable {
    open val optionName = "Licence"

    open fun payPalButtonId(isSandBox: Boolean) =
        if (isSandBox) "TURRUESSCUG8N" else "LBUDF8DSWJAZ8"

    fun getFormattedPrice(
        context: Context,
        currencyFormatter: ICurrencyFormatter,
        currencyUnit: CurrencyUnit,
        withExtra: Boolean,
        usesSubscription: Boolean,
    ): String {
        val formatted = getFormattedPriceRaw(currencyUnit, currencyFormatter)
        return getFormattedPrice(context, formatted, withExtra, usesSubscription)
    }

    open fun getFormattedPrice(
        context: Context,
        formatted: String,
        withExtra: Boolean,
        usesSubscription: Boolean
    ) = formatted

    fun getFormattedPriceRaw(currencyUnit: CurrencyUnit, currencyFormatter: ICurrencyFormatter) =
        currencyFormatter.formatMoney(Money(currencyUnit, defaultPrice))

    @Parcelize
    @Keep
    data object Contrib : Package(1440)

    @Parcelize
    @Keep
    data object Upgrade : Package(660)

    @Parcelize
    @Keep
    data object Extended : Package(1890)
}

@Suppress("ClassName")
@Keep
sealed class ProfessionalPackage(defaultPrice: Long, val duration: Int) : Package(defaultPrice) {
    @Parcelize
    @Keep
    data object Professional_1 : ProfessionalPackage(100, 1)

    @Parcelize
    @Keep
    data object Professional_6 : ProfessionalPackage(549, 6)

    @Parcelize
    @Keep
    data object Professional_12 : ProfessionalPackage(985, 12)

    @Parcelize
    @Keep
    data object Professional_24 : ProfessionalPackage(1819, 24)

    @Parcelize
    @Keep
    data object Amazon : ProfessionalPackage(900, 0)

    fun getDuration(withExtra: Boolean): Int {
        val base = duration
        return if (withExtra) base + DURATION_EXTRA else base
    }

    override fun getFormattedPrice(
        context: Context,
        formatted: String,
        withExtra: Boolean,
        usesSubscription: Boolean
    ) =
        formatWithDuration(context, formatted, withExtra, usesSubscription)

/*    fun getMonthlyPrice(withExtra: Boolean) =
            ceil(defaultPrice.toDouble() / getDuration(withExtra)).toLong()*/

    private fun formatWithDuration(
        context: Context,
        formattedPrice: String?,
        withExtra: Boolean,
        usesSubscription: Boolean
    ): String {
        val duration = getDuration(withExtra)
        val formattedDuration: String
        var format = "%s (%s)"
        if (usesSubscription && duration == 1) formattedDuration = context.getString(R.string.monthly_plain)
        else if (usesSubscription && duration == 12) formattedDuration = context.getString(R.string.yearly_plain)
        else {
            format = "%s / %s"
            formattedDuration = context.getString(R.string.n_months, duration)
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
sealed class AddOnPackage(
    val feature: ContribFeature,
    private val isContribFeature: Boolean = feature.licenceStatus == LicenceStatus.CONTRIB
) : Package(490) {

    companion object {
        //We cannot use an initializer here, because the objects we want to list might not be constructed
        //thus giving us a list of nulls:
        //https://youtrack.jetbrains.com/issue/KT-8970/Object-is-uninitialized-null-when-accessed-from-static-context-ex.-companion-object-with-initialization-loop
        val values: List<AddOnPackage>
            get() = listOf(
                SplitTemplate, History, Budget, Ocr, WebUi, CategoryTree,
                AccountsUnlimited, PlansUnlimited, SplitTransaction, Distribution, Print, AdFree,
                CsvImport, Synchronization, Banking, AutomaticFxDownload
            )
    }

    override val optionName = "AddOn"
    @Suppress("unused")
    val sku: String
        get() = this::class.simpleName!!.lowercase(Locale.ROOT)

    override fun payPalButtonId(isSandBox: Boolean) =
        when {
            isSandBox -> if (isContribFeature) "UAWN7XUQNZ5PS" else "9VF4Z9KSLHXZN"
            isContribFeature -> "48RQY4SKUHTAQ"
            else -> "FNEEWJWU5YJ44"
        }

    @Parcelize
    @Keep
    data object SplitTemplate : AddOnPackage(ContribFeature.SPLIT_TEMPLATE)

    @Parcelize
    @Keep
    data object History : AddOnPackage(ContribFeature.HISTORY)

    @Parcelize
    @Keep
    data object Budget : AddOnPackage(ContribFeature.BUDGET)

    @Parcelize
    @Keep
    data object Ocr : AddOnPackage(ContribFeature.OCR)

    @Parcelize
    @Keep
    data object WebUi : AddOnPackage(ContribFeature.WEB_UI)

    @Parcelize
    @Keep
    data object CategoryTree : AddOnPackage(ContribFeature.CATEGORY_TREE)

    @Parcelize
    @Keep
    data object AccountsUnlimited : AddOnPackage(ContribFeature.ACCOUNTS_UNLIMITED)

    @Parcelize
    @Keep
    data object PlansUnlimited : AddOnPackage(ContribFeature.PLANS_UNLIMITED)

    @Parcelize
    @Keep
    data object SplitTransaction : AddOnPackage(ContribFeature.SPLIT_TRANSACTION)

    @Parcelize
    @Keep
    data object Distribution : AddOnPackage(ContribFeature.DISTRIBUTION)

    @Parcelize
    @Keep
    data object Print : AddOnPackage(ContribFeature.PRINT)

    @Parcelize
    @Keep
    data object AdFree : AddOnPackage(ContribFeature.AD_FREE)

    @Parcelize
    @Keep
    data object CsvImport : AddOnPackage(ContribFeature.CSV_IMPORT)

    @Parcelize
    @Keep
    data object Synchronization : AddOnPackage(ContribFeature.SYNCHRONIZATION)

    @Parcelize
    @Keep
    data object Banking : AddOnPackage(ContribFeature.BANKING)

    @Parcelize
    @Keep
    data object AutomaticFxDownload: AddOnPackage(ContribFeature.AUTOMATIC_FX_DOWNLOAD)
}