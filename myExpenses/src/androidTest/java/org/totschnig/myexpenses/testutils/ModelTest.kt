package org.totschnig.myexpenses.testutils

import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model2.Category

abstract class ModelTest : BaseProviderTest() {

    fun writeCategory(label: String, parentId: Long?) =
        repository.saveCategory(Category(label = label, parentId = parentId))!!

    fun deleteCategory(id: Long) {
        if(id != 0L) {
            repository.deleteCategory(id)
        }
    }
}