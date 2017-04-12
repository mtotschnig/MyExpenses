package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.Menu;
import android.view.MenuItem;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.Utils;

public abstract class SortableListFragment extends ContextualActionBarFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  protected static final int SORTABLE_CURSOR = -1;

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem menuItem = menu.findItem(R.id.SORT_COMMAND);
    if (menuItem == null) return;
    Utils.configureSortMenu(menuItem.getSubMenu(),getCurrentSortOrder());
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
        Utils.requireLoader(manager, SORTABLE_CURSOR, null, this);
      }
      return true;
    }
    return false;
  }


  protected abstract PrefKey getSortOrderPrefKey();
}
