package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
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
import java.util.List;

import dagger.internal.Preconditions;

class LocalFileBackendProvider extends AbstractSyncBackendProvider {

  private File baseDir, accountDir;

  LocalFileBackendProvider(String filePath) {
    super();
    baseDir = new File(filePath);
    if (!baseDir.isDirectory()) {
      throw new RuntimeException("No directory " + filePath);
    }
  }

  @Override
  public boolean withAccount(Account account) {
    accountDir = new File(baseDir, account.uuid);
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
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return new FileInputStream(new File(accountDir, relativeUri));
  }

  @Override
  protected void saveUri(String fileName, Uri uri) throws IOException {
    FileCopyUtils.copy(uri, Uri.fromFile(new File(accountDir, fileName)));
  }

  @Override
  protected long getLastSequence() {
    return Stream.of(filterFiles(0))
        .map(file -> getSequenceFromFileName(file.getName()))
        .max(this::compareInt)
        .orElse(0L);
  }

  private File[] filterFiles(long sequenceNumber) {
    Preconditions.checkNotNull(accountDir);
    return accountDir.listFiles(file -> isNewerJsonFile(sequenceNumber, file.getName()));
  }

  @Override
  public boolean lock() {
    return true;
  }

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

  private void saveFileContents(File file, String fileContents) throws IOException {
    OutputStreamWriter out;
    out = new OutputStreamWriter(new FileOutputStream(file));
    out.write(fileContents);
    out.close();
  }

  @Override
  public List<AccountMetaData> getRemoteAccountList() {
    return Stream.of(baseDir.listFiles(File::isDirectory))
        .map(directory -> new File(directory, ACCOUNT_METADATA_FILENAME))
        .filter(File::exists)
        .map(this::getAccountMetaDataFromFile)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }
}
