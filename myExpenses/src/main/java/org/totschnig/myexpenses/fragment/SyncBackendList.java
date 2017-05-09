package org.totschnig.myexpenses.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Collectors;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.SyncBackendAdapter;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.support.design.widget.BaseTransientBottomBar.LENGTH_INDEFINITE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;

public class SyncBackendList extends Fragment implements
    ExpandableListView.OnGroupExpandListener {

  private static final int ACCOUNT_CURSOR = -1;

  private SyncBackendAdapter syncBackendAdapter;
  private LoaderManager mManager;
  private ExpandableListView listView;
  private int metadataLoadingCount = 0;
  private Snackbar snackbar;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    mManager = getLoaderManager();
  }

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity context = getActivity();
    View v = inflater.inflate(R.layout.sync_backends_list, container, false);
    listView = (ExpandableListView) v.findViewById(R.id.list);
    View emptyView = v.findViewById(R.id.empty);
    List<String> data = getAccountList();
    syncBackendAdapter = new SyncBackendAdapter(context, data);
    listView.setAdapter(syncBackendAdapter);
    listView.setEmptyView(emptyView);
    listView.setOnGroupExpandListener(this);
    snackbar = Snackbar.make(listView, "Loading available accounts from backend.", LENGTH_INDEFINITE);
    if (MyApplication.getThemeType().equals(MyApplication.ThemeType.dark)) {
      //Workaround for https://issuetracker.google.com/issues/37120757
      View snackbarView = snackbar.getView();
      snackbarView.setBackgroundColor(Color.WHITE);
      TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
      textView.setTextColor(Color.BLACK);
    }
    mManager.initLoader(ACCOUNT_CURSOR, null, new LocalAccountInfoCallbacks());
    registerForContextMenu(listView);
    return v;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo) menuInfo).packedPosition;
    int commandId;
    int titleId;
    boolean isSyncAvailable = ContribFeature.SYNCHRONIZATION.isAvailable();
    if (ExpandableListView.getPackedPositionType(packedPosition) ==
        ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
      if (isSyncAvailable) {
        switch (syncBackendAdapter.getSyncState(packedPosition)) {
          case SYNCED_TO_THIS:
            commandId = R.id.SYNC_UNLINK_COMMAND;
            titleId = R.string.menu_sync_unlink;
            break;
          case UNSYNCED:
            commandId = R.id.SYNC_LINK_COMMAND;
            titleId = R.string.menu_sync_link;
            break;
          case SYNCED_TO_OTHER:
            commandId = R.id.SYNCED_TO_OTHER_COMMAND;
            titleId = R.string.menu_sync_link;
            break;
          case UNKNOWN:
            commandId = R.id.SYNC_DOWNLOAD_COMMAND;
            titleId = R.string.menu_sync_download;
            break;
          default:
            throw new IllegalStateException("Unknown state");
        }
        menu.add(Menu.NONE, commandId, 0, titleId);
      }
    } else {
      if (isSyncAvailable) {
        menu.add(Menu.NONE, R.id.SYNC_COMMAND, 0, R.string.menu_sync_now);
      }
      menu.add(Menu.NONE, R.id.SYNC_REMOVE_BACKEND_COMMAND, 0, R.string.menu_remove);
    }

    super.onCreateContextMenu(menu, v, menuInfo);
  }

  protected List<String> getAccountList() {
    return GenericAccountService.getAccountsAsStream(getActivity())
        .map(account -> account.name)
        .collect(Collectors.toList());
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo())
        .packedPosition;
    switch (item.getItemId()) {
      case R.id.SYNC_COMMAND: {
        requestSync(packedPosition);
        return true;
      }
      case R.id.SYNC_UNLINK_COMMAND: {
        DialogUtils.showSyncUnlinkConfirmationDialog(getActivity(),
            getAccountForSync(packedPosition));
        return true;
      }
      case R.id.SYNCED_TO_OTHER_COMMAND: {
        Account account = getAccountForSync(packedPosition);
        MessageDialogFragment.newInstance(
            0,
            getString(R.string.dialog_synced_to_other, account.uuid),
            MessageDialogFragment.Button.okButton(),
            null, null)
            .show(getFragmentManager(), "SYNCED_TO_OTHER");
        return true;
      }
      case R.id.SYNC_LINK_COMMAND: {
        Account account = getAccountForSync(packedPosition);
        MessageDialogFragment.newInstance(
            R.string.menu_sync_link,
            getString(R.string.dialog_sync_link, account.uuid),
            new MessageDialogFragment.Button(R.string.dialog_command_sync_link_remote, R.id.SYNC_LINK_COMMAND_REMOTE, packedPosition),
            MessageDialogFragment.Button.nullButton(android.R.string.cancel),
            new MessageDialogFragment.Button(R.string.dialog_command_sync_link_local, R.id.SYNC_LINK_COMMAND_LOCAL, packedPosition))
            .show(getFragmentManager(), "SYNC_LINK");
        return true;
      }
      case R.id.SYNC_REMOVE_BACKEND_COMMAND: {
        String syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition);
        Bundle b = new Bundle();
        b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
            getString(R.string.dialog_confirm_sync_remove_backend, syncAccountName));
        b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_REMOVE_BACKEND_COMMAND);
        b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_remove);
        b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
        b.putString(KEY_SYNC_ACCOUNT_NAME, syncAccountName);
        ConfirmationDialogFragment.newInstance(b).show(getFragmentManager(), "SYNC_REMOVE_BACKEND");
      }
    }
    return super.onContextItemSelected(item);
  }

  private void requestSync(long packedPosition) {
    String syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition);
    android.accounts.Account account = GenericAccountService.GetAccount(syncAccountName);
    if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
      Bundle bundle = new Bundle();
      bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
      ContentResolver.requestSync(account,
          TransactionProvider.AUTHORITY, bundle);
    } else {
      Toast.makeText(getContext(), "Backend is not ready to be synced", Toast.LENGTH_LONG).show();
    }
  }

  public void reloadAccountList() {
    syncBackendAdapter.setAccountList(getAccountList());
    int count = syncBackendAdapter.getGroupCount();
    for (int i = 0; i < count; i++) {
      listView.collapseGroup(i);
    }
  }

  @Override
  public void onGroupExpand(int groupPosition) {
    if (!syncBackendAdapter.hasAccountMetdata(groupPosition)) {
      metadataLoadingCount++;
      if (!snackbar.isShownOrQueued()) {
        snackbar.show();
      }
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
          new String[]{KEY_UUID, KEY_SYNC_ACCOUNT_NAME}, null, null, null);
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
      metadataLoadingCount--;
      if (metadataLoadingCount == 0) {
        snackbar.dismiss();
      }
      if (result.getResult() != null) {
        syncBackendAdapter.setAccountMetadata(loader.getId(), result.getResult());
      } else {
        Toast.makeText(getActivity(), result.getError() != null ? result.getError().getMessage() :
            "Could not get account metadata for backend", Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onLoaderReset(Loader<AccountMetaDataLoaderResult> loader) {

    }
  }

  //TODO replace by Exceptional
  private static class AccountMetaDataLoaderResult {
    private final List<AccountMetaData> result;
    private final Throwable error;

    AccountMetaDataLoaderResult(List<AccountMetaData> result, Throwable error) {
      this.result = result;
      this.error = error;
    }


    public Throwable getError() {
      return error;
    }

    public List<AccountMetaData> getResult() {
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
      try {
        return SyncBackendProviderFactory.get(getContext(), GenericAccountService.GetAccount(accountName))
            .map(this::getRemoteAccountList)
            .getOrThrow();
      } catch (Throwable throwable) {
        return new AccountMetaDataLoaderResult(null, throwable);
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
