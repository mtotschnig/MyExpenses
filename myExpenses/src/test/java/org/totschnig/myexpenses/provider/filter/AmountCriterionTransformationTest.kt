package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AmountCriterionTransformationTest(
    private val operation: Operation,
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
            assertThat(sign).isEqualTo(amountCriterion.sign)
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
                Operation.EQ, true, 100L, null,
                AmountCriterion(Operation.EQ, listOf(100L), CURRENCY, true)
            ),
            arrayOf(
                Operation.GTE, true, 100L, null,
                AmountCriterion(Operation.GTE, listOf(100L), CURRENCY, true)
            ),
            arrayOf(
                Operation.GTE, false, 100L, null,
                AmountCriterion(Operation.LTE, listOf(-100L), CURRENCY, false)
            ),
            arrayOf(
                Operation.LTE, true, 100L, null,
                AmountCriterion(Operation.BTW, listOf(0L, 100L), CURRENCY, true)
            ),
            arrayOf(
                Operation.LTE, false, 100L, null,
                AmountCriterion(Operation.BTW, listOf(-100L, 0), CURRENCY, false)
            ),
            arrayOf(
                Operation.BTW, true, 100L, 200L,
                AmountCriterion(Operation.BTW, listOf(100L, 200L), CURRENCY, true)
            ),
            arrayOf(
                Operation.BTW, false, 100L, 200L,
                AmountCriterion(Operation.BTW, listOf(-200L, -100L), CURRENCY, false)
            )
        )
    }
}