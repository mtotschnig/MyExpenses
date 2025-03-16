package org.totschnig.myexpenses.repository

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.budgetAllocation
import org.totschnig.myexpenses.db2.loadCategory
import org.totschnig.myexpenses.db2.mergeCategories
import org.totschnig.myexpenses.db2.moveCategory
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.CATEGORY_TREE_URI
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.budgetAllocationUri
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import org.totschnig.shared_test.CursorSubject

@RunWith(RobolectricTestRunner::class)
class CategoryTest : BaseTestWithRepository() {

    @Test
    fun subCategoryShouldInheritTypeOnInsert() {
        val parent = repository.saveCategory(Category(label = "Main", type = FLAG_EXPENSE))!!
        val sub = repository.saveCategory(Category(label = "Sub", parentId = parent))!!
        assertThat(repository.loadCategory(sub)!!.type).isEqualTo(FLAG_EXPENSE)
    }

    private val Category.saveCopy
        get() = copy(id = repository.saveCategory(this)!!)

    @Test
    fun subCategoryShouldInheritTypeAfterUpdateOfType() {
        val mainCategory = Category(label = "Main", type = FLAG_EXPENSE).saveCopy
        //we first create sub1 and sub2 under main, and then move sub1 to sub2, in order to
        //create a condition where the original trigger had failed
        val sub1 = repository.saveCategory(Category(label = "Sub", parentId = mainCategory.id))!!
        val sub2 = repository.saveCategory(Category(label = "SubSub", parentId = mainCategory.id))!!
        repository.moveCategory(sub1, sub2)
        repository.saveCategory(mainCategory.copy(type = FLAG_INCOME))
        assertThat(repository.loadCategory(mainCategory.id!!)!!.type).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub1)!!.type).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub2)!!.type).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun subCategoryShouldInheritTypeMoveSubCategoryToOtherParent() {
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).saveCopy
        val incomeCategory = Category(label = "Income", type = FLAG_INCOME).saveCopy
        val sub = repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!
        val subSub = repository.saveCategory(Category(label = "SubSub", parentId = sub))!!
        repository.moveCategory(sub, incomeCategory.id)
        val subCategory = repository.loadCategory(sub)!!
        assertThat(subCategory.parentId).isEqualTo(incomeCategory.id)
        assertThat(subCategory.type).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(subSub)!!.type).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun subCategoryShouldInheritTypeMoveMainCategoryToOtherParent() {
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).saveCopy
        val sub = repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!
        val incomeCategory = Category(label = "Income", type = FLAG_INCOME).saveCopy
        repository.moveCategory(expenseCategory.id!!, incomeCategory.id)
        val new = repository.loadCategory(expenseCategory.id)!!
        assertThat(new.parentId).isEqualTo(incomeCategory.id)
        assertThat(new.type).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub)!!.type).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun shouldKeepTypeTransformSubIntoMain() {
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).saveCopy
        val sub = repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!
        val subSub = repository.saveCategory(Category(label = "SubSub", parentId = sub))!!
        repository.moveCategory(sub, null)
        val subCategory = repository.loadCategory(sub)!!
        assertThat(subCategory.parentId).isNull()
        assertThat(subCategory.type).isEqualTo(FLAG_EXPENSE)
        assertThat(repository.loadCategory(subSub)!!.type).isEqualTo(FLAG_EXPENSE)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun shouldNotAllowUpdateOfTypeForSubCategory() {
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).saveCopy
        val sub = repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!
        contentResolver.update(
            ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, sub),
            ContentValues().apply {
                put(DatabaseConstants.KEY_TYPE, FLAG_INCOME.toInt())
            },
            null,
            null
        )
    }

    @Test
    fun shouldMergeRecursively() {
        val main1 = Category(label = "Main 1", type = FLAG_EXPENSE).saveCopy
        val main2 = Category(label = "Main 2", type = FLAG_EXPENSE).saveCopy
        val sub1 = Category(label = "Sub", parentId = main1.id).saveCopy
        val sub2 = Category(label = "Sub", parentId = main2.id).saveCopy
        val subSub1 = Category(label = "SubSub", parentId = sub1.id).saveCopy
        Category(label = "SubSub", parentId = sub2.id).saveCopy
        repository.mergeCategories(listOf(main2.id!!), main1.id!!)
        val cursor = contentResolver.query(
            CATEGORY_TREE_URI, null, null, null, null
        )!!
        cursor.moveToFirst()
        val tree = CategoryViewModel.ingest(
            false,
            cursor,
            null,
            1
        )
        assertThat(tree).containsExactly(
            org.totschnig.myexpenses.viewmodel.data.Category(
                id = main1.id,
                level = 1,
                label = "Main 1",
                children = listOf(
                    org.totschnig.myexpenses.viewmodel.data.Category(
                        id = sub1.id!!,
                        level = 2,
                        label = "Sub",
                        path = "Main 1 > Sub",
                        parentId = main1.id,
                        children = listOf(
                            org.totschnig.myexpenses.viewmodel.data.Category(
                                id = subSub1.id!!,
                                level = 3,
                                label = "SubSub",
                                path = "Main 1 > Sub > SubSub",
                                parentId = sub1.id,
                                isMatching = true,
                                typeFlags = FLAG_EXPENSE
                            )
                        ),
                        isMatching = true,
                        typeFlags = FLAG_EXPENSE
                    )
                ),
                isMatching = true,
                typeFlags = FLAG_EXPENSE
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotMergeCategoriesWithDifferentType() {
        val main1 = Category(label = "Main 1", type = FLAG_EXPENSE).saveCopy
        val main2 = Category(label = "Main 2", type = FLAG_INCOME).saveCopy
        repository.mergeCategories(listOf(main2.id!!), main1.id!!)
    }

    @Test
    fun mergeShouldUpdateReferencedObjects() {
        val testAccountId = insertAccount("Test account")
        val main1 = Category(label = "Main 1", type = FLAG_EXPENSE).saveCopy
        val main2 = Category(label = "Main 2", type = FLAG_EXPENSE).saveCopy
        val transactionId = insertTransaction(testAccountId, 100, categoryId = main2.id!!).first
        val templateId = insertTemplate(testAccountId, "Template", 100, main2.id)
        val budgetId = insertBudget(testAccountId, "Budget", 100)
        contentResolver.update(
            budgetAllocationUri(budgetId, main2.id),
            ContentValues().apply {
                put(KEY_BUDGET, 500)
            },
            null,
            null
        )
        repository.mergeCategories(listOf(main2.id), main1.id!!)
        with(
            CursorSubject.assertThat(
                contentResolver.query(
                    ContentUris.withAppendedId(TRANSACTIONS_URI, transactionId),
                    arrayOf(KEY_CATID),
                    null,
                    null,
                    null
                )!!
            )
        ) {
            movesToFirst()
            hasLong(0, main1.id)
        }
        with(
            CursorSubject.assertThat(
                contentResolver.query(
                    ContentUris.withAppendedId(TEMPLATES_URI, templateId),
                    arrayOf(KEY_CATID),
                    null,
                    null,
                    null
                )!!
            )
        ) {
            movesToFirst()
            hasLong(0, main1.id)
        }
        Truth.assertThat(
            repository.budgetAllocation(budgetId, main1.id, null)
        ).isEqualTo(500)
    }
}