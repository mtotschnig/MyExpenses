package org.totschnig.myexpenses.sync;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import org.acra.util.IOUtils;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.Preconditions;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class DropboxBackendProvider extends AbstractSyncBackendProvider {
  private static final String LOCK_FILE = ".lock";
  private DbxClientV2 mDbxClient;
  private String basePath;

  DropboxBackendProvider(Context context, String folderName) {
    super(context);
    basePath = "/" + folderName;
  }

  @Override
  public Result setUp(String authToken) {
    if (authToken == null) {
      return new Result(false, "authToken is null");
    }
    String userLocale = Locale.getDefault().toString();
    DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale).build();
    mDbxClient = new DbxClientV2(requestConfig, authToken);
    return Result.SUCCESS;
  }

  private boolean requireSetup(android.accounts.Account account) {
    AccountManager accountManager = AccountManager.get(MyApplication.getInstance());
    try {
      String authToken = accountManager.blockingGetAuthToken(account, GenericAccountService.Authenticator.AUTH_TOKEN_TYPE, true);
      return setUp(authToken).success;
    } catch (OperationCanceledException | IOException | AuthenticatorException e) {
      Timber.e("Error getting auth token.", e);
      return false;
    }
  }

  @Override
  public void withAccount(Account account) throws IOException {
    setAccountUuid(account);
    String accountPath = getAccountPath();
    try {
      requireFolder(accountPath);
      String metadataPath = getResourcePath(ACCOUNT_METADATA_FILENAME);
      if (!exists(metadataPath)) {
        saveFileContents(ACCOUNT_METADATA_FILENAME, buildMetadata(account), MIMETYPE_JSON);
        createWarningFile();
      }
    } catch (DbxException | IOException e) {
      throw new IOException(e);
    }
  }

  private boolean exists(String path) throws DbxException {
    return exists(mDbxClient, path);
  }

  private void requireFolder(String path) throws DbxException {
    if (!exists(path)) {
      mDbxClient.files().createFolderV2(path);
    }
  }

  public static boolean exists(DbxClientV2 mDbxClient, String path) throws DbxException {
    try {
      mDbxClient.files().getMetadata(path);
      return true;
    } catch (GetMetadataErrorException e) {
      if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
        return false;
      } else {
        throw e;
      }
    }
  }

  private String getAccountPath() {
    Preconditions.checkArgument(!TextUtils.isEmpty(basePath));
    Preconditions.checkArgument(!TextUtils.isEmpty(accountUuid));
    return basePath + "/" + accountUuid;
  }

  private String getBackupPath() {
    return basePath + "/" + BACKUP_FOLDER_NAME;
  }

  private String getResourcePath(String resource) {
    return getAccountPath() + "/" + resource;
  }

  @Override
  public void resetAccountData(@NonNull String uuid) throws IOException {
    Preconditions.checkArgument(!TextUtils.isEmpty(basePath));
    Preconditions.checkArgument(!TextUtils.isEmpty(uuid));
    try {
      mDbxClient.files().deleteV2(basePath + "/" + uuid);
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected String getExistingLockToken() throws IOException {
    String lockFilePath = getLockFilePath();
    try {
      if (exists(lockFilePath)) {
        return IOUtils.streamToString(getInputStream(lockFilePath));
      }
    } catch (DbxException e) {
      throw new IOException(e);
    }
    return null;
  }

  private InputStream getInputStream(String resourcePath) throws IOException {
    try {
      return mDbxClient.files().download(resourcePath).getInputStream();
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void writeLockToken(String lockToken) throws IOException {
    saveInputStream(getLockFilePath(), new ByteArrayInputStream(lockToken.getBytes()));
  }

  @Override
  public void unlock() throws IOException {
    try {
      mDbxClient.files().deleteV2(getLockFilePath());
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @NonNull
  private String getLockFilePath() {
    return getResourcePath(LOCK_FILE);
  }

  @NonNull
  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException {
    return merge(filterMetadata(sequenceNumber).map(this::getChangeSetFromMetadata))
        .orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getChangeSetFromMetadata(Metadata metadata) {
    try {
      return getChangeSetFromInputStream(getSequenceFromFileName(metadata.getName()), getInputStream(metadata.getPathLower()));
    } catch (IOException e) {
      return ChangeSet.failed;
    }
  }

  private Stream<Metadata> filterMetadata(long sequenceNumber) throws IOException {
    try {
      return Stream.of(mDbxClient.files().listFolder(getAccountPath()).getEntries())
          .filter(metadata -> isNewerJsonFile(sequenceNumber, metadata.getName()));
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return getInputStream(getResourcePath(relativeUri));
  }

  @Override
  public InputStream getInputStreamForBackup(android.accounts.Account account, String backupFile) throws IOException {
    if (requireSetup(account)) {
      return getInputStream(getBackupPath() + "/" + backupFile);
    } else {
      throw new IOException(getContext().getString(R.string.sync_io_error_cannot_connect));
    }
  }

  @Override
  public void storeBackup(Uri uri, String fileName) throws IOException {
    try {
      String backupPath = getBackupPath();
      requireFolder(backupPath);
      saveUriToFolder(fileName, uri, backupPath);
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    saveUriToFolder(fileName, uri, getAccountPath());
  }

  private void saveUriToFolder(String fileName, Uri uri, String folder) throws IOException {
    InputStream in = MyApplication.getInstance().getContentResolver()
        .openInputStream(uri);
    if (in == null) {
      throw new IOException("Could not read " + uri.toString());
    }
    String finalFileName = getLastFileNamePart(fileName);
    saveInputStream(folder + "/" +  finalFileName, in);
  }

  @Override
  protected long getLastSequence(long start) throws IOException {
    return filterMetadata(start)
        .map(metadata -> getSequenceFromFileName(metadata.getName()))
        .max(this::compareInt)
        .orElse(start);
  }

  @Override
  void saveFileContents(String fileName, String fileContents, String mimeType) throws IOException {
    saveInputStream(getAccountPath() + "/" +  fileName, new ByteArrayInputStream(fileContents.getBytes()));
  }

  private void saveInputStream(String path, InputStream contents) throws IOException {
    try {
      mDbxClient.files().uploadBuilder(path)
          .withMode(WriteMode.OVERWRITE)
          .uploadAndFinish(contents);
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @NonNull
  @Override
  public Stream<AccountMetaData> getRemoteAccountList(android.accounts.Account account) throws IOException {
    Stream<AccountMetaData> result = Stream.empty();
    if (requireSetup(account)) {
      try {
        result = Stream.of(mDbxClient.files().listFolder(basePath).getEntries())
            .filter(metadata -> metadata instanceof FolderMetadata)
            .map(metadata -> basePath + "/" + metadata.getName() + "/" + ACCOUNT_METADATA_FILENAME)
            .filter(accountMetadataPath -> {
              try {
                mDbxClient.files().getMetadata(accountMetadataPath);
                return true;
              } catch (DbxException e) {
                return false;
              }
            })
            .map(this::getAccountMetaDataFromPath)
            .filter(Optional::isPresent)
            .map(Optional::get);
      } catch (DbxException e) {
        throw new IOException(e);
      }
    }
    return result;
  }

  private Optional<AccountMetaData> getAccountMetaDataFromPath(String path) {
    try {
      return getAccountMetaDataFromInputStream(getInputStream(path));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  @NonNull
  @Override
  public List<String> getStoredBackups(android.accounts.Account account) throws IOException {
    if (requireSetup(account)) {
      try {
        return Stream.of(mDbxClient.files().listFolder(getBackupPath()).getEntries())
            .map(Metadata::getName)
            .toList();
      } catch (DbxException ignored) {}
    }
    return new ArrayList<>();
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "webdav_backend";
  }

  @Override
  public boolean isAuthException(IOException e) {
    if (Utils.getCause(e) instanceof InvalidAccessTokenException) {
      return true;
    } else {
      return false;
    }
  }
}
