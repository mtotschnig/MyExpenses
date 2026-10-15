package org.totschnig.myexpenses.viewmodel.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryNaturalSortTest {

    private fun mockCategory(id: Long, label: String, children: List<Category> = emptyList()) =
        Category(
            id = id,
            label = label,
            level = 0,
            children = children,
            sum = 0,
            budget = BudgetAllocation(0, 0, 0, false)
        )

    @Test
    fun sortChildrenByLabelNatural_Recursive_ordersAlphabeticallyAndNumerically() {
        val root = mockCategory(
            0, "Root", listOf(
                mockCategory(1, "Beta"),
                mockCategory(
                    2, "Alpha", listOf(
                        mockCategory(3, "Gamma 9"),
                        mockCategory(4, "Gamma 10")
                    )
                )
            )
        )

        //depends on Android's android.icu.text.Collator,
        //hence we run on Robolectric
        val sorted = root.sortChildrenByLabelNaturalRecursive()

        // 1. Alpha (ID 2) before Beta (ID 1)
        assertThat(sorted.children[0].id).isEqualTo(2L)

        // 2. Natural sort: Gamma 9 (ID 3) before Gamma 10 (ID 4)
        val alphaChildren = sorted.children[0].children
        assertThat(alphaChildren[0].id).isEqualTo(3L)
        assertThat(alphaChildren[1].id).isEqualTo(4L)
    }
}