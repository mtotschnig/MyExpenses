package org.totschnig.myexpenses.fragment;

import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.adapter.SyncBackendProviderArrayAdapter;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;

import java.util.ServiceLoader;

public class SyncBackendList extends ContextualActionBarFragment {
  private ServiceLoader<SyncBackendProviderFactory> backendProviderServiceLoader;
  private SyncBackendProviderArrayAdapter syncBackendProviderArrayAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    //TODO Move to background?
    backendProviderServiceLoader = ServiceLoader.load(SyncBackendProviderFactory.class);
  }

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity context = getActivity();
    View v = inflater.inflate(R.layout.sync_backends_list, container, false);
    final ListView lv = (ListView) v.findViewById(R.id.list);
    lv.setEmptyView(v.findViewById(R.id.empty));
    syncBackendProviderArrayAdapter = new SyncBackendProviderArrayAdapter(context, android.R.layout.simple_list_item_1);
    lv.setAdapter(syncBackendProviderArrayAdapter);
    //registerForContextualActionBar(lv);
    return v;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.sync_backend, menu);
    SubMenu createSubMenu = menu.findItem(R.id.CREATE_COMMAND).getSubMenu();
    for (SyncBackendProviderFactory factory: backendProviderServiceLoader) {
      createSubMenu.add(
          Menu.NONE, factory.getId(), Menu.NONE, factory.getLabel());
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    for (SyncBackendProviderFactory factory: backendProviderServiceLoader) {
      if (factory.getId() == item.getItemId()) {
        factory.startSetup((ManageSyncBackends) getActivity());
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  public void loadData() {
    syncBackendProviderArrayAdapter.loadData();
  }

}
