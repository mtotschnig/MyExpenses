package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.databinding.ActivityCategoryComposeBinding
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForCategories
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.CategoryViewModel

open class ManageCategories2 : ProtectedFragmentActivity() {
    val viewModel: CategoryViewModel by viewModels()
    private lateinit var binding: ActivityCategoryComposeBinding
    private val sortOrder: String
        get() = preferredOrderByForCategories(
            getSortOrderPrefKey(),
            prefHandler,
            defaultSortOrder
        )!!

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (action != ACTION_SELECT_FILTER) {
            menuInflater.inflate(R.menu.categories, menu)
        }
        menuInflater.inflate(R.menu.search, menu)
        configureSearch(this, menu, this::onQueryTextChange)
        super.onCreateOptionsMenu(menu)
        return true
    }

    private fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setFilter(
            if (newText.isNullOrEmpty()) {
                ""
                //configureImportButton(true)
            } else {
                newText
                // if a filter results in an empty list,
                // we do not want to show the setup default categories button
                //configureImportButton(false)
            }
        )
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.findItem(R.id.SORT_COMMAND)
        if (menuItem != null) {
            val currentItem = menuItem.subMenu.findItem(getCurrentSortOrder().commandId)
            if (currentItem != null) {
                currentItem.isChecked = true
            }
        }
        prepareSearch(menu, viewModel.getFilter())
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = Sort.fromCommandId(item.itemId)?.let {
        if (!item.isChecked) {
            prefHandler.putString(getSortOrderPrefKey(), it.name)
            invalidateOptionsMenu()
            viewModel.setSortOrder(sortOrder)
        }
        true
    } ?: super.onOptionsItemSelected(item)

    protected val defaultSortOrder get() = Sort.USAGES

    protected fun getCurrentSortOrder() =
        enumValueOrDefault(
            prefHandler.getString(getSortOrderPrefKey(), null),
            defaultSortOrder
        )

    protected fun getSortOrderPrefKey() = PrefKey.SORT_ORDER_CATEGORIES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        viewModel.setSortOrder(sortOrder)
        binding.composeView.setContent {
            AppTheme(this) {
                Category(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.general_padding)),
                    nodeModel = viewModel.categoryTree.collectAsState(initial = Category.EMPTY).value,
                    state = rememberMutableStateListOf(),
                    level = 0
                )
            }
        }
    }

    val action get() = intent.action ?: ACTION_SELECT_MAPPING
}