package org.totschnig.myexpenses.fragment;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
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
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.ACCOUNT_SERVICE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;

public class SyncBackendList extends Fragment implements
    ExpandableListView.OnGroupExpandListener {

  private static final int ACCOUNT_CURSOR = -1;

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
    mManager.initLoader(ACCOUNT_CURSOR, null, new LocalAccountInfoCallbacks());
    registerForContextMenu(listView);
    return v;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo) menuInfo).packedPosition;
    int commandId;
    int titleId;
    if (ExpandableListView.getPackedPositionType(packedPosition) ==
        ExpandableListView.PACKED_POSITION_TYPE_CHILD ) {
      switch (syncBackendAdapter.getSyncState(packedPosition)) {
        case SYNCED:
          commandId = R.id.SYNC_UNLINK_COMMAND;
          titleId = R.string.menu_sync_unlink;
          break;
        case KNOWN:
          commandId = R.id.SYNC_LINK_COMMAND;
          titleId = R.string.menu_sync_link;
          break;
        case UNKNOWN:
          commandId = R.id.SYNC_DOWNLOAD_COMMAND;
          titleId = R.string.menu_sync_download;
          break;
        default:
          throw new IllegalStateException("Unknown state");
      }
    } else {
      commandId = R.id.SYNC_COMMAND;
      titleId = R.string.menu_sync_now;
    }

    menu.add(Menu.NONE, commandId, 0, titleId);
    super.onCreateContextMenu(menu, v, menuInfo);
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

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo())
        .packedPosition;
    switch (item.getItemId()) {
      case R.id.SYNC_COMMAND: {
        String syncAccountName = syncBackendAdapter.getSyncAccountName(
            packedPosition);
        requestSync(syncAccountName);
        return true;
      }
      case R.id.SYNC_UNLINK_COMMAND: {
        DialogUtils.showSyncUnlinkConfirmationDialog(getActivity(),
            syncBackendAdapter.getAccountForSync(packedPosition));
        return true;
      }
    }
    return super.onContextItemSelected(item);
  }

  private void requestSync(String syncAccountName) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
    ContentResolver.requestSync(GenericAccountService.GetAccount(syncAccountName),
        TransactionProvider.AUTHORITY, bundle);
  }

  public void reloadAccountList() {
    syncBackendAdapter.setAccountList(getAccountList());
  }

  @Override
  public void onGroupExpand(int groupPosition) {
    if (!syncBackendAdapter.hasAccountMetdata(groupPosition)) {
      Utils.requireLoader(mManager, groupPosition, null, new AccountMetaDataLoaderCallbacks());
    }
  }

  public Account getAccountForSync(long packedPosition) {
    return syncBackendAdapter.getAccountForSync(packedPosition);
  }

  public void reloadLocalAccountInfo() {
    Utils.requireLoader(mManager, ACCOUNT_CURSOR, null, new LocalAccountInfoCallbacks());
  }

  private class LocalAccountInfoCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      return new CursorLoader(getActivity(), TransactionProvider.ACCOUNTS_BASE_URI,
          new String[] {KEY_UUID, KEY_SYNC_ACCOUNT_NAME}, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
      Map<String, String> uuid2syncMap = new HashMap<>();
      cursor.moveToFirst();
      while (!cursor.isAfterLast()) {
        int columnIndexUuid = cursor.getColumnIndex(KEY_UUID);
        int columnIndexSyncAccountName = cursor.getColumnIndex(KEY_SYNC_ACCOUNT_NAME);
        uuid2syncMap.put(cursor.getString(columnIndexUuid), cursor.getString(columnIndexSyncAccountName));
        cursor.moveToNext();
      }
      syncBackendAdapter.setLocalAccountInfo(uuid2syncMap);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
  }

  private class AccountMetaDataLoaderCallbacks implements LoaderManager.LoaderCallbacks<AccountMetaDataLoaderResult> {
    @Override
    public Loader<AccountMetaDataLoaderResult> onCreateLoader(int id, Bundle args) {
      return new AccountMetaDataLoader(getActivity(), (String) syncBackendAdapter.getGroup(id));
    }

    @Override
    public void onLoadFinished(Loader<AccountMetaDataLoaderResult> loader,
                               AccountMetaDataLoaderResult result) {
      if (result.getResult() != null) {
        syncBackendAdapter.setAccountMetadata(loader.getId(), result.getResult());
      } else {
        Toast.makeText(getActivity(), result.getError().toString(), Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onLoaderReset(Loader<AccountMetaDataLoaderResult> loader) {

    }
  }

  private static class AccountMetaDataLoaderResult {
    private final List result;
    private final Exception error;
    public AccountMetaDataLoaderResult(List result, Exception error) {
      this.result = result;
      this.error = error;
    }


    public Exception getError() {
      return error;
    }

    public List getResult() {
      return result;
    }

  }

  private static class AccountMetaDataLoader extends AsyncTaskLoader<AccountMetaDataLoaderResult> {
    private final String accountName;
    private boolean hasResult = false;
    private AccountMetaDataLoaderResult data;

    AccountMetaDataLoader(Context context, String accountName) {
      super(context);
      this.accountName = accountName;
      onContentChanged();
    }

    @Override
    public AccountMetaDataLoaderResult loadInBackground() {
      Optional<SyncBackendProvider> syncBackendProviderOptional = SyncBackendProviderFactory.get(
          GenericAccountService.GetAccount(accountName),
          AccountManager.get(getContext()));
      if (syncBackendProviderOptional.isPresent()) {
        return getRemoteAccountList(syncBackendProviderOptional.get());
      } else {
        return new AccountMetaDataLoaderResult(null, new Exception("Unable to get info for account " + accountName));
      }
    }

    private AccountMetaDataLoaderResult getRemoteAccountList(SyncBackendProvider provider) {
      try {
        return new AccountMetaDataLoaderResult(provider.getRemoteAccountList(), null);
      } catch (IOException e) {
        return new AccountMetaDataLoaderResult(null, e);
      }
    }

    @Override
    protected void onStartLoading() {
      if (takeContentChanged())
        forceLoad();
      else if (hasResult)
        deliverResult(data);
    }

    @Override
    public void deliverResult(final AccountMetaDataLoaderResult data) {
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
