package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;
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

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.Preconditions;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.io.StreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

public class DropboxBackendProvider extends AbstractSyncBackendProvider {
  private static final String LOCK_FILE = ".lock";
  private DbxClientV2 mDbxClient;
  private String basePath;

  DropboxBackendProvider(Context context, String folderName) {
    super(context);
    basePath = "/" + folderName;
  }

  @Override
  public Exceptional<Void> setUp(@Nullable String authToken, @Nullable String encryptionPassword, boolean create) {
    if (authToken == null) {
      return Exceptional.of(new Exception("authToken is null"));
    }
    setupClient(authToken);
    return super.setUp(authToken, encryptionPassword, create);
  }

  @Override
  protected boolean isEmpty() throws IOException {
    try {
      return mDbxClient.files().listFolder(basePath).getEntries().isEmpty();
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  private void setupClient(String authToken) {
    String userLocale = Locale.getDefault().toString();
    DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale).build();
    mDbxClient = new DbxClientV2(requestConfig, authToken);
  }

  @Override
  protected String readEncryptionToken() throws IOException {
    final String resourcePath = basePath + "/" + ENCRYPTION_TOKEN_FILE_NAME;
    if (!exists(resourcePath)) {
      return null;
    }
    return new StreamReader(getInputStream(resourcePath)).read();
  }

  @Override
  public void withAccount(Account account) throws IOException {
    setAccountUuid(account);
    String accountPath = getAccountPath();
    requireFolder(accountPath);
    String metadataPath = getResourcePath(getAccountMetadataFilename());
    if (!exists(metadataPath)) {
      saveFileContentsToAccountDir(null, getAccountMetadataFilename(), buildMetadata(account), getMimeTypeForData(), true);
      createWarningFile();
    }
  }

  @Override
  protected void writeAccount(Account account, boolean update) throws IOException {
    String metadataPath = getResourcePath(getAccountMetadataFilename());
    if (update || !exists(metadataPath)) {
      saveFileContentsToAccountDir(null, getAccountMetadataFilename(), buildMetadata(account), getMimeTypeForData(), true);
      if (!update) {
        createWarningFile();
      }
    }
  }

  @Override
  public Exceptional<AccountMetaData> readAccountMetaData() {
    return getAccountMetaDataFromPath(getResourcePath(getAccountMetadataFilename()));
  }

  private boolean exists(String path) throws IOException {
    try {
      return exists(mDbxClient, path);
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  private void requireFolder(String path) throws IOException {
    if (!exists(path)) {
      try {
        mDbxClient.files().createFolderV2(path);
      } catch (DbxException e) {
        throw new IOException(e);
      }
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
    if (exists(lockFilePath)) {
      return new StreamReader(getInputStream(lockFilePath)).read();
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
    saveInputStream(getLockFilePath(), toInputStream(lockToken, false));
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
  public Optional<ChangeSet> getChangeSetSince(SequenceNumber sequenceNumber, Context context) throws IOException {
    List<ChangeSet> changeSetList = new ArrayList<>();
    for (Pair<Integer, Metadata> integerMetadataPair: filterMetadata(sequenceNumber)) {
      changeSetList.add(getChangeSetFromMetadata(integerMetadataPair));
    }
    return merge(changeSetList);
  }

  @NonNull
  private ChangeSet getChangeSetFromMetadata(Pair<Integer, Metadata> metadata) throws IOException {
    return getChangeSetFromInputStream(new SequenceNumber(metadata.first, getSequenceFromFileName(metadata.second.getName())),
        getInputStream(metadata.second.getPathLower()));
  }

  private List<Pair<Integer, Metadata>> filterMetadata(SequenceNumber sequenceNumber) throws IOException {
    final String accountPath = getAccountPath();
    String shardPath = sequenceNumber.shard == 0 ? accountPath : accountPath + "/_" + sequenceNumber.shard;
    try {
      final List<Pair<Integer, Metadata>> entries = Stream.of(mDbxClient.files().listFolder(shardPath).getEntries())
          .filter(metadata -> isNewerJsonFile(sequenceNumber.number, metadata.getName()))
          .map(metadata -> Pair.create(sequenceNumber.shard, metadata)).collect(Collectors.toList());
      int nextShard = sequenceNumber.shard + 1;
      while (true) {
        final String nextShardPath = accountPath + "/_" + nextShard;
        if (exists(nextShardPath)) {
          int finalNextShard = nextShard;
          Stream.of(mDbxClient.files().listFolder(nextShardPath).getEntries())
              .filter(metadata -> isNewerJsonFile(0, metadata.getName()))
              .map(metadata -> Pair.create(finalNextShard, metadata)).forEach(entries::add);
          nextShard++;
        } else {
          break;
        }
      }
      return entries;
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
  public InputStream getInputStreamForBackup(String backupFile) throws IOException {
    return getInputStream(getBackupPath() + "/" + backupFile);
  }

  @Override
  public void storeBackup(Uri uri, String fileName) throws IOException {
    String backupPath = getBackupPath();
    requireFolder(backupPath);
    saveUriToFolder(fileName, uri, backupPath, false);
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    saveUriToFolder(fileName, uri, getAccountPath(), true);
  }

  private void saveUriToFolder(String fileName, Uri uri, String folder, boolean maybeEncrypt) throws IOException {
    InputStream in = MyApplication.getInstance().getContentResolver().openInputStream(uri);
    if (in == null) {
      throw new IOException("Could not read " + uri.toString());
    }
    String finalFileName = getLastFileNamePart(fileName);
    saveInputStream(folder + "/" + finalFileName, maybeEncrypt ? maybeEncrypt(in) : in);
  }

  @Override
  protected SequenceNumber getLastSequence(SequenceNumber start) throws IOException {
    final Comparator<Metadata> resourceComparator = (o1, o2) -> Utils.compare(getSequenceFromFileName(o1.getName()), getSequenceFromFileName(o2.getName()));
    try {
      final String accountPath = getAccountPath();
      final List<Metadata> mainEntries = mDbxClient.files().listFolder(accountPath).getEntries();
      Optional<Metadata> lastShardOptional =
          Stream.of(mainEntries)
              .filter(metadata -> metadata instanceof FolderMetadata && isAtLeastShardDir(start.shard, metadata.getName()))
              .max(resourceComparator);
      List<Metadata> lastShard;
      int lastShardInt, reference;
      if (lastShardOptional.isPresent()) {
        final String lastShardName = lastShardOptional.get().getName();
        lastShard = mDbxClient.files().listFolder(accountPath + "/" + lastShardName).getEntries();
        lastShardInt = getSequenceFromFileName(lastShardName);
        reference = lastShardInt == start.shard ? start.number : 0;
      } else {
        if (start.shard > 0) return start;
        lastShard = mainEntries;
        lastShardInt = 0;
        reference = start.number;
      }
      return Stream.of(lastShard)
          .filter(metadata -> isNewerJsonFile(reference, metadata.getName()))
          .max(resourceComparator)
          .map(metadata -> new SequenceNumber(lastShardInt, getSequenceFromFileName(metadata.getName())))
          .orElse(start);
    } catch (DbxException e) {
      throw new IOException(e);
    }
  }

  @Override
  void saveFileContentsToAccountDir(String folder, String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    String path;
    final String accountPath = getAccountPath();
    if (folder == null) {
      path = accountPath;
    } else {
      path = accountPath + "/" + folder;
      requireFolder(path);
    }
    saveInputStream(path + "/" + fileName, toInputStream(fileContents, maybeEncrypt));
  }

  @Override
  void saveFileContentsToBase(String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    saveInputStream(basePath + "/" + fileName, toInputStream(fileContents, maybeEncrypt));
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
  public Stream<Exceptional<AccountMetaData>> getRemoteAccountList() throws IOException  {
    Stream<Exceptional<AccountMetaData>> result;
    try {
      result = Stream.of(mDbxClient.files().listFolder(basePath).getEntries())
          .filter(metadata -> metadata instanceof FolderMetadata)
          .filter(metadata -> !metadata.getName().equals(BACKUP_FOLDER_NAME))
          .map(metadata -> basePath + "/" + metadata.getName() + "/" + getAccountMetadataFilename())
          .filter(accountMetadataPath -> {
            try {
              mDbxClient.files().getMetadata(accountMetadataPath);
              return true;
            } catch (DbxException e) {
              return false;
            }
          })
          .map(this::getAccountMetaDataFromPath);
    } catch (DbxException e) {
      throw new IOException(e);
    }
    return result;
  }

  private Exceptional<AccountMetaData> getAccountMetaDataFromPath(String path) {
    try {
      return getAccountMetaDataFromInputStream(getInputStream(path));
    } catch (IOException e) {
      return Exceptional.of(e);
    }
  }

  @NonNull
  @Override
  public List<String> getStoredBackups() throws IOException {
    try {
      return Stream.of(mDbxClient.files().listFolder(getBackupPath()).getEntries())
          .map(Metadata::getName)
          .toList();
    } catch (DbxException ignored) {
    }
    return new ArrayList<>();
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "webdav_backend";
  }

  @Override
  public boolean isAuthException(Exception e) {
    return Utils.getCause(e) instanceof InvalidAccessTokenException;
  }
}
