package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.viewmodel.CategoryViewModel

class ManageCategories2: ProtectedFragmentActivity() {
    val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Category(
                nodeModel = viewModel.loadCategoryTree()
                    .collectAsState(initial = Category.EMPTY).value,
                state = remember { mutableStateListOf() },
                isRoot = true
            )
        }
    }
}