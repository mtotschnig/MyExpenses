package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getLong

/**
 * maps header to an array that holds an array of following sums:
 * [0] incomeSum
 * [1] expenseSum
 * [2] transferSum
 * [3] previousBalance
 * [4] delta (incomSum - expenseSum + transferSum)
 * [5] interimBalance
 * [6] mappedCategories
 *
 * long previousBalance = mAccount.openingBalance.getAmountMinor();
do {
long sumIncome = c.getLong(columnIndexGroupSumIncome);
long sumExpense = c.getLong(columnIndexGroupSumExpense);
long sumTransfer = c.getLong(columnIndexGroupSumTransfer);
long delta = sumIncome + sumExpense + sumTransfer;
long interimBalance = previousBalance + delta;
long mappedCategories = c.getLong(columnIndexGroupMappedCategories);
headerData.put(calculateHeaderId(c.getInt(columnIndexGroupYear), c.getInt(columnIndexGroupSecond)),
new Long[]{sumIncome, sumExpense, sumTransfer, previousBalance, delta, interimBalance, mappedCategories});
previousBalance = interimBalance;
} while (c.moveToNext());
 */

data class HeaderData(
    val incomeSum: Long,
    val expenseSum: Long,
    val transferSum: Long,
    val previousBalance: Long,
    val interimBalance: Long,
    val mappedCategories: Boolean
) {
    val delta: Long = incomeSum + expenseSum + transferSum

    companion object {
        fun fromCursor(cursor: Cursor) = HeaderData(
            incomeSum = cursor.getLong(DatabaseConstants.KEY_SUM_INCOME),
            expenseSum = cursor.getLong(DatabaseConstants.KEY_SUM_EXPENSES),
            transferSum = cursor.getLong(DatabaseConstants.KEY_SUM_TRANSFERS),
            previousBalance = 0, //TODO
            interimBalance = 0, //TODO
            mappedCategories = cursor.getLong(DatabaseConstants.KEY_MAPPED_CATEGORIES) > 0
        )
    }
}