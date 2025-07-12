package org.totschnig.myexpenses.util

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.model.SortDirection.ASC
import org.totschnig.myexpenses.model.SortDirection.DESC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.util.ui.resolveThemeColor

fun configureSearch(activity: Activity, menu: Menu, callback: (String) -> Boolean) {
    (activity.getSystemService(Context.SEARCH_SERVICE) as? SearchManager)?.let { manager ->
        (menu.findItem(R.id.SEARCH_COMMAND).actionView as? SearchView)?.let {
            it.setSearchableInfo(manager.getSearchableInfo(activity.componentName))
            it.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String) = callback(newText)
            })
        }
    }
}

fun Menu.prepareSearch(filter: String?) {
    findItem(R.id.SEARCH_COMMAND)?.let { item ->
        if (!TextUtils.isEmpty(filter)) {
            item.expandActionView()
            (item.actionView as? SearchView)?.apply {
                setQuery(filter, false)
                clearFocus()
            }
        }
    }
}

fun MenuItem.populateWithSync(accountNames: Array<String>) {
    setEnabledAndVisible(accountNames.isNotEmpty())
    subMenu?.let {
        it.clear()
        for (account in accountNames) {
            it.add(itemId, Menu.NONE, Menu.NONE, account)
        }
    }
}


fun Menu.prepareSync(
    context: Context,
) {
    val accountNames = GenericAccountService.getAccountNames(context)
    findItem(R.id.SYNC_COMMAND)?.let { item ->
        item.setEnabledAndVisible(accountNames.isNotEmpty())
        item.subMenu?.let {
            it.findItem(R.id.SYNC_COMMAND_EXPORT)?.populateWithSync(accountNames)
            it.findItem(R.id.SYNC_COMMAND_IMPORT)?.populateWithSync(accountNames)
        }
    }
}

fun MenuItem.setEnabledAndVisible(enabled: Boolean) {
    setEnabled(enabled).isVisible = enabled
}

fun BaseActivity.checkMenuIcon(
    menuItem: MenuItem,
    @DrawableRes uncheckedIcon: Int
) {
    if (!menuItem.isChecked) {
        menuItem.setIcon(uncheckedIcon)
    } else {
       menuItem.icon = createCheckedIconDrawable(this, uncheckedIcon)
    }
}

fun createCheckedIconDrawable(context: Context, @DrawableRes icon: Int): LayerDrawable {
    // Resolve theme colors
    val colorOnSurface = resolveThemeColor(context, com.google.android.material.R.attr.colorOnSurface)
    val colorSurfaceVariant = resolveThemeColor(context, com.google.android.material.R.attr.colorSurfaceVariant)

    // Convert dp to px helper
    fun dp(value: Float): Int = UiUtils.dp2Px(value, context.resources)

    // Outer oval with stroke
    val shadowWidth = dp(1.5f)
    val padding = dp(3f)
    val outerOval = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setStroke(shadowWidth, colorOnSurface)
        cornerRadius = dp(16f).toFloat()
    }

    // Slightly smaller oval with solid color and padding
    val innerOval = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colorSurfaceVariant)
        cornerRadius = dp(14.5f).toFloat()
    }

    // Actual icon drawable
    val iconDrawable = ContextCompat.getDrawable(context, icon)

    return LayerDrawable(
        arrayOf(
            outerOval,
            innerOval,
            iconDrawable!!
        )
    ).apply {
        setLayerInset(
            1,
            shadowWidth,
            shadowWidth,
            shadowWidth,
            shadowWidth
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setLayerGravity(2, Gravity.CENTER)
            setLayerSize(0, dp(24f), dp(24f))
            setLayerSize(1, dp(21f), dp(21f))
            setLayerSize(2, dp(18f), dp(18f))
        }
    }
}

fun configureSortDirectionMenu(
    context: Context,
    subMenu: SubMenu,
    currentSortBy: String,
    currentSortDirection: SortDirection,
) {
    for (i in 0 until subMenu.size) {
        val item = subMenu[i]
        val date = context.getString(R.string.date)
        val amount = context.getString(R.string.amount)
        val ascending = context.getString(R.string.sort_direction_ascending)
        val descending = context.getString(R.string.sort_direction_descending)
        when (item.itemId) {
            R.id.SORT_BY_DATE_ASCENDING_COMMAND -> {
                item.title = "$date / $ascending"
                if (currentSortBy == KEY_DATE && currentSortDirection == ASC) {
                    item.isChecked = true
                }
            }

            R.id.SORT_BY_DATE_DESCENDING_COMMAND -> {
                item.title = "$date / $descending"
                if (currentSortBy == KEY_DATE && currentSortDirection == DESC) {
                    item.isChecked = true
                }
            }

            R.id.SORT_BY_AMOUNT_ASCENDING_COMMAND -> {
                item.title = "$amount / $ascending"
                if (currentSortBy == KEY_AMOUNT && currentSortDirection == ASC) {
                    item.isChecked = true
                }
            }

            R.id.SORT_BY_AMOUNT_DESCENDING_COMMAND -> {
                item.title = "$amount / $descending"
                if (currentSortBy == KEY_AMOUNT && currentSortDirection == DESC) {
                    item.isChecked = true
                }
            }
        }
    }
}

fun getSortDirectionFromMenuItemId(id: Int) = when (id) {
    R.id.SORT_BY_DATE_ASCENDING_COMMAND -> {
        KEY_DATE to ASC
    }

    R.id.SORT_BY_DATE_DESCENDING_COMMAND -> {
        KEY_DATE to DESC
    }

    R.id.SORT_BY_AMOUNT_ASCENDING_COMMAND -> {
        KEY_AMOUNT to ASC
    }

    R.id.SORT_BY_AMOUNT_DESCENDING_COMMAND -> {
        KEY_AMOUNT to DESC
    }

    else -> null
}