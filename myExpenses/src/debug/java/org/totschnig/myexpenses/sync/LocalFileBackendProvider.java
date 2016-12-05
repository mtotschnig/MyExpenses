package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import com.annimon.stream.Stream;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

public class LocalFileBackendProvider implements SyncBackendProvider {

  private Gson gson;

  private DocumentFile baseDir;

  public LocalFileBackendProvider(String filePath) {
    File baseFolder = new File(filePath);
    if (!baseFolder.isDirectory()) {
      throw new RuntimeException("No directory " + filePath);
    }
    /*    File accountFolder = new File(baseFolder, "_" + accountId);
    accountFolder.mkdir();
    if (!accountFolder.isDirectory()) {
    }*/
    this.baseDir = DocumentFile.fromFile(baseFolder);
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
  }

  private long getLastSequence() {
    return Stream.of(baseDir.listFiles())
        .map(this::getSequenceFromFileName)
        .max(Long::compare)
        .orElse(0L);
  }

  @NonNull
  private Long getSequenceFromFileName(DocumentFile file) {
    return Long.parseLong(Files.getNameWithoutExtension(file.getName()).substring(1));
  }

  @Override
  public boolean lock() {
    return true;
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context)  {
    return Stream.of(baseDir.listFiles())
        .filter(file -> Long.parseLong(Files.getNameWithoutExtension(file.getName()).substring(1)) > sequenceNumber)
        .map(file -> getFromFile(file, context))
        .takeWhile(changeset -> !changeset.equals(ChangeSet.failed))
        .reduce(ChangeSet::merge).orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getFromFile(DocumentFile file, Context context) {
    try {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(
          context.getContentResolver().openInputStream(file.getUri())));
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
    long nextSequence = getLastSequence() + 1;
    DocumentFile changeSetFile = baseDir.createFile("text/json", "_" + nextSequence + ".json");
    if (changeSetFile != null && changeSetFile.getUri() != null) {
      OutputStreamWriter out;
      try {
        out = new OutputStreamWriter(
            context.getContentResolver().openOutputStream(changeSetFile.getUri()), "UTF-8");
        out.write(gson.toJson(changeSet));
        out.close();
        return nextSequence;
      } catch (IOException e) {
        e.printStackTrace();
      }
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
    return baseDir.getUri().toString();
  }
}
