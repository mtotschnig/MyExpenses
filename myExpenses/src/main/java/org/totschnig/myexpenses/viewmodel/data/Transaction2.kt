package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod.Companion.translateIfPredefined
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.IS_SAME_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DISPLAY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_PARENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_PEER_PARENT
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getAmountHomeEquivalent
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.DbUtils.typeWithFallBack
import org.totschnig.myexpenses.provider.FULL_LABEL
import org.totschnig.myexpenses.provider.effectiveTypeExpression
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.splitStringList
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
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
    val icon: String? = null,
    val attachmentCount: Int = 0,
    val type: UByte = FLAG_NEUTRAL
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
        it.translateIfPredefined(context) to methodIcon
    }


    companion object {

        fun projection(
            accountId: Long,
            grouping: Grouping,
            homeCurrency: String,
            prefHandler: PrefHandler,
            extended: Boolean = true
        ) = buildList {
            addAll(projection(grouping, extended, DataBaseAccount.isHomeAggregate(accountId), homeCurrency, prefHandler))
            if (DataBaseAccount.isAggregate(accountId) && extended) {
                addAll(additionalAggregateColumns)
            }
        }.toTypedArray()

        private fun projection(grouping: Grouping, extended: Boolean, isHomeAggregate: Boolean, homeCurrency: String, prefHandler: PrefHandler): Array<String> =
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
                KEY_ICON,
                effectiveTypeExpression(typeWithFallBack(prefHandler))
            ).let {
                if (extended) it + listOf(
                    KEY_CURRENCY,
                    "$TRANSFER_PEER_PARENT AS $KEY_TRANSFER_PEER_PARENT",
                    KEY_ATTACHMENT_COUNT
                ) else it
            }.toTypedArray()

        private val additionalAggregateColumns = listOf(
            KEY_COLOR,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            "$IS_SAME_CURRENCY AS $KEY_IS_SAME_CURRENCY"
        )

        fun fromCursor(
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
                icon = cursor.getStringOrNull(KEY_ICON),
                attachmentCount = cursor.getIntIfExists(KEY_ATTACHMENT_COUNT) ?: 0,
                type = cursor.getInt(KEY_TYPE).toUByte()
            )
        }
    }
}
