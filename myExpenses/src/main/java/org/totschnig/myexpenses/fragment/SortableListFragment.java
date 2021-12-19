package org.totschnig.myexpenses.fragment;

import android.view.Menu;
import android.view.MenuItem;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Sort;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public abstract class SortableListFragment extends ContextualActionBarFragment {

  @Inject
  PrefHandler prefHandler;

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    MenuItem menuItem = menu.findItem(R.id.SORT_COMMAND);
    if (menuItem != null) {
      final MenuItem currentItem = menuItem.getSubMenu().findItem(getCurrentSortOrder().getCommandId());
      if (currentItem != null) {
        currentItem.setChecked(true);
      }
    }
  }

  protected Sort getDefaultSortOrder() {
    return Sort.USAGES;
  }

  @NonNull
  protected Sort getCurrentSortOrder() {
    Sort defaultSortOrder = getDefaultSortOrder();
    try {
      return Sort.valueOf(prefHandler.getString(getSortOrderPrefKey(), defaultSortOrder.name()));
    } catch (IllegalArgumentException e) {
      return defaultSortOrder;
    }
  }

  protected boolean handleSortOption(MenuItem item) {
    Sort newSortOrder = Sort.fromCommandId(item.getItemId());
    if (newSortOrder != null) {
      if (!item.isChecked()) {
        prefHandler.putString(getSortOrderPrefKey(), newSortOrder.name());
        requireActivity().invalidateOptionsMenu();
        loadData();
      }
      return true;
    }
    return false;
  }


  protected abstract PrefKey getSortOrderPrefKey();
  protected abstract void loadData();
}
