package org.totschnig.myexpenses.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.CategoryTreeAdapter
import org.totschnig.myexpenses.databinding.CategoriesListBinding
import org.totschnig.myexpenses.databinding.CategoryRowBinding
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.prepareSearch

class CategoryList: AbstractCategoryList<CategoryRowBinding>() {
    private var _binding: CategoriesListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CategoriesListBinding.inflate(inflater, container, false)
        configureImportButton(true)
        listView.emptyView = binding.empty
        mAdapter = CategoryTreeAdapter(requireContext(), currencyFormatter, null, isWithMainColors,
                false, action == ACTION_SELECT_FILTER)
        listView.setAdapter(mAdapter)
        loadData()
        registerForContextualActionBar(listView)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity == null) return
        inflater.inflate(R.menu.search, menu)
        configureSearch(requireActivity(), menu) { newText: String? -> onQueryTextChange(newText) }
    }

    private fun onQueryTextChange(newText: String?): Boolean {
        if (newText.isNullOrEmpty()) {
            mFilter = ""
            configureImportButton(true)
        } else {
            mFilter = newText
            // if a filter results in an empty list,
            // we do not want to show the setup default categories button
            configureImportButton(false)
        }
        collapseAll()
        loadData()
        return true
    }

    private fun configureImportButton(visible: Boolean) {
        binding.SETUPCATEGORIESDEFAULTCOMMAND.visibility = if (visible /* && getResources().getBoolean(R.bool.has_localized_categories)*/) View.VISIBLE else View.GONE
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        prepareSearch(menu, mFilter)
    }

    override fun getListView() = binding.list
}