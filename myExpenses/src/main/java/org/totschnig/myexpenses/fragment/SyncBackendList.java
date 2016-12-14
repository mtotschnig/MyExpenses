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
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.adapter.SyncBackendAdapter;
import org.totschnig.myexpenses.adapter.SyncBackendAdapter.SyncAccount;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;

import java.util.List;

import static android.content.Context.ACCOUNT_SERVICE;

public class SyncBackendList extends ContextualActionBarFragment implements AdapterView.OnItemClickListener {
  private List<SyncBackendProviderFactory> backendProviders = ServiceLoader.load();
  private SyncBackendAdapter syncBackendAdapter;
  private ExpandableListView listView;
  private View emptyView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity context = getActivity();
    View v = inflater.inflate(R.layout.sync_backends_list, container, false);
    listView = (ExpandableListView) v.findViewById(R.id.list);
    emptyView = v.findViewById(R.id.empty);
    List<SyncAccount> data = getAccountList();
    syncBackendAdapter = new SyncBackendAdapter(context, data);
    listView.setAdapter(syncBackendAdapter);
    listView.setEmptyView(emptyView);
    //lv.setOnItemClickListener(this);
    //registerForContextualActionBar(lv);
    return v;
  }


  protected List<SyncAccount> getAccountList() {
    AccountManager accountManager = (AccountManager) getActivity().getSystemService(ACCOUNT_SERVICE);
    return Stream.of(accountManager.getAccountsByType(GenericAccountService.ACCOUNT_TYPE))
        .map(SyncAccount::new)
        .collect(Collectors.toList());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.sync_backend, menu);
    SubMenu createSubMenu = menu.findItem(R.id.CREATE_COMMAND).getSubMenu();
    for (SyncBackendProviderFactory factory: backendProviders) {
      createSubMenu.add(
          Menu.NONE, factory.getId(), Menu.NONE, factory.getLabel());
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    for (SyncBackendProviderFactory factory: backendProviders) {
      if (factory.getId() == item.getItemId()) {
        factory.startSetup((ManageSyncBackends) getActivity());
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  public void loadData() {
    List<SyncAccount> accountList = getAccountList();
    syncBackendAdapter.setData(accountList);
    //syncBackendAdapter.setParentList(accountList, true);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    String accountName = "";//syncBackendAdapter.getParentList().get(position).name;
    SyncBackendProviderFactory.get(
        GenericAccountService.GetAccount(accountName),
        AccountManager.get(getActivity()))
        .executeIfPresent(this::getRemoteAccountList)
        .executeIfAbsent(() -> Toast.makeText(getActivity(),
            "Unable to get info for  account " + accountName, Toast.LENGTH_SHORT).show());
  }

  private void getRemoteAccountList(SyncBackendProvider syncBackendProvider) {

  }

}
