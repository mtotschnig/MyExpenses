package org.totschnig.myexpenses.sync;

import android.accounts.AccountManager;
import android.content.Context;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.webdav.CertificateHelper;
import org.totschnig.myexpenses.sync.webdav.HttpException;
import org.totschnig.myexpenses.sync.webdav.InvalidCertificateException;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

public class WebDavBackendProvider implements SyncBackendProvider {

  public static final String KEY_WEB_DAV_CERTIFICATE = "webDavCertificate";/**/

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
    } catch (HttpException e) {
      return false;
    }
    return true;
  }

  @Override
  public boolean lock() {
    return true;
  }

  @Override
  public ChangeSet getChangeSetSince(long sequenceNumber, Context context) {
    return null;
  }

  @Override
  public long writeChangeSet(List<TransactionChange> changeSet, Context context) {
    return 0;
  }

  @Override
  public boolean unlock() {
    return true;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

}
