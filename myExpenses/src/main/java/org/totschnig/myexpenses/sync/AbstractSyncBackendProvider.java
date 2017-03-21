package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dagger.internal.Preconditions;

abstract class AbstractSyncBackendProvider implements SyncBackendProvider {
  private static final String TAG = "AbstractSyncBackendP";
  static final String BACKUP_FOLDER_NAME = "BACKUPS";
  static final String MIMETYPE_JSON = "application/json";
  static final String ACCOUNT_METADATA_FILENAME = "metadata.json";
  private static final Pattern FILE_PATTERN = Pattern.compile("_\\d+");
  private Gson gson;

  AbstractSyncBackendProvider() {
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
  }

  @Override
  public boolean setUp() {
   return true;
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
    List<TransactionChange> changeSetRead = new ArrayList<>();
    for (TransactionChange transactionChange : changes) {
      if (transactionChange.isEmpty()) {
        Log.w(TAG,"found empty transaction change in changes table");
        continue;
      }
      if (transactionChange.pictureUri() != null) {
        changeSetRead.add(transactionChange.toBuilder()
            .setPictureUri(ingestPictureUri(transactionChange.pictureUri())).build());
      } else {
        changeSetRead.add(transactionChange);
      }
    }
    return ChangeSet.create(sequenceNumber, changeSetRead);
  }

  private String ingestPictureUri(String relativeUri) throws IOException {
    Uri homeUri = org.totschnig.myexpenses.util.Utils.getOutputMediaUri(false);
    if (homeUri == null) {
      throw new IOException("Unable to write picture");
    }
    FileCopyUtils.copy(getInputStreamForPicture(relativeUri),
        MyApplication.getInstance().getContentResolver()
        .openOutputStream(homeUri));
    return homeUri.toString();
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

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet, Context context) throws IOException {
    long nextSequence = getLastSequence() + 1;
    List<TransactionChange> changeSetToWrite = new ArrayList<>();
    for (TransactionChange transactionChange : changeSet) {
      if (transactionChange.pictureUri() != null) {
        String newUri = transactionChange.uuid() + "_" +
            Uri.parse(transactionChange.pictureUri()).getLastPathSegment();
        saveUriToAccountDir(newUri, Uri.parse(transactionChange.pictureUri()));
        changeSetToWrite.add(transactionChange.toBuilder().setPictureUri(newUri).build());
      } else {
        changeSetToWrite.add(transactionChange);
      }
    }
    saveFileContents("_" + nextSequence + ".json", gson.toJson(changeSetToWrite), MIMETYPE_JSON);
    return nextSequence;
  }

  protected abstract void saveUriToAccountDir(String fileName, Uri uri) throws IOException;

  String buildMetadata(Account account) {
    return gson.toJson(AccountMetaData.from(account));
  }

  protected abstract long getLastSequence() throws IOException;

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
}
