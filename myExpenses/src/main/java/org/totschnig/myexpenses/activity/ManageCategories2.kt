package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.viewmodel.CategoryViewModel

class ManageCategories2: ProtectedFragmentActivity() {
    val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(this) {
                Category(
                    nodeModel = viewModel.loadCategoryTree()
                        .collectAsState(initial = Category.EMPTY).value,
                    state = rememberMutableStateListOf(),
                    level = 0
                )
            }
        }
    }
}