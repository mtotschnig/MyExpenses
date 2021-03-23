package org.totschnig.myexpenses.sync;

import android.content.Context;
import android.net.Uri;

import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.io.FileCopyUtils;
import org.totschnig.myexpenses.util.io.StreamReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
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
    accountDir = new File(baseDir, account.getUuid());
    //noinspection ResultOfMethodCallIgnored
    accountDir.mkdir();
    if (accountDir.isDirectory()) {
      writeAccount(account, false);
    } else {
      throw new IOException("Cannot create account dir");
    }
  }

  @Override
  protected void writeAccount(Account account, boolean update) throws IOException {
    File metaData = new File(accountDir, getAccountMetadataFilename());
    if (update || !metaData.exists()) {
      saveFileContents(metaData, buildMetadata(account), true);
      if (!update) {
        createWarningFile();
      }
    }
  }

  @Override
  public Exceptional<AccountMetaData> readAccountMetaData() {
    return getAccountMetaDataFromFile(new File(accountDir, getAccountMetadataFilename()));
  }

  @Override
  public void resetAccountData(String uuid) throws IOException {
    //we do not set the member, this needs to be done through withAccount
    File accountDir = new File(baseDir, uuid);
    if (accountDir.isDirectory()) {
      for (String file : accountDir.list()) {
        if (!(new File(accountDir, file).delete())) {
          throw new IOException("Cannot reset account dir");
        }
      }
    }
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "local_file_backend"; // currently not used
  }

  @Override
  protected String readEncryptionToken() throws IOException {
    try {
      return new StreamReader(new FileInputStream(new File(baseDir, ENCRYPTION_TOKEN_FILE_NAME))).read();
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return new FileInputStream(new File(accountDir, relativeUri));
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    try (InputStream input = getContext().getContentResolver().openInputStream(uri);
         OutputStream output = maybeEncrypt(new FileOutputStream(new File(accountDir, fileName)))) {
      if (input == null) {
        throw new IOException("Could not open InputStream " + uri.toString());
      }
      FileCopyUtils.copy(input, output);
    }
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
  public List<String> getStoredBackups() throws IOException {
    String[] list = new File(baseDir, BACKUP_FOLDER_NAME).list();
    return list != null ? Arrays.asList(list) : new ArrayList<>();
  }

  @Override
  public InputStream getInputStreamForBackup(String backupFile) throws FileNotFoundException {
    return new FileInputStream(new File(new File(baseDir, BACKUP_FOLDER_NAME), backupFile));
  }

  @Override
  protected SequenceNumber getLastSequence(SequenceNumber start) {
    final Comparator<File> fileComparator = (o1, o2) -> Utils.compare(getSequenceFromFileName(o1.getName()), getSequenceFromFileName(o2.getName()));
    Optional<File> lastShardOptional = Stream.of(accountDir.listFiles(
        file -> file.isDirectory() && isAtLeastShardDir(start.shard, file.getName())))
        .max(fileComparator);
    File lastShard;
    int lastShardInt, reference;
    if (lastShardOptional.isPresent()) {
      lastShard = lastShardOptional.get();
      lastShardInt = getSequenceFromFileName(lastShard.getName());
      reference = lastShardInt == start.shard ? start.number : 0;
    } else {
      if (start.shard > 0) return start;
      lastShard = accountDir;
      lastShardInt = 0;
      reference = start.number;
    }
    return Stream.of(lastShard.listFiles(file -> isNewerJsonFile(reference, file.getName())))
        .max(fileComparator)
        .map(file -> new SequenceNumber(lastShardInt, getSequenceFromFileName(file.getName())))
        .orElse(start);
  }

  private List<Pair<Integer, File>> filterFiles(SequenceNumber sequenceNumber) {
    Preconditions.checkNotNull(accountDir);
    File shardDir = sequenceNumber.shard == 0 ? accountDir : new File(accountDir, "_" +sequenceNumber.shard);
    if (!shardDir.isDirectory()) return new ArrayList<>();
    List<Pair<Integer, File>> result = Stream.of(shardDir.listFiles(file -> isNewerJsonFile(sequenceNumber.number, file.getName())))
        .map(file -> Pair.create(sequenceNumber.shard, file)).collect(Collectors.toList());
    int nextShard = sequenceNumber.shard + 1;
    while(true) {
      File nextShardDir = new File(accountDir, "_" + nextShard);
      if (nextShardDir.isDirectory()) {
        int finalNextShard = nextShard;
        Stream.of(nextShardDir.listFiles(file -> isNewerJsonFile(0, file.getName())))
            .map(file -> Pair.create(finalNextShard, file)).forEach(result::add);
        nextShard++;
      } else {
        break;
      }
    }
    return result;
  }

  @Override
  public void lock() {
  }

  @NonNull
  @Override
  public Optional<ChangeSet> getChangeSetSince(SequenceNumber sequenceNumber, Context context) throws IOException {
    List<ChangeSet> changeSets = new ArrayList<>();
    for (Pair<Integer, File> file: filterFiles(sequenceNumber)) {
      changeSets.add(getChangeSetFromFile(file));
    }
    return merge(changeSets);
  }

  @NonNull
  private ChangeSet getChangeSetFromFile(Pair<Integer, File> file) throws IOException {
    FileInputStream inputStream = new FileInputStream(file.second);
    return getChangeSetFromInputStream(
        new SequenceNumber(file.first, getSequenceFromFileName(file.second.getName())), inputStream);
  }

  private Exceptional<AccountMetaData> getAccountMetaDataFromFile(File file) {
    try {
      FileInputStream inputStream = new FileInputStream(file);
      return getAccountMetaDataFromInputStream(inputStream);
    } catch (IOException e) {
      log().e(e);
      return Exceptional.of(e);
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
  void saveFileContentsToAccountDir(String folder, String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    Preconditions.checkNotNull(accountDir);
    File dir = folder == null ? accountDir : new File(accountDir, folder);
    //noinspection ResultOfMethodCallIgnored
    dir.mkdir();
    if (dir.isDirectory()) {
      saveFileContents(new File(dir, fileName), fileContents, maybeEncrypt);
    } else {
      throw new IOException("Cannot create dir");
    }
  }

  @Override
  void saveFileContentsToBase(String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    saveFileContents(new File(baseDir, fileName), fileContents, maybeEncrypt);
  }

  @Override
  protected String getExistingLockToken() {
    return null;
  }

  @Override
  protected void writeLockToken(String lockToken) {
  }

  private void saveFileContents(File file, String fileContents, boolean maybeEncrypt) throws IOException {
    OutputStreamWriter out;
    final FileOutputStream fileOutputStream = new FileOutputStream(file);
    out = new OutputStreamWriter(maybeEncrypt ? maybeEncrypt(fileOutputStream) : fileOutputStream);
    out.write(fileContents);
    out.close();
  }

  @NonNull
  @Override
  public Stream<Exceptional<AccountMetaData>> getRemoteAccountList()  {
    return Stream.of(baseDir.listFiles(File::isDirectory))
        .filter(directory -> !directory.getName().equals(BACKUP_FOLDER_NAME))
        .map(directory -> new File(directory, getAccountMetadataFilename()))
        .filter(File::exists)
        .map(this::getAccountMetaDataFromFile);
  }

  @Override
  protected boolean isEmpty() {
    return baseDir.list().length == 0;
  }
}
