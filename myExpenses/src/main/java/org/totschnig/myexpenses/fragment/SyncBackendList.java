package org.totschnig.myexpenses.fragment;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.dropbox.core.InvalidAccessTokenException;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BaseActivity;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.SyncBackendAdapter;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import eltos.simpledialogfragment.SimpleDialog;

import static com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;

public class SyncBackendList extends Fragment implements
    ExpandableListView.OnGroupExpandListener, SimpleDialog.OnDialogResultListener {

  private static final String DIALOG_INACTIVE_BACKEND = "inactive_backend";

  private SyncBackendAdapter syncBackendAdapter;
  private ExpandableListView listView;
  private int metadataLoadingCount = 0;
  private Snackbar snackbar;
  private AbstractSyncBackendViewModel viewModel;

  @Inject
  PrefHandler prefHandler;
  @Inject
  CurrencyContext currencyContext;
  @Inject
  Class<? extends AbstractSyncBackendViewModel> modelClass;
  @Inject
  LicenceHandler licenceHandler;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    viewModel = new ViewModelProvider(this).get(modelClass);
  }

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ProtectedFragmentActivity context = (ProtectedFragmentActivity) getActivity();
    View v = inflater.inflate(R.layout.sync_backends_list, container, false);
    listView = v.findViewById(R.id.list);
    View emptyView = v.findViewById(R.id.empty);
    syncBackendAdapter = new SyncBackendAdapter(context, currencyContext, getAccountList());
    listView.setAdapter(syncBackendAdapter);
    listView.setEmptyView(emptyView);
    listView.setOnGroupExpandListener(this);
    snackbar = Snackbar.make(listView, R.string.sync_loading_accounts_from_backend, LENGTH_INDEFINITE);
    UiUtils.increaseSnackbarMaxLines(snackbar);
    viewModel.getLocalAccountInfo().observe(getViewLifecycleOwner(),
        stringStringMap -> syncBackendAdapter.setLocalAccountInfo(stringStringMap));
    viewModel.loadLocalAccountInfo();
    registerForContextMenu(listView);
    return v;
  }

  @Override
  public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
    long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo) menuInfo).packedPosition;
    int commandId;
    int titleId;
    boolean isSyncAvailable = licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION);
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
          default://ERROR
            commandId = 0;
            titleId = 0;
        }
        if (commandId != 0)
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

  protected List<Pair<String, Boolean>> getAccountList() {
    return viewModel.getAccounts(requireActivity());
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo())
        .packedPosition;
    int itemId = item.getItemId();
    ((BaseActivity) requireActivity()).trackCommand(itemId);
    if (itemId == R.id.SYNC_COMMAND) {
      requestSync(packedPosition);
      return true;
    } else if (itemId == R.id.SYNC_UNLINK_COMMAND) {
      final Account accountForSync = getAccountForSync(packedPosition);
      if (accountForSync != null) {
        DialogUtils.showSyncUnlinkConfirmationDialog(requireActivity(),
            accountForSync);
      }
      return true;
    } else if (itemId == R.id.SYNCED_TO_OTHER_COMMAND) {
      Account account = getAccountForSync(packedPosition);
      if (account != null) {
        ((ProtectedFragmentActivity) requireActivity()).showMessage(
            getString(R.string.dialog_synced_to_other, account.getUuid()));
      }
      return true;
    } else if (itemId == R.id.SYNC_LINK_COMMAND) {
      Account account = getAccountForSync(packedPosition);
      if (account != null) {
        MessageDialogFragment.newInstance(
            getString(R.string.menu_sync_link),
            getString(R.string.dialog_sync_link, account.getUuid()),
            new MessageDialogFragment.Button(R.string.dialog_command_sync_link_remote, R.id.SYNC_LINK_COMMAND_REMOTE, account),
            MessageDialogFragment.nullButton(android.R.string.cancel),
            new MessageDialogFragment.Button(R.string.dialog_command_sync_link_local, R.id.SYNC_LINK_COMMAND_LOCAL, account))
            .show(getParentFragmentManager(), "SYNC_LINK");
      }
      return true;
    } else if (itemId == R.id.SYNC_REMOVE_BACKEND_COMMAND) {
      String syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition);
      Bundle b = new Bundle();
      final String message = getString(R.string.dialog_confirm_sync_remove_backend, syncAccountName)
          + " " + getString(R.string.continue_confirmation);
      b.putString(ConfirmationDialogFragment.KEY_MESSAGE, message);
      b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_REMOVE_BACKEND_COMMAND);
      b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_remove);
      b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
      b.putString(KEY_SYNC_ACCOUNT_NAME, syncAccountName);
      ConfirmationDialogFragment.newInstance(b).show(getParentFragmentManager(), "SYNC_REMOVE_BACKEND");
    }
    return super.onContextItemSelected(item);
  }

  private void requestSync(long packedPosition) {
    String syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition);
    android.accounts.Account account = GenericAccountService.getAccount(syncAccountName);
    if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
      Bundle bundle = new Bundle();
      bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
      ContentResolver.requestSync(account,
          TransactionProvider.AUTHORITY, bundle);
    } else {
      Bundle bundle = new Bundle(1);
      bundle.putString(KEY_SYNC_ACCOUNT_NAME, syncAccountName);
      SimpleDialog.build()
          .msg("Backend is not ready to be synced")
          .pos("Activate again")
          .extra(bundle)
          .show(this, DIALOG_INACTIVE_BACKEND);
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
    if (!syncBackendAdapter.hasAccountMetadata(groupPosition)) {
      metadataLoadingCount++;
      if (!snackbar.isShownOrQueued()) {
        snackbar.show();
      }
      final String backendLabel = syncBackendAdapter.getBackendLabel(groupPosition);
      viewModel.accountMetadata(backendLabel).observe(getViewLifecycleOwner(), result -> {
        metadataLoadingCount--;
        if (metadataLoadingCount == 0) {
          snackbar.dismiss();
        }
        try {
          syncBackendAdapter.setAccountMetadata(groupPosition, result.getOrThrow());
        } catch (Throwable throwable) {
          ManageSyncBackends activity = (ManageSyncBackends) requireActivity();
          if (Utils.getCause(throwable) instanceof InvalidAccessTokenException) {
            activity.requestDropboxAccess(backendLabel);
          } else {
            activity.showSnackbar(throwable.getMessage(), Snackbar.LENGTH_SHORT);
          }
        }
      });
    }
  }

  @Nullable
  public Account getAccountForSync(long packedPosition) {
    return syncBackendAdapter.getAccountForSync(packedPosition);
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (dialogTag.equals(DIALOG_INACTIVE_BACKEND) && which == BUTTON_POSITIVE) {
      GenericAccountService.activateSync(GenericAccountService.getAccount(extras.getString(KEY_SYNC_ACCOUNT_NAME)), prefHandler);
    }
    return false;
  }
}
