package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_IS_ARCHIVED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_IS_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.DatabaseConstants.YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart
import org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart
import org.totschnig.myexpenses.provider.DbUtils.typeWithFallBack
import org.totschnig.myexpenses.provider.TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.effectiveTypeExpression
import org.totschnig.myexpenses.provider.getBooleanIfExists
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.splitStringList
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.time.ZonedDateTime
import kotlin.math.max

@Parcelize
@Immutable
data class Transaction2(
    val id: Long,
    val _date: Long,
    val _valueDate: Long = _date,
    val currency: String? = null,
    val amount: Money? = null,
    val displayAmount: Money,
    val originalAmount: Money? = null,
    val parentId: Long? = null,
    val comment: String? = null,
    val catId: Long? = null,
    val categoryPath: String? = null,
    val party: DisplayParty? = null,
    val transferPeer: Long? = null,
    val transferAccount: Long? = null,
    val transferAccountLabel: String? = null,
    val accountId: Long,
    val methodId: Long? = null,
    val methodLabel: String? = null,
    val methodIcon: String? = null,
    val crStatus: CrStatus = CrStatus.UNRECONCILED,
    val referenceNumber: String? = null,
    val color: Int? = null,
    val transferPeerIsPart: Boolean? = null,
    val transferPeerIsArchived: Boolean? = null,
    val status: Int = STATUS_NONE,
    val accountLabel: String? = null,
    val accountType: Long?,
    val tagList: List<Triple<Long, String, Int?>> = emptyList(),
    val year: Int,
    val month: Int,
    val week: Int,
    val day: Int,
    val icon: String? = null,
    val attachmentCount: Int = 0,
    val type: Byte = FLAG_NEUTRAL,
    val isSameCurrency: Boolean = true
) : Parcelable {

    val isSplit: Boolean
        get() = catId == SPLIT_CATID

    val isTransfer: Boolean
        get() = transferPeer != null

    val date: ZonedDateTime
        get() = epoch2ZonedDateTime(_date)

    val valueDate: ZonedDateTime
        get() = epoch2ZonedDateTime(_valueDate)

    val isArchive: Boolean
        get() = status == STATUS_ARCHIVE

    val isSplitPart: Boolean
        get() = parentId != null

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
            prefHandler: PrefHandler,
            extended: Boolean = true
        ) = buildList {
            addAll(
                projection(
                    grouping,
                    extended,
                    prefHandler
                )
            )
            add(KEY_ORIGINAL_CURRENCY)
            add(KEY_ORIGINAL_AMOUNT)
            if (DataBaseAccount.isAggregate(accountId) && extended) {
                addAll(additionalAggregateColumns)
            }
        }.toTypedArray()

        private fun projection(
            grouping: Grouping,
            extended: Boolean,
            prefHandler: PrefHandler
        ): Array<String> =
            listOf(
                KEY_ROWID,
                KEY_DATE,
                KEY_VALUE_DATE,
                KEY_CURRENCY,
                KEY_AMOUNT,
                KEY_DISPLAY_AMOUNT,
                KEY_COMMENT,
                KEY_CATID,
                KEY_PATH,
                TRANSFER_ACCOUNT_LABEL,
                KEY_PAYEEID,
                KEY_PAYEE_NAME,
                KEY_SHORT_NAME,
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
                "${effectiveTypeExpression(typeWithFallBack(prefHandler))} AS $KEY_TYPE"
            ).let {
                if (extended) it + listOf(
                    KEY_TRANSFER_PEER_IS_PART,
                    KEY_TRANSFER_PEER_IS_ARCHIVED,
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
            currencyContext: CurrencyContext,
            cursor: Cursor,
            tags: Map<String, Pair<String, Int?>>,
            accountCurrency: CurrencyUnit? = null
        ): Transaction2 {
            val currency = cursor.getString(KEY_CURRENCY)
            val amountRaw = cursor.getLong(KEY_DISPLAY_AMOUNT)
            val typeRaw = amountRaw > 0
            val currencyUnit = currencyContext[currency]
            val displayAmount = Money(accountCurrency ?: currencyUnit, amountRaw)
            val transferPeer = cursor.getLongOrNull(KEY_TRANSFER_PEER)

            return Transaction2(
                id = cursor.getLongOrNull(KEY_ROWID) ?: 0,
                currency = currency,
                amount = if (accountCurrency != null && currencyUnit.code != accountCurrency.code) Money(currencyUnit, cursor.getLong(KEY_AMOUNT)) else null,
                displayAmount = displayAmount,
                parentId = cursor.getLongOrNull(KEY_PARENTID),
                _date = cursor.getLong(KEY_DATE),
                _valueDate = cursor.getLong(KEY_VALUE_DATE),
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = cursor.getLongOrNull(KEY_CATID),
                party = DisplayParty.fromCursor(cursor),
                methodLabel = cursor.getStringOrNull(KEY_METHOD_LABEL),
                methodIcon = cursor.getStringIfExists(KEY_METHOD_ICON),
                categoryPath = cursor.getStringOrNull(KEY_PATH, allowEmpty = true),
                transferPeer = transferPeer,
                transferAccount = cursor.getLongOrNull(KEY_TRANSFER_ACCOUNT),
                transferAccountLabel = cursor.getStringOrNull(KEY_TRANSFER_ACCOUNT_LABEL),
                accountId = cursor.getLong(KEY_ACCOUNTID),
                methodId = cursor.getLongOrNull(KEY_METHODID),
                crStatus = enumValueOrDefault(
                    cursor.getString(KEY_CR_STATUS),
                    CrStatus.UNRECONCILED
                ),
                referenceNumber = cursor.getStringOrNull(KEY_REFERENCE_NUMBER),
                accountLabel = cursor.getStringIfExists(KEY_ACCOUNT_LABEL),
                accountType = cursor.getLongIfExists(KEY_ACCOUNT_TYPE),
                transferPeerIsPart = cursor.getBooleanIfExists(KEY_TRANSFER_PEER_IS_PART),
                transferPeerIsArchived = cursor.getBooleanIfExists(KEY_TRANSFER_PEER_IS_ARCHIVED),
                tagList = cursor.splitStringList(KEY_TAGLIST).mapNotNull { id ->
                    tags[id]?.let { Triple(id.toLong(), it.first, it.second) }
                },
                color = cursor.getIntIfExists(KEY_COLOR),
                status = cursor.getInt(KEY_STATUS),
                year = cursor.getInt(KEY_YEAR),
                month = cursor.getInt(KEY_MONTH),
                week = cursor.getInt(KEY_WEEK),
                day = cursor.getInt(KEY_DAY),
                icon = cursor.getStringIfExists(KEY_ICON),
                attachmentCount = cursor.getIntIfExists(KEY_ATTACHMENT_COUNT) ?: 0,
                type = cursor.getIntIfExists(KEY_TYPE)?.toByte()
                    ?: if (typeRaw) FLAG_INCOME else FLAG_EXPENSE,
                isSameCurrency = cursor.getBooleanIfExists(KEY_IS_SAME_CURRENCY) != false,
                originalAmount = cursor.getStringIfExists(KEY_ORIGINAL_CURRENCY)?.let { currency ->
                    //for historical reasons, original amount is stored unsigned in DB, we convert it
                    //with the sign stored for amount
                    val originalAmountRaw = cursor.getLong(KEY_ORIGINAL_AMOUNT).let {
                        if (typeRaw) it else -it
                    }
                    Money(currencyContext[currency], originalAmountRaw)
                }
            )
        }
    }
}

fun Iterable<Transaction2>.mergeTransfers(account: DataBaseAccount, homeCurrency: String): List<Transaction2> {
    require(account.isAggregate)
    return groupBy { max(it.id, it.transferPeer ?: 0) }
        .map { (_, list) ->
            if (list.size == 1) list.first() else
                (if (!account.isHomeAggregate) list.first() else
                    list.firstOrNull { it.currency == homeCurrency }
                        ?: list.first()).copy(type = FLAG_NEUTRAL)
        }
}

