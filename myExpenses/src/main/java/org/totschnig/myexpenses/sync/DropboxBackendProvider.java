package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.GetMetadataErrorException;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.Result;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DropboxBackendProvider extends AbstractSyncBackendProvider {
  private DbxClientV2 mDbxClient;
  private String basePath;

  DropboxBackendProvider(Context context) {
    super(context);
  }

  @Override
  public Result setUp(String authToken) {
    String userLocale = Locale.getDefault().toString();
    DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale).build();
    mDbxClient = new DbxClientV2(requestConfig, authToken);
    basePath = "MyExpensesDebug";
    return Result.SUCCESS;
  }

  @Override
  public boolean withAccount(Account account) {
    setAccountUuid(account);
    try {
      mDbxClient.files().getMetadata(basePath + "/" + accountUuid);
    } catch (GetMetadataErrorException e) {
      if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
        try {
          mDbxClient.files().createFolderV2(basePath + "/" + accountUuid);
        } catch (DbxException e1) {
          return false;
        }
      } else {
        return false;
      }
    } catch (DbxException e2) {
      return false;
    }
    return true;
  }

  @Override
  public boolean resetAccountData(String uuid) {
    return false;
  }

  @NonNull
  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException {
    return ChangeSet.failed;
  }

  @Override
  public boolean unlock() {
    return false;
  }

  @NonNull
  @Override
  public Stream<AccountMetaData> getRemoteAccountList() throws IOException {
    return Stream.empty();
  }

  @Override
  public void storeBackup(Uri uri, String fileName) throws IOException {

  }

  @NonNull
  @Override
  public List<String> getStoredBackups() throws IOException {
    return new ArrayList<>();
  }

  @Override
  public InputStream getInputStreamForBackup(String backupFile) throws IOException {
    return null;
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return null;
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return null;
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {

  }

  @Override
  protected long getLastSequence(long start) throws IOException {
    return 0;
  }

  @Override
  void saveFileContents(String fileName, String fileContents, String mimeType) throws IOException {

  }

  @Override
  protected String getExistingLockToken() throws IOException {
    return null;
  }

  @Override
  protected boolean writeLockToken(String lockToken) throws IOException {
    return false;
  }
}
