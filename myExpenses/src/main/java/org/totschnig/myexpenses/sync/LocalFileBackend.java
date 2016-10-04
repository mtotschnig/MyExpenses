package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import com.annimon.stream.Stream;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

class LocalFileBackend implements SyncBackend {

  private final Gson gson;

  LocalFileBackend(DocumentFile baseDir, Context context) {
    this.baseDir = baseDir;
    this.context = context;
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
  }

  private DocumentFile baseDir;
  private Context context;

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
    return false;
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber)  {
    return Stream.of(baseDir.listFiles())
        .filter(file -> Long.parseLong(Files.getNameWithoutExtension(file.getName()).substring(1)) > sequenceNumber)
        .map(this::getFromFile)
        .takeWhile(changeset -> !changeset.equals(ChangeSet.failed))
        .reduce(ChangeSet::merge).orElse(ChangeSet.empty);
  }

  private ChangeSet getFromFile(DocumentFile file) {
    try {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(
          context.getContentResolver().openInputStream(file.getUri())));
      return ChangeSet.create(getSequenceFromFileName(file), Utils.getChanges(gson, reader));
    } catch (FileNotFoundException e) {
      return ChangeSet.failed;
    }
  }

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet) {
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
    return nextSequence;
  }

  @Override
  public boolean unlock() {
    return false;
  }

}
