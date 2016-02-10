package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES;

/**
 * Created by michaeltotschnig on 10.02.16.
 */
public abstract class SortableListFragment extends ContextualActionBarFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  public static final String SORT_ORDER_USAGES = "USAGES";
  public static final String SORT_ORDER_LAST_USED = "LAST_USED";
  public static final String SORT_ORDER_AMOUNT = "AMOUNT";
  public static final String SORT_ORDER_TITLE = "TITLE";
  protected static final int SORTABLE_CURSOR = -1;

  protected void configureSortMenu(Menu menu) {
    MenuItem menuItem = menu.findItem(R.id.SORT_COMMAND);
    if (menuItem == null) return;
    SubMenu sortMenu = menuItem.getSubMenu();
    MenuItem activeItem;
    switch (getCurrentSortOrder()) {
      case TemplatesList.SORT_ORDER_USAGES:
        activeItem = sortMenu.findItem(R.id.SORT_USAGES_COMMAND);
        break;
      case TemplatesList.SORT_ORDER_LAST_USED:
        activeItem = sortMenu.findItem(R.id.SORT_LAST_USED_COMMAND);
        break;
      case TemplatesList.SORT_ORDER_AMOUNT:
        activeItem = sortMenu.findItem(R.id.SORT_AMOUNT_COMMAND);
        break;
      default:
        activeItem = sortMenu.findItem(R.id.SORT_TITLE_COMMAND);
    }
    activeItem.setChecked(true);
  }

  @NonNull
  protected String getCurrentSortOrder() {
    return getSortOrderPrefKey().getString("USAGES");
  }

  protected String getSortOrderSql(String textColumn) {
    String sortOrder = textColumn + " COLLATE LOCALIZED";
    switch (getCurrentSortOrder()) {
      case SORT_ORDER_USAGES:
        sortOrder = KEY_USAGES + " DESC, " + sortOrder;
        break;
      case SORT_ORDER_LAST_USED:
        sortOrder = KEY_LAST_USED + " DESC, " + sortOrder;
        break;
      case SORT_ORDER_AMOUNT:
        sortOrder =  "abs(" + KEY_AMOUNT + ") DESC, " + sortOrder;
        break;
      //default is textColumn
    }
    return sortOrder;
  }

  protected boolean handleSortOption(MenuItem item) {
    String newSortOrder = null;
    switch (item.getItemId()) {
      case R.id.SORT_USAGES_COMMAND:
        newSortOrder = SORT_ORDER_USAGES;
        break;
      case R.id.SORT_LAST_USED_COMMAND:
        newSortOrder = SORT_ORDER_LAST_USED;
        break;
      case R.id.SORT_AMOUNT_COMMAND:
        newSortOrder = SORT_ORDER_AMOUNT;
        break;
      case R.id.SORT_TITLE_COMMAND:
        newSortOrder = SORT_ORDER_TITLE;
        break;
    }
    if (newSortOrder != null && !item.isChecked()) {
      getSortOrderPrefKey().putString(newSortOrder);
      getActivity().supportInvalidateOptionsMenu();
      LoaderManager manager = getLoaderManager();
      if (manager.getLoader(SORTABLE_CURSOR) != null && !manager.getLoader(SORTABLE_CURSOR).isReset())
        manager.restartLoader(SORTABLE_CURSOR, null, this);
      else
        manager.initLoader(SORTABLE_CURSOR, null, this);
      return true;
    }
    return false;
  }


  protected abstract MyApplication.PrefKey getSortOrderPrefKey();
}
