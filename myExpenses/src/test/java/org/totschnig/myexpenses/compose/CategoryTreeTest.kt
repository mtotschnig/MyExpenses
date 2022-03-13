package org.totschnig.myexpenses.compose

import com.google.common.truth.Truth
import org.junit.Test

class CategoryTreeTest {

    @Test
    fun shouldKeepDeeplyNestedMatch() {
        val category = Category(
            label = "ROOT", children = listOf(
                Category(
                    label = "1", isMatching = false, children = listOf(
                        Category(
                            label = "1.1", isMatching = false, children = listOf(
                                Category(label = "1.1.2", isMatching = true)
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.pruneNonMatching()).isEqualTo(category)
    }

    @Test
    fun shouldRemoveNonMatchingChildren() {
        val category = Category(
            label = "ROOT", children = listOf(
                Category(
                    label = "1", isMatching = false, children = listOf(
                        Category(
                            label = "1.1", isMatching = false, children = listOf(
                                Category(label = "1.1.2", isMatching = false)
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.pruneNonMatching()!!.children).isEmpty()
    }
}