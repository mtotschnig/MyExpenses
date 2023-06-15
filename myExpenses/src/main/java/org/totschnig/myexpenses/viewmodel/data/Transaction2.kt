package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
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
    val parentId: Long? = null,
    val comment: String? = null,
    val catId: Long? = null,
    val label: String? = null,
    val payee: String? = null,
    val transferPeer: Long? = null,
    val transferAccount: Long? = null,
    val accountId: Long,
    val methodId: Long? = null,
    val methodLabel: String? = null,
    val methodIcon: String? = null,
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
    fun getMethodInfo(context: Context): Pair<String, String?>? = methodLabel?.let {
        (enumValueOrNull<PreDefinedPaymentMethod>(it)?.getLocalizedLabel(context)
            ?: methodLabel) to methodIcon
    }


    companion object {

        fun projection(
            accountId: Long,
            grouping: Grouping,
            homeCurrency: String,
            extended: Boolean = true
        ) = buildList {
            addAll(projection(grouping, extended, DataBaseAccount.isHomeAggregate(accountId), homeCurrency))
            if (DataBaseAccount.isAggregate(accountId) && extended) {
                addAll(additionalAggregateColumns)
            }
        }.toTypedArray()

        private fun projection(grouping: Grouping, extended: Boolean, isHomeAggregate: Boolean, homeCurrency: String): Array<String> =
            listOf(
                KEY_ROWID,
                KEY_DATE,
                KEY_VALUE_DATE,
                (if (isHomeAggregate) getAmountHomeEquivalent(
                    if (extended) VIEW_EXTENDED else VIEW_COMMITTED,
                    homeCurrency
                ) else KEY_AMOUNT)  + " AS $KEY_DISPLAY_AMOUNT",
                KEY_COMMENT,
                KEY_CATID,
                FULL_LABEL,
                KEY_PAYEE_NAME,
                KEY_TRANSFER_PEER,
                KEY_TRANSFER_ACCOUNT,
                KEY_ACCOUNTID,
                KEY_METHODID,
                KEY_METHOD_LABEL,
                KEY_METHOD_ICON,
                KEY_CR_STATUS,
                KEY_REFERENCE_NUMBER,
                KEY_PICTURE_URI,
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
            ).let {
                if (extended) it + listOf(
                    KEY_CURRENCY,
                    "$TRANSFER_PEER_PARENT AS $KEY_TRANSFER_PEER_PARENT"
                ) else it
            }.toTypedArray()

        private val additionalAggregateColumns = listOf(
            KEY_COLOR,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            "$IS_SAME_CURRENCY AS $KEY_IS_SAME_CURRENCY"
        )

        fun fromCursor(
            context: Context,
            cursor: Cursor,
            accountCurrency: CurrencyUnit
        ): Transaction2 {
            val amountRaw = cursor.getLong(KEY_DISPLAY_AMOUNT)
            val money = Money(accountCurrency, amountRaw)
            val transferPeer = cursor.getLongOrNull(KEY_TRANSFER_PEER)

            return Transaction2(
                id = cursor.getLongOrNull(KEY_ROWID) ?: 0,
                amount = money,
                parentId = cursor.getLongOrNull(KEY_PARENTID),
                _date = cursor.getLong(KEY_DATE),
                _valueDate = cursor.getLong(KEY_VALUE_DATE),
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = cursor.getLongOrNull(KEY_CATID),
                payee = cursor.getStringOrNull(KEY_PAYEE_NAME),
                methodLabel = cursor.getStringOrNull(KEY_METHOD_LABEL),
                methodIcon = cursor.getStringOrNull(KEY_METHOD_ICON),
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
                transferPeerParent = cursor.getLongIfExists(KEY_TRANSFER_PEER_PARENT),
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
