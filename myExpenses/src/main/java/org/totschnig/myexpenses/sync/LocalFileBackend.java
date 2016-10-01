package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.support.v4.util.Pair;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.IOException;
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

  DocumentFile baseDir;
  Context context;

  private long getLastSequence() {
    long result = 0;
    for (DocumentFile file: baseDir.listFiles()) {
      result = Math.max(Long.parseLong(Files.getNameWithoutExtension(file.getName()).substring(1)), result);
    }
    return result;
  }


  @Override
  public boolean lock() {
    return false;
  }

  @Override
  public Pair<Long, List<TransactionChange>> getChangeSetSince(long sequenceNumber) {
    return null;
  }

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet) throws IOException {
    long nextSequence = getLastSequence() + 1;
    DocumentFile changeSetFile = baseDir.createFile("text/json", "_" + nextSequence + ".json");
    OutputStreamWriter out = new OutputStreamWriter(
        context.getContentResolver().openOutputStream(changeSetFile.getUri()), "UTF-8");
    for (TransactionChange change: changeSet) {
      out.write(gson.toJson(change));
    }
    out.close();
    return  nextSequence;
  }

  @Override
  public boolean unlock() {
    return false;
  }
}
