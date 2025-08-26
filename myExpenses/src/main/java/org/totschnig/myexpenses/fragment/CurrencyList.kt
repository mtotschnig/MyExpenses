package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.ListFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.EDIT_REQUEST
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.dialog.EditCurrencyDialog
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel
import java.util.*
import javax.inject.Inject

class CurrencyList : ListFragment() {
    private lateinit var currencyViewModel: EditCurrencyViewModel
    private lateinit var currencyAdapter: CurrencyAdapter

    @JvmField
    @Inject
    var currencyContext: CurrencyContext? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        setAdapter()
        currencyViewModel = ViewModelProvider(this)[EditCurrencyViewModel::class.java]
        appComponent.inject(currencyViewModel)
        lifecycleScope.launchWhenStarted {
            currencyViewModel.currencies.collect {
                currencyAdapter.clear()
                currencyAdapter.addAll(it)
            }
        }
        currencyViewModel.deleteComplete.observe(this) { success: Boolean? ->
            if (success != null && !success) {
                (activity as ProtectedFragmentActivity?)!!.showSnackBar(R.string.currency_still_used)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerForContextMenu(listView)
        listView.isNestedScrollingEnabled = true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        val currency = currencyAdapter.getItem(
            (menuInfo as AdapterContextMenuInfo?)!!.position
        )
        if (!Utils.isKnownCurrency(currency!!.code)) {
            menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.DELETE_COMMAND) {
            currencyViewModel.deleteCurrency(
                currencyAdapter.getItem((item.menuInfo as AdapterContextMenuInfo?)!!.position)!!.code
            )
            return true
        }
        return false
    }

    private fun setAdapter() {
        currencyAdapter =
            object : CurrencyAdapter(requireActivity(), android.R.layout.simple_list_item_1) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    val item = currencyAdapter.getItem(position)
                    val (_, symbol, fractionDigits) = currencyContext!![item!!.code]
                    v.text = String.format(
                        Locale.getDefault(), "%s (%s, %d)", v.text,
                        symbol,
                        fractionDigits
                    )
                    return v
                }
            }
        listAdapter = currencyAdapter
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDIT_REQUEST && resultCode == Activity.RESULT_OK) {
            currencyAdapter.notifyDataSetChanged()
            if (data != null) {
                val result = data.getIntExtra(EditCurrencyDialog.KEY_RESULT, 0)
                (activity as ProtectedFragmentActivity?)!!.showSnackBar(
                    getString(
                        R.string.change_fraction_digits_result,
                        result,
                        data.getStringExtra(DatabaseConstants.KEY_CURRENCY)
                    )
                )
            }
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val item = currencyAdapter.getItem(position)
        val editCurrencyDialog = EditCurrencyDialog.newInstance(item)
        editCurrencyDialog.setTargetFragment(this, EDIT_REQUEST)
        editCurrencyDialog.show(parentFragmentManager, "SET_FRACTION_DIGITS")
    }
}