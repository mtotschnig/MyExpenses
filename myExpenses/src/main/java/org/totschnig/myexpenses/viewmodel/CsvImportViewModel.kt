package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.export.qif.QifUtils
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Payee
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.PaymentMethod.PreDefined
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable
import java.io.InputStreamReader
import java.math.BigDecimal
import java.util.*

class CsvImportViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun parseFile(uri: Uri, delimiter: Char, encoding: String): LiveData<Result<List<CSVRecord>>> =
            liveData(context = coroutineContext()) {
        try {
            contentResolver.openInputStream(uri)?.use {
                emit(Result.success(CSVFormat.DEFAULT.withDelimiter(delimiter).parse(InputStreamReader(it, encoding)).records))
            } ?: throw java.lang.Exception("OpenInputStream returned null")
        } catch (e: Exception) {
            emit(Result.failure<List<CSVRecord>>(e))
        }
    }

    fun importData(data: ArrayList<CSVRecord>, columnToFieldMap: IntArray, discardedRows: SparseBooleanArrayParcelable, dateFormat: QifDateFormat, accountCreator: () -> Account): LiveData<Result<Triple<Pair<Int, String>, Int, Int>>> = liveData(context = coroutineContext()) {
        var totalImported = 0
        var totalDiscarded = 0
        var totalFailed = 0
        val payeeToId: MutableMap<String, Long> = HashMap()
        val categoryToId: MutableMap<String, Long> = HashMap()
        val account: Account = accountCreator()
        val columnIndexAmount: Int = columnToFieldMap.indexOf(R.string.amount)
        val columnIndexExpense: Int = columnToFieldMap.indexOf(R.string.expense)
        val columnIndexIncome: Int = columnToFieldMap.indexOf(R.string.income)
        val columnIndexDate: Int = columnToFieldMap.indexOf(R.string.date)
        val columnIndexPayee: Int = columnToFieldMap.indexOf(R.string.payer_or_payee)
        val columnIndexNotes: Int = columnToFieldMap.indexOf(R.string.comment)
        val columnIndexCategory: Int = columnToFieldMap.indexOf(R.string.category)
        val columnIndexSubcategory: Int = columnToFieldMap.indexOf(R.string.subcategory)
        val columnIndexMethod: Int = columnToFieldMap.indexOf(R.string.method)
        val columnIndexStatus: Int = columnToFieldMap.indexOf(R.string.status)
        val columnIndexNumber: Int = columnToFieldMap.indexOf(R.string.reference_number)
        val columnIndexSplit: Int = columnToFieldMap.indexOf(R.string.split_transaction)

        var isSplitParent = false
        var isSplitPart = false
        var t: Transaction
        var splitParent: Long? = null
        contentResolver.call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_BULK_START, null, null)
        for (i in data.indices) {
            var transferAccountId: Long = -1
            if (discardedRows.get(i, false)) {
                totalDiscarded++
            } else {
                val record: CSVRecord = data[i]
                var categoryInfo: String? = null
                if (columnIndexSplit != -1) {
                    isSplitPart = saveGetFromRecord(record, columnIndexSplit) == SplitTransaction.CSV_PART_INDICATOR
                    isSplitParent = saveGetFromRecord(record, columnIndexSplit) == SplitTransaction.CSV_INDICATOR
                }
                val amount = try {
                    if (columnIndexAmount != -1) {
                        QifUtils.parseMoney(saveGetFromRecord(record, columnIndexAmount), account.currencyUnit)
                    } else {
                        val income = if (columnIndexIncome != -1) QifUtils.parseMoney(saveGetFromRecord(record, columnIndexIncome), account.currencyUnit).abs() else BigDecimal(0)
                        val expense = if (columnIndexExpense != -1) QifUtils.parseMoney(saveGetFromRecord(record, columnIndexExpense), account.currencyUnit).abs() else BigDecimal(0)
                        income.subtract(expense)
                    }
                } catch (e: IllegalArgumentException) {
                    emit(Result.failure<Triple<Pair<Int, String>, Int, Int>>(Exception("Amounts in data exceed storage limit")))
                    return@liveData
                }
                val m = Money(account.currencyUnit, amount)
                if (!isSplitParent && columnIndexCategory != -1) {
                    val category: String = saveGetFromRecord(record, columnIndexCategory)
                    if (category != "") {
                        val subCategory = if (columnIndexSubcategory != -1) saveGetFromRecord(record, columnIndexSubcategory) else ""
                        if (category == localizedContext.getString(R.string.transfer) &&
                                subCategory != "" &&
                                QifUtils.isTransferCategory(subCategory)) {
                            transferAccountId = Account.findAnyOpen(subCategory.substring(1, subCategory.length - 1))
                        } else if (QifUtils.isTransferCategory(category)) {
                            transferAccountId = Account.findAnyOpen(category.substring(1, category.length - 1))
                        }
                        if (transferAccountId == -1L) {
                            categoryInfo = category
                            if (subCategory != "") {
                                categoryInfo += ":$subCategory"
                            }
                        }
                    }
                }
                if (isSplitPart) {
                    if (transferAccountId != -1L) {
                        t = Transfer.getNewInstance(account.id, transferAccountId, splitParent)
                        t.setAmount(m)
                    } else {
                        t = Transaction.getNewInstance(account.id, splitParent)
                        t.amount = m
                    }
                } else {
                    t = if (isSplitParent) {
                        SplitTransaction(account.id, m)
                    } else {
                        if (transferAccountId != -1L) {
                            Transfer(account.id, m, transferAccountId)
                        } else {
                            Transaction(account.id, m)
                        }
                    }
                }
                if (!TextUtils.isEmpty(categoryInfo)) {
                    CategoryInfo(categoryInfo).insert(categoryToId, false)
                    t.catId = categoryToId[categoryInfo]
                }
                if (columnIndexDate != -1) {
                    t.setDate(QifUtils.parseDate(saveGetFromRecord(record, columnIndexDate), dateFormat))
                }
                if (columnIndexPayee != -1) {
                    val payee: String = saveGetFromRecord(record, columnIndexPayee)
                    if (payee != "") {
                        val id = Payee.extractPayeeId(payee, payeeToId)
                        if (id != -1L) {
                            payeeToId[payee] = id
                            t.payeeId = id
                        }
                    }
                }
                if (columnIndexNotes != -1) {
                    t.comment = saveGetFromRecord(record, columnIndexNotes)
                }
                if (columnIndexMethod != -1) {
                    var method: String = saveGetFromRecord(record, columnIndexMethod)
                    if (method != "") {
                        for (preDefined in PreDefined.values()) {
                            if (preDefined.localizedLabel == method) {
                                method = preDefined.name
                                break
                            }
                        }
                        val methodId = PaymentMethod.find(method)
                        if (methodId != -1L) {
                            t.methodId = methodId
                        }
                    }
                }
                if (columnIndexStatus != -1) {
                    t.crStatus = CrStatus.fromQifName(saveGetFromRecord(record, columnIndexStatus))
                }
                if (columnIndexNumber != -1) {
                    t.referenceNumber = saveGetFromRecord(record, columnIndexNumber)
                }
                if (t.save() != null) {
                    if (isSplitParent) {
                        splitParent = t.id
                    }
                    if (!isSplitPart) {
                        totalImported++
                    }
                } else {
                    totalFailed++
                }
               /* if (totalImported % 10 == 0) {
                    publishProgress(totalImported)
                }*/
            }
        }
        contentResolver.call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_BULK_END, null, null)
        emit(Result.success(Triple(Pair(totalImported, account.label), totalFailed, totalDiscarded)))
    }

    private fun saveGetFromRecord(record: CSVRecord, index: Int): String {
        return if (record.size() > index) record[index].trim() else ""
    }
}