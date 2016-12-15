package org.totschnig.myexpenses.fragment;

import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.adapter.SyncBackendAdapter;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.util.Utils;

import java.util.List;

import static android.content.Context.ACCOUNT_SERVICE;

public class SyncBackendList extends ContextualActionBarFragment implements
    ExpandableListView.OnGroupExpandListener, LoaderManager.LoaderCallbacks<Optional<List<AccountMetaData>>> {
  private List<SyncBackendProviderFactory> backendProviders = ServiceLoader.load();
  private SyncBackendAdapter syncBackendAdapter;
  private LoaderManager mManager;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    mManager = getLoaderManager();
  }

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity context = getActivity();
    View v = inflater.inflate(R.layout.sync_backends_list, container, false);
    ExpandableListView listView = (ExpandableListView) v.findViewById(R.id.list);
    View emptyView = v.findViewById(R.id.empty);
    List<String> data = getAccountList();
    syncBackendAdapter = new SyncBackendAdapter(context, data);
    listView.setAdapter(syncBackendAdapter);
    listView.setEmptyView(emptyView);
    listView.setOnGroupExpandListener(this);
    //lv.setOnItemClickListener(this);
    //registerForContextualActionBar(lv);
    return v;
  }


  protected List<String> getAccountList() {
    AccountManager accountManager = (AccountManager) getActivity().getSystemService(ACCOUNT_SERVICE);
    return Stream.of(accountManager.getAccountsByType(GenericAccountService.ACCOUNT_TYPE))
        .map(account -> account.name)
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
    syncBackendAdapter.setAccountList(getAccountList());
  }

  @Override
  public void onGroupExpand(int groupPosition) {
    if (!syncBackendAdapter.hasAccountMetdata(groupPosition)) {
      Utils.requireLoader(mManager, groupPosition, null, this);
    }
  }

  @Override
  public Loader<Optional<List<AccountMetaData>>> onCreateLoader(int id, Bundle args) {
    return new AccountMetaDataLoader(getActivity(), (String) syncBackendAdapter.getGroup(id));
  }

  @Override
  public void onLoadFinished(Loader<Optional<List<AccountMetaData>>> loader,
                             Optional<List<AccountMetaData>> optionalData) {
    optionalData.executeIfPresent(data -> syncBackendAdapter.setAccountMetadata(loader.getId(), data))
        .executeIfAbsent(() -> Toast.makeText(getActivity(),
            "Unable to get info for account " + loader.getId(), Toast.LENGTH_SHORT).show());
  }

  @Override
  public void onLoaderReset(Loader<Optional<List<AccountMetaData>>> loader) {

  }

  private static class AccountMetaDataLoader extends AsyncTaskLoader<Optional<List<AccountMetaData>>> {
    private final String accountName;
    private boolean hasResult = false;
    private Optional<List<AccountMetaData>> data;

    AccountMetaDataLoader(Context context, String accountName) {
      super(context);
      this.accountName = accountName;
      onContentChanged();
    }

    @Override
    public Optional<List<AccountMetaData>> loadInBackground() {
      return SyncBackendProviderFactory.get(
          GenericAccountService.GetAccount(accountName),
          AccountManager.get(getContext()))
          .map(SyncBackendProvider::getRemoteAccountList);
    }
    @Override
    protected void onStartLoading() {
      if (takeContentChanged())
        forceLoad();
      else if (hasResult)
        deliverResult(data);
    }

    @Override
    public void deliverResult(final Optional<List<AccountMetaData>> data) {
      this.data = data;
      hasResult = true;
      super.deliverResult(data);
    }

    @Override
    protected void onReset() {
      super.onReset();
      onStopLoading();
      if (hasResult) {
        data = null;
        hasResult = false;
      }
    }
  }
}
