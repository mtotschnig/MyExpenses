package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.internal.Preconditions;

class LocalFileBackendProvider extends AbstractSyncBackendProvider {
  private File baseDir, accountDir;

  LocalFileBackendProvider(Context context, String filePath) {
    super(context);
    baseDir = new File(filePath);
    if (!baseDir.isDirectory()) {
      throw new RuntimeException("No directory " + filePath);
    }
  }

  @Override
  public boolean withAccount(Account account) {
    accountDir = new File(baseDir, account.uuid);
    //noinspection ResultOfMethodCallIgnored
    accountDir.mkdir();
    if (accountDir.isDirectory()) {
      File metaData = new File(accountDir, ACCOUNT_METADATA_FILENAME);
      if (!metaData.exists()) {
        try {
          saveFileContents(metaData, buildMetadata(account));
          createWarningFile();
        } catch (IOException e) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean resetAccountData(String uuid) {
    //we do not set the member, this needs to be done through withAccount
    File accountDir = new File(baseDir, uuid);
    if (accountDir.isDirectory()) {
      for (String file : accountDir.list()) {
        if (!(new File(accountDir, file).delete())) {
          return false;
        }
      }
    }
    return true;
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "local_file_backend"; // currently not used
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return new FileInputStream(new File(accountDir, relativeUri));
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    saveUriToFolder(fileName, uri, accountDir);
  }

  private void saveUriToFolder(String fileName, Uri uri, File folder) throws IOException {
    FileCopyUtils.copy(uri, Uri.fromFile(new File(folder, fileName)));
  }

  @Override
  public void storeBackup(Uri uri) throws IOException {
    File backupDir = new File(baseDir, BACKUP_FOLDER_NAME);
    //noinspection ResultOfMethodCallIgnored
    backupDir.mkdir();
    if (!backupDir.isDirectory()) {
      throw new IOException("Unable to create directory for backups");
    }
    saveUriToFolder(uri.getLastPathSegment(), uri, backupDir);
  }

  @NonNull
  @Override
  public List<String> getStoredBackups() throws IOException {
    String[] list = new File(baseDir, BACKUP_FOLDER_NAME).list();
    return list != null ? Arrays.asList(list) : new ArrayList<>();
  }

  @Override
  public InputStream getInputStreamForBackup(String backupFile) throws FileNotFoundException {
    return new FileInputStream(new File(new File(baseDir, BACKUP_FOLDER_NAME), backupFile));
  }

  @Override
  protected long getLastSequence(long start) {
    return Stream.of(filterFiles(start))
        .map(file -> getSequenceFromFileName(file.getName()))
        .max(this::compareInt)
        .orElse(start);
  }

  private File[] filterFiles(long sequenceNumber) {
    Preconditions.checkNotNull(accountDir);
    return accountDir.listFiles(file -> isNewerJsonFile(sequenceNumber, file.getName()));
  }

  @Override
  public boolean lock() {
    return true;
  }

  @NonNull
  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException {
    List<ChangeSet> changeSets = new ArrayList<>();
    for (File file: filterFiles(sequenceNumber)) {
      changeSets.add(getChangeSetFromFile(file));
    }
    return merge(Stream.of(changeSets)).orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getChangeSetFromFile(File file) throws IOException {
    return getChangeSetFromInputStream(getSequenceFromFileName(file.getName()), new FileInputStream(file));
  }

  private Optional<AccountMetaData> getAccountMetaDataFromFile(File file) {
    try {
      return getAccountMetaDataFromInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean unlock() {
    return true;
  }

  @Override
  public String toString() {
    return baseDir.toString();
  }

  @Override
  void saveFileContents(String fileName, String fileContents, String mimeType) throws IOException {
    Preconditions.checkNotNull(accountDir);
    saveFileContents(new File(accountDir, fileName), fileContents);
  }

  @Override
  protected String getExistingLockToken() throws IOException {
    return null;
  }

  @Override
  protected boolean writeLockToken(String lockToken) throws IOException {
    return false;
  }

  private void saveFileContents(File file, String fileContents) throws IOException {
    OutputStreamWriter out;
    out = new OutputStreamWriter(new FileOutputStream(file));
    out.write(fileContents);
    out.close();
  }

  @NonNull
  @Override
  public Stream<AccountMetaData> getRemoteAccountList() {
    return Stream.of(baseDir.listFiles(File::isDirectory))
        .map(directory -> new File(directory, ACCOUNT_METADATA_FILENAME))
        .filter(File::exists)
        .map(this::getAccountMetaDataFromFile)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }
}
