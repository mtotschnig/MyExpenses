package org.totschnig.myexpenses.next.search

import androidx.annotation.StringRes
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SearchTextCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.filter.TransferCriterion
import org.totschnig.myexpenses.next.core.common.R
import kotlin.reflect.KClass

/**
 * The kinds of filter the search of the new UI offers, in the order of their chips.
 * Each kind is one [SimpleCriterion] class; several values of a kind, e.g. two categories,
 * are held by that one criterion.
 */
enum class SearchKind(val clazz: KClass<out SimpleCriterion<*>>, @param:StringRes val label: Int) {
    /** The text typed into the search field, it has no chip */
    Text(SearchTextCriterion::class, R.string.next_search_placeholder),
    Period(DateCriterion::class, R.string.next_search_period),
    Category(CategoryCriterion::class, R.string.next_search_category),
    Payee(PayeeCriterion::class, R.string.next_search_payee),
    Amount(AmountCriterion::class, R.string.next_search_amount),
    Tag(TagCriterion::class, R.string.next_search_tags),
    Status(CrStatusCriterion::class, R.string.next_search_status),
    Method(MethodCriterion::class, R.string.next_search_method),
    Transfer(TransferCriterion::class, R.string.next_search_transfer),
    Account(AccountCriterion::class, R.string.next_search_account),

    /** Search in the note only, from filters made in the classic UI */
    Comment(CommentCriterion::class, R.string.next_search_comment);

    companion object {
        fun of(criterion: SimpleCriterion<*>) = entries.find { it.clazz == criterion::class }
    }
}

/**
 * One filter of a search
 *
 * @param excluded finds the transactions that do not match [criterion]
 */
data class SearchPart(val criterion: SimpleCriterion<*>, val excluded: Boolean = false)

/**
 * What the search of an account looks for, built from the criterion stored for the account
 */
sealed interface SearchFilter {

    /** What is typed into the search field */
    val text: SearchTextCriterion?

    /** Sets what is typed into the search field, or removes it with null */
    fun withText(text: SearchTextCriterion?): SearchFilter

    /** The criterion to store, null without any filter */
    fun toCriterion(): Criterion?

    /**
     * Filters of different kinds that all have to match, at most one per kind
     */
    data class Simple(val parts: Map<SearchKind, SearchPart> = emptyMap()) : SearchFilter {

        val isEmpty get() = parts.isEmpty()

        operator fun get(kind: SearchKind) = parts[kind]

        override val text get() = parts[SearchKind.Text]?.takeIf { !it.excluded }?.criterion as? SearchTextCriterion

        override fun withText(text: SearchTextCriterion?) = with(SearchKind.Text, text?.let { SearchPart(it) })

        /** Sets the filter of [kind], or removes it with null */
        fun with(kind: SearchKind, part: SearchPart?) = Simple(
            if (part == null) parts - kind
            else {
                require(SearchKind.of(part.criterion) == kind) { "${part.criterion} is no $kind" }
                // Keeps the order of the chips, so the stored criterion does not depend on the order of editing
                (parts + (kind to part)).toSortedMap().toMap()
            }
        )

        override fun toCriterion() = parts.values
            .map { if (it.excluded) NotCriterion(it.criterion) else it.criterion }
            .joined()
    }

    /**
     * A filter made in the classic UI that cannot be shown as [Simple]: criteria joined by OR,
     * several of the same kind or nested ones. It can only be removed as a whole, but text can
     * still be searched within it.
     */
    data class Advanced(val criterion: Criterion, override val text: SearchTextCriterion? = null) : SearchFilter {

        override fun withText(text: SearchTextCriterion?) = copy(text = text)

        override fun toCriterion() = listOfNotNull(criterion, text).joined()
    }

    companion object {
        fun of(criterion: Criterion?): SearchFilter {
            if (criterion == null) return Simple()
            val criteria = if (criterion is AndCriterion) criterion.criteria else setOf(criterion)
            val parts = criteria.map { it.toPart() ?: return advanced(criteria) }
            val byKind = parts.associateBy { SearchKind.of(it.criterion)!! }
            return if (byKind.size == parts.size) Simple(byKind.toSortedMap().toMap()) else advanced(criteria)
        }

        /** Keeps the text typed apart, so the search field can show and change it */
        private fun advanced(criteria: Set<Criterion>): Advanced {
            val text = criteria.filterIsInstance<SearchTextCriterion>().singleOrNull()
            val rest = (criteria - setOfNotNull(text)).joined()
            return if (text != null && rest != null) Advanced(rest, text) else Advanced(criteria.joined()!!)
        }

        private fun Collection<Criterion>.joined(): Criterion? = when (size) {
            0 -> null
            1 -> single()
            else -> AndCriterion(toSet())
        }

        private fun Criterion.toPart(): SearchPart? = when (this) {
            is NotCriterion -> (criterion as? SimpleCriterion<*>)
                ?.takeIf { SearchKind.of(it) != null }
                ?.let { SearchPart(it, excluded = true) }

            is SimpleCriterion<*> -> takeIf { SearchKind.of(it) != null }?.let { SearchPart(it) }
            else -> null
        }
    }
}

/**
 * The free text search for [query], which also finds the amount when [query] is a number with at
 * most [fractionDigits] decimals, e.g. "12,50". Null for an empty query.
 */
fun searchText(query: String, fractionDigits: Int): SearchTextCriterion? {
    val trimmed = query.trim().takeIf { it.isNotEmpty() } ?: return null
    return SearchTextCriterion(trimmed, parseAmount(trimmed, fractionDigits))
}
