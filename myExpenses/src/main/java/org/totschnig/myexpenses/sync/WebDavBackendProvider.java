package org.totschnig.myexpenses.sync;

import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.webdav.CertificateHelper;
import org.totschnig.myexpenses.sync.webdav.InvalidCertificateException;
import org.totschnig.myexpenses.sync.webdav.LockableDavResource;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;
import org.totschnig.myexpenses.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class WebDavBackendProvider extends AbstractSyncBackendProvider {

  public static final String KEY_WEB_DAV_CERTIFICATE = "webDavCertificate";
  public static final String KEY_WEB_DAV_FALLBACK_TO_CLASS1 = "fallbackToClass1";
  private final MediaType MIME_JSON = MediaType.parse(MIMETYPE_JSON + "; charset=utf-8");
  private static final String FALLBACK_LOCK_FILENAME = ".lock";

  private WebDavClient webDavClient;
  private boolean fallbackToClass1 = false;

  WebDavBackendProvider(Context context, android.accounts.Account account, AccountManager accountManager) throws SyncParseException {
    super(context);
    String url = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL);
    String userName = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_USERNAME);
    String password = accountManager.getPassword(account);

    fallbackToClass1 = accountManager.getUserData(account, KEY_WEB_DAV_FALLBACK_TO_CLASS1) != null;

    X509Certificate certificate = null;
    if (accountManager.getUserData(account, KEY_WEB_DAV_CERTIFICATE) != null) {
      try {
        certificate = CertificateHelper.fromString(accountManager.getUserData(account, KEY_WEB_DAV_CERTIFICATE));
      } catch (CertificateException e) {
        throw new SyncParseException(e);
      }
    }
    try {
      webDavClient = new WebDavClient(url, userName, password, certificate);
    } catch (InvalidCertificateException e) {
      throw new SyncParseException(e);
    }
  }

  @Override
  public void withAccount(Account account) throws IOException {
    setAccountUuid(account);
    try {
      webDavClient.mkCol(accountUuid);
      LockableDavResource metaData = webDavClient.getResource(accountUuid, ACCOUNT_METADATA_FILENAME);
      if (!metaData.exists()) {
        metaData.put(RequestBody.create(MIME_JSON, buildMetadata(account)), null, false);
        createWarningFile();
      }
    } catch (HttpException e) {
      throw new IOException(e);
    }
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
    LockableDavResource lockfile = getLockFile();
    try {
      return lockfile.get("text/plain").string();
    } catch (HttpException | DavException e) {
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
    return webDavClient.getResource(accountUuid, FALLBACK_LOCK_FILENAME);
  }

  @NonNull
  @Override
  public ChangeSet getChangeSetSince(SequenceNumber sequenceNumber, Context context) throws IOException {
    return merge(filterDavResources(sequenceNumber).map(this::getChangeSetFromDavResource))
        .orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getChangeSetFromDavResource(DavResource davResource) {
    try {
      return getChangeSetFromInputStream(new SequenceNumber(0, getSequenceFromFileName(davResource.fileName())),
          davResource.get(MIMETYPE_JSON).byteStream());
    } catch (IOException | HttpException | DavException e) {
      return null;
    }
  }

  private Stream<DavResource> filterDavResources(SequenceNumber sequenceNumber) throws IOException {
    return Stream.of(webDavClient.getFolderMembers(accountUuid))
        .filter(davResource -> isNewerJsonFile(sequenceNumber.number, davResource.fileName()));
  }

  @NonNull
  @Override
  protected String getSharedPreferencesName() {
    return "webdav_backend";
  }

  @NonNull
  @Override
  protected InputStream getInputStreamForPicture(String relativeUri) throws IOException {
    return getInputStream(accountUuid, relativeUri);
  }

  @Override
  public InputStream getInputStreamForBackup(android.accounts.Account account, String backupFile) throws IOException {
    return getInputStream(BACKUP_FOLDER_NAME, backupFile);
  }

  private InputStream getInputStream(String folderName, String resourceName) throws IOException {
    try {
      return webDavClient.getResource(folderName, resourceName).get("*/*").byteStream();
    } catch (HttpException | DavException e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void saveUriToAccountDir(String fileName, Uri uri) throws IOException {
    saveUriToFolder(fileName, uri, accountUuid);
  }

  private void saveUriToFolder(String fileName, Uri uri, String folder) throws IOException {
    String finalFileName = getLastFileNamePart(fileName);
    try {
      RequestBody requestBody = new RequestBody() {
        @Override
        public MediaType contentType() {
          return MediaType.parse(getMimeType(finalFileName));
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
          Source source = null;
          try {
            InputStream in = MyApplication.getInstance().getContentResolver()
                .openInputStream(uri);
            if (in == null) {
              throw new IOException("Could not read " + uri.toString());
            }
            source = Okio.source(in);
            sink.writeAll(source);
          } finally {
            Util.closeQuietly(source);
          }
        }
      };
      webDavClient.upload(folder, finalFileName, requestBody);
    } catch (HttpException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void storeBackup(Uri uri, String fileName) throws IOException {
    try {
      webDavClient.mkCol(BACKUP_FOLDER_NAME);
    } catch (HttpException e) {
      throw new IOException(e);
    }
    saveUriToFolder(fileName, uri, BACKUP_FOLDER_NAME);
  }

  @NonNull
  @Override
  public List<String> getStoredBackups(android.accounts.Account account) {
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
    return filterDavResources(start)
        .map(davResource -> getSequenceFromFileName(davResource.fileName()))
        .max(Utils::compare)
        .map(max -> new SequenceNumber(0, max))
        .orElse(start);
  }

  @Override
  void saveFileContents(String folder, String fileName, String fileContents, String mimeType) throws IOException {
    try {
      webDavClient.upload(accountUuid, fileName, fileContents,
          MediaType.parse(mimeType + "; charset=utf-8"));
    } catch (HttpException e) {
      throw e.getCause() instanceof IOException ? ((IOException) e.getCause()) : new IOException(e);
    }
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

  @NonNull
  @Override
  public Stream<AccountMetaData> getRemoteAccountList(android.accounts.Account account) throws IOException {
    return Stream.of(webDavClient.getFolderMembers(null))
        .filter(LockableDavResource::isCollection)
        .map(davResource -> webDavClient.getResource(davResource.location, ACCOUNT_METADATA_FILENAME))
        .filter(LockableDavResource::exists)
        .map(this::getAccountMetaDataFromDavResource)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Optional<AccountMetaData> getAccountMetaDataFromDavResource(LockableDavResource lockableDavResource) {
    try {
      return getAccountMetaDataFromInputStream(lockableDavResource.get(MIMETYPE_JSON).byteStream());
    } catch (DavException | HttpException | IOException e) {
      return Optional.empty();
    }
  }
}
