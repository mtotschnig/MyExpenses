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
import org.totschnig.myexpenses.export.qif.QifUtils.trimFirstChar
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportAccount.Builder
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.CurrencyUnit
import java.io.IOException
import java.math.BigDecimal

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
            } else if (peek.startsWith("!Account")) {
                r.readLine()
                parseTransactions(parseAccount())
            } else if (peek.startsWith("!Type:Cat")) {
                r.readLine()
                parseCategories()
            } else if (peek.startsWith("!Type") && !peek.startsWith("!Type:Class")) {
                parseTransactions(Builder())
            } else {
                r.readLine()
            }
        }
        categories.addAll(categoriesFromTransactions)
    }

    @Throws(IOException::class)
    private fun parseCategories() {
        do {
            var name: String? = null
            var isIncome = false
            while (true) {
                val line = r.readLine() ?: break
                if (line.startsWith("^")) {
                    break
                }
                if (line.startsWith("N")) {
                    name = trimFirstChar(line)
                } else if (line.startsWith("I")) {
                    isIncome = true
                }
            }
            name?.let { CategoryInfo(it, isIncome) }?.let {
                categories.add(it)
            }
        } while (shouldReadOn())
    }

    @Throws(IOException::class)
    private fun parseTransactions(account: Builder) {
        val peek = r.peekLine()
        if (peek != null && peek.startsWith("!Type:")) {
            applyAccountType(account, peek)
            r.readLine()
            do {
                val t = readTransaction(r, dateFormat, currency)
                if (t.isOpeningBalance) {
                    account.openingBalance(t.amount ?: BigDecimal.ZERO)
                    t.toAccount.takeIf { !it.isNullOrEmpty() }?.let {
                        account.memo(it)
                    }
                } else {
                    addPayeeFromTransaction(t)
                    addCategoryFromTransaction(t)
                    account.addTransaction(t)
                }
            } while (shouldReadOn())
        }
        accounts.add(account.build())
    }

    private fun readTransaction(r: QifBufferedReader, dateFormat: QifDateFormat, currency: CurrencyUnit): ImportTransaction.Builder {
        val builder = ImportTransaction.Builder()
        var split: ImportTransaction.Builder? = null
        val splits = mutableListOf<ImportTransaction.Builder>()
        var line : String?
        do {
            line = r.readLine() ?: break
            if (line.startsWith("^")) {
                break
            }
            if (line.startsWith("D")) {
                builder.date(QifUtils.parseDate(trimFirstChar(line), dateFormat))
            } else if (line.startsWith("T")) {
                builder.amount(QifUtils.parseMoney(trimFirstChar(line), currency))
            } else if (line.startsWith("P")) {
                builder.payee(trimFirstChar(line))
            } else if (line.startsWith("M")) {
                builder.memo(trimFirstChar(line))
            } else if (line.startsWith("C")) {
                builder.status(trimFirstChar(line))
            } else if (line.startsWith("N")) {
                builder.number(trimFirstChar(line))
            } else if (line.startsWith("L")) {
                parseCategory(builder, line)
            } else if (line.startsWith("S")) {
                split?.let { splits.add(it) }
                split = ImportTransaction.Builder()
                parseCategory(split, line)
            } else if (line.startsWith("$")) {
                split?.amount(QifUtils.parseMoney(trimFirstChar(line), currency))
            } else if (line.startsWith("E")) {
                split?.memo(trimFirstChar(line))
            }
        } while (true)
        split?.let { splits.add(it) }
        splits.forEach {
            builder.addSplit(it)
        }
        return builder
    }

    private fun parseCategory(builder: ImportTransaction.Builder, line: String) {
        var category = trimFirstChar(line)
        val i = category.indexOf('/')
        if (i != -1) {
            builder.categoryClass(category.substring(i + 1))
            category = category.substring(0, i)
        }
        if (QifUtils.isTransferCategory(category)) {
            builder.toAccount(category.substring(1, category.length - 1))
        } else {
            builder.category(category)
        }
    }

    @Throws(IOException::class)
    private fun parseAccount(): Builder {
        val builder = Builder()
        var line: String?
        do {
            line = r.readLine() ?: break
            if (line.startsWith("^")) {
                break
            }
            if (line.startsWith("N")) {
                builder.memo(trimFirstChar(line))
            } else if (line.startsWith("T")) {
                builder.type(trimFirstChar(line))
            } else if (line.startsWith("D")) {
                builder.desc(trimFirstChar(line))
            }

        } while (true)
        return builder
    }

    private fun applyAccountType(account: Builder, peek: String) {
        if (account.type == null) {
            account.type(peek.substring(6))
        }
    }

    private fun addPayeeFromTransaction(t: ImportTransaction.Builder) {
        t.payee?.takeIf { it.isNotEmpty() }?.let {
            payees.add(it)
        }
    }

    private fun addCategoryFromTransaction(t: ImportTransaction.Builder) {
        for (split in t.splits) {
            addCategoryFromTransaction(split)
        }
        t.category?.takeIf { it.isNotEmpty() }?.let {
            categoriesFromTransactions.add(CategoryInfo(it, false))
        }
        t.categoryClass?.takeIf { it.isNotEmpty() }?.let {
            classes.add(it)
        }
    }

    @Throws(IOException::class)
    private fun shouldReadOn(): Boolean {
        val peek = r.peekLine()
        return !(peek == null || peek.startsWith("!"))
    }
}