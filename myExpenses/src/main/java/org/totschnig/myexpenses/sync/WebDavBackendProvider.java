package org.totschnig.myexpenses.sync;

import android.accounts.AccountManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.webdav.CertificateHelper;
import org.totschnig.myexpenses.sync.webdav.InvalidCertificateException;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;
import org.totschnig.myexpenses.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.LockableDavResource;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import timber.log.Timber;

public class WebDavBackendProvider extends AbstractSyncBackendProvider {

  public static final String KEY_WEB_DAV_CERTIFICATE = "webDavCertificate";
  public static final String KEY_WEB_DAV_FALLBACK_TO_CLASS1 = "fallbackToClass1";
  public static final String KEY_ALLOW_UNVERIFIED= "allow_unverified";
  private final MediaType MIME_JSON = MediaType.parse(getMimeTypeForData() + "; charset=utf-8");
  private static final String FALLBACK_LOCK_FILENAME = ".lock";

  private WebDavClient webDavClient;
  private boolean fallbackToClass1 = false;

  WebDavBackendProvider(Context context, android.accounts.Account account, AccountManager accountManager) throws SyncParseException {
    super(context);
    String url = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL);
    if (url == null) throw new SyncParseException(new NullPointerException("sync_provider_url is null"));
    String userName = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_USERNAME);
    String password = accountManager.getPassword(account);

    fallbackToClass1 = accountManager.getUserData(account, KEY_WEB_DAV_FALLBACK_TO_CLASS1) != null;
    boolean allowUnverified = "true".equals(accountManager.getUserData(account, KEY_ALLOW_UNVERIFIED));

    X509Certificate certificate = null;
    if (accountManager.getUserData(account, KEY_WEB_DAV_CERTIFICATE) != null) {
      try {
        certificate = CertificateHelper.fromString(accountManager.getUserData(account, KEY_WEB_DAV_CERTIFICATE));
      } catch (CertificateException e) {
        throw new SyncParseException(e);
      }
    }
    try {
      webDavClient = new WebDavClient(url, userName, password, certificate, allowUnverified);
    } catch (InvalidCertificateException e) {
      throw new SyncParseException(e);
    }
  }

  @Override
  public void withAccount(Account account) throws IOException {
    setAccountUuid(account);
    webDavClient.mkCol(accountUuid);
    writeAccount(account, false);
  }

  @Override
  protected void writeAccount(Account account, boolean update) throws IOException {
    final String accountMetadataFilename = getAccountMetadataFilename();
    LockableDavResource metaData = webDavClient.getResource(accountMetadataFilename, accountUuid);
    if (update || !metaData.exists()) {
      saveFileContentsToAccountDir(null, accountMetadataFilename, buildMetadata(account), getMimeTypeForData(), true);
      if (!update) {
        createWarningFile();
      }
    }
  }

  @Override
  public Exceptional<AccountMetaData> readAccountMetaData() {
    return getAccountMetaDataFromDavResource(webDavClient.getResource(getAccountMetadataFilename(), accountUuid));
  }

  @Override
  public void resetAccountData(String uuid) throws IOException {
    try {
      for (DavResource davResource : webDavClient.getFolderMembers(uuid)) {
        davResource.delete(null);
      }
    } catch (HttpException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected String getExistingLockToken() throws IOException {
    return readResourceIfExists(getLockFile());
  }

  @Override
  protected String readEncryptionToken() throws IOException {
    return readResourceIfExists(webDavClient.getResource(ENCRYPTION_TOKEN_FILE_NAME, (String[]) null));
  }

  private String readResourceIfExists(LockableDavResource resource) throws IOException {
    if (resource.exists()) {
      try {
        return resource.get("text/plain").string();
      } catch (HttpException | DavException e) {
        throw new IOException(e);
      }
    } else {
      return null;
    }
  }

  @Override
  protected void writeLockToken(String lockToken) throws IOException {
    LockableDavResource lockfile = getLockFile();
    try {
      lockfile.put(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), lockToken), null, false);
    } catch (HttpException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void lock() throws IOException {
    if (fallbackToClass1) {
      super.lock();
    } else {
      if (!webDavClient.lock(accountUuid)) {
        throw new IOException("Backend cannot be locked");
      }
    }
  }

  private LockableDavResource getLockFile() {
    return webDavClient.getResource(FALLBACK_LOCK_FILENAME, accountUuid);
  }

  @NonNull
  @Override
  public Optional<ChangeSet> getChangeSetSince(SequenceNumber sequenceNumber, Context context) throws IOException {
    List<ChangeSet> changeSetList = new ArrayList<>();
    for (Pair<Integer, DavResource> davResourcePair: filterDavResources(sequenceNumber)) {
      changeSetList.add(getChangeSetFromDavResource(davResourcePair));
    }
    return merge(changeSetList);
  }

  @NonNull
  private ChangeSet getChangeSetFromDavResource(Pair<Integer, DavResource> davResource) throws IOException {
    try {
      return getChangeSetFromInputStream(new SequenceNumber(davResource.first, getSequenceFromFileName(davResource.second.fileName())),
          davResource.second.get(getMimeTypeForData()).byteStream());
    } catch (HttpException | DavException e) {
      throw new IOException(e);
    }
  }

  private List<Pair<Integer, DavResource>> filterDavResources(SequenceNumber sequenceNumber) throws IOException {
    LockableDavResource shardResource = sequenceNumber.shard == 0 ?
        webDavClient.getCollection(accountUuid, (String[]) null) :
        webDavClient.getCollection("_" + sequenceNumber.shard, accountUuid);
    if (!shardResource.exists()) {
      return new ArrayList<>();
    }
    List<Pair<Integer, DavResource>> result = Stream.of(webDavClient.getFolderMembers(shardResource))
        .filter(davResource -> isNewerJsonFile(sequenceNumber.number, davResource.fileName()))
        .map(davResource -> Pair.create(sequenceNumber.shard, davResource)).collect(Collectors.toList());
    int nextShard = sequenceNumber.shard + 1;
    while(true) {
      final String nextShardFolder = "_" + nextShard;
      LockableDavResource nextShardResource = webDavClient.getCollection(nextShardFolder, accountUuid);
      if (nextShardResource.exists()) {
        int finalNextShard = nextShard;
        Stream.of(webDavClient.getFolderMembers(nextShardResource))
            .filter(davResource -> isNewerJsonFile(0, davResource.fileName()))
            .map(davResource -> Pair.create(finalNextShard, davResource)).forEach(result::add);
        nextShard++;
      } else {
        break;
      }
    }
    return result;
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "webdav_backend";
  }

  @Override
  protected boolean isEmpty() throws IOException {
    return webDavClient.getFolderMembers((String[]) null).isEmpty();
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return getInputStream(accountUuid, relativeUri);
  }

  @Override
  public InputStream getInputStreamForBackup(String backupFile) throws IOException {
    return getInputStream(BACKUP_FOLDER_NAME, backupFile);
  }

  private InputStream getInputStream(String folderName, String resourceName) throws IOException {
    try {
      return webDavClient.getResource(resourceName, folderName).get("*/*").byteStream();
    } catch (HttpException | DavException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    saveUriToFolder(fileName, uri, accountUuid, true);
  }

  private long calculateSize(Uri uri) {
    final long size;
    Timber.d("Uri %s", uri);
    if ("file".equals(uri.getScheme())) {
      size = new File(uri.getPath()).length();
    } else {
      try (Cursor c = getContext().getContentResolver().query(uri, null, null, null, null)) {
        if (c != null) {
          c.moveToFirst();
          size = c.getLong(c.getColumnIndex(OpenableColumns.SIZE));
        } else {
          size = -1;
        }
      }
    }
    Timber.d("Size %d", size);
    return size;
  }

  private void saveUriToFolder(String fileName, Uri uri, String folder, boolean maybeEncrypt) throws IOException {
    String finalFileName = getLastFileNamePart(fileName);
    boolean encrypt = isEncrypted() && maybeEncrypt;
    long contentLength = encrypt ? -1 : calculateSize(uri);
    RequestBody requestBody = new RequestBody() {
      @Override
      public long contentLength() {
        return contentLength;
      }

      @Override
      public MediaType contentType() {
        return MediaType.parse(getMimeType(finalFileName));
      }

      @Override
      public void writeTo(@NonNull BufferedSink sink) throws IOException {
        Source source = null;
        try {
          InputStream in = getContext().getContentResolver().openInputStream(uri);
          if (in == null) {
            throw new IOException("Could not read " + uri.toString());
          }
          source = Okio.source(maybeEncrypt ? maybeEncrypt(in) : in);
          sink.writeAll(source);
        } finally {
          Util.closeQuietly(source);
        }
      }
    };
    try {
      webDavClient.upload(finalFileName, requestBody, folder);
    } catch (HttpException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void storeBackup(Uri uri, String fileName) throws IOException {
    webDavClient.mkCol(BACKUP_FOLDER_NAME);
    saveUriToFolder(fileName, uri, BACKUP_FOLDER_NAME, false);
  }

  @NonNull
  @Override
  public List<String> getStoredBackups() {
    try {
      return Stream.of(webDavClient.getFolderMembers(BACKUP_FOLDER_NAME))
          .map(DavResource::fileName)
          .toList();
    } catch (IOException e) {
      return new ArrayList<>();
    }
  }

  @Override
  protected SequenceNumber getLastSequence(SequenceNumber start) throws IOException {
    final Comparator<DavResource> resourceComparator = (o1, o2) -> Utils.compare(getSequenceFromFileName(o1.fileName()), getSequenceFromFileName(o2.fileName()));
    final Set<DavResource> mainMembers = webDavClient.getFolderMembers(accountUuid);
    Optional<DavResource> lastShardOptional =
        Stream.of(mainMembers)
        .filter(davResource -> LockableDavResource.isCollection(davResource) && isAtLeastShardDir(start.shard, davResource.fileName()))
        .max(resourceComparator);
    Set<DavResource> lastShard;
    int lastShardInt, reference;
    if (lastShardOptional.isPresent()) {
      final String lastShardName = lastShardOptional.get().fileName();
      lastShard = webDavClient.getFolderMembers(new String[] {accountUuid, lastShardName});
      lastShardInt = getSequenceFromFileName(lastShardName);
      reference = lastShardInt == start.shard ? start.number : 0;
    } else {
      if (start.shard > 0) return start;
      lastShard = mainMembers;
      lastShardInt = 0;
      reference = start.number;
    }
    return Stream.of(lastShard)
        .filter(davResource -> isNewerJsonFile(reference, davResource.fileName()))
        .max(resourceComparator)
        .map(davResource -> new SequenceNumber(lastShardInt, getSequenceFromFileName(davResource.fileName())))
        .orElse(start);
  }

  @Override
  void saveFileContentsToAccountDir(String folder, String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    LockableDavResource base = webDavClient.getCollection(accountUuid, (String[]) null);
    LockableDavResource parent;
    if (folder != null) {
      webDavClient.mkCol(folder, base);
      parent = webDavClient.getCollection(folder, accountUuid);
      if (!parent.exists()) {
        throw new IOException("Cannot make folder");
      }
    } else {
      parent = base;
    }

    saveFileContents(fileName, fileContents, mimeType, maybeEncrypt, parent);
  }

  private IOException transform(HttpException e) {
    return e.getCause() instanceof IOException ? ((IOException) e.getCause()) : new IOException(e);
  }

  private void saveFileContents(String fileName, String fileContents, String mimeType,
                                boolean maybeEncrypt, LockableDavResource parent) throws IOException {
    boolean encrypt = isEncrypted() && maybeEncrypt;
    MediaType mediaType =  MediaType.parse(mimeType + "; charset=utf-8");
    RequestBody requestBody = encrypt ? new RequestBody() {
      @Override
      public MediaType contentType() {
        return mediaType;
      }

      @Override
      public void writeTo(@NonNull BufferedSink sink) throws IOException {
        Source source = null;
        try {
          source = Okio.source(toInputStream(fileContents, true));
          sink.writeAll(source);
        } finally {
          Util.closeQuietly(source);
        }
      }
    } : RequestBody.create(mediaType, fileContents);
    try {
      webDavClient.upload(fileName, requestBody, parent);
    } catch (HttpException e) {
      throw transform(e);
    }
  }

  @Override
  void saveFileContentsToBase(String fileName, String fileContents, String mimeType, boolean maybeEncrypt) throws IOException {
    saveFileContents(fileName, fileContents, mimeType, maybeEncrypt, webDavClient.getBase());
  }

  @Override
  public void unlock() throws IOException {
    if (fallbackToClass1) {
      try {
        getLockFile().delete(null);
      } catch (HttpException e) {
        throw new IOException(e);
      }
    } else {
      if (!webDavClient.unlock(accountUuid)) {
        throw new IOException("Error while unlocking backend");
      }
    }
  }

  private String getLastPathSegment(HttpUrl httpUrl) {
    List<String> segments = httpUrl.pathSegments();
    return segments.get(segments.size() - 1);
  }

  @NonNull
  @Override
  public Stream<Exceptional<AccountMetaData>> getRemoteAccountList() throws IOException {
    return Stream.of(webDavClient.getFolderMembers((String[]) null))
        .filter(LockableDavResource::isCollection)
        .filter(davResource -> !getLastPathSegment(davResource.location).equals(BACKUP_FOLDER_NAME))
        .map(davResource -> webDavClient.getResource(davResource.location, getAccountMetadataFilename()))
        .filter(LockableDavResource::exists)
        .map(this::getAccountMetaDataFromDavResource);
  }

  private Exceptional<AccountMetaData> getAccountMetaDataFromDavResource(LockableDavResource lockableDavResource) {
    try {
      return getAccountMetaDataFromInputStream(lockableDavResource.get(getMimeTypeForData()).byteStream());
    } catch (DavException | HttpException | IOException e) {
      return Exceptional.of(e);
    }
  }
}
