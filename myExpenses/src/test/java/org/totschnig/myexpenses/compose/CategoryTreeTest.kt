package org.totschnig.myexpenses.compose

import com.google.common.truth.Truth
import org.junit.Test
import org.totschnig.myexpenses.viewmodel.data.Category2

class CategoryTreeTest {

    @Test
    fun shouldKeepDeeplyNestedMatch() {
        val category = Category2(
            label = "ROOT", children = listOf(
                Category2(
                    label = "1", isMatching = false, children = listOf(
                        Category2(
                            label = "1.1", isMatching = false, children = listOf(
                                Category2(label = "1.1.2", isMatching = true)
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.pruneNonMatching()).isEqualTo(category)
    }

    @Test
    fun shouldFlattenDepthFirst() {
        val category = Category2(
            label = "ROOT", children = listOf(
                Category2(
                    label = "1", children = listOf(
                        Category2(
                            label = "1.1", children = listOf(
                                Category2(label = "1.1.1")
                            )
                        )
                    )
                ),
                Category2(
                    label = "2", children = listOf(
                        Category2(
                            label = "2.1", children = listOf(
                                Category2(label = "2.1.1")
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.flatten().map { it.label }).containsExactly(
            "ROOT", "1", "1.1", "1.1.1", "2", "2.1", "2.1.1"
        )
    }

    @Test
    fun shouldRemoveNonMatchingChildren() {
        val category = Category2(
            label = "ROOT", children = listOf(
                Category2(
                    label = "1", isMatching = false, children = listOf(
                        Category2(
                            label = "1.1", isMatching = false, children = listOf(
                                Category2(label = "1.1.2", isMatching = false)
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.pruneNonMatching()!!.children).isEmpty()
    }
}