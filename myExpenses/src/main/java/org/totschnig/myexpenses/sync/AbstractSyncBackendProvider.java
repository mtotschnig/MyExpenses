package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.support.annotation.NonNull;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;

abstract class AbstractSyncBackendProvider implements SyncBackendProvider {

  public static final Pattern FILE_PATTERN = Pattern.compile("_\\d+");
  protected Gson gson;

  AbstractSyncBackendProvider() {
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
  }

  protected ChangeSet getFromInputStream(long sequenceNumber, InputStream inputStream) {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    List<TransactionChange> changes = Utils.getChanges(gson, reader);
    if (changes == null || changes.size() == 0) {
      return ChangeSet.failed;
    }
    return ChangeSet.create(sequenceNumber, changes);
  }

  protected boolean accept(long sequenceNumber, String name) {
    String fileName = Files.getNameWithoutExtension(name);
    String fileExtension = Files.getFileExtension(name);
    return fileExtension.equals("json") && FILE_PATTERN.matcher(fileName).matches() &&
        Long.parseLong(fileName.substring(1)) > sequenceNumber;
  }

  protected Optional<ChangeSet> merge(Stream<ChangeSet> changeSetStream) {
    return changeSetStream.takeWhile(changeSet -> !changeSet.equals(ChangeSet.failed))
        .reduce(ChangeSet::merge);
  }

  @NonNull
  protected Long getSequenceFromFileName(String fileName) {
    return Long.parseLong(Files.getNameWithoutExtension(fileName).substring(1));
  }

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet, Context context) {
    long nextSequence = getLastSequence() + 1;
    try {
      saveFileContents("_" + nextSequence + ".json", gson.toJson(changeSet));
    } catch (IOException e) {
      return ChangeSet.FAILED;
    }
    return nextSequence;
  }

  protected abstract long getLastSequence();

  abstract void saveFileContents(String fileName, String fileContents) throws IOException;
}
