package org.totschnig.myexpenses.sync;

import android.accounts.AccountManager;
import android.content.Context;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.webdav.CertificateHelper;
import org.totschnig.myexpenses.sync.webdav.HttpException;
import org.totschnig.myexpenses.sync.webdav.InvalidCertificateException;
import org.totschnig.myexpenses.sync.webdav.LockableDavResource;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.DavException;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class WebDavBackendProvider extends AbstractSyncBackendProvider {

  public static final String KEY_WEB_DAV_CERTIFICATE = "webDavCertificate";
  public static final String MIMETYPE_JSON = "application/json";
  public final MediaType MIME_JSON = MediaType.parse(MIMETYPE_JSON + "; charset=utf-8");

  private WebDavClient webDavClient;
  /**
   * this holds the uuid of the db account which data is currently synced
   */
  private String accountUuid;

  public WebDavBackendProvider(android.accounts.Account account, AccountManager accountManager) throws SyncParseException {
    String url = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL);
    String userName = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_USERNAME);
    String password = accountManager.getPassword(account);
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
  public boolean withAccount(Account account) {
    accountUuid = account.uuid;
    try {
      webDavClient.mkCol(accountUuid);
      LockableDavResource metaData = webDavClient.getResource(accountUuid, ACCOUNT_METADATA_FILENAME);
      if (!metaData.exists()) {

        metaData.put(RequestBody.create(MIME_JSON, buildMetadata(account)), null, false);
      }
    } catch (HttpException | at.bitfire.dav4android.exception.HttpException | IOException e) {
      return false;
    }
    return true;
  }

  @Override
  public boolean resetAccountData(String uuid) {
    return false;
  }

  @Override
  public boolean lock() {
    return webDavClient.lock(accountUuid);
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) throws IOException {
    return merge(filterDavResources(sequenceNumber).map(this::getChangeSetFromDavResource))
        .orElse(ChangeSet.empty(sequenceNumber));
  }

  private ChangeSet getChangeSetFromDavResource(DavResource davResource) {
    try {
      return getChangeSetFromInputStream(getSequenceFromFileName(davResource.fileName()),
          davResource.get(MIMETYPE_JSON).byteStream());
    } catch (IOException | at.bitfire.dav4android.exception.HttpException | DavException e) {
      return ChangeSet.failed;
    }
  }

  private Stream<DavResource> filterDavResources(long sequenceNumber) throws IOException {
    return webDavClient.getFolderMembers(accountUuid)
        .filter(davResource -> isNewerJsonFile(sequenceNumber, davResource.fileName()));
  }

  @Override
  protected long getLastSequence() throws IOException {
    return filterDavResources(0)
        .map(davResource -> getSequenceFromFileName(davResource.fileName()))
        .max(Long::compare)
        .orElse(0L);
  }

  @Override
  void saveFileContents(String fileName, String fileContents) throws IOException {
    try {
      webDavClient.upload(accountUuid, fileName, fileContents, MIME_JSON);
    } catch (HttpException e) {
      throw e.getCause() instanceof IOException ? ((IOException) e.getCause()) : new IOException(e);
    }
  }

  @Override
  public boolean unlock() {
    return webDavClient.unlock(accountUuid);
  }

  @Override
  public List<AccountMetaData> getRemoteAccountList() throws IOException {
    return webDavClient.getFolderMembers(null)
        .filter(LockableDavResource::isCollection)
        .map(davResource -> webDavClient.getResource(davResource.location, ACCOUNT_METADATA_FILENAME))
        .filter(davResoure -> {
          try {
            return davResoure.exists();
          } catch (HttpException e) {
            return false;
          }
        })
        .map(this::getAccountMetaDataFromDavResource)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private Optional<AccountMetaData> getAccountMetaDataFromDavResource(LockableDavResource lockableDavResource) {
    try {
      return Optional.of(getAccountMetaDataFromInputStream(lockableDavResource.get(MIMETYPE_JSON).byteStream()));
    } catch (DavException | at.bitfire.dav4android.exception.HttpException | IOException e) {
      return Optional.empty();
    }
  }
}
