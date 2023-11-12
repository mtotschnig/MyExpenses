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
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Category

@RunWith(RobolectricTestRunner::class)
class CategoryTest: BaseTestWithRepository() {

    @Test
    fun subCategoryShouldInheritTypeOnInsert() {
        val parent = ContentUris.parseId(repository.saveCategory(Category(label = "Main", typeFlags = FLAG_EXPENSE))!!)
        val sub = ContentUris.parseId(repository.saveCategory(Category(label = "Sub", parentId = parent))!!)
        assertThat(repository.loadCategory(sub)!!.typeFlags).isEqualTo(FLAG_EXPENSE)
    }

    @Test
    fun subCategoryShouldInheritTypeAfterUpdateOfType() {
        val mainCategory = Category(label = "Main", typeFlags = FLAG_EXPENSE).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        //we first create sub1 and sub2 under main, and then move sub1 to sub2, in order to
        //create a condition where the original trigger had failed
        val sub1 = ContentUris.parseId(repository.saveCategory(Category(label = "Sub", parentId = mainCategory.id))!!)
        val sub2 = ContentUris.parseId(repository.saveCategory(Category(label = "SubSub", parentId = mainCategory.id))!!)
        repository.moveCategory(sub1, sub2)
        repository.saveCategory(mainCategory.copy(typeFlags = FLAG_INCOME))
        assertThat(repository.loadCategory(mainCategory.id)!!.typeFlags).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub1)!!.typeFlags).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub2)!!.typeFlags).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun subCategoryShouldInheritTypeMoveSubCategoryToOtherParent() {
        val expenseCategory = Category(label = "Expense", typeFlags = FLAG_EXPENSE).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        val incomeCategory = Category(label = "Income", typeFlags = FLAG_INCOME).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        val sub = ContentUris.parseId(repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!)
        val subSub = ContentUris.parseId(repository.saveCategory(Category(label = "SubSub", parentId = sub))!!)
        repository.moveCategory(sub, incomeCategory.id)
        val subCategory = repository.loadCategory(sub)!!
        assertThat(subCategory.parentId).isEqualTo(incomeCategory.id)
        assertThat(subCategory.typeFlags).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(subSub)!!.typeFlags).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun subCategoryShouldInheritTypeMoveMainCategoryToOtherParent() {
        val expenseCategory = Category(label = "Expense", typeFlags = FLAG_EXPENSE).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        val sub = ContentUris.parseId(repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!)
        val incomeCategory = Category(label = "Income", typeFlags = FLAG_INCOME).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        repository.moveCategory(expenseCategory.id, incomeCategory.id)
        val new = repository.loadCategory(expenseCategory.id)!!
        assertThat(new.parentId).isEqualTo(incomeCategory.id)
        assertThat(new.typeFlags).isEqualTo(FLAG_INCOME)
        assertThat(repository.loadCategory(sub)!!.typeFlags).isEqualTo(FLAG_INCOME)
    }

    @Test
    fun shouldKeepTypeTransformSubIntoMain() {
        val expenseCategory = Category(label = "Expense", typeFlags = FLAG_EXPENSE).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        val sub = ContentUris.parseId(repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!)
        val subSub = ContentUris.parseId(repository.saveCategory(Category(label = "SubSub", parentId = sub))!!)
        repository.moveCategory(sub, null)
        val subCategory = repository.loadCategory(sub)!!
        assertThat(subCategory.parentId).isNull()
        assertThat(subCategory.typeFlags).isEqualTo(FLAG_EXPENSE)
        assertThat(repository.loadCategory(subSub)!!.typeFlags).isEqualTo(FLAG_EXPENSE)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun shouldNotAllowUpdateOfTypeForSubCategory() {
        val expenseCategory = Category(label = "Expense", typeFlags = FLAG_EXPENSE).run {
            copy(id = ContentUris.parseId(repository.saveCategory(this)!!))
        }
        val sub = ContentUris.parseId(repository.saveCategory(Category(label = "Sub", parentId = expenseCategory.id))!!)
        assertThat(contentResolver.update(ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, sub), ContentValues().apply {
            put(DatabaseConstants.KEY_TYPE, FLAG_INCOME.toInt())
        }, null, null)).isEqualTo(1)

    }
}