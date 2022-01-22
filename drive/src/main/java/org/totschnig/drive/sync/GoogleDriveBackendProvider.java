package org.totschnig.drive.sync;

import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.model.File;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SequenceNumber;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.io.StreamReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class GoogleDriveBackendProvider extends AbstractSyncBackendProvider {
  public static final String KEY_GOOGLE_ACCOUNT_EMAIL = "googleAccountEmail";
  private static final String ACCOUNT_METADATA_CURRENCY_KEY = "accountMetadataCurrency";
  private static final String ACCOUNT_METADATA_COLOR_KEY = "accountMetadataColor";
  private static final String ACCOUNT_METADATA_UUID_KEY = "accountMetadataUuid";
  private static final String ACCOUNT_METADATA_OPENING_BALANCE_KEY = "accountMetadataOpeningBalance";
  private static final String ACCOUNT_METADATA_DESCRIPTION_KEY = "accountMetadataDescription";
  private static final String ACCOUNT_METADATA_TYPE_KEY = "accountMetadataType";
  private static final String LOCK_TOKEN_KEY = KEY_LOCK_TOKEN;
  private static final String IS_BACKUP_FOLDER = "isBackupFolder";
  public static final String IS_SYNC_FOLDER = "isSyncFolder";
  private final String folderId;
  private File baseFolder, accountFolder;

  private final DriveServiceHelper driveServiceHelper;

  GoogleDriveBackendProvider(Context context, android.accounts.Account account, AccountManager accountManager) throws SyncParseException {
    super(context);
    folderId = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL);
    if (folderId == null) {
      throw new SyncParseException("Drive folder not set");
    }
    try {
      driveServiceHelper = new DriveServiceHelper(context, accountManager.getUserData(account, KEY_GOOGLE_ACCOUNT_EMAIL));
    } catch (Exception e) {
      throw new SyncParseException(e);
    }
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "google_drive_backend";
  }

  @Override
  protected String readEncryptionToken() throws IOException {
    requireBaseFolder();
    try {
      return new StreamReader(getInputStream(baseFolder, ENCRYPTION_TOKEN_FILE_NAME)).read();
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  public void setUp(AccountManager accountManager, android.accounts.Account account, String encryptionPassword, boolean create) throws Exception {
    try {
      super.setUp(accountManager, account, encryptionPassword, create);
    } catch (UserRecoverableAuthIOException e) {
      throw new AuthException(e, e.getIntent());
    }
  }

  @Override
  protected boolean isEmpty() throws IOException {
    return driveServiceHelper.listChildren(baseFolder).isEmpty();
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return getInputStream(accountFolder, relativeUri);
  }

  @Override
  public InputStream getInputStreamForBackup(String backupFile) throws IOException {
    final File backupFolder = getBackupFolder(false);
    if (backupFolder != null) {
      return getInputStream(backupFolder, backupFile);
    } else {
      throw new IOException("No backup folder found");
    }
  }

  private InputStream getInputStream(File folder, String title) throws IOException {
    return driveServiceHelper.downloadFile(folder, title);
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    saveUriToFolder(fileName, uri, accountFolder, true);
  }

  private void saveUriToFolder(String fileName, Uri uri, File driveFolder, boolean maybeEncrypt) throws IOException {
    InputStream in = getContext().getContentResolver().openInputStream(uri);
    if (in == null) {
      throw new IOException("Could not read " + uri.toString());
    }
    saveInputStream(fileName, maybeEncrypt ? maybeEncrypt(in) : in, getMimeType(fileName), driveFolder);
    in.close();
  }

  @Override
  public void storeBackup(Uri uri, String fileName) throws IOException {
    saveUriToFolder(fileName, uri, getBackupFolder(true), false);
  }

  @NonNull
  @Override
  public List<String> getStoredBackups() throws IOException {
    List<String> result = new ArrayList<>();
    final File backupFolder = getBackupFolder(false);
    if (backupFolder != null) {
      result = Stream.of(driveServiceHelper.listChildren(backupFolder))
          .map(File::getName)
          .toList();
    }
    return result;
  }

  @Override
  protected SequenceNumber getLastSequence(SequenceNumber start) throws IOException {
    final Comparator<File> resourceComparator = (o1, o2) -> Utils.compare(getSequenceFromFileName(o1.getName()), getSequenceFromFileName(o2.getName()));

    Optional<File> lastShardOptional =
        Stream.of(driveServiceHelper.listFolders(accountFolder))
            .filter(file -> isAtLeastShardDir(start.getShard(), file.getName()))
            .max(resourceComparator);

    List<File> lastShard;
    int lastShardInt, reference;
    if (lastShardOptional.isPresent()) {
      lastShard = driveServiceHelper.listChildren(lastShardOptional.get());
      lastShardInt = getSequenceFromFileName(lastShardOptional.get().getName());
      reference = lastShardInt == start.getShard() ? start.getNumber() : 0;
    } else {
      if (start.getShard() > 0) return start;
      lastShard = driveServiceHelper.listChildren(accountFolder);
      lastShardInt = 0;
      reference = start.getNumber();
    }
    return Stream.of(lastShard)
        .filter(metadata -> isNewerJsonFile(reference, metadata.getName()))
        .max(resourceComparator)
        .map(metadata -> new SequenceNumber(lastShardInt, getSequenceFromFileName(metadata.getName())))
        .orElse(start);
  }

  @Override
  protected void saveFileContentsToBase(String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    saveFileContents(baseFolder, fileName, fileContents, mimeType, maybeEncrypt);
  }

  @Override
  protected void saveFileContentsToAccountDir(String folder, String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    File driveFolder;
    if (folder == null) {
      driveFolder = accountFolder;
    } else {
      driveFolder = getSubFolder(folder);
      if (driveFolder == null) {
        driveFolder = driveServiceHelper.createFolder(accountFolder.getId(), folder, null);
      }
    }
    saveFileContents(driveFolder, fileName, fileContents, mimeType, maybeEncrypt);
  }

  private void saveFileContents(File driveFolder, String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    InputStream contents = toInputStream(fileContents, maybeEncrypt);
    saveInputStream(fileName, contents, mimeType, driveFolder);
    contents.close();
  }

  @Override
  protected String getExistingLockToken() {
    final Map<String, String> appProperties = accountFolder.getAppProperties();
    return appProperties != null ? appProperties.get(LOCK_TOKEN_KEY) : null;
  }

  @Override
  protected void writeLockToken(String lockToken) throws IOException {
    driveServiceHelper.setMetadataProperty(accountFolder.getId(), LOCK_TOKEN_KEY, lockToken);
  }

  private void saveInputStream(String fileName, InputStream contents, String mimeType, File driveFolder) throws IOException {
    File file = driveServiceHelper.createFile(driveFolder.getId(), fileName, mimeType, null);
    driveServiceHelper.saveFile(file.getId(), mimeType, contents);
  }

  @Override
  public void withAccount(Account account) throws IOException {
    setAccountUuid(account);
    writeAccount(account, false);
  }

  @Override
  protected void writeAccount(Account account, boolean update) throws IOException {
    accountFolder = getExistingAccountFolder(account.getUuid());
    if (update || accountFolder == null ) {
      if (accountFolder == null) {
        accountFolder = driveServiceHelper.createFolder(baseFolder.getId(), accountUuid, null);
        createWarningFile();
      }
      saveFileContentsToAccountDir(null, getAccountMetadataFilename(), buildMetadata(account), getMimeTypeForData(), true);
    }
  }

  @Override
  public Exceptional<AccountMetaData> readAccountMetaData() {
    return getAccountMetaDataFromDriveMetadata(accountFolder);
  }

  @Override
  public void resetAccountData(String uuid) throws IOException {
    File existingAccountFolder = getExistingAccountFolder(uuid);
    if (existingAccountFolder != null) {
      driveServiceHelper.delete(existingAccountFolder.getId());
    }
  }

  @Override
  @NonNull
  public Optional<ChangeSet> getChangeSetSince(SequenceNumber sequenceNumber, Context context) throws IOException {
    File shardFolder;
    if (sequenceNumber.getShard() == 0) {
      shardFolder = accountFolder;
    } else {
      shardFolder = getSubFolder("_" + sequenceNumber.getShard());
      if (shardFolder == null) throw new IOException("shard folder not found");
    }
    List<File> fileList = driveServiceHelper.listChildren(shardFolder);

    log().i("Getting data from shard %d", sequenceNumber.getShard());
    List<ChangeSet> changeSetList = new ArrayList<>();
    for (File metadata : fileList) {
      if (isNewerJsonFile(sequenceNumber.getNumber(), metadata.getName())) {
        log().i("Getting data from file %s", metadata.getName());
        changeSetList.add(getChangeSetFromMetadata(sequenceNumber.getShard(), metadata));
      }
    }
    int nextShard = sequenceNumber.getShard() + 1;
    while (true) {
      File nextShardFolder = getSubFolder("_" + nextShard);
      if (nextShardFolder != null) {
        fileList = driveServiceHelper.listChildren(nextShardFolder);
        log().i("Getting data from shard %d", nextShard);
        for (File metadata : fileList) {
          if (isNewerJsonFile(0, metadata.getName())) {
            log().i("Getting data from file %s", metadata.getName());
            changeSetList.add(getChangeSetFromMetadata(nextShard, metadata));
          }
        }
        nextShard++;
      } else {
        break;
      }
    }
    return merge(changeSetList);
  }

  @Nullable
  private File getSubFolder(String shard) throws IOException {
    return driveServiceHelper.getFileByNameAndParent(accountFolder, shard);
  }

  private ChangeSet getChangeSetFromMetadata(int shard, File metadata) throws IOException {
    return getChangeSetFromInputStream(new SequenceNumber(shard, getSequenceFromFileName(metadata.getName())),
        driveServiceHelper.read(metadata.getId()));
  }

  @Override
  public void unlock() throws IOException {
    driveServiceHelper.setMetadataProperty(accountFolder.getId(), LOCK_TOKEN_KEY, null);
  }

  @NonNull
  @Override
  public Stream<Exceptional<AccountMetaData>> getRemoteAccountStream() throws IOException {
    requireBaseFolder();
    List<File> fileList = driveServiceHelper.listChildren(baseFolder);
    return Stream.of(fileList)
        .filter(driveServiceHelper::isFolder)
        .filter(metadata -> !metadata.getName().equals(BACKUP_FOLDER_NAME))
        .map(this::getAccountMetaDataFromDriveMetadata);
  }

  private Exceptional<AccountMetaData> getAccountMetaDataFromDriveMetadata(File metadata) {
    File accountMetadata;
    try {
      accountMetadata = driveServiceHelper.getFileByNameAndParent(metadata, getAccountMetadataFilename());
    } catch (IOException e) {
      return Exceptional.of(e);
    }
    if (accountMetadata != null) {
      try (InputStream inputStream = driveServiceHelper.read(accountMetadata.getId())) {
        return getAccountMetaDataFromInputStream(inputStream);
      } catch (IOException e) {
        return Exceptional.of(e);
      }
    }

    //legacy
    final Map<String, String> appProperties = metadata.getAppProperties();
    if (appProperties == null) {
      return Exceptional.of(new Exception("appProperties are null"));
    }
    String uuid = appProperties.get(ACCOUNT_METADATA_UUID_KEY);
    if (uuid == null) {
      Timber.d("UUID property not set");
      return Exceptional.of(new Exception("UUID property not set"));
    }
    return Exceptional.of(() -> AccountMetaData.builder()
        .setType(getPropertyWithDefault(appProperties, ACCOUNT_METADATA_TYPE_KEY, AccountType.CASH.name()))
        .setOpeningBalance(getPropertyWithDefault(appProperties, ACCOUNT_METADATA_OPENING_BALANCE_KEY, 0L))
        .setDescription(getPropertyWithDefault(appProperties, ACCOUNT_METADATA_DESCRIPTION_KEY, ""))
        .setColor(getPropertyWithDefault(appProperties, ACCOUNT_METADATA_COLOR_KEY, Account.DEFAULT_COLOR))
        .setCurrency(getPropertyWithDefault(appProperties, ACCOUNT_METADATA_CURRENCY_KEY,
            Utils.getHomeCurrency().getCode()))
        .setUuid(uuid)
        .setLabel(metadata.getName()).build());
  }

  private String getPropertyWithDefault(Map<String, String> metadata,
                                        String key,
                                        String defaultValue) {
    String result = metadata.get(key);
    return result != null ? result : defaultValue;
  }

  private long getPropertyWithDefault(Map<String, String> metadata,
                                      String key,
                                      long defaultValue) {
    String result = metadata.get(key);
    return result != null ? Long.parseLong(result) : defaultValue;
  }

  private int getPropertyWithDefault(Map<String, String> metadata,
                                     String key,
                                     int defaultValue) {
    String result = metadata.get(key);
    return result != null ? Integer.parseInt(result) : defaultValue;
  }

  private boolean getPropertyWithDefault(Map<String, String> metadata,
                                         String key,
                                         boolean defaultValue) {
    String result = metadata.get(key);
    return result != null ? Boolean.parseBoolean(result) : defaultValue;
  }

  @Nullable
  private File getBackupFolder(boolean require) throws IOException {
    requireBaseFolder();
    File file = driveServiceHelper.getFileByNameAndParent(baseFolder, BACKUP_FOLDER_NAME);
    if (file != null && file.getAppProperties() != null && getPropertyWithDefault(file.getAppProperties(), IS_BACKUP_FOLDER, false)) {
      return file;
    }
    if (require) {
      Map<String, String> properties = new HashMap<>();
      properties.put(IS_BACKUP_FOLDER, "true");
      return driveServiceHelper.createFolder(baseFolder.getId(), BACKUP_FOLDER_NAME, properties);
    }
    return null;
  }

  private File getExistingAccountFolder(String uuid) throws IOException {
    requireBaseFolder();
    return driveServiceHelper.getFileByNameAndParent(baseFolder, uuid);

  }

  private void requireBaseFolder() throws IOException {
    if (baseFolder == null) {
      baseFolder = driveServiceHelper.getFile(folderId);
    }
  }

}
