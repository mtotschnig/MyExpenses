package org.totschnig.myexpenses.provider

import org.totschnig.myexpenses.provider.DatabaseConstants.*

fun checkSealedWithAlias(baseTable: String, innerTable: String) =
    "max(" + checkForSealedAccount(
        baseTable,
        innerTable
    ) + ", " + checkForSealedDebt(baseTable) + ") AS " + KEY_SEALED

/**
 * we check if the object is linked to a sealed account, either via its account, it transfer_account, or its children.
 * For Children, we only need to check for transfer_account, since there account is identical to their parent.
 */
fun checkForSealedAccount(baseTable: String, innerTable: String) =
    "(SELECT max($KEY_SEALED) FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = $KEY_ACCOUNTID OR $KEY_ROWID = $KEY_TRANSFER_ACCOUNT OR $KEY_ROWID in (SELECT $KEY_TRANSFER_ACCOUNT FROM $innerTable WHERE $KEY_PARENTID = $baseTable.$KEY_ROWID))"

/**
 * we check if the object is linked to a sealed debt.
 * This can be used for queries that also include parts, where we do not need to include the parts here
 */
const val checkForSealedDebt = "coalesce((SELECT $KEY_SEALED FROM $TABLE_DEBTS WHERE $KEY_ROWID = $KEY_DEBT_ID), 0)"

/**
 * we check if the object (or any of its children) is linked to a sealed debt.
 */
fun checkForSealedDebt(baseTable: String) =
    "(SELECT max($KEY_SEALED) FROM $TABLE_DEBTS WHERE $KEY_ROWID = $KEY_DEBT_ID OR $KEY_ROWID in (SELECT $KEY_DEBT_ID FROM $TABLE_TRANSACTIONS WHERE $KEY_PARENTID = $baseTable.$KEY_ROWID))"
