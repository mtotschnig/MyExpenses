package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
sealed class ComplexCriterion : Criterion {
    abstract val criteria: List<Criterion>
    abstract val operator: String
    override fun getSelectionForParts(
        tableName: String
    ) = criteria.joinToString(" $operator ") {
        it.getSelectionForParts(tableName)
    }

    override fun getSelectionForParents(
        tableName: String,
        forExport: Boolean,
    ) = criteria.joinToString(" $operator ") {
        it.getSelectionForParents(tableName, forExport)
    }

    override fun getSelectionArgs(queryParts: Boolean) =
        criteria.flatMap { it.getSelectionArgs(queryParts).toList() }.toTypedArray()

    override fun prettyPrint(context: Context) = criteria.joinToString {
        it.prettyPrint(context)
    }

    fun with(criteria: List<Criterion>): ComplexCriterion = when (this) {
        is AndCriterion -> AndCriterion(criteria)
        is OrCriterion -> OrCriterion(criteria)
    }

    fun plus(criterion: Criterion) = with(criteria = this.criteria + criterion)
    fun minus(criterion: Criterion) = with(criteria = this.criteria - criterion)
    fun negate(atIndex: Int) = with(criteria = this.criteria.mapIndexed { index, criterion ->
        if (index == atIndex) if (criterion is NotCriterion) criterion.criterion else NotCriterion(
            criterion
        ) else criterion

    })
}