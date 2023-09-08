package org.totschnig.myexpenses.test.model

import android.content.ContentUris
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.testutils.BaseProviderTest
import org.totschnig.myexpenses.viewmodel.data.Category

abstract class ModelTest : BaseProviderTest() {

    fun writeCategory(label: String, parentId: Long?) =
        ContentUris.parseId(repository.saveCategory(Category(label = label, parentId = parentId))!!)
}