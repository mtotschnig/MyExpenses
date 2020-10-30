package org.totschnig.myexpenses.util

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.text.TextUtils
import android.view.Menu
import androidx.appcompat.widget.SearchView
import org.totschnig.myexpenses.R

fun configureSearch(activity: Activity, menu: Menu, callback: (String?) -> Boolean) {
    (activity.getSystemService(Context.SEARCH_SERVICE) as? SearchManager)?.let { manager ->
        (menu.findItem(R.id.SEARCH_COMMAND).actionView as? SearchView)?.let {
            it.setSearchableInfo(manager.getSearchableInfo(activity.getComponentName()))
            it.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String?) = callback(newText)
            })
        }
    }
}

fun prepareSearch(menu: Menu, filter: String?) {
    menu.findItem(R.id.SEARCH_COMMAND)?.let { item ->
        if (!TextUtils.isEmpty(filter)) {
            item.expandActionView()
            (item.actionView as? SearchView)?.apply {
                setQuery(filter, false)
                clearFocus()
            }
        }
    }
}