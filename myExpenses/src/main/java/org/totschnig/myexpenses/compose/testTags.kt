package org.totschnig.myexpenses.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.model.Money

const val TEST_TAG_PAGER = "PAGER"
const val TEST_TAG_LIST = "LIST"
const val TEST_TAG_ROW = "ROW"
const val TEST_TAG_HEADER = "HEADER"
const val TEST_TAG_ACCOUNTS = "ACCOUNTS"
const val TEST_TAG_EDIT_TEXT = "EDIT_TEXT"
const val TEST_TAG_POSITIVE_BUTTON = "POSITIVE_BUTTON"
const val TEST_TAG_SELECT_DIALOG = "SELECT_DIALOG"
const val TEST_TAG_CONTEXT_MENU =  "CONTEXT_MENU"
const val TEST_TAG_BUDGET_ROOT = "BUDGET_ROOT"
const val TEST_TAG_BUDGET_BUDGET = "BUDGET_BUDGET"
const val TEST_TAG_BUDGET_ALLOCATION = "BUDGET_ALLOCATION"
const val TEST_TAG_BUDGET_SPENT = "BUDGET_SPENT"
const val TEST_TAG_GROUP_SUMMARY = "GROUP_SUMMARY"
const val TEST_TAG_GROUP_SUMS = "GROUP_SUMS"
const val TEST_TAG_FILTER_CARD = "FILTER_CARD"
const val TEST_TAG_DIALOG_ROOT = "DIALOG_ROOT"
const val TEST_TAG_PART_LIST = "PART_LIST"
const val TEST_TAG_DIALOG = "DIALOG"

val amountProperty = SemanticsPropertyKey<Long>("amount")

val headerProperty = SemanticsPropertyKey<Int>("header")

fun Modifier.amountSemantics(money: Money) = amountSemantics(money.amountMinor)

fun Modifier.amountSemantics(amount: Long) = if (BuildConfig.DEBUG)
    semantics { set(amountProperty, amount) } else this

fun Modifier.headerSemantics(headerId: Int) = semantics(mergeDescendants = true) {
    if (BuildConfig.DEBUG) set(headerProperty, headerId)
}