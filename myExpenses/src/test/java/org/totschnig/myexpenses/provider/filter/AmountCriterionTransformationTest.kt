package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AmountCriterionTransformationTest(
    private val operation: WhereFilter.Operation,
    private val type: Boolean,
    private val value1: Long,
    private val value2: Long?,
    private val amountCriterion: AmountCriterion
) {

    @Test
    fun createOperation() {
        with(AmountCriterion.create(operation, CURRENCY, type, value1, value2)) {
            assertThat(operation).isEqualTo(amountCriterion.operation)
            assertThat(values).isEqualTo(amountCriterion.values)
            assertThat(type).isEqualTo(amountCriterion.type)
        }
    }

    @Test
    fun transformForUi() {
        with(amountCriterion.transformForUi()) {
            assertThat(first).isEqualTo(operation)
            assertThat(second.first()).isEqualTo(value1)
            assertThat(second.getOrNull(1)).isEqualTo(value2)
        }
    }

    companion object {
        const val CURRENCY = "EUR"

        @JvmStatic
        @Parameterized.Parameters(name = "input={0},{1},{2},{3}->{4}")
        fun data() = listOf(
            arrayOf(
                WhereFilter.Operation.EQ, true, 100L, null,
                AmountCriterion(WhereFilter.Operation.EQ, arrayOf(100L), CURRENCY, true)
            ),
            arrayOf(
                WhereFilter.Operation.GTE, true, 100L, null,
                AmountCriterion(WhereFilter.Operation.GTE, arrayOf(100L), CURRENCY, true)
            ),
            arrayOf(
                WhereFilter.Operation.GTE, false, 100L, null,
                AmountCriterion(WhereFilter.Operation.LTE, arrayOf(-100L), CURRENCY, false)
            ),
            arrayOf(
                WhereFilter.Operation.LTE, true, 100L, null,
                AmountCriterion(WhereFilter.Operation.BTW, arrayOf(0L, 100L), CURRENCY, true)
            ),
            arrayOf(
                WhereFilter.Operation.LTE, false, 100L, null,
                AmountCriterion(WhereFilter.Operation.BTW, arrayOf(-100L, 0), CURRENCY, false)
            ),
            arrayOf(
                WhereFilter.Operation.BTW, true, 100L, 200L,
                AmountCriterion(WhereFilter.Operation.BTW, arrayOf(100L, 200L), CURRENCY, true)
            ),
            arrayOf(
                WhereFilter.Operation.BTW, false, 100L, 200L,
                AmountCriterion(WhereFilter.Operation.BTW, arrayOf(-200L, -100L), CURRENCY, false)
            )
        )
    }
}