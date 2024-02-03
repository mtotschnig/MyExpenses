package org.totschnig.myexpenses.repository

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.loadCategory
import org.totschnig.myexpenses.db2.moveCategory
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

@RunWith(RobolectricTestRunner::class)
class CategoryTest: BaseTestWithRepository() {

    @Test
    fun subCategoryShouldInheritTypeOnInsert() {
        val parent = repository.saveCategory(Category(label = "Main", type = FLAG_EXPENSE))!!
        val sub = repository.saveCategory(Category(label = "Sub", parentId = parent))!!
        assertThat(repository.loadCategory(sub)!!.type).isEqualTo(FLAG_EXPENSE)
    }

    @Test
    fun subCategoryShouldInheritTypeAfterUpdateOfType() {
        val mainCategory = Category(label = "Main", type = FLAG_EXPENSE).run {
            copy(id = repository.saveCategory(this)!!)
        }
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
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).run {
            copy(id = repository.saveCategory(this)!!)
        }
        val incomeCategory = Category(label = "Income", type = FLAG_INCOME).run {
            copy(id = repository.saveCategory(this)!!)
        }
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
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).run {
            copy(id = repository.saveCategory(this)!!)
        }
        val sub = repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!
        val incomeCategory = Category(label = "Income", type = FLAG_INCOME).run {
            copy(id = repository.saveCategory(this)!!)
        }
        repository.moveCategory(expenseCategory.id!!, incomeCategory.id)
        val new = repository.loadCategory(expenseCategory.id!!)!!
        assertThat(new.parentId).isEqualTo(incomeCategory.id)
        assertThat(new.type).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub)!!.type).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun shouldKeepTypeTransformSubIntoMain() {
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).run {
            copy(id = repository.saveCategory(this)!!)
        }
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
        val expenseCategory = Category(label = "Expense", type = FLAG_EXPENSE).run {
            copy(id = repository.saveCategory(this)!!)
        }
        val sub = repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!
        assertThat(contentResolver.update(ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, sub), ContentValues().apply {
            put(DatabaseConstants.KEY_TYPE, FLAG_INCOME.toInt())
        }, null, null)).isEqualTo(1)

    }
}