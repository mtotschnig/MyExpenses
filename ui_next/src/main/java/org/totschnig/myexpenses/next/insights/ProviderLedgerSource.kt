package org.totschnig.myexpenses.next.insights

import android.database.Cursor
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import android.content.ContentResolver
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import java.text.Collator
import java.time.LocalDate
import org.totschnig.myexpenses.next.core.common.ledger.Memo
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.BankingAttribute
import org.totschnig.myexpenses.next.core.model.ledger.Catalog
import org.totschnig.myexpenses.next.core.model.ledger.CategoryInfo
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerPart
import org.totschnig.myexpenses.next.core.model.ledger.LedgerSource
import org.totschnig.myexpenses.next.core.model.ledger.LedgerTransaction
import org.totschnig.myexpenses.next.core.model.ledger.OwnAccounts
import org.totschnig.myexpenses.next.core.model.ledger.normalizeIban
import org.totschnig.myexpenses.next.core.model.ledger.ownTransferTarget
import org.totschnig.myexpenses.next.isDailyAccountType
import org.totschnig.myexpenses.next.core.common.logTimings
import org.totschnig.myexpenses.next.core.common.shareWhileShown
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.KEY_ACCOUNT_TYPE_LABEL
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_AMOUNT_HOME_EQUIVALENT
import org.totschnig.myexpenses.provider.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_COMMENT
import org.totschnig.myexpenses.provider.KEY_CONTEXT
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_IBAN
import org.totschnig.myexpenses.provider.KEY_ICON
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PATH
import org.totschnig.myexpenses.provider.KEY_PAYEEID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_STATUS
import org.totschnig.myexpenses.provider.KEY_TAGID
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.KEY_TYPE
import org.totschnig.myexpenses.provider.KEY_VALUE
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_ATTRIBUTES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_MINIMAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.CATEGORIES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.next.core.model.ledger.TagInfo

/**
 * Loads the [Ledger] once for all features of the new UI from the provider of the app, see
 * [org.totschnig.myexpenses.next.NextData], which keeps it for the process, so that it outlives the
 * activity and widgets or notifications can read it too. The view models of the features get it
 * as [LedgerSource], see the README of the ledger package. Its flows run in [scope] while they are
 * collected.
 *
 * Transactions are only read after the user agreed. The consent is stored in the data store,
 * which is part of the backup.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ProviderLedgerSource(
    private val contentResolver: ContentResolver,
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    /** See [today] */
    today: Flow<LocalDate>,
) : LedgerSource {

    override val consent: Flow<Boolean?> =
        dataStore.data.map { it[KEY_CONSENT] }.distinctUntilChanged()

    override fun setConsent(consent: Boolean) {
        scope.launch { dataStore.edit { it[KEY_CONSENT] = consent } }
    }

    /** The accounts of the user with their label; cash, bank and credit card accounts are daily ones */
    private val ownAccounts: Flow<OwnAccounts> =
        contentResolver.observeQuery(
            uri = ACCOUNTS_MINIMAL_URI,
            selection = "$KEY_PARENTID IS NULL",
            notifyForDescendants = false
        ).mapToList { Triple(it.getLong(KEY_ROWID), it.getString(KEY_LABEL), isDailyAccountType(it.getString(KEY_ACCOUNT_TYPE_LABEL))) }
            .map { accounts ->
                OwnAccounts(
                    dailyIds = accounts.filter { it.third }.mapTo(HashSet()) { it.first },
                    labels = accounts.associate { it.first to it.second }
                )
            }
            .distinctUntilChanged()

    /**
     * IBANs of own accounts, stored when they were connected with the bank (FinTS), to the id of
     * the account. A payment whose counterpart has one of them is a movement between own accounts.
     */
    private val ownIbans: Flow<Map<String, Long>> =
        contentResolver.observeQuery(
            uri = ACCOUNTS_ATTRIBUTES_URI,
            projection = arrayOf(KEY_ACCOUNTID, KEY_VALUE),
            selection = "$KEY_CONTEXT = ? AND $KEY_ATTRIBUTE_NAME = ?",
            selectionArgs = arrayOf(BankingAttribute.CONTEXT, BankingAttribute.IBAN.name),
            notifyForDescendants = true
        ).mapToList { normalizeIban(it.getString(KEY_VALUE)) to it.getLong(KEY_ACCOUNTID) }
            .map { it.toMap() }

    /** Ids of the tags of transactions by transaction */
    private val tagLinks: Flow<Map<Long, Set<Long>>> =
        contentResolver.observeQuery(
            uri = TRANSACTIONS_TAGS_URI,
            projection = arrayOf(KEY_TRANSACTIONID, KEY_TAGID),
            notifyForDescendants = true
        ).mapToList { it.getLong(KEY_TRANSACTIONID) to it.getLong(KEY_TAGID) }
            .map { links -> links.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() } }
            .logTimings("ledger: tag links query")

    private val categories: Flow<List<CategoryInfo>> =
        contentResolver.observeQuery(
            uri = CATEGORIES_URI,
            projection = arrayOf(KEY_ROWID, KEY_PARENTID, KEY_LABEL, KEY_TYPE),
            selection = "$KEY_ROWID != $SPLIT_CATID",
            notifyForDescendants = true
        ).mapToList {
            RawCategory(it.getLong(KEY_ROWID), it.getLongOrNull(KEY_PARENTID), it.getString(KEY_LABEL), it.getInt(KEY_TYPE))
        }.map(::categoryInfos)

    private val tags: Flow<List<TagInfo>> = run {
        val collator = Collator.getInstance()
        contentResolver.observeQuery(
            uri = TAGS_URI,
            projection = arrayOf(KEY_ROWID, KEY_LABEL, KEY_COLOR),
            notifyForDescendants = true
        ).mapToList { TagInfo(it.getLong(KEY_ROWID), it.getString(KEY_LABEL), it.getIntOrNull(KEY_COLOR)) }
            .map { tags -> tags.sortedWith { a, b -> collator.compare(a.label, b.label) } }
    }

    override val catalog: Flow<Catalog> =
        combine(categories, tags, ::Catalog)
            .flowOn(Dispatchers.Default)
            .shareWhileShown(scope)

    override val today: Flow<LocalDate> = today
        .distinctUntilChanged()
        .shareWhileShown(scope)

    /** The parts of split transactions by the id of the split transaction, since [historyStart] */
    private fun splitParts(historyStart: String): Flow<Map<Long, List<LedgerPart>>> =
        contentResolver.observeQuery(
            uri = TRANSACTIONS_URI,
            projection = PARTS_PROJECTION,
            selection = PARTS_SELECTION,
            selectionArgs = arrayOf(historyStart),
            notifyForDescendants = true
        ).mapToList {
            it.getLong(KEY_PARENTID) to LedgerPart(
                amount = it.getLong(KEY_AMOUNT),
                homeAmount = it.getLong(KEY_AMOUNT_HOME_EQUIVALENT),
                categoryId = it.getLongOrNull(KEY_CATID),
                transferAccountId = it.getLongOrNull(KEY_TRANSFER_ACCOUNT)
            )
        }.map { parts -> parts.groupBy({ it.first }, { it.second }) }

    /** A transaction as read, before it is known whether it is a movement between own accounts */
    private data class Row(val transaction: LedgerTransaction, val iban: String?)

    /** What the ledger is built from */
    private data class LedgerInput(
        val rows: List<Row>,
        val ibans: Map<String, Long>,
        val accounts: OwnAccounts,
        val tagLinks: Map<Long, Set<Long>>,
        val splitParts: Map<Long, List<LedgerPart>>,
    )

    private val ledgerMemo = Memo<LedgerInput, Ledger>("ledger: build") { (rows, ibans, accounts, tagLinks, splitParts) ->
        Ledger(
            rows.map { row ->
                val transaction = row.transaction
                transaction.copy(
                    targetAccountId = ownTransferTarget(
                        transaction.accountId, transaction.transferAccountId, row.iban, ibans
                    ),
                    tagIds = tagLinks[transaction.id].orEmpty(),
                    parts = splitParts[transaction.id].orEmpty()
                )
            },
            accounts
        )
    }

    /** The last ledger read, null before the first one and without consent */
    @Volatile
    private var latestLedger: Ledger? = null

    override val ledger: Flow<Ledger?> =
        combine(consent.map { it == true }.distinctUntilChanged(), today, ::Pair).flatMapLatest { (hasConsent, today) ->
            if (hasConsent) combine(
                contentResolver.observeQuery(
                    uri = TRANSACTIONS_URI,
                    projection = PROJECTION,
                    selection = SELECTION,
                    selectionArgs = arrayOf(historyStart(today)),
                    sortOrder = "$KEY_DATE ASC",
                    notifyForDescendants = true
                ).mapToList { Row(it.toLedgerTransaction(), it.getStringOrNull(KEY_IBAN)) }
                    .logTimings("ledger: transactions query"),
                ownIbans,
                ownAccounts,
                tagLinks,
                splitParts(historyStart(today)),
                ::LedgerInput
            )
                // A change, e.g. of the tags of a transaction, notifies several tables at once
                .debounce(LEDGER_DEBOUNCE_MILLIS)
                .map<LedgerInput, Ledger?> { ledgerMemo.of(it) }
                // On a restart, e.g. after the app was in the background, the last ledger stays
                // until the new one is read, which is the same unless something changed
                .onStart { if (latestLedger == null) emit(null) }
                .onEach { latestLedger = it }
            else flowOf(null).onEach { latestLedger = null }
        }
            .distinctUntilChanged()
            .logTimings("ledger")
            .flowOn(Dispatchers.Default)
            // All features read the ledger, it should be loaded only once
            .shareWhileShown(scope)

    private fun Cursor.toLedgerTransaction() = LedgerTransaction(
        id = getLong(KEY_ROWID),
        date = epoch2LocalDate(getLong(KEY_DATE)),
        amount = getLong(KEY_AMOUNT),
        homeAmount = getLong(KEY_AMOUNT_HOME_EQUIVALENT),
        accountId = getLong(KEY_ACCOUNTID),
        accountLabel = getStringOrNull(KEY_ACCOUNT_LABEL),
        currency = getStringOrNull(KEY_CURRENCY),
        originalCurrency = getStringOrNull(KEY_ORIGINAL_CURRENCY),
        payeeId = getLongOrNull(KEY_PAYEEID),
        payeeName = getStringOrNull(KEY_PAYEE_NAME),
        comment = getStringOrNull(KEY_COMMENT),
        categoryId = getLongOrNull(KEY_CATID),
        categoryPath = getStringOrNull(KEY_PATH),
        categoryIcon = getStringOrNull(KEY_ICON),
        templateId = getLongOrNull(KEY_TEMPLATEID),
        transferAccountId = getLongOrNull(KEY_TRANSFER_ACCOUNT),
        isSplit = getLongOrNull(KEY_CATID) == SPLIT_CATID,
        isSealed = getBoolean(KEY_SEALED)
    )

    companion object {
        /** Notifications closer than this lead to one ledger */
        private const val LEDGER_DEBOUNCE_MILLIS = 50L

        private fun historyStart(today: LocalDate) = today.minusMonths(Ledger.HISTORY_MONTHS).toEpoch().toString()

        /** Allows the analysis of transactions for all features, the key is kept from when only contracts used it */
        private val KEY_CONSENT = booleanPreferencesKey("next_contracts_consent")

        private val PROJECTION = arrayOf(
            KEY_ROWID,
            KEY_DATE,
            KEY_AMOUNT,
            KEY_AMOUNT_HOME_EQUIVALENT,
            KEY_ACCOUNTID,
            KEY_ACCOUNT_LABEL,
            KEY_CURRENCY,
            KEY_ORIGINAL_CURRENCY,
            KEY_PAYEEID,
            KEY_PAYEE_NAME,
            KEY_COMMENT,
            KEY_CATID,
            KEY_PATH,
            KEY_ICON,
            KEY_TEMPLATEID,
            KEY_TRANSFER_ACCOUNT,
            KEY_IBAN,
            KEY_SEALED
        )

        /**
         * Debits and credits, transfers included, since they can put money aside. Split
         * transactions are taken as a whole, because the payee is stored with the parent.
         * Archived transactions are left out (they are split parts of the archive).
         */
        private val SELECTION = "$KEY_AMOUNT != 0 AND $WHERE_NOT_SPLIT_PART" +
                " AND $KEY_STATUS NOT IN ($STATUS_UNCOMMITTED, $STATUS_ARCHIVE) AND $WHERE_NOT_VOID" +
                " AND $KEY_DATE >= ?"

        private val PARTS_PROJECTION = arrayOf(
            KEY_PARENTID,
            KEY_AMOUNT,
            KEY_AMOUNT_HOME_EQUIVALENT,
            KEY_CATID,
            KEY_TRANSFER_ACCOUNT
        )

        /**
         * Parts have the date of their split transaction. Parts of transactions that are not in the
         * ledger, e.g. of archives, are read too but not used.
         */
        private val PARTS_SELECTION = "$KEY_PARENTID IS NOT NULL AND $KEY_DATE >= ?"
    }
}
