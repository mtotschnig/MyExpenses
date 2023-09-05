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
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
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
    val accounts: MutableList<ImportAccount> = mutableListOf()
    val categories: MutableSet<CategoryInfo> = mutableSetOf()
    private val categoriesFromTransactions: MutableSet<CategoryInfo> = mutableSetOf()
    val payees: MutableSet<String> = mutableSetOf()
    val classes: MutableSet<String> = mutableSetOf()
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
                            accounts.add(parseAccount().build())
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
                parseTransactions(ImportAccount.Builder())
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
    private fun parseTransactions(account: ImportAccount.Builder) {
        val peek = r.peekLine()
        if (peek != null && peek.startsWith("!Type:")) {
            applyAccountType(account, peek)
            r.readLine()
            do {
                val t = ImportTransaction.readFrom(r, dateFormat, currency)
                if (t.isOpeningBalance) {
                    account.openingBalance(t.amount)
                    if (!t.toAccount.isNullOrEmpty()) account.memo(t.toAccount)
                } else {
                    addPayeeFromTransaction(t)
                    addCategoryFromTransaction(t)
                    account.addTransaction(t)
                }
            } while (shouldReadOn())
        }
        accounts.add(account.build())
    }

    @Throws(IOException::class)
    private fun parseAccount() = ImportAccount.readFrom(r)

    private fun applyAccountType(account: ImportAccount.Builder, peek: String) {
        if (account.type.isNullOrEmpty()) {
            account.type(peek.substring(6))
        }
    }

    private fun addPayeeFromTransaction(t: ImportTransaction) {
        t.payee?.takeIf { it.isNotEmpty() }?.let {
            payees.add(it)
        }
    }

    private fun addCategoryFromTransaction(t: ImportTransaction) {
        if (t.isSplit) {
            for (split in t.splits!!) {
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