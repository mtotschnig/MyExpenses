package org.totschnig.myexpenses.io

import android.content.Context
import org.apache.commons.csv.CSVRecord
import org.apache.commons.text.StringTokenizer
import org.apache.commons.text.matcher.StringMatcherFactory
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.export.qif.QifUtils
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.SplitTransaction
import java.math.BigDecimal

class CSVParser(
    private val context: Context,
    private val data: List<CSVRecord>,
    private val columnToFieldMap: IntArray,
    private val dateFormat: QifDateFormat,
    private val currency: CurrencyUnit,
    private val type: AccountType
) {

    private val accountBuilders: MutableSet<ImportAccount.Builder> = mutableSetOf()

    val accounts: List<ImportAccount> by lazy {
        accountBuilders.map { it.build() }
    }

    val categories: MutableSet<CategoryInfo> = mutableSetOf()
    val payees: MutableSet<String> = mutableSetOf()
    val tags: MutableSet<String> = mutableSetOf()

    private fun requireAccount(label: String) =
        accountBuilders.find { it.memo == label } ?: ImportAccount.Builder().memo(label).type(type)
            .also {
                accountBuilders.add(it)
            }

    private fun saveGetFromRecord(record: CSVRecord, index: Int) =
        if (record.size() > index) record[index].trim() else ""

    fun parse() {
        val columnIndexAccount = columnToFieldMap.indexOf(R.string.account)
        val columnIndexAmount = columnToFieldMap.indexOf(R.string.amount)
        val columnIndexExpense = columnToFieldMap.indexOf(R.string.expense)
        val columnIndexIncome = columnToFieldMap.indexOf(R.string.income)
        val columnIndexDate = columnToFieldMap.indexOf(R.string.date).takeIf { it > -1 }
            ?: columnToFieldMap.indexOf(R.string.booking_date)
        val columnIndexValueDate = columnToFieldMap.indexOf(R.string.value_date)
        val columnIndexPayee = columnToFieldMap.indexOf(R.string.payer_or_payee)
        val columnIndexNotes = columnToFieldMap.indexOf(R.string.notes)
        val columnIndexCategory = columnToFieldMap.indexOf(R.string.category)
        val columnIndexSubcategory = columnToFieldMap.indexOf(R.string.subcategory)
        val columnIndexMethod = columnToFieldMap.indexOf(R.string.method)
        val columnIndexStatus = columnToFieldMap.indexOf(R.string.status)
        val columnIndexNumber = columnToFieldMap.indexOf(R.string.reference_number)
        val columnIndexSplit = columnToFieldMap.indexOf(R.string.split_transaction)
        val defaultAccount = ImportAccount.Builder()

        var isSplitParent = false
        var isSplitPart = false
        var splitParent: ImportTransaction.Builder? = null
        for (record in data) {
            val transaction = ImportTransaction.Builder()
            if (columnIndexSplit != -1) {
                val split = saveGetFromRecord(record, columnIndexSplit)
                isSplitPart = split == SplitTransaction.CSV_PART_INDICATOR
                isSplitParent = split == SplitTransaction.CSV_INDICATOR
            }

            transaction.amount(
                try {
                    if (columnIndexAmount != -1) {
                        QifUtils.parseMoney(saveGetFromRecord(record, columnIndexAmount), currency)
                    } else {
                        val income = if (columnIndexIncome != -1) QifUtils.parseMoney(
                            saveGetFromRecord(
                                record,
                                columnIndexIncome
                            ), currency
                        ).abs() else BigDecimal(0)
                        val expense = if (columnIndexExpense != -1) QifUtils.parseMoney(
                            saveGetFromRecord(
                                record,
                                columnIndexExpense
                            ), currency
                        ).abs() else BigDecimal(0)
                        income.subtract(expense)
                    }
                } catch (e: IllegalArgumentException) {
                    BigDecimal.ZERO
                }
            )

            if (!isSplitParent && columnIndexCategory != -1) {
                val category: String = saveGetFromRecord(record, columnIndexCategory)
                if (category != "") {
                    val subCategory = if (columnIndexSubcategory != -1) saveGetFromRecord(
                        record,
                        columnIndexSubcategory
                    ) else ""
                    if (category == context.getString(R.string.transfer) &&
                        subCategory != "" &&
                        QifUtils.isTransferCategory(subCategory)
                    ) {
                        transaction.toAccount(subCategory.substring(1, subCategory.length - 1))
                        transaction.category(R.string.transfer)
                    } else if (QifUtils.isTransferCategory(category)) {
                        transaction.toAccount(category.substring(1, category.length - 1))
                        transaction.category(R.string.transfer)
                    } else {
                        val category1 = category +
                                (subCategory.takeIf { it.isNotEmpty() }?.let { ":$it" } ?: "")
                        categories.add(CategoryInfo(category1))
                        transaction.category(category1)
                    }
                }
            }
            if (columnIndexDate != -1) {
                transaction.date(
                    QifUtils.parseDate(
                        saveGetFromRecord(record, columnIndexDate),
                        dateFormat
                    )
                )
            }
            if (columnIndexValueDate != -1) {
                transaction.valueDate(
                    QifUtils.parseDate(
                        saveGetFromRecord(
                            record,
                            columnIndexValueDate
                        ), dateFormat
                    )
                )
            }
            if (columnIndexPayee != -1) {
                saveGetFromRecord(record, columnIndexPayee).takeIf { it.isNotEmpty() }?.let {
                    payees.add(it)
                    transaction.payee(it)
                }
            }
            if (columnIndexNotes != -1) {
                transaction.memo(saveGetFromRecord(record, columnIndexNotes))
            }
            if (columnIndexMethod != -1) {
                transaction.method(saveGetFromRecord(record, columnIndexMethod))
            }
            if (columnIndexStatus != -1) {
                transaction.status(saveGetFromRecord(record, columnIndexStatus))
            }
            if (columnIndexNumber != -1) {
                transaction.number(saveGetFromRecord(record, columnIndexNumber))
            }
            columnToFieldMap.indexOf(R.string.tags).takeIf { it != -1 }?.let { it ->
                saveGetFromRecord(record, it).takeIf { it.isNotEmpty() }?.let { tagList ->
                    val tokenizer = StringTokenizer(tagList)
                    tokenizer.quoteMatcher = StringMatcherFactory.INSTANCE.quoteMatcher()
                    tokenizer.delimiterMatcher = StringMatcherFactory.INSTANCE.commaMatcher()
                    with(tokenizer.tokenList.filterNotNull()) {
                        transaction.addTags(this)
                        tags.addAll(this)
                    }
                }
            }
            if (isSplitParent) {
                splitParent = transaction
            }
            if (isSplitPart) {
                splitParent?.addSplit(transaction)
            } else {
                (if (columnIndexAccount != -1) {
                    requireAccount(saveGetFromRecord(record, columnIndexAccount))
                } else defaultAccount)
                    .addTransaction(transaction)
            }
        }
        if (columnIndexAccount == -1) {
            accountBuilders.add(defaultAccount)
        }
    }
}