/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.CategoryInfo.Companion.readFrom
import org.totschnig.myexpenses.model.CurrencyUnit
import java.io.IOException

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/11/11 10:32 PM
 */
class QifParser(
    private val r: QifBufferedReader,
    private val dateFormat: QifDateFormat,
    private val currency: CurrencyUnit
) {
    @JvmField
    val accounts: MutableList<QifAccount> = ArrayList()
    @JvmField
    val categories: MutableSet<CategoryInfo> = HashSet()
    private val categoriesFromTransactions: MutableSet<CategoryInfo> = HashSet()
    @JvmField
    val payees: MutableSet<String> = HashSet()
    @JvmField
    val classes: MutableSet<String> = HashSet()
    @Throws(IOException::class)
    fun parse() {
        var peek: String?
        while (r.peekLine().also { peek = it } != null) {
            if (peek!!.startsWith("!Option:AutoSwitch")) {
                @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var line = r.readLine()
                outer@ while (true) {
                    line = r.readLine()
                    if (line == null) {
                        return
                    }
                    if (line == "!Account") {
                        while (true) {
                            peek = r.peekLine()
                            if (peek == null) {
                                return
                            }
                            if (peek == "!Clear:AutoSwitch") {
                                r.readLine()
                                break@outer
                            }
                            val a = parseAccount()
                            accounts.add(a)
                        }
                    }
                }
            } else if (peek!!.startsWith("!Account")) {
                r.readLine()
                parseTransactions(parseAccount())
            } else if (peek!!.startsWith("!Type:Cat")) {
                r.readLine()
                parseCategories()
            } else if (peek!!.startsWith("!Type") && !peek!!.startsWith("!Type:Class")) {
                parseTransactions(QifAccount())
            } else {
                r.readLine()
            }
        }
        categories.addAll(categoriesFromTransactions)
    }

    @Throws(IOException::class)
    private fun parseCategories() {
        do {
            readFrom(r)?.let {
                categories.add(it)
            }
        } while (shouldReadOn())
    }

    @Throws(IOException::class)
    private fun parseTransactions(account: QifAccount) {
        accounts.add(account)
        val peek = r.peekLine()
        if (peek != null && peek.startsWith("!Type:")) {
            applyAccountType(account, peek)
            r.readLine()
            do {
                val t = QifTransaction()
                t.readFrom(r, dateFormat, currency)
                if (t.isOpeningBalance) {
                    account.openinBalance = t.amount
                    if (!t.toAccount.isNullOrEmpty()) account.memo = t.toAccount
                } else {
                    addPayeeFromTransaction(t)
                    addCategoryFromTransaction(t)
                    account.transactions.add(t)
                }
            } while (shouldReadOn())
        }
    }

    @Throws(IOException::class)
    private fun parseAccount(): QifAccount {
        val account = QifAccount()
        account.readFrom(r)
        return account
    }

    private fun applyAccountType(account: QifAccount, peek: String) {
        if (account.type.isEmpty()) {
            account.type = peek.substring(6)
        }
    }

    private fun addPayeeFromTransaction(t: QifTransaction) {
        t.payee?.takeIf { it.isNotEmpty() }?.let {
            payees.add(it)
        }
    }

    private fun addCategoryFromTransaction(t: QifTransaction) {
        if (t.isSplit) {
            for (split in t.splits) {
                addCategoryFromTransaction(split)
            }
        } else {
            t.category?.takeIf { it.isNotEmpty() }?.let {
                categoriesFromTransactions.add(CategoryInfo(it, false))
            }
            t.categoryClass?.takeIf { it.isNotEmpty() }?.let {
                classes.add(it)
            }
        }
    }

    @Throws(IOException::class)
    private fun shouldReadOn(): Boolean {
        val peek = r.peekLine()
        return !(peek == null || peek.startsWith("!"))
    }
}