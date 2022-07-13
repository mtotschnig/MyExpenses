package org.totschnig.myexpenses.test.model

import android.content.ContentUris
import org.mockito.Mockito
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.BaseProviderTest
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.viewmodel.data.Category

abstract class ModelTest : BaseProviderTest() {
    protected val repository: Repository
        get() = Repository(
            targetContextWrapper,
            Mockito.mock(CurrencyContext::class.java),
            Mockito.mock(CurrencyFormatter::class.java),
            Mockito.mock(PrefHandler::class.java)
        )

    fun writeCategory(label: String, parentId: Long?) =
        ContentUris.parseId(repository.saveCategory(Category(label = label, parentId = parentId))!!)

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        Model.setContentResolver(mockContentResolver)
    }
}