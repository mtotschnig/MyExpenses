/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.util

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.totschnig.myexpenses.CategoryInfoSubject.Companion.assertThat
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.qif.QifBufferedReader
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.export.qif.QifParser
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Collections

class QifParserTest {
    private lateinit var p: QifParser

    private fun ImportAccount.assertAccountType(accountType: AccountType) {
        Truth.assertThat(this.type).isEqualTo(accountType.name)
    }

    @Test
    fun should_parse_empty_file() {
        parseQif("")
    }

    @Test
    fun should_parse_empty_account() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        Assert.assertEquals("My Cash Account", p.accounts[0].memo)
        p.accounts[0].assertAccountType(AccountType.CASH)
    }

    @Test
    fun should_parse_a_couple_of_empty_accounts() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                !Account
                NMy Bank Account
                TBank
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.accounts.size.toLong())
        Assert.assertEquals("My Cash Account", p.accounts[0].memo)
        p.accounts[0].assertAccountType(AccountType.CASH)
        Assert.assertEquals("My Bank Account", p.accounts[1].memo)
        p.accounts[1].assertAccountType(AccountType.BANK)
    }

    @Test
    fun should_parse_account_list() {
        parseQif(
            """
                !Option:AutoSwitch
                !Account
                NBankkonto
                TBank
                DKontonr. 111111111
                ^
                NGeldtasche
                TCash
                ^
                !Clear:AutoSwitch
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.accounts.size.toLong())
        Assert.assertEquals("Bankkonto", p.accounts[0].memo)
        p.accounts[0].assertAccountType(AccountType.BANK)
        Assert.assertEquals("Kontonr. 111111111", p.accounts[0].desc)
        Assert.assertEquals("Geldtasche", p.accounts[1].memo)
        p.accounts[1].assertAccountType(AccountType.CASH)
    }

    @Test
    fun should_parse_with_missing_account_header() {
        parseQif(
            """
                !Type:Cash
                D08/02/2011
                T10.00
                LP1
                ^
                D07/02/2011
                T-20.56
                LP1:c1
                PPayee 1
                MSome note here...
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals(2, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(10.00f)
        Assert.assertEquals("P1", t.category)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-20.56f)
        Assert.assertEquals("P1:c1", t.category)
        Assert.assertEquals("Payee 1", t.payee)
        Assert.assertEquals("Some note here...", t.memo)
    }

    @Test
    fun should_parse_account_with_a_couple_of_transactions() {
        parseQif(
            """
                !Type:Cat
                NP1
                E
                ^
                NP1:c1
                E
                ^
                NP2
                I
                ^
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D08/02/2011
                T10.00
                LP1
                ^
                D07/02/2011
                T-20.56
                LP1:c1
                PPayee 1
                MSome note here...
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(3, p.categories.size.toLong())
        val categories = getCategoriesList()
        Assert.assertEquals("P1", categories[0].name)
        assertThat(categories[0]).isExpense()
        Assert.assertEquals("P1:c1", categories[1].name)
        assertThat(categories[1]).isExpense()
        Assert.assertEquals("P2", categories[2].name)
        assertThat(categories[2]).isIncome()
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals("My Cash Account", a.memo)
        a.assertAccountType(AccountType.CASH)
        Assert.assertEquals(2, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(10.00f)
        Assert.assertEquals("P1", t.category)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-20.56f)
        Assert.assertEquals("P1:c1", t.category)
        Assert.assertEquals("Payee 1", t.payee)
        Assert.assertEquals("Some note here...", t.memo)
    }

    @Test
    fun should_parse_date_according_to_format_MDY() {
        parseQif(
            """
                !Type:Cat
                NP1
                E
                ^
                NP1:c1
                E
                ^
                NP2
                I
                ^
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D2.8'11
                T10.00
                LP1
                ^
                D02/07/2011
                T-20.56
                LP1:c1
                PPayee 1
                MSome note here...
                ^
                
                """.trimIndent(), QifDateFormat.US
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals(2, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
    }

    @Test
    fun should_parse_date_according_to_format_YMD() {
        parseQif(
            """
                !Type:Cat
                NP1
                E
                ^
                NP1:c1
                E
                ^
                NP2
                I
                ^
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D2011-02-08
                T10.00
                LP1
                ^
                D2011/02/07
                T-20.56
                LP1:c1
                PPayee 1
                MSome note here...
                ^
                
                """.trimIndent(), QifDateFormat.YMD
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals(2, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
    }

    @Test
    fun should_parse_account_with_a_couple_of_transactions_without_category_list() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D08/02/2011
                T10.00
                LP1
                ^
                D07/02/2011
                T-20.56
                LP1:c1
                PPayee 1
                MSome note here...
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.categories.size.toLong())
        val categories = getCategoriesList()
        Assert.assertEquals("P1", categories[0].name)
        assertThat(categories[0]).isExpense()
        Assert.assertEquals("P1:c1", categories[1].name)
        assertThat(categories[1]).isExpense()
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals("My Cash Account", a.memo)
        a.assertAccountType(AccountType.CASH)
        Assert.assertEquals(2, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(10.00f)
        Assert.assertEquals("P1", t.category)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-20.56f)
        Assert.assertEquals("P1:c1", t.category)
        Assert.assertEquals("Payee 1", t.payee)
        Assert.assertEquals("Some note here...", t.memo)
    }

    @Test
    fun should_parse_multiple_accounts() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D08/02/2011
                T10.00
                ^
                D07/02/2011
                T-23.45
                ^
                D01/01/2011
                T-67.80
                ^
                !Account
                NMy Bank Account
                TBank
                ^
                !Type:Bank
                D08/02/2011
                T-20.00
                ^
                D02/01/2011
                T54.00
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.accounts.size.toLong())
        var a = p.accounts[0]
        Assert.assertEquals("My Cash Account", a.memo)
        a.assertAccountType(AccountType.CASH)
        Assert.assertEquals(3, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(10.00f)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-23.45f)
        t = a.transactions[2]
        Assert.assertEquals(DateTime.date(2011, 1, 1).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-67.80f)
        a = p.accounts[1]
        Assert.assertEquals("My Bank Account", a.memo)
        a.assertAccountType(AccountType.BANK)
        t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-20.00f)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 1, 2).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(54.00f)
    }

    @Test
    fun should_parse_categories_directly_from_transactions() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D08/02/2011
                T10.00
                LP1:к1
                ^
                D07/02/2011
                T11.00
                LP1
                ^
                D06/02/2011
                T12.00
                LP1:к1
                ^
                D05/02/2011
                T-13.80
                LP1:c2
                ^
                D04/02/2011
                T-14.80
                LP2:c1
                ^
                D03/02/2011
                T-15.80
                LP2:c1
                ^
                D02/02/2011
                T-16.80
                LP2
                ^
                
                """.trimIndent()
        )
        val categories: Set<CategoryInfo> = p.categories
        Assert.assertEquals(5, categories.size.toLong())
    }

    @Test
    fun should_parse_classes() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D08/02/2011
                T10.00
                LP1/Class1
                ^
                D07/02/2011
                T-23.45
                LP1:c1/Class1
                ^
                D01/01/2011
                T-67.80
                LP1:c1/Class1:Subclass1
                ^
                D01/01/2010
                T-1.20
                L/Class2
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals(4, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(10.00f)
        Assert.assertEquals("P1", t.category)
        Assert.assertEquals("Class1", t.categoryClass)
        t = a.transactions[1]
        Assert.assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-23.45f)
        Assert.assertEquals("P1:c1", t.category)
        Assert.assertEquals("Class1", t.categoryClass)
        t = a.transactions[2]
        Assert.assertEquals(DateTime.date(2011, 1, 1).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-67.80f)
        Assert.assertEquals("P1:c1", t.category)
        Assert.assertEquals("Class1:Subclass1", t.categoryClass)
        t = a.transactions[3]
        Assert.assertEquals(DateTime.date(2010, 1, 1).atMidnight().asDate(), t.date)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-1.20f)
        Assert.assertEquals("Class2", t.categoryClass)
        Assert.assertEquals(3, p.classes.size.toLong())
    }

    @Test
    fun should_parse_transfers() {
        parseQif(
            """
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D08/02/2011
                T20.00
                L[My Bank Account]
                ^
                !Account
                NMy Bank Account
                TBank
                ^
                !Type:Bank
                D08/02/2011
                T-20.00
                L[My Cash Account]/Vacation
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.accounts.size.toLong())
        var a = p.accounts[0]
        Assert.assertEquals("My Cash Account", a.memo)
        a.assertAccountType(AccountType.CASH)
        Assert.assertEquals(1, a.transactions.size.toLong())
        var t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Assert.assertEquals("My Bank Account", t.toAccount)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(20.00f)
        Assert.assertNull(t.category)
        a = p.accounts[1]
        Assert.assertEquals("My Bank Account", a.memo)
        a.assertAccountType(AccountType.BANK)
        Assert.assertEquals(1, a.transactions.size.toLong())
        t = a.transactions[0]
        Assert.assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date)
        Assert.assertEquals("My Cash Account", t.toAccount)
        Assert.assertEquals("Vacation", t.categoryClass)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-20.00f)
        Assert.assertNull(t.category)
        Assert.assertEquals(1, p.classes.size.toLong())
        Assert.assertEquals("Vacation", p.classes.iterator().next())
    }

    @Test
    fun should_collect_categories_from_splits() {
        parseQif(
            """
                !Type:Cash
                D12/07/2011
                T-2,600.66
                SA:A1
                $-1,100.56
                ENote on first split
                SA:A2
                $-1,000.00
                S<NO_CATEGORY>
                $500.10
                ENote on third split
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(3, p.categories.size.toLong())
    }

    @Test
    fun should_parse_splits() {
        parseQif(
            """
                !Type:Cat
                NA
                E
                ^
                NA:A1
                E
                ^
                NA:A1:AA1
                E
                ^
                NA:A2
                E
                ^
                NB
                E
                ^
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D12/07/2011
                T-2,600.66
                SA:A1
                $-1,100.56
                ENote on first split
                SA:A2
                $-1,000.00
                S<NO_CATEGORY>
                $500.10
                ENote on third split
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals(1, a.transactions.size.toLong())
        val t = a.transactions[0]
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-2600.66f)
        Assert.assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), t.date)
        Assert.assertEquals(3, t.splits!!.size.toLong())
        var s = t.splits[0]
        Assert.assertEquals("A:A1", s.category)
        Truth.assertThat(s.amount.toFloat()).isEqualTo(-1100.56f)
        Assert.assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), s.date)
        Assert.assertEquals("Note on first split", s.memo)
        s = t.splits[1]
        Assert.assertEquals("A:A2", s.category)
        Truth.assertThat(s.amount.toFloat()).isEqualTo(-1000.00f)
        Assert.assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), s.date)
        s = t.splits[2]
        Assert.assertEquals("<NO_CATEGORY>", s.category)
        Truth.assertThat(s.amount.toFloat()).isEqualTo(500.10f)
        Assert.assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), s.date)
        Assert.assertEquals("Note on third split", s.memo)
    }

    @Test
    fun should_parse_transfer_splits() {
        parseQif(
            """
                !Type:Cat
                NA
                E
                ^
                NA:A1
                E
                ^
                NA:A1:AA1
                E
                ^
                NA:A2
                E
                ^
                NB
                E
                ^
                !Account
                NMy Cash Account
                TCash
                ^
                !Type:Cash
                D12/07/2011
                T-2,100.00
                SA:A1
                $-1,100.00
                ENote on first split
                S[My Bank Account]
                $-1,000.00
                ^
                !Account
                NMy Bank Account
                TBank
                ^
                !Type:Bank
                D12/07/2011
                T1000.00
                L[My Cash Account]
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.accounts.size.toLong())
        var a = p.accounts[0]
        Assert.assertEquals("My Cash Account", a.memo)
        a.assertAccountType(AccountType.CASH)
        Assert.assertEquals(1, a.transactions.size.toLong())
        var t = a.transactions[0]
        Truth.assertThat(t.amount.toFloat()).isEqualTo(-2100.00f)
        Assert.assertEquals(2, t.splits!!.size.toLong())
        var s = t.splits[0]
        Assert.assertEquals("A:A1", s.category)
        Truth.assertThat(s.amount.toFloat()).isEqualTo(-1100.00f)
        Assert.assertEquals("Note on first split", s.memo)
        s = t.splits[1]
        Assert.assertTrue(s.isTransfer)
        Assert.assertEquals("My Bank Account", s.toAccount)
        Truth.assertThat(s.amount.toFloat()).isEqualTo(-1000.00f)
        a = p.accounts[1]
        Assert.assertEquals("My Bank Account", a.memo)
        a.assertAccountType(AccountType.BANK)
        Assert.assertEquals(1, a.transactions.size.toLong())
        t = a.transactions[0]
        Assert.assertTrue(t.isTransfer)
        Assert.assertEquals("My Cash Account", t.toAccount)
        Truth.assertThat(t.amount.toFloat()).isEqualTo(1000.00f)
    }

    @Test
    fun should_parse_account_with_opening_balance_without_account_name() {
        parseQif(
            """
                !Type:Bank
                D12/03/95
                T4,706.57
                CX
                POpening Balance
                ^
            """.trimIndent()
        )
        Assert.assertEquals(BigDecimal("4706.57"), p.accounts[0].openingBalance)
    }

    @Test
    fun should_not_add_cat_if_cat_list_is_empty() {
        parseQif(
            """
                !Type:Cat
                ^
                !Account
                NMy Cash Account
                TCash
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(0, p.categories.size.toLong())
    }

    @Test
    fun should_trim_space_around_categories() {
        parseQif(
            """
                !Type:Cat
                NP1
                ^
                NP1:c1
                ^
                NP1 
                ^
                
                """.trimIndent()
        )
        Assert.assertEquals(2, p.categories.size.toLong())
        val categories = getCategoriesList()
        Assert.assertEquals("P1", categories[0].name)
        assertThat(categories[0]).isExpense()
        Assert.assertEquals("P1:c1", categories[1].name)
        assertThat(categories[1]).isExpense()
    }

    @Test
    fun should_parse_opening_balance_and_memo_from_first_entry() {
        parseQif(
            """
                !Type:Bank
                D19.12.14
                T222,22
                Cx
                POpening Balance
                L[My Account Name]
                ^
                D01.02.15
                T-100,00
                Cx
                """.trimIndent()
        )
        Assert.assertEquals(1, p.accounts.size.toLong())
        val a = p.accounts[0]
        Assert.assertEquals("My Account Name", a.memo)
        Assert.assertEquals(BigDecimal("222.22"), a.openingBalance)
        Assert.assertEquals(1, a.transactions.size.toLong())
    }

    @Test
    fun should_throw_exception_on_astronomic_amount() {
        try {
            parseQif(
                """
                    !Type:Bank
                    D19.12.14
                    T12345678901234567
                    Cx
                    POpening Balance
                    L[My Account Name]
                    
                    """.trimIndent()
            )
            Assert.fail("Should not accept large amount input")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun shouldParseCategoryList() {
        parseQif(
            """
            !Type:Cat
            NMain category A
            ^
            NMain category A:Sub category A.1
            ^
            NMain category A:Sub category A.1:Sub sub category A.1.1
            """.trimIndent()
        )
        Truth.assertThat(p.categories).containsExactly(
            CategoryInfo("Main category A", FLAG_EXPENSE),
            CategoryInfo("Main category A:Sub category A.1", FLAG_EXPENSE),
            CategoryInfo("Main category A:Sub category A.1:Sub sub category A.1.1", FLAG_EXPENSE)
        )
    }

    @Test
    fun parseLongLine() {
        parseQif(
            """
            !Type:Cat
            NMain category A
            ^
            NMain category A:Sub category A.1
            ^
            NMain category A:Sub category A.1:Sub sub category A.1.1:Main category A:Sub category A.1:Sub sub category A.1.1:Main category A:Sub category A.1:Sub sub category A.1.1:Main category A:Sub category A.1:Sub sub category A.1.1:Main category A:Sub category A.1:Sub sub category A.1.1
            """.trimIndent()
        )
    }

    private fun parseQif(fileContent: String, dateFormat: QifDateFormat = QifDateFormat.EU) {
        val r = QifBufferedReader(
            BufferedReader(
                InputStreamReader(
                    ByteArrayInputStream(fileContent.toByteArray()),
                    StandardCharsets.UTF_8
                )
            )
        )
        p = QifParser(r, dateFormat, DebugInstance)
        p.parse()
    }

    private fun getCategoriesList(): List<CategoryInfo> {
        val categories: MutableList<CategoryInfo> = ArrayList(
            p.categories.size
        )
        categories.addAll(p.categories)
        Collections.sort(categories, Comparator.comparing(CategoryInfo::name))
        return categories
    }
}