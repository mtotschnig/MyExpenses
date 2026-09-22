package org.totschnig.myexpenses.viewmodel.data

import com.google.common.truth.Truth
import org.junit.Test
import org.totschnig.myexpenses.util.Utils

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
    fun shouldFlattenDepthFirst() {
        val category = Category(
            label = "ROOT", children = listOf(
                Category(
                    label = "1", children = listOf(
                        Category(
                            label = "1.1", children = listOf(
                                Category(label = "1.1.1")
                            )
                        )
                    )
                ),
                Category(
                    label = "2", children = listOf(
                        Category(
                            label = "2.1", children = listOf(
                                Category(label = "2.1.1")
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
    fun testGetExpandedForSelected() {
        val category = Category(
            label = "ROOT", children = listOf(
                Category(
                    id = 1,
                    label = "1", children = listOf(
                        Category(
                            id = 2,
                            label = "1.1", children = listOf(
                                Category(id =3, label = "1.1.1")
                            )
                        )
                    )
                ),
                Category(
                    id = 4,
                    label = "2", children = listOf(
                        Category(
                            id = 5,
                            label = "2.1", children = listOf(
                                Category(id = 6, label = "2.1.1")
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.getExpandedForSelected(listOf(1, 6))).containsExactly(4L,5L)
    }

    @Test
    fun shouldRemoveNonMatchingChildren() {
        val category = Category(
            label = "ROOT", children = listOf(
                Category(
                    id= 1, label = "1", isMatching = false, children = listOf(
                        Category(
                            id = 2, label = "1.1", isMatching = false, children = listOf(
                                Category(id =3 ,label = "1.1.2", isMatching = false)
                            )
                        )
                    )
                )
            )
        )
        Truth.assertThat(category.pruneNonMatching()).isNull()
    }

    @Test
    fun shouldKeepDiacriticInsensitiveMatch() {
        val category = Category(
            label = "ROOT", children = listOf(
                Category(id = 1, label = "Café"),
                Category(id = 2, label = "Über"),
                Category(id = 3, label = "Restauración")
            )
        )

        fun filterCategory(filterText: String): List<String> {
            val normalizedFilter = Utils.normalize(filterText)
            val pruned = category.pruneByCriterion {
                Utils.normalize(it.label)?.contains(normalizedFilter) == true
            }
            return pruned?.children?.map { it.label } ?: emptyList()
        }

        Truth.assertThat(filterCategory("cafe")).containsExactly("Café")
        Truth.assertThat(filterCategory("café")).containsExactly("Café")
        Truth.assertThat(filterCategory("uber")).containsExactly("Über")
        Truth.assertThat(filterCategory("restauracion")).containsExactly("Restauración")
    }
}