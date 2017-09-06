package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.FileCopyUtils;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Result;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import dagger.internal.Preconditions;
import timber.log.Timber;

import static org.totschnig.myexpenses.sync.SyncAdapter.LOCK_TIMEOUT_MINUTES;

abstract class AbstractSyncBackendProvider implements SyncBackendProvider {
  protected static final String KEY_LOCK_TOKEN = "lockToken";
  static final String BACKUP_FOLDER_NAME = "BACKUPS";
  static final String MIMETYPE_JSON = "application/json";
  static final String ACCOUNT_METADATA_FILENAME = "metadata.json";
  private static final Pattern FILE_PATTERN = Pattern.compile("_\\d+");
  private static final String KEY_OWNED_BY_US = "ownedByUs";
  private static final String KEY_TIMESTAMP = "timestamp";
  private static final long LOCK_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(LOCK_TIMEOUT_MINUTES);
  protected SharedPreferences sharedPreferences;
  private Gson gson;
  private Context context;
  @Nullable
  private String appInstance;

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

  @NonNull
  protected abstract String getSharedPreferencesName();

  @Override
  public Result setUp() {
   return Result.SUCCESS;
  }

  @Override
  public void tearDown() {
  }

  ChangeSet getChangeSetFromInputStream(long sequenceNumber, InputStream inputStream)
      throws IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    List<TransactionChange> changes = Utils.getChanges(gson, reader);
    if (changes == null || changes.size() == 0) {
      return ChangeSet.failed;
    }
    for (ListIterator<TransactionChange> iterator = changes.listIterator(); iterator.hasNext(); ) {
      TransactionChange transactionChange = iterator.next();
      if (transactionChange.isEmpty()) {
        Timber.w("found empty transaction change in json");
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
      FileCopyUtils.copy(getInputStreamForPicture(transactionChange.pictureUri()),
          MyApplication.getInstance().getContentResolver()
              .openOutputStream(homeUri));
      return transactionChange.toBuilder().setPictureUri(homeUri.toString()).build();
    }
    return transactionChange;
  }

  @NonNull
  protected abstract InputStream getInputStreamForPicture(String relativeUri) throws IOException;

  Optional<AccountMetaData> getAccountMetaDataFromInputStream(InputStream inputStream) {
    try {
      return Optional.of(gson.fromJson(
          new BufferedReader(new InputStreamReader(inputStream)), AccountMetaData.class));
    } catch (Exception e) {
      AcraHelper.report(e);
      return Optional.empty();
    }
  }

  boolean isNewerJsonFile(long sequenceNumber, String name) {
    String fileName = getNameWithoutExtension(name);
    String fileExtension = getFileExtension(name);
    return fileExtension.equals("json") && FILE_PATTERN.matcher(fileName).matches() &&
        Long.parseLong(fileName.substring(1)) > sequenceNumber;
  }

  protected Optional<ChangeSet> merge(Stream<ChangeSet> changeSetStream) {
    return changeSetStream.reduce(ChangeSet::merge);
  }

  @NonNull
  Long getSequenceFromFileName(String fileName) {
    return Long.parseLong(getNameWithoutExtension(fileName).substring(1));
  }

  //from Guava
  private String getNameWithoutExtension(String file) {
    Preconditions.checkNotNull(file);
    String fileName = new File(file).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  //from Guava
  String getFileExtension(String fullName) {
    Preconditions.checkNotNull(fullName);
    String fileName = new File(fullName).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
  }


  private TransactionChange mapPictureDuringWrite(TransactionChange transactionChange) throws IOException {
    if (transactionChange.pictureUri() != null) {
      String newUri = transactionChange.uuid() + "_" +
          Uri.parse(transactionChange.pictureUri()).getLastPathSegment();
      saveUriToAccountDir(newUri, Uri.parse(transactionChange.pictureUri()));
      return transactionChange.toBuilder().setPictureUri(newUri).build();
    } else {
      return transactionChange;
    }
  };

  @Override
  public long writeChangeSet(long lastSequenceNumber, List<TransactionChange> changeSet, Context context) throws IOException {
    long nextSequence = getLastSequence(lastSequenceNumber) + 1;
    for (int i = 0; i < changeSet.size(); i++) {
      TransactionChange mappedChange = mapPictureDuringWrite(changeSet.get(i));
      if (appInstance != null) {
        mappedChange = mappedChange.toBuilder().setAppInstance(appInstance).build();
      }
      if (mappedChange.splitParts() != null) {
        for (int j = 0; j < mappedChange.splitParts().size(); j++) {
          mappedChange.splitParts().set(j, mapPictureDuringWrite(mappedChange.splitParts().get(j)));
        }
      }
      changeSet.set(i, mappedChange);
    }
    String fileName = "_" + nextSequence + ".json";
    String fileContents = gson.toJson(changeSet);
    Timber.tag(SyncAdapter.TAG).i("Writing to %s", fileName);
    Timber.tag(SyncAdapter.TAG).i(fileContents);
    saveFileContents(fileName, fileContents, MIMETYPE_JSON);
    return nextSequence;
  }

  protected abstract void saveUriToAccountDir(String fileName, Uri uri) throws IOException;

  String buildMetadata(Account account) {
    return gson.toJson(AccountMetaData.from(account));
  }

  protected abstract long getLastSequence(long start) throws IOException;

  abstract void saveFileContents(String fileName, String fileContents, String mimeType) throws IOException;

  //from API 19 Long.compare
  int compareInt(long x, long y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  void createWarningFile() {
    try {
      saveFileContents("IMPORTANT_INFORMATION",
          MyApplication.getInstance().getString(R.string.warning_synchronization_folder_usage),
          "text/plain");
    } catch (IOException e) {
      AcraHelper.report(e);
    }
  }

  protected abstract String getExistingLockToken() throws IOException;

  protected abstract boolean writeLockToken(String lockToken) throws IOException;

  @Override
  public boolean lock() {
    try {
      String existingLockTocken = getExistingLockToken();
      Timber.i("ExistingLockTocken: %s", existingLockTocken);
      if (existingLockTocken == null || shouldOverrideLock(existingLockTocken)) {
        String lockToken = Model.generateUuid();
        if (!writeLockToken(lockToken)) {
          Timber.i("Write lock tocken failed");
          return false;
        }
        saveLockTokenToPreferences(lockToken, System.currentTimeMillis(), true);
        return true;
      }
    } catch (IOException e) {
      Timber.e(e);
    }
    return false;
  }

  private boolean shouldOverrideLock(String locktoken) {
    boolean result;
    long now = System.currentTimeMillis();
    String storedLockToken = sharedPreferences.getString(KEY_LOCK_TOKEN, "");
    boolean ownedByUs = sharedPreferences.getBoolean(KEY_OWNED_BY_US, false);
    long timestamp = sharedPreferences.getLong(KEY_TIMESTAMP, 0);
    long since = now - timestamp;
    Timber.i("Stored: %s, ownedByUs : %b, since: %d", storedLockToken, ownedByUs, since);
    if (locktoken.equals(storedLockToken)) {
      result = ownedByUs || since > LOCK_TIMEOUT_MILLIS;
      Timber.i("tokens are equal, result: %b", result);
    } else {
      saveLockTokenToPreferences(locktoken, now, false);
      result = false;
      Timber.i("tokens are not equal, result: %b", result);
    }
    return result;
  }

  private void saveLockTokenToPreferences(String locktoken, long timestamp, boolean ownedByUs) {
    sharedPreferences.edit().putString(KEY_LOCK_TOKEN, locktoken).putLong(KEY_TIMESTAMP, timestamp)
        .putBoolean(KEY_OWNED_BY_US, ownedByUs).commit();
  }

  Context getContext() {
    return context;
  }
}
