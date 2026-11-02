package org.totschnig.myexpenses.next.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.OrCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SearchTextCriterion
import java.time.LocalDate

class SearchFilterTest {

    private val october = DateCriterion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
    private val groceries = CategoryCriterion("Groceries", 1, 2)
    private val rewe = PayeeCriterion("Rewe", 5)

    @Test
    fun noCriterionIsAnEmptySearch() {
        val filter = SearchFilter.of(null) as SearchFilter.Simple
        assertTrue(filter.isEmpty)
        assertNull(filter.toCriterion())
    }

    @Test
    fun readsSingleAndJoinedCriteria() {
        assertEquals(SearchPart(groceries), (SearchFilter.of(groceries) as SearchFilter.Simple)[SearchKind.Category])

        val filter = SearchFilter.of(AndCriterion(setOf(rewe, NotCriterion(groceries)))) as SearchFilter.Simple
        assertEquals(SearchPart(rewe), filter[SearchKind.Payee])
        assertEquals(SearchPart(groceries, excluded = true), filter[SearchKind.Category])
    }

    @Test
    fun writesWhatItRead() {
        val criterion = AndCriterion(setOf(october, groceries, NotCriterion(rewe)))
        assertEquals(criterion, (SearchFilter.of(criterion) as SearchFilter.Simple).toCriterion())
        assertEquals(groceries, (SearchFilter.of(groceries) as SearchFilter.Simple).toCriterion())
    }

    @Test
    fun keepsTheOrderOfTheChips() {
        val filter = SearchFilter.Simple()
            .with(SearchKind.Payee, SearchPart(rewe))
            .with(SearchKind.Period, SearchPart(october))
        assertEquals(listOf(SearchKind.Period, SearchKind.Payee), filter.parts.keys.toList())
        assertEquals(listOf(october, rewe), (filter.toCriterion() as AndCriterion).criteria.toList())
    }

    @Test
    fun replacesAndRemovesAFilter() {
        val other = CategoryCriterion("Rent", 3)
        val filter = SearchFilter.Simple()
            .with(SearchKind.Category, SearchPart(groceries))
            .with(SearchKind.Category, SearchPart(other, excluded = true))
        assertEquals(NotCriterion(other), filter.toCriterion())
        assertTrue(filter.with(SearchKind.Category, null).isEmpty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesACriterionOfAnotherKind() {
        SearchFilter.Simple().with(SearchKind.Payee, SearchPart(groceries))
    }

    @Test
    fun filtersOfTheClassicUiThatDoNotFitAreAdvanced() {
        val or = OrCriterion(setOf(groceries, rewe))
        assertEquals(SearchFilter.Advanced(or), SearchFilter.of(or))

        val twice = AndCriterion(setOf(CommentCriterion("a"), CommentCriterion("b")))
        assertEquals(SearchFilter.Advanced(twice), SearchFilter.of(twice))

        val nested = AndCriterion(setOf(rewe, NotCriterion(AndCriterion(setOf(groceries, october)))))
        assertEquals(SearchFilter.Advanced(nested), SearchFilter.of(nested))
    }

    @Test
    fun readsAllKinds() {
        val status = CrStatusCriterion(listOf(CrStatus.CLEARED))
        val text = SearchTextCriterion("Rewe")
        val filter = SearchFilter.of(AndCriterion(setOf(status, CommentCriterion("x"), text))) as SearchFilter.Simple
        assertEquals(listOf(SearchKind.Text, SearchKind.Status, SearchKind.Comment), filter.parts.keys.toList())
    }

    @Test
    fun keepsTheTextApartFromAnAdvancedFilter() {
        val or = OrCriterion(setOf(groceries, rewe))
        val text = SearchTextCriterion("Kino")
        val filter = SearchFilter.of(or).withText(text)
        assertEquals(AndCriterion(setOf(or, text)), filter.toCriterion())
        assertEquals(SearchFilter.Advanced(or, text), SearchFilter.of(filter.toCriterion()))
        assertEquals(or, filter.withText(null).toCriterion())
    }

    @Test
    fun setsTheTextOfASimpleFilter() {
        val filter = SearchFilter.of(groceries).withText(SearchTextCriterion("Rewe"))
        assertEquals(SearchTextCriterion("Rewe"), filter.text)
        assertEquals(groceries, filter.withText(null).toCriterion())
    }

    @Test
    fun readsAmountsFromTheText() {
        assertEquals(SearchTextCriterion("12,50", 1250), searchText(" 12,50 ", 2))
        assertEquals(SearchTextCriterion("12.5", 1250), searchText("12.5", 2))
        assertEquals(SearchTextCriterion("-7", 700), searchText("-7", 2))
        assertEquals(SearchTextCriterion("1.250", null), searchText("1.250", 2))
        assertEquals(SearchTextCriterion("12", 12), searchText("12", 0))
        assertEquals(SearchTextCriterion("Rewe 12", null), searchText("Rewe 12", 2))
        assertNull(searchText("  ", 2))
    }
}
