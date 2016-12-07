package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.regex.Pattern;

public class LocalFileBackendProvider implements SyncBackendProvider {

  private Gson gson;

  private File baseDir, accountDir;

  public LocalFileBackendProvider(String filePath) {
    baseDir = new File(filePath);
    if (!baseDir.isDirectory()) {
      throw new RuntimeException("No directory " + filePath);
    }
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
  }

  @Override
  public void withAccount(Account account) {
    accountDir = new File(baseDir, account.uuid);
    accountDir.mkdir();
    if (!accountDir.isDirectory()) {
      throw new RuntimeException("Could not create directory for account");
    }
  }

  private long getLastSequence() {
    return filterFiles(0)
        .map(this::getSequenceFromFileName)
        .max(Long::compare)
        .orElse(0L);
  }

  private Stream<File> filterFiles(long sequenceNumber) {
    Preconditions.checkNotNull(accountDir);
    Pattern pattern = Pattern.compile("_\\d+");
    return Stream.of(accountDir.listFiles())
        .filter(file -> {
          String fileName = Files.getNameWithoutExtension(file.getName());
          String fileExtension = Files.getFileExtension(file.getName());
          return fileExtension.equals("json") && pattern.matcher(fileName).matches() &&
              Long.parseLong(fileName.substring(1)) > sequenceNumber;
        });
  }

  @NonNull
  private Long getSequenceFromFileName(File file) {
    return Long.parseLong(Files.getNameWithoutExtension(file.getName()).substring(1));
  }

  @Override
  public boolean lock() {
    return true;
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) {

    return filterFiles(sequenceNumber)
        .map(file -> getFromFile(file, context))
        .takeWhile(changeset -> !changeset.equals(ChangeSet.failed))
        .reduce(ChangeSet::merge).orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getFromFile(File file, Context context) {
    try {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(
          new FileInputStream(file)));
      List<TransactionChange> changes = Utils.getChanges(gson, reader);
      if (changes == null || changes.size() == 0) {
        return ChangeSet.failed;
      }
      return ChangeSet.create(getSequenceFromFileName(file), changes);
    } catch (FileNotFoundException e) {
      return ChangeSet.failed;
    }
  }

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet, Context context) {
    Preconditions.checkNotNull(accountDir);
    long nextSequence = getLastSequence() + 1;
    File changeSetFile = new File(accountDir, "_" + nextSequence + ".json");
    OutputStreamWriter out;
    try {
      out = new OutputStreamWriter(new FileOutputStream(changeSetFile));
      out.write(gson.toJson(changeSet));
      out.close();
      return nextSequence;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ChangeSet.FAILED;
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
}
