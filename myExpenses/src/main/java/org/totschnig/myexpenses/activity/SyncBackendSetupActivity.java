package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.Menu;
import android.view.SubMenu;

import com.annimon.stream.Exceptional;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.NewMessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;
import org.totschnig.myexpenses.task.SyncAccountTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;
import icepick.State;
import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ConstantsKt.SYNC_BACKEND_SETUP_REQUEST;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_PASSWORD_ENCRYPTION;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_URL;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_USERNAME;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_ALLOW_UNVERIFIED;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_FETCH_SYNC_ACCOUNT_DATA;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN;

public abstract class SyncBackendSetupActivity extends ProtectedFragmentActivity
    implements EditTextDialog.EditTextDialogListener, SimpleInputDialog.OnDialogResultListener {
  private static final String DIALOG_TAG_PASSWORD = "password";
  private static final int REQUEST_CODE_RESOLUTION = 1;

  protected List<SyncBackendProviderFactory> backendProviders;

  private boolean isResumed = false;
  private boolean setupPending = false;

  @State
  int selectedFactoryId;
  @NotNull
  public static final String KEY_SYNC_PROVIDER_ID  = "syncProviderId";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    backendProviders = ServiceLoader.load(this);
  }

  //LocalFileBackend
  public void onFinishEditDialog(Bundle args) {
    String filePath = args.getString(EditTextDialog.KEY_RESULT);
    File baseFolder = new File(filePath);
    if (!baseFolder.isDirectory()) {
      showSnackbar("No directory " + filePath, Snackbar.LENGTH_SHORT);
    } else {
      String accountName = getSyncBackendProviderFactoryByIdOrThrow(R.id.SYNC_BACKEND_LOCAL).buildAccountName(filePath);
      Bundle bundle = new Bundle(1);
      bundle.putString(KEY_SYNC_PROVIDER_URL, filePath);
      createAccount(accountName, null, null, bundle);
    }
  }

  //WebDav
  public void onFinishWebDavSetup(Bundle data) {
    String userName = data.getString(AccountManager.KEY_ACCOUNT_NAME);
    String password = data.getString(AccountManager.KEY_PASSWORD);
    String url = data.getString(KEY_SYNC_PROVIDER_URL);
    String certificate = data.getString(KEY_WEB_DAV_CERTIFICATE);
    String accountName = getSyncBackendProviderFactoryByIdOrThrow(R.id.SYNC_BACKEND_WEBDAV).buildAccountName(url);

    Bundle bundle = new Bundle();
    bundle.putString(KEY_SYNC_PROVIDER_URL, url);
    bundle.putString(KEY_SYNC_PROVIDER_USERNAME, userName);
    if (certificate != null) {
      bundle.putString(KEY_WEB_DAV_CERTIFICATE, certificate);
    }
    if (data.getBoolean(KEY_WEB_DAV_FALLBACK_TO_CLASS1)) {
      bundle.putString(KEY_WEB_DAV_FALLBACK_TO_CLASS1, "1");
    }
    if (prefHandler.getBoolean(PrefKey.WEBDAV_ALLOW_UNVERIFIED_HOST, false)) {
      bundle.putString(KEY_ALLOW_UNVERIFIED, "true");
    }
    createAccount(accountName, password, null, bundle);
  }

  @Override
  protected void onResume() {
    super.onResume();
    isResumed = true;
    if (setupPending) {
      startSetupDo();
      setupPending = false;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    isResumed = false;
  }

  public void startSetup(int itemId) {
    selectedFactoryId = itemId;
    if (isResumed) {
      startSetupDo();
    } else {
      setupPending = true;
    }
  }

  private void startSetupDo() {
    SyncBackendProviderFactory syncBackendProviderFactory =
        getSyncBackendProviderFactoryById(selectedFactoryId);
    if (syncBackendProviderFactory != null) {
      syncBackendProviderFactory.startSetup(this);
    }
  }

  //Google Drive & Dropbox
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == SYNC_BACKEND_SETUP_REQUEST && resultCode == RESULT_OK && intent != null) {
      String accountName = getSyncBackendProviderFactoryByIdOrThrow(intent.getIntExtra(KEY_SYNC_PROVIDER_ID, 0))
          .buildAccountName(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
      createAccount(accountName, null,
          intent.getStringExtra(AccountManager.KEY_AUTHTOKEN), intent.getBundleExtra(AccountManager.KEY_USERDATA));
    }
    if (requestCode == REQUEST_CODE_RESOLUTION) {
      showSnackbar("Please try again");
    }
  }

  protected void createAccount(String accountName, String password, String authToken, Bundle bundle) {
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putString(AccountManager.KEY_PASSWORD, password);
    args.putString(AccountManager.KEY_AUTHTOKEN, authToken);
    args.putParcelable(AccountManager.KEY_USERDATA, bundle);
    args.putBoolean(SyncAccountTask.KEY_RETURN_REMOTE_DATA_LIST, createAccountTaskShouldReturnDataList());
    SimpleFormDialog.build().msg(R.string.passphrase_for_synchronization)
        .fields(Input.password(KEY_PASSWORD_ENCRYPTION).required().hint(R.string.input_label_passphrase))
        .extra(args)
        .neut(R.string.button_label_no_encryption)
        .show(this, DIALOG_TAG_PASSWORD);
  }

    protected void createAccountDo(Bundle args) {
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_CREATE_SYNC_ACCOUNT), ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(getString(R.string.progress_dialog_fetching_data_from_sync_backend)), PROGRESS_TAG)
        .commit();
  }

  public void fetchAccountData(String accountName) {
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putBoolean(SyncAccountTask.KEY_RETURN_REMOTE_DATA_LIST, true);
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_FETCH_SYNC_ACCOUNT_DATA), ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(getString(R.string.progress_dialog_fetching_data_from_sync_backend)), PROGRESS_TAG)
        .commit();
  }

  protected boolean createAccountTaskShouldReturnDataList() {
    return false;
  }

  public void onCancelEditDialog() {

  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TASK_CREATE_SYNC_ACCOUNT: {
        final Exceptional exceptional = (Exceptional) o;
        if (exceptional.isPresent()) {
          recordUsage(ContribFeature.SYNCHRONIZATION);
          if ("xiaomi".equalsIgnoreCase(android.os.Build.MANUFACTURER)) {
            NewMessageDialogFragment.Companion.newInstance("On some Xiaomi devices, synchronization does not work without Autostart permission. Visit <a href=\"https://github.com/mtotschnig/MyExpenses/wiki/FAQ:-Synchronization#q2\">MyExpenses FAQ</a> for more information.", true).show(getSupportFragmentManager(), "XIAOMI_WARNING");
          }
        } else {
          Throwable throwable = exceptional.getException();
          if (throwable instanceof SyncBackendProvider.ResolvableSetupException) {
            try {
              final PendingIntent resolution = ((SyncBackendProvider.ResolvableSetupException) throwable).getResolution();
              if (resolution != null) {
                startIntentSenderForResult(resolution.getIntentSender(), REQUEST_CODE_RESOLUTION, null, 0, 0, 0);
              }
            } catch (IntentSender.SendIntentException e) {
              Timber.e(e, "Exception while starting resolution activity");
            }
          } else {
            showSnackbar("Unable to set up account: " + throwable.getMessage());
          }
        }
        break;
      }
      case TASK_WEBDAV_TEST_LOGIN: {
        getWebdavFragment().onTestLoginResult((Exceptional<Void>) o);
        break;
      }
    }
  }

  public void addSyncProviderMenuEntries(SubMenu subMenu) {
    for (SyncBackendProviderFactory factory : backendProviders) {
      subMenu.add(Menu.NONE, factory.getId(), Menu.NONE, factory.getLabel());
    }
  }

  public @Nullable
  SyncBackendProviderFactory getSyncBackendProviderFactoryById(int id) {
    try {
      return getSyncBackendProviderFactoryByIdOrThrow(id);
    } catch (IllegalStateException e) {
      return null;
    }
  }

  public @NonNull
  SyncBackendProviderFactory getSyncBackendProviderFactoryByIdOrThrow(int id) throws IllegalStateException {
    for (SyncBackendProviderFactory factory : backendProviders) {
      if (factory.getId() == id) {
        return factory;
      }
    }
    throw new IllegalStateException();
  }

  protected SetupWebdavDialogFragment getWebdavFragment() {
    return (SetupWebdavDialogFragment) getSupportFragmentManager().findFragmentByTag(
        WebDavBackendProviderFactory.WEBDAV_SETUP);
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (DIALOG_TAG_PASSWORD.equals(dialogTag)) {
      if (which != BUTTON_POSITIVE || "".equals(extras.getString(KEY_PASSWORD_ENCRYPTION))) {
        extras.remove(KEY_PASSWORD_ENCRYPTION);
      }
      createAccountDo(extras);
    }
    return false;
  }
}
