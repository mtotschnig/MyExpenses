package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.io.FileCopyUtils;
import org.totschnig.myexpenses.util.Utils;

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
  public void withAccount(Account account) throws IOException {
    setAccountUuid(account);
    accountDir = new File(baseDir, account.uuid);
    //noinspection ResultOfMethodCallIgnored
    accountDir.mkdir();
    if (accountDir.isDirectory()) {
      File metaData = new File(accountDir, ACCOUNT_METADATA_FILENAME);
      if (!metaData.exists()) {
          saveFileContents(metaData, buildMetadata(account));
          createWarningFile();
      }
    } else {
      throw new IOException("Cannot create accout dir");
    }
  }

  @Override
  public void resetAccountData(String uuid) throws IOException {
    //we do not set the member, this needs to be done through withAccount
    File accountDir = new File(baseDir, uuid);
    if (accountDir.isDirectory()) {
      for (String file : accountDir.list()) {
        if (!(new File(accountDir, file).delete())) {
          throw new IOException("Cannot reset accout dir");
        }
      }
    }
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
  public void storeBackup(Uri uri, String fileName) throws IOException {
    File backupDir = new File(baseDir, BACKUP_FOLDER_NAME);
    //noinspection ResultOfMethodCallIgnored
    backupDir.mkdir();
    if (!backupDir.isDirectory()) {
      throw new IOException("Unable to create directory for backups");
    }
    saveUriToFolder(fileName, uri, backupDir);
  }

  @NonNull
  @Override
  public List<String> getStoredBackups(android.accounts.Account account) throws IOException {
    String[] list = new File(baseDir, BACKUP_FOLDER_NAME).list();
    return list != null ? Arrays.asList(list) : new ArrayList<>();
  }

  @Override
  public InputStream getInputStreamForBackup(android.accounts.Account account, String backupFile) throws FileNotFoundException {
    return new FileInputStream(new File(new File(baseDir, BACKUP_FOLDER_NAME), backupFile));
  }

  @Override
  protected long getLastSequence(long start) {
    return Stream.of(filterFiles(start))
        .map(file -> getSequenceFromFileName(file.getName()))
        .max(Utils::compare)
        .orElse(start);
  }

  private File[] filterFiles(long sequenceNumber) {
    Preconditions.checkNotNull(accountDir);
    return accountDir.listFiles(file -> isNewerJsonFile(sequenceNumber, file.getName()));
  }

  @Override
  public void lock() {
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
    FileInputStream inputStream = new FileInputStream(file);
    ChangeSet changeSetFromInputStream = getChangeSetFromInputStream(getSequenceFromFileName(file.getName()), inputStream);
    inputStream.close();
    return changeSetFromInputStream;
  }

  private Optional<AccountMetaData> getAccountMetaDataFromFile(File file) {
    try {
      FileInputStream inputStream = new FileInputStream(file);
      Optional<AccountMetaData> result = getAccountMetaDataFromInputStream(inputStream);
      inputStream.close();
      return result;
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  @Override
  public void unlock() {
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
  protected void writeLockToken(String lockToken) throws IOException {
  }

  private void saveFileContents(File file, String fileContents) throws IOException {
    OutputStreamWriter out;
    out = new OutputStreamWriter(new FileOutputStream(file));
    out.write(fileContents);
    out.close();
  }

  @NonNull
  @Override
  public Stream<AccountMetaData> getRemoteAccountList(android.accounts.Account account) {
    return Stream.of(baseDir.listFiles(File::isDirectory))
        .map(directory -> new File(directory, ACCOUNT_METADATA_FILENAME))
        .filter(File::exists)
        .map(this::getAccountMetaDataFromFile)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }
}
