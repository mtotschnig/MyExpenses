package org.totschnig.myexpenses.test.model

import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.testutils.BaseProviderTest

abstract class ModelTest : BaseProviderTest() {

    fun writeCategory(label: String, parentId: Long?) =
        repository.saveCategory(Category(label = label, parentId = parentId))!!
}