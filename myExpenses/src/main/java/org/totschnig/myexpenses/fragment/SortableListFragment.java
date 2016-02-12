package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.util.Utils;

/**
 * Created by michaeltotschnig on 10.02.16.
 */
public abstract class SortableListFragment extends ContextualActionBarFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  protected static final int SORTABLE_CURSOR = -1;

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem menuItem = menu.findItem(R.id.SORT_COMMAND);
    if (menuItem == null) return;
    Utils.configureSortMenu(menuItem.getSubMenu(),getCurrentSortOrder());
    SubMenu sortMenu = menuItem.getSubMenu();
    MenuItem activeItem;
    switch (getCurrentSortOrder()) {
      case ProtectedFragmentActivity.SORT_ORDER_USAGES:
        activeItem = sortMenu.findItem(R.id.SORT_USAGES_COMMAND);
        break;
      case ProtectedFragmentActivity.SORT_ORDER_LAST_USED:
        activeItem = sortMenu.findItem(R.id.SORT_LAST_USED_COMMAND);
        break;
      case ProtectedFragmentActivity.SORT_ORDER_AMOUNT:
        activeItem = sortMenu.findItem(R.id.SORT_AMOUNT_COMMAND);
        break;
      default:
        activeItem = sortMenu.findItem(R.id.SORT_TITLE_COMMAND);
    }
    activeItem.setChecked(true);
  }

  @NonNull
  protected String getCurrentSortOrder() {
    return getSortOrderPrefKey().getString(ProtectedFragmentActivity.SORT_ORDER_USAGES);
  }

  protected boolean handleSortOption(MenuItem item) {
    String newSortOrder = Utils.getSortOrderFromMenuItemId(item.getItemId());
    if (newSortOrder != null) {
      if (!item.isChecked()) {
        getSortOrderPrefKey().putString(newSortOrder);
        getActivity().supportInvalidateOptionsMenu();
        LoaderManager manager = getLoaderManager();
        if (manager.getLoader(SORTABLE_CURSOR) != null && !manager.getLoader(SORTABLE_CURSOR).isReset())
          manager.restartLoader(SORTABLE_CURSOR, null, this);
        else
          manager.initLoader(SORTABLE_CURSOR, null, this);
      }
      return true;
    }
    return false;
  }


  protected abstract MyApplication.PrefKey getSortOrderPrefKey();
}
