package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.FULL_LABEL
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.splitStringList
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.io.File
import java.time.ZonedDateTime

@Parcelize
@Immutable
data class Transaction2(
    val id: Long,
    val _date: Long,
    val _valueDate: Long = _date,
    val amount: Money,
    val equivalentAmount: Money? = null,
    val comment: String? = null,
    val catId: Long? = null,
    val label: String? = null,
    val payee: String? = null,
    val transferPeer: Long? = null,
    val transferAccount: Long? = null,
    val accountId: Long,
    val methodId: Long? = null,
    val methodLabel: String? = null,
    val crStatus: CrStatus = CrStatus.UNRECONCILED,
    val referenceNumber: String? = null,
    val pictureUri: Uri? = null,
    val color: Int? = null,
    val transferPeerParent: Long? = null,
    val status: Int = STATUS_NONE,
    val accountLabel: String? = null,
    val accountType: AccountType? = AccountType.CASH,
    val tagList: List<String> = emptyList(),
    val year: Int,
    val month: Int,
    val week: Int,
    val day: Int,
    val icon: String? = null
) : Parcelable {

    val currency: CurrencyUnit
        get() = amount.currencyUnit

    val isSplit: Boolean
        get() = catId == SPLIT_CATID

    val isTransfer: Boolean
        get() = transferPeer != null

    val date: ZonedDateTime
        get() = epoch2ZonedDateTime(_date)

    val valueDate: ZonedDateTime
        get() = epoch2ZonedDateTime(_valueDate)

    /**
     * pair of localized label and icon
     */
    fun getMethodInfo(context: Context): Pair<String, ImageVector?>? = methodLabel?.let {
        enumValueOrNull<PreDefinedPaymentMethod>(it)?.let { predefined ->
            predefined.getLocalizedLabel(context) to predefined.icon
        } ?: (methodLabel to null)
    }


    companion object {
        fun projection(grouping: Grouping) = arrayOf(
            KEY_ROWID,
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT,
            KEY_COMMENT,
            KEY_CATID,
            FULL_LABEL,
            KEY_PAYEE_NAME,
            KEY_TRANSFER_PEER,
            KEY_TRANSFER_ACCOUNT,
            KEY_ACCOUNTID,
            KEY_METHODID,
            KEY_METHOD_LABEL,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            KEY_CURRENCY,
            KEY_PICTURE_URI,
            "$TRANSFER_PEER_PARENT AS $KEY_TRANSFER_PEER_PARENT",
            KEY_STATUS,
            KEY_TAGLIST,
            KEY_PARENTID,
            when (grouping) {
                Grouping.MONTH -> getYearOfMonthStart()
                Grouping.WEEK -> getYearOfWeekStart()
                else -> YEAR
            } + " AS $KEY_YEAR",
            "${getMonth()} AS $KEY_MONTH",
            "${getWeek()} AS $KEY_WEEK",
            "$DAY AS $KEY_DAY",
            KEY_ICON
        )

        val additionalAggregateColumns = arrayOf(
            KEY_COLOR,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            "$IS_SAME_CURRENCY AS $KEY_IS_SAME_CURRENCY"
        )

        val additionGrandTotalColumns: Array<String>
            get() = arrayOf(
                KEY_CURRENCY,
                "${getAmountHomeEquivalent(VIEW_EXTENDED)} AS $KEY_EQUIVALENT_AMOUNT"
            )

        fun fromCursor(
            context: Context,
            cursor: Cursor,
            currencyContext: CurrencyContext,
            homeCurrency: CurrencyUnit?
        ): Transaction2 {
            val currencyUnit = currencyContext.get(cursor.getString(KEY_CURRENCY))
            val amountRaw = cursor.getLong(KEY_AMOUNT)
            val money = Money(currencyUnit, amountRaw)
            val transferPeer = cursor.getLongOrNull(KEY_TRANSFER_PEER)

            return Transaction2(
                id = cursor.getLongOrNull(KEY_ROWID) ?: 0,
                amount = money,
                equivalentAmount = if (transferPeer == null) {
                    cursor.getLongIfExists(KEY_EQUIVALENT_AMOUNT)?.let {
                        homeCurrency?.let { it1 -> Money(it1, it) }
                    }
                } else null,
                _date = cursor.getLong(KEY_DATE),
                _valueDate = cursor.getLong(KEY_VALUE_DATE),
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = cursor.getLongOrNull(KEY_CATID),
                payee = cursor.getStringOrNull(KEY_PAYEE_NAME),
                methodLabel = cursor.getStringOrNull(KEY_METHOD_LABEL),
                label = cursor.getStringOrNull(KEY_LABEL),
                transferPeer = transferPeer,
                transferAccount = cursor.getLongOrNull(KEY_TRANSFER_ACCOUNT),
                accountId = cursor.getLong(KEY_ACCOUNTID),
                methodId = cursor.getLongOrNull(KEY_METHODID),
                pictureUri = cursor.getStringOrNull(KEY_PICTURE_URI)
                    ?.let { uri ->
                        var parsedUri = Uri.parse(uri)
                        if ("file" == parsedUri.scheme) { // Upgrade from legacy uris
                            parsedUri.path?.let {
                                try {
                                    parsedUri = AppDirHelper.getContentUriForFile(context, File(it))
                                } catch (ignored: IllegalArgumentException) {
                                }
                            }
                        }
                        parsedUri
                    },
                crStatus = enumValueOrDefault(
                    cursor.getString(KEY_CR_STATUS),
                    CrStatus.UNRECONCILED
                ),
                referenceNumber = cursor.getStringOrNull(KEY_REFERENCE_NUMBER),
                accountLabel = cursor.getStringIfExists(KEY_ACCOUNT_LABEL),
                accountType = enumValueOrNull<AccountType>(
                    cursor.getStringIfExists(KEY_ACCOUNT_TYPE),
                ),
                transferPeerParent = cursor.getLongOrNull(KEY_TRANSFER_PEER_PARENT),
                tagList = cursor.splitStringList(KEY_TAGLIST),
                color = cursor.getIntIfExists(KEY_COLOR),
                status = cursor.getInt(KEY_STATUS),
                year = cursor.getInt(KEY_YEAR),
                month = cursor.getInt(KEY_MONTH),
                week = cursor.getInt(KEY_WEEK),
                day = cursor.getInt(KEY_DAY),
                icon = cursor.getStringOrNull(KEY_ICON)
            )
        }
    }
}
