package org.totschnig.myexpenses.sync;

import android.content.Context;

import com.annimon.stream.Stream;
import com.google.common.base.Preconditions;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.ChangeSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class LocalFileBackendProvider extends AbstractSyncBackendProvider {

  private File baseDir, accountDir;

  public LocalFileBackendProvider(String filePath) {
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
    return accountDir.isDirectory();
  }

  @Override
  protected long getLastSequence() {
    return filterFiles(0)
        .map(file -> getSequenceFromFileName(file.getName()))
        .max(Long::compare)
        .orElse(0L);
  }

  private Stream<File> filterFiles(long sequenceNumber) {
    Preconditions.checkNotNull(accountDir);
    return Stream.of(accountDir.listFiles())
        .filter(file -> accept(sequenceNumber, file.getName()));
  }

  @Override
  public boolean lock() {
    return true;
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) {
    return merge(filterFiles(sequenceNumber).map(this::getFromFile))
        .orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getFromFile(File file) {
    try {
      return getFromInputStream(getSequenceFromFileName(file.getName()), new FileInputStream(file));
    } catch (FileNotFoundException e) {
      return ChangeSet.failed;
    }
  }


  @Override
  public boolean unlock() {
    return true;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public String toString() {
    return baseDir.toString();
  }

  @Override
  void saveFileContents(String fileName, String fileContents) throws IOException {
    Preconditions.checkNotNull(accountDir);
    File changeSetFile = new File(accountDir, fileName);
    OutputStreamWriter out;
    out = new OutputStreamWriter(new FileOutputStream(changeSetFile));
    out.write(gson.toJson(fileContents));
    out.close();
  }
}
