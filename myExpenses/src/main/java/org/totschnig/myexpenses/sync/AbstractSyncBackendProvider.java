package org.totschnig.myexpenses.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crypt.EncryptionHelper;
import org.totschnig.myexpenses.util.io.FileCopyUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.internal.Preconditions;
import timber.log.Timber;

import static org.totschnig.myexpenses.sync.SyncAdapter.LOCK_TIMEOUT_MINUTES;
import static org.totschnig.myexpenses.sync.json.Utils.getChanges;

abstract class AbstractSyncBackendProvider implements SyncBackendProvider {
  static final String KEY_LOCK_TOKEN = "lockToken";
  static final String BACKUP_FOLDER_NAME = "BACKUPS";
  private static final String MIME_TYPE_JSON = "application/json";
  private static final String ACCOUNT_METADATA_FILENAME = "metadata";
  protected static final Pattern FILE_PATTERN = Pattern.compile("_\\d+");
  private static final String KEY_OWNED_BY_US = "ownedByUs";
  private static final String KEY_TIMESTAMP = "timestamp";
  private static final long LOCK_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(LOCK_TIMEOUT_MINUTES);
  protected static final String ENCRYPTION_TOKEN_FILE_NAME = "ENCRYPTION_TOKEN";
  private static final String MIME_TYPE_OCTET_STREAM = "application/octet-stream";

  /**
   * this holds the uuid of the db account which data is currently synced
   */
  protected String accountUuid;
  SharedPreferences sharedPreferences;
  private Gson gson;
  private Context context;
  @Nullable
  private String appInstance;
  @Nullable
  private String encryptionPassword;

  AbstractSyncBackendProvider(Context context) {
    this.context = context;
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
    if (BuildConfig.DEBUG) {
      appInstance = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    sharedPreferences = context.getSharedPreferences(getSharedPreferencesName(), 0);
  }

  public String getMimeTypeForData() {
    return isEncrypted() ? MIME_TYPE_JSON : MIME_TYPE_OCTET_STREAM;
  }

  protected boolean isEncrypted() {
    return encryptionPassword != null;
  }

  public String getAccountMetadataFilename() {
    return String.format("%s.%s", ACCOUNT_METADATA_FILENAME, getExtensionForData());
  }

  private String getExtensionForData() {
    return isEncrypted() ? "enc" : "json";
  }

  public void setAccountUuid(Account account) {
    this.accountUuid = account.getUuid();
  }

  @NonNull
  protected abstract String getSharedPreferencesName();

  @Override
  public void initEncryption() throws GeneralSecurityException, IOException {
    saveFileContentsToBase(ENCRYPTION_TOKEN_FILE_NAME,
        encrypt(EncryptionHelper.generateRandom(10)), MIME_TYPE_OCTET_STREAM, false);
  }

  @Override
  public Exceptional<Void> setUp(@Nullable String authToken, @Nullable String encryptionPassword, boolean create) {
    this.encryptionPassword = encryptionPassword;
    try {
      String encryptionToken = readEncryptionToken();
      if (encryptionToken == null) {
        if (encryptionPassword != null) {
          if (create && isEmpty()) {
            initEncryption();
          } else {
            return Exceptional.of(EncryptionException.notEncrypted(context));
          }
        }
      } else {
        if (encryptionPassword == null) {
          return Exceptional.of(EncryptionException.encrypted(context));
        } else {
          try {
            decrypt(encryptionToken);
          } catch (GeneralSecurityException e) {
            return Exceptional.of(EncryptionException.wrongPassphrase(context));
          }
        }
      }
      return Exceptional.of(() -> null);
    } catch (IOException | GeneralSecurityException e) {
      return Exceptional.of(e);
    }
  }

  protected abstract boolean isEmpty() throws IOException;

  private String decrypt(String input) throws GeneralSecurityException {
    return new String(EncryptionHelper.decrypt(Base64.decode(input, Base64.DEFAULT), encryptionPassword));
  }

  protected abstract String readEncryptionToken() throws IOException;

  protected String encrypt(byte[] plain) throws GeneralSecurityException {
    return Base64.encodeToString(EncryptionHelper.encrypt(plain, encryptionPassword), Base64.DEFAULT);
  }

  protected OutputStream maybeEncrypt(OutputStream outputStream) throws IOException {
    try {
      return isEncrypted() ? EncryptionHelper.encrypt(outputStream, encryptionPassword) : outputStream;
    } catch (GeneralSecurityException e) {
      throw new IOException(e);
    }
  }

  protected InputStream maybeEncrypt(InputStream inputStream) throws IOException {
    try {
      return isEncrypted() ? EncryptionHelper.encrypt(inputStream, encryptionPassword) : inputStream;
    } catch (GeneralSecurityException e) {
      throw new IOException(e);
    }
  }

  protected InputStream maybeDecrypt(InputStream inputStream) throws IOException {
    try {
      return isEncrypted() ? EncryptionHelper.decrypt(inputStream, encryptionPassword) : inputStream;
    } catch (GeneralSecurityException e) {
      throw new IOException(e);
    }
  }

  protected InputStream toInputStream(String fileContents, boolean maybeEncrypt) throws IOException {
    final InputStream inputStream = new ByteArrayInputStream(fileContents.getBytes());
    return maybeEncrypt ? maybeEncrypt(inputStream) : inputStream;
  }

  @Override
  public void tearDown() {
  }

  @Override
  public boolean isAuthException(Exception e) {
    return false;
  }

  @NonNull
  ChangeSet getChangeSetFromInputStream(SequenceNumber sequenceNumber, InputStream inputStream)
      throws IOException {
    List<TransactionChange> changes;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(maybeDecrypt(inputStream)))) {
      changes = getChanges(gson, reader);
    }
    if (changes == null || changes.size() == 0) {
      return ChangeSet.empty(sequenceNumber);
    }
    for (ListIterator<TransactionChange> iterator = changes.listIterator(); iterator.hasNext(); ) {
      TransactionChange transactionChange = iterator.next();
      if (transactionChange.isEmpty()) {
        log().w("found empty transaction change in json");
        iterator.remove();
      } else {
        iterator.set(mapPictureDuringRead(transactionChange));
        if (transactionChange.splitParts() != null) {
          for (ListIterator<TransactionChange> jterator = transactionChange.splitParts().listIterator();
               jterator.hasNext(); ) {
            TransactionChange splitPart = jterator.next();
            jterator.set(mapPictureDuringRead(splitPart));
          }
        }
      }
    }
    return ChangeSet.create(sequenceNumber, changes);
  }

  private TransactionChange mapPictureDuringRead(TransactionChange transactionChange) throws IOException {
    if (transactionChange.pictureUri() != null) {
      Uri homeUri = PictureDirHelper.getOutputMediaUri(false);
      if (homeUri == null) {
        throw new IOException("Unable to write picture");
      }
      InputStream input = getInputStreamForPicture(transactionChange.pictureUri());
      OutputStream output = MyApplication.getInstance().getContentResolver()
          .openOutputStream(homeUri);
      if (output == null) {
        throw new IOException("Unable to write picture");
      }
      FileCopyUtils.copy(maybeDecrypt(input), output);
      input.close();
      output.close();
      return transactionChange.toBuilder().setPictureUri(homeUri.toString()).build();
    }
    return transactionChange;
  }

  @NonNull
  protected abstract InputStream getInputStreamForPicture(String relativeUri) throws IOException;

  Exceptional<AccountMetaData> getAccountMetaDataFromInputStream(InputStream inputStream) {
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(maybeDecrypt(inputStream)))) {
      final AccountMetaData accountMetaData = gson.fromJson(bufferedReader, AccountMetaData.class);
      if (accountMetaData == null) {
        throw new IOException("accountMetaData not found in input stream");
      }
      return Exceptional.of(() -> accountMetaData);
    } catch (Exception e) {
      log().e(e);
      return Exceptional.of(e);
    }
  }

  boolean isAtLeastShardDir(int shardNumber, String name) {
    return FILE_PATTERN.matcher(name).matches() &&
        Integer.parseInt(name.substring(1)) >= shardNumber;
  }

  boolean isNewerJsonFile(int sequenceNumber, String name) {
    String fileName = getNameWithoutExtension(name);
    String fileExtension = getFileExtension(name);
    return fileExtension.equals(getExtensionForData()) && FILE_PATTERN.matcher(fileName).matches() &&
        Integer.parseInt(fileName.substring(1)) > sequenceNumber;
  }

  protected Optional<ChangeSet> merge(List<ChangeSet> changeSetStream) {
    return Stream.of(changeSetStream).reduce(ChangeSet::merge);
  }

  int getSequenceFromFileName(String fileName) {
    return Integer.parseInt(getNameWithoutExtension(fileName).substring(1));
  }

  //from Guava
  private String getNameWithoutExtension(String file) {
    Preconditions.checkNotNull(file);
    String fileName = new File(file).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  //from Guava
  private String getFileExtension(String fullName) {
    Preconditions.checkNotNull(fullName);
    String fileName = new File(fullName).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
  }


  private TransactionChange mapPictureDuringWrite(TransactionChange transactionChange) throws IOException {
    if (transactionChange.pictureUri() != null) {
      String newUri = String.format("%s_%s%s", transactionChange.uuid(),
          Uri.parse(transactionChange.pictureUri()).getLastPathSegment(),
          isEncrypted() ? ".enc" : "");
      try {
        saveUriToAccountDir(newUri, Uri.parse(transactionChange.pictureUri()));
      } catch (IOException e) {
        if (e instanceof FileNotFoundException) {
          newUri = null;
          log().e(e, "Picture was deleted, %s", transactionChange.pictureUri());
        } else {
          throw e;
        }
      }
      return transactionChange.toBuilder().setPictureUri(newUri).build();
    }
    return transactionChange;
  }

  @NonNull
  @Override
  public SequenceNumber writeChangeSet(SequenceNumber lastSequenceNumber, List<TransactionChange> changeSet, Context context) throws IOException {
    ArrayList<TransactionChange> changeSetMutable = new ArrayList<>(changeSet);
    SequenceNumber nextSequence = getLastSequence(lastSequenceNumber).next();
    for (int i = 0; i < changeSetMutable.size(); i++) {
      TransactionChange mappedChange = mapPictureDuringWrite(changeSetMutable.get(i));
      if (appInstance != null) {
        mappedChange = mappedChange.toBuilder().setAppInstance(appInstance).build();
      }
      if (mappedChange.splitParts() != null) {
        ArrayList<TransactionChange> splitPartsMutable = new ArrayList<>(mappedChange.splitParts());
        for (int j = 0; j < splitPartsMutable.size(); j++) {
          splitPartsMutable.set(j, mapPictureDuringWrite(splitPartsMutable.get(j)));
        }
        mappedChange = mappedChange.toBuilder().setSplitParts(splitPartsMutable).build();
      }
      changeSetMutable.set(i, mappedChange);
    }
    String fileName = String.format(Locale.ROOT, "_%d.%s", nextSequence.number, getExtensionForData());
    String fileContents = gson.toJson(changeSetMutable);
    log().i("Writing to %s", fileName);
    log().i(fileContents);
    saveFileContentsToAccountDir(nextSequence.shard == 0 ? null : "_" + nextSequence.shard, fileName, fileContents, getMimeTypeForData(), true);
    return nextSequence;
  }

  /**
   * should encrypt if backend is configured with encryption
   */
  protected abstract void saveUriToAccountDir(String fileName, Uri uri) throws IOException;

  String buildMetadata(Account account) {
    return gson.toJson(AccountMetaData.from(account));
  }

  @NonNull
  String getMimeType(String fileName) {
    String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(fileName));
    return result != null ? result : MIME_TYPE_OCTET_STREAM;
  }

  String getLastFileNamePart(String fileName) {
    return fileName.contains("/") ?
        StringUtils.substringAfterLast(fileName, "/") : fileName;
  }

  protected abstract SequenceNumber getLastSequence(SequenceNumber start) throws IOException;

  abstract void saveFileContentsToAccountDir(@Nullable String folder, String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException;

  abstract void saveFileContentsToBase(String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException;

  void createWarningFile() {
    try {
      saveFileContentsToAccountDir(null, "IMPORTANT_INFORMATION.txt",
          Utils.getTextWithAppName(context, R.string.warning_synchronization_folder_usage).toString(),
          "text/plain", false);
    } catch (IOException e) {
      log().w(e);
    }
  }

  protected abstract String getExistingLockToken() throws IOException;

  protected abstract void writeLockToken(String lockToken) throws IOException;

  @Override
  public void updateAccount(Account account) throws IOException {
    writeAccount(account, true);
  }

  protected abstract void writeAccount(Account account, boolean update) throws IOException;

  @Override
  public void lock() throws IOException {
    String existingLockToken = getExistingLockToken();
    log().i("ExistingLockToken: %s", existingLockToken);
    if (TextUtils.isEmpty(existingLockToken) || shouldOverrideLock(existingLockToken)) {
      String lockToken = Model.generateUuid();
      writeLockToken(lockToken);
      saveLockTokenToPreferences(lockToken, System.currentTimeMillis(), true);
    } else {
      throw new IOException("Backend cannot be locked");
    }
  }

  private boolean shouldOverrideLock(String lockToken) {
    boolean result;
    long now = System.currentTimeMillis();
    String storedLockToken = sharedPreferences.getString(accountPrefKey(KEY_LOCK_TOKEN), "");
    boolean ownedByUs = sharedPreferences.getBoolean(accountPrefKey(KEY_OWNED_BY_US), false);
    long timestamp = sharedPreferences.getLong(accountPrefKey(KEY_TIMESTAMP), 0);
    long since = now - timestamp;
    log().i("Stored: %s, ownedByUs : %b, since: %d", storedLockToken, ownedByUs, since);
    if (lockToken.equals(storedLockToken)) {
      result = ownedByUs || since > LOCK_TIMEOUT_MILLIS;
      log().i("tokens are equal, result: %b", result);
    } else {
      saveLockTokenToPreferences(lockToken, now, false);
      result = false;
      log().i("tokens are not equal, result: %b", false);
    }
    return result;
  }

  @SuppressLint("ApplySharedPref")
  private void saveLockTokenToPreferences(String lockToken, long timestamp, boolean ownedByUs) {
    sharedPreferences.edit().putString(accountPrefKey(KEY_LOCK_TOKEN), lockToken)
        .putLong(accountPrefKey(KEY_TIMESTAMP), timestamp)
        .putBoolean(accountPrefKey(KEY_OWNED_BY_US), ownedByUs).commit();
  }

  Context getContext() {
    return context;
  }

  private String accountPrefKey(String key) {
    return String.format(Locale.ROOT, "%s-%s", accountUuid, key);
  }

  @NonNull
  protected Timber.Tree log() {
    return Timber.tag(SyncAdapter.TAG);
  }
}
