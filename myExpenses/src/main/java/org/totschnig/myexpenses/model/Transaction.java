/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.model;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.RemoteException;

import com.android.calendar.CalendarContractCompat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.FileCopyUtils;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.FULL_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.IS_SAME_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_END;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR_OF_MONTH_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_PEER_PARENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeekEnd;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeekStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart;
import static org.totschnig.myexpenses.provider.DbUtils.getLongOrNull;

/**
 * Domain class for transactions
 *
 * @author Michael Totschnig
 */
public class Transaction extends Model {
  public String comment = "", payee = "", referenceNumber = "";
  /**
   * stores a short label of the category or the account the transaction is linked to
   */
  public String label = "";
  protected Date date;
  protected Money amount;
  protected Money transferAmount;
  private Long catId;
  public Long accountId;
  public Long transfer_peer;
  public Long transfer_account;
  public Long methodId;
  public String methodLabel = "";
  public Long parentId = null;
  public Long payeeId = null;
  /**
   * id of the template which defines the plan for which this transaction has been created
   */
  public Template originTemplate = null;
  /**
   * id of an instance of the event (plan) for which this transaction has been created
   */
  public Long originPlanInstanceId = null;
  /**
   * 0 = is normal, special states are
   * {@link org.totschnig.myexpenses.provider.DatabaseConstants#STATUS_EXPORTED} and
   * {@link org.totschnig.myexpenses.provider.DatabaseConstants#STATUS_UNCOMMITTED}
   */
  public int status = 0;
  public static String[] PROJECTION_BASE, PROJECTION_EXTENDED, PROJECTION_EXTENDED_AGGREGATE;

  static {
    buildProjection();
  }

  public static void buildProjection() {
    PROJECTION_BASE = new String[]{
        KEY_ROWID,
        KEY_DATE,
        KEY_AMOUNT,
        KEY_COMMENT,
        KEY_CATID,
        LABEL_MAIN,
        LABEL_SUB,
        KEY_PAYEE_NAME,
        KEY_TRANSFER_PEER,
        KEY_METHODID,
        KEY_METHOD_LABEL,
        KEY_CR_STATUS,
        KEY_REFERENCE_NUMBER,
        KEY_PICTURE_URI,
        getYearOfWeekStart() + " AS " + KEY_YEAR_OF_WEEK_START,
        getYearOfMonthStart() + " AS " + KEY_YEAR_OF_MONTH_START,
        YEAR + " AS " + KEY_YEAR,
        getMonth() + " AS " + KEY_MONTH,
        getWeek() + " AS " + KEY_WEEK,
        DAY + " AS " + KEY_DAY,
        getThisYearOfWeekStart() + " AS " + KEY_THIS_YEAR_OF_WEEK_START,
        THIS_YEAR + " AS " + KEY_THIS_YEAR,
        getThisWeek() + " AS " + KEY_THIS_WEEK,
        THIS_DAY + " AS " + KEY_THIS_DAY,
        getWeekStart() + " AS " + KEY_WEEK_START,
        getWeekEnd() + " AS " + KEY_WEEK_END
    };

    //extended
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength + 4];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = KEY_COLOR;
    //the definition of column TRANSFER_PEER_PARENT refers to view_extended,
    //thus can not be used in PROJECTION_BASE
    PROJECTION_EXTENDED[baseLength + 1] = TRANSFER_PEER_PARENT + " AS transfer_peer_parent";
    PROJECTION_EXTENDED[baseLength + 2] = KEY_STATUS;
    PROJECTION_EXTENDED[baseLength + 3] = KEY_ACCOUNT_LABEL;

    //extended for aggregate include is_same_currecny
    int extendedLength = baseLength + 4;
    PROJECTION_EXTENDED_AGGREGATE = new String[extendedLength + 1];
    System.arraycopy(PROJECTION_EXTENDED, 0, PROJECTION_EXTENDED_AGGREGATE, 0, extendedLength);
    PROJECTION_EXTENDED_AGGREGATE[extendedLength] = IS_SAME_CURRENCY + " AS " + KEY_IS_SAME_CURRENCY;
  }

  public static final Uri CONTENT_URI = TransactionProvider.TRANSACTIONS_URI;
  public static final Uri EXTENDED_URI = CONTENT_URI.buildUpon().appendQueryParameter(
      TransactionProvider.QUERY_PARAMETER_EXTENDED, "1").build();
  public static final Uri CALLER_IS_SYNC_ADAPTER_URI = CONTENT_URI.buildUpon()
      .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_CALLER_IS_SYNCADAPTER, "1").build();

  public Money getAmount() {
    return amount;
  }

  public void setAmount(Money amount) {
    this.amount = amount;
  }

  public Money getTransferAmount() {
    return transferAmount;
  }

  public enum CrStatus {
    UNRECONCILED(Color.GRAY, ""), CLEARED(Color.BLUE, "*"), RECONCILED(Color.GREEN, "X"), VOID(Color.RED, null);
    public int color;
    public String symbol;

    CrStatus(int color, String symbol) {
      this.color = color;
      this.symbol = symbol;
    }

    public String toString() {
      Context ctx = MyApplication.getInstance();
      switch (this) {
        case CLEARED:
          return ctx.getString(R.string.status_cleared);
        case RECONCILED:
          return ctx.getString(R.string.status_reconciled);
        case UNRECONCILED:
          return ctx.getString(R.string.status_uncreconciled);
        case VOID:
          return ctx.getString(R.string.status_void);
      }
      return super.toString();
    }

    public static final String JOIN;

    static {
      JOIN = TextUtils.joinEnum(CrStatus.class);
    }

    public static CrStatus fromQifName(String qifName) {
      if (qifName == null)
        return UNRECONCILED;
      if (qifName.equals("*")) {
        return CLEARED;
      } else if (qifName.equalsIgnoreCase("X")) {
        return RECONCILED;
      } else {
        return UNRECONCILED;
      }
    }
  }

  public CrStatus crStatus;
  transient protected Uri pictureUri;

  /**
   * factory method for retrieving an instance from the db with the given id
   *
   * @param id
   * @return instance of {@link Transaction} or {@link Transfer} or null if not found
   */
  public static Transaction getInstanceFromDb(long id) {
    Transaction t;
    String[] projection = new String[]{KEY_ROWID, KEY_DATE, KEY_AMOUNT, KEY_COMMENT, KEY_CATID,
        FULL_LABEL, KEY_PAYEEID, KEY_PAYEE_NAME, KEY_TRANSFER_PEER, KEY_TRANSFER_ACCOUNT,
        KEY_ACCOUNTID, KEY_METHODID, KEY_PARENTID, KEY_CR_STATUS, KEY_REFERENCE_NUMBER,
        KEY_PICTURE_URI, KEY_METHOD_LABEL, KEY_STATUS, TRANSFER_AMOUNT, KEY_TEMPLATEID, KEY_UUID};

    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection, null, null, null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    Long transfer_peer = getLongOrNull(c, KEY_TRANSFER_PEER);
    long account_id = c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID));
    long amount = c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT));
    Long parent_id = getLongOrNull(c, KEY_PARENTID);
    Long catId = getLongOrNull(c, KEY_CATID);
    if (transfer_peer != null) {
      t = parent_id != null ? new SplitPartTransfer(account_id, amount, parent_id) :
          new Transfer(account_id, amount);
      t.transfer_peer = transfer_peer;
      t.transfer_account = getLongOrNull(c, KEY_TRANSFER_ACCOUNT);
      t.transferAmount = new Money(Account.getInstanceFromDb(t.transfer_account).currency,
          c.getLong(c.getColumnIndex(KEY_TRANSFER_AMOUNT)));
    } else {
      if (DatabaseConstants.SPLIT_CATID.equals(catId)) {
        t = new SplitTransaction(account_id, amount);
      } else {
        t = parent_id != null ? new SplitPartCategory(account_id, amount, parent_id) :
            new Transaction(account_id, amount);
      }
    }
    try {
      t.crStatus = CrStatus.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_CR_STATUS)));
    } catch (IllegalArgumentException ex) {
      t.crStatus = CrStatus.UNRECONCILED;
    }
    t.methodId = getLongOrNull(c, KEY_METHODID);
    t.methodLabel = DbUtils.getString(c, KEY_METHOD_LABEL);
    t.setCatId(catId);
    t.payeeId = getLongOrNull(c, KEY_PAYEEID);
    t.payee = DbUtils.getString(c, KEY_PAYEE_NAME);
    t.setId(id);
    t.setDate(c.getLong(
        c.getColumnIndexOrThrow(KEY_DATE)) * 1000L);
    t.comment = DbUtils.getString(c, KEY_COMMENT);
    t.referenceNumber = DbUtils.getString(c, KEY_REFERENCE_NUMBER);
    t.label = DbUtils.getString(c, KEY_LABEL);
    int pictureUriColumnIndex = c.getColumnIndexOrThrow(KEY_PICTURE_URI);
    t.pictureUri = c.isNull(pictureUriColumnIndex) ?
        null :
        Uri.parse(c.getString(pictureUriColumnIndex));
    t.status = c.getInt(c.getColumnIndexOrThrow(KEY_STATUS));
    Long originTemplateId = getLongOrNull(c, KEY_TEMPLATEID);
    t.originTemplate = originTemplateId == null ? null : Template.getInstanceFromDb(originTemplateId);
    t.uuid = DbUtils.getString(c, KEY_UUID);
    c.close();
    return t;
  }

  public static Transaction getInstanceFromTemplate(long id) {
    Template te = Template.getInstanceFromDb(id);
    return te == null ? null : getInstanceFromTemplate(te);
  }

  public static Transaction getInstanceFromTemplate(Template te) {
    Transaction tr;
    if (te.isTransfer()) {
      tr = new Transfer(te.accountId, te.amount);
      tr.transfer_account = te.transfer_account;
    } else {
      tr = new Transaction(te.accountId, te.amount);
      tr.methodId = te.methodId;
      tr.methodLabel = te.methodLabel;
      tr.setCatId(te.getCatId());
    }
    tr.comment = te.comment;
    tr.payee = te.payee;
    tr.label = te.label;
    tr.originTemplate = te;
    cr().update(
        TransactionProvider.TEMPLATES_URI
            .buildUpon()
            .appendPath(String.valueOf(te.getId()))
            .appendPath(TransactionProvider.URI_SEGMENT_INCREASE_USAGE)
            .build(),
        null, null, null);
    return tr;
  }

  /**
   * factory method for creating an object of the correct type and linked to a given account
   *
   * @param accountId the account the transaction belongs to if account no longer exists {@link Account#getInstanceFromDb(long) is called with 0}
   * @return instance of {@link Transaction} or {@link Transfer} or {@link SplitTransaction} with date initialized to current date
   * if parentId == 0L, otherwise {@link SplitPartCategory} or {@link SplitPartTransfer}
   */
  public static Transaction getNewInstance(long accountId) {
    Account account = Account.getInstanceFromDbWithFallback(accountId);
    if (account == null) {
      return null;
    }
    return new Transaction(account, 0L);
  }

  public static void delete(long id, boolean markAsVoid) {
    Uri.Builder builder = ContentUris.appendId(CONTENT_URI.buildUpon(), id);
    if (markAsVoid) {
      builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MARK_VOID, "1");
    }
    cr().delete(builder.build(), null, null);
  }

  public static void undelete(long id) {
    Uri uri = ContentUris.appendId(CONTENT_URI.buildUpon(), id)
        .appendPath(TransactionProvider.URI_SEGMENT_UNDELETE).build();
    cr().update(uri, null, null, null);
  }

  protected Transaction() {
    setDate(new Date());
    this.crStatus = CrStatus.UNRECONCILED;
  }

  /**
   * new empty transaction
   */
  public Transaction(long accountId, Long amount) {
    this(Account.getInstanceFromDb(accountId), amount);
  }

  public Transaction(long accountId, Money amount) {
    this();
    this.accountId = accountId;
    this.amount = amount;
  }

  public Transaction(Account account, long amount) {
    this();
    this.accountId = account.getId();
    this.amount = new Money(account.currency, amount);
  }

  public Long getCatId() {
    return catId;
  }

  public void setCatId(Long catId) {
    this.catId = catId;
  }

  public void setDate(Date date) {
    if (date == null) {
      throw new RuntimeException("Transaction date cannot be set to null");
    }
    this.date = date;
  }

  private void setDate(Long unixEpoch) {
    this.date = new Date(unixEpoch);
  }

  public Date getDate() {
    return date;
  }

  /**
   * updates the payee string to a new value
   * it will me mapped to an existing or new row in payee table during save
   *
   * @param payee
   */
  public void setPayee(String payee) {
    if (!this.payee.equals(payee)) {
      this.payeeId = null;
    }
    this.payee = payee;
  }

  /**
   * updates the payee to a row that already exists in the DB
   *
   * @param payee
   * @param payeeId
   */
  public void updatePayeeWithId(String payee, Long payeeId) {
    this.payee = payee;
    this.payeeId = payeeId;
  }

  @Override
  public Uri save() {
    Uri uri;
    try {
      ContentProviderResult[] result = cr().applyBatch(TransactionProvider.AUTHORITY,
          buildSaveOperations(0, -1, false));
      if (getId() == 0) {
        //we need to find a uri, otherwise we would crash. Need to handle?
        uri = result[0].uri;
        updateFromResult(result);
      } else {
        uri = Uri.parse(CONTENT_URI + "/" + getId());
      }
    } catch (RemoteException | OperationApplicationException e) {
      return null;
    }

    if (pictureUri != null) {
      ContribFeature.ATTACH_PICTURE.recordUsage();
    }

    if (originTemplate != null && originTemplate.getId() == 0) {
      originTemplate.save();
      //now need to find out the instance number
      Uri.Builder eventsUriBuilder = CalendarProviderProxy.INSTANCES_URI.buildUpon();
      DateTime instant = DateTime.forInstant(originTemplate.getPlan().dtstart, TimeZone.getDefault());
      ContentUris.appendId(eventsUriBuilder, instant.getStartOfDay().getMilliseconds(TimeZone.getDefault()));
      ContentUris.appendId(eventsUriBuilder, instant.getEndOfDay().getMilliseconds(TimeZone.getDefault()));
      Uri eventsUri = eventsUriBuilder.build();
      Cursor c = cr().query(eventsUri,
          null,
          String.format(Locale.US, CalendarContractCompat.Instances.EVENT_ID + " = %d",
              originTemplate.getPlan().getId()),
          null,
          null);
      if (c != null) {
        if (c.moveToFirst()) {
          long instance_id = c.getLong(c.getColumnIndex(CalendarContractCompat.Instances._ID));
          ContentValues values = new ContentValues();
          values.put(KEY_TEMPLATEID, originTemplate.getId());
          values.put(KEY_INSTANCEID, instance_id);
          values.put(KEY_TRANSACTIONID, getId());
          cr().insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
        }
        c.close();
      }
    }
    return uri;
  }

  protected void updateFromResult(ContentProviderResult[] result) {
    setId(ContentUris.parseId(result[0].uri));
  }

  /**
   * Constructs the {@link ArrayList} of {@link ContentProviderOperation}s necessary for saving
   * the transaction
   * as a side effect calls {@link Payee#require(String)}
   *
   * @param offset       Number of operations that are already added to the batch, needed for calculating back references
   * @param parentOffset if not -1, it indicates at which position in the batch the parent of a new split transaction is situated.
   *                     Is used from SyncAdapter for creating split transactions
   * @param callerIsSyncAdapter
   * @return the URI of the transaction. Upon creation it is returned from the content provider
   */
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset, int parentOffset, boolean callerIsSyncAdapter) {
    Uri uri = getUriForSave(callerIsSyncAdapter);
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ContentValues initialValues = buildInitialValues();
    if (getId() == 0) {
      ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri).withValues(initialValues);
      if (parentOffset != -1) {
        builder.withValueBackReference(KEY_PARENTID, parentOffset);
      }
      ops.add(builder.build());
      addOriginPlanInstance(ops);
    } else {
      ops.add(ContentProviderOperation
          .newUpdate(uri.buildUpon().appendPath(String.valueOf(getId())).build())
          .withValues(initialValues).build());
    }
    return ops;
  }

  protected Uri getUriForSave(boolean callerIsSyncAdapter) {
    return callerIsSyncAdapter ? CALLER_IS_SYNC_ADAPTER_URI : CONTENT_URI;
  }

  protected void addOriginPlanInstance(ArrayList<ContentProviderOperation> ops) {
    if (originPlanInstanceId != null) {
      ContentValues values = new ContentValues();
      values.put(KEY_TEMPLATEID, originTemplate.getId());
      values.put(KEY_INSTANCEID, originPlanInstanceId);
      ops.add(ContentProviderOperation.newInsert(TransactionProvider.PLAN_INSTANCE_STATUS_URI)
          .withValues(values).withValueBackReference(KEY_TRANSACTIONID, 0).build());
    }
  }

  ContentValues buildInitialValues() {
    ContentValues initialValues = new ContentValues();

    Long payeeStore;
    if (payeeId != null) {
      payeeStore = payeeId;
    } else {
      payeeStore =
          (payee != null && !payee.equals("")) ?
              Payee.require(payee) :
              null;
    }
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_REFERENCE_NUMBER, referenceNumber);
    //store in UTC
    initialValues.put(KEY_DATE, date.getTime() / 1000);

    initialValues.put(KEY_AMOUNT, amount.getAmountMinor());
    initialValues.put(KEY_CATID, getCatId());
    initialValues.put(KEY_PAYEEID, payeeStore);
    initialValues.put(KEY_METHODID, methodId);
    initialValues.put(KEY_CR_STATUS, crStatus.name());
    initialValues.put(KEY_ACCOUNTID, accountId);
    initialValues.put(KEY_UUID, requireUuid());

    savePicture(initialValues);
    if (getId() == 0) {
      initialValues.put(KEY_PARENTID, parentId);
      initialValues.put(KEY_STATUS, status);
    }
    return initialValues;
  }

  private void throwExternalNotAvailable() {
    throw new ExternalStorageNotAvailableException();
  }

  protected void savePicture(ContentValues initialValues) {
    if (pictureUri != null) {
      String pictureUriBase = PictureDirHelper.getPictureUriBase(false);
      if (pictureUriBase == null) {
        throwExternalNotAvailable();
      }
      if (pictureUri.toString().startsWith(pictureUriBase)) {
        Timber.d("got Uri in our home space, nothing todo");
      } else {
        pictureUriBase = PictureDirHelper.getPictureUriBase(true);
        if (pictureUriBase == null) {
          throwExternalNotAvailable();
        }
        boolean isInTempFolder = pictureUri.toString().startsWith(pictureUriBase);
        Uri homeUri = PictureDirHelper.getOutputMediaUri(false);
        if (homeUri == null) {
          throwExternalNotAvailable();
        }
        try {
          if (isInTempFolder && homeUri.getScheme().equals("file")) {
            if (new File(pictureUri.getPath()).renameTo(new File(homeUri.getPath()))) {
              setPictureUri(homeUri);
            } else {
              //fallback
              copyPictureHelper(isInTempFolder, homeUri);
            }
          } else {
            copyPictureHelper(isInTempFolder, homeUri);
          }
        } catch (IOException e) {
          throw new UnknownPictureSaveException(pictureUri, homeUri, e);
        }
      }
      initialValues.put(KEY_PICTURE_URI, pictureUri.toString());
    } else {
      initialValues.putNull(KEY_PICTURE_URI);
    }
  }

  private void copyPictureHelper(boolean delete, Uri homeUri) throws IOException {
    FileCopyUtils.copy(pictureUri, homeUri);
    if (delete) {
      new File(pictureUri.getPath()).delete();
    }
    setPictureUri(homeUri);
  }

  public Uri saveAsNew() {
    setId(0L);
    uuid = null;
    return save();
  }

  /**
   * @param whichTransactionId
   * @param whereAccountId
   */
  public static void move(long whichTransactionId, long whereAccountId) {
    ContentValues args = new ContentValues();
    args.put(KEY_ACCOUNTID, whereAccountId);
    cr().update(Uri.parse(
        CONTENT_URI + "/" + whichTransactionId + "/" + TransactionProvider.URI_SEGMENT_MOVE + "/" + whereAccountId),
        null, null, null);
  }

  public static int count(Uri uri, String selection, String[] selectionArgs) {
    Cursor cursor = cr().query(uri, new String[]{"count(*)"},
        selection, selectionArgs, null);
    if (cursor.getCount() == 0) {
      cursor.close();
      return 0;
    } else {
      cursor.moveToFirst();
      int result = cursor.getInt(0);
      cursor.close();
      return result;
    }
  }

  public static int countAll(Uri uri) {
    return count(uri, null, null);
  }

  public static int countPerCategory(Uri uri, long catId) {
    return count(uri, KEY_CATID + " = ?", new String[]{String.valueOf(catId)});
  }

  public static int countPerMethod(Uri uri, long methodId) {
    return count(uri, KEY_METHODID + " = ?", new String[]{String.valueOf(methodId)});
  }

  public static int countPerAccount(Uri uri, long accountId) {
    return count(uri, KEY_ACCOUNTID + " = ?", new String[]{String.valueOf(accountId)});
  }

  public static int countPerCategory(long catId) {
    return countPerCategory(CONTENT_URI, catId);
  }

  public static int countPerMethod(long methodId) {
    return countPerMethod(CONTENT_URI, methodId);
  }

  public static int countPerAccount(long accountId) {
    return countPerAccount(CONTENT_URI, accountId);
  }

  public static int countPerUuid(String uuid) {
    return countPerUuid(CONTENT_URI, uuid);
  }

  private static int countPerUuid(Uri contentUri, String uuid) {
    return count(contentUri, KEY_UUID + " = ?", new String[]{uuid});
  }

  public static int countAll() {
    return countAll(CONTENT_URI);
  }

  /**
   * @return the number of transactions that have been created since creation of the db based on sqllite sequence
   */
  public static Long getSequenceCount() {
    Cursor mCursor = cr().query(TransactionProvider.SQLITE_SEQUENCE_TRANSACTIONS_URI,
        null, null, null, null);
    if (mCursor == null) {
      return 0L;
    }
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return 0L;
    }
    mCursor.moveToFirst();
    Long result = mCursor.getLong(0);
    mCursor.close();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Transaction other = (Transaction) obj;
    if (accountId == null) {
      if (other.accountId != null)
        return false;
    } else if (!accountId.equals(other.accountId))
      return false;
    if (amount == null) {
      if (other.amount != null)
        return false;
    } else if (!amount.equals(other.amount))
      return false;
    if (getCatId() == null) {
      if (other.getCatId() != null)
        return false;
    } else if (!getCatId().equals(other.getCatId()))
      return false;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    if (date == null) {
      if (other.date != null)
        return false;
    } else if (Math.abs(date.getTime() - other.date.getTime()) > 30000) //30 seconds tolerance
      return false;
    if (getId() == null) {
      if (other.getId() != null)
        return false;
    } else if (!getId().equals(other.getId()))
      return false;
    //label is constructed on hoc by database as a consquence of transfer_account and category
    //and is not yet set when transaction is not saved, hence we do not consider it relevant
    //here for equality
/*    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;*/
    if (methodId == null) {
      if (other.methodId != null)
        return false;
    } else if (!methodId.equals(other.methodId))
      return false;
    if (payee == null) {
      if (other.payee != null)
        return false;
    } else if (!payee.equals(other.payee))
      return false;
    if (transfer_account == null) {
      if (other.transfer_account != null)
        return false;
    } else if (!transfer_account.equals(other.transfer_account))
      return false;
    if (transfer_peer == null) {
      if (other.transfer_peer != null)
        return false;
    } else if (!transfer_peer.equals(other.transfer_peer))
      return false;
    if (pictureUri == null) {
      if (other.pictureUri != null)
        return false;
    } else if (!pictureUri.equals(other.pictureUri))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.comment != null ? this.comment.hashCode() : 0;
    result = 31 * result + (this.payee != null ? this.payee.hashCode() : 0);
    result = 31 * result + (this.referenceNumber != null ? this.referenceNumber.hashCode() : 0);
    result = 31 * result + (this.label != null ? this.label.hashCode() : 0);
    result = 31 * result + (this.date != null ? this.date.hashCode() : 0);
    result = 31 * result + (this.amount != null ? this.amount.hashCode() : 0);
    result = 31 * result + (this.transferAmount != null ? this.transferAmount.hashCode() : 0);
    result = 31 * result + (this.catId != null ? this.catId.hashCode() : 0);
    result = 31 * result + (this.accountId != null ? this.accountId.hashCode() : 0);
    result = 31 * result + (this.transfer_peer != null ? this.transfer_peer.hashCode() : 0);
    result = 31 * result + (this.transfer_account != null ? this.transfer_account.hashCode() : 0);
    result = 31 * result + (this.methodId != null ? this.methodId.hashCode() : 0);
    result = 31 * result + (this.methodLabel != null ? this.methodLabel.hashCode() : 0);
    result = 31 * result + (this.parentId != null ? this.parentId.hashCode() : 0);
    result = 31 * result + (this.payeeId != null ? this.payeeId.hashCode() : 0);
    result = 31 * result + (this.originTemplate != null ? this.originTemplate.hashCode() : 0);
    result = 31 * result + (this.originPlanInstanceId != null ? this.originPlanInstanceId.hashCode() : 0);
    result = 31 * result + this.status;
    result = 31 * result + (this.crStatus != null ? this.crStatus.hashCode() : 0);
    result = 31 * result + (this.pictureUri != null ? this.pictureUri.hashCode() : 0);
    return result;
  }

  public Uri getPictureUri() {
    return pictureUri;
  }

  public void setPictureUri(Uri pictureUriIn) {
    this.pictureUri = pictureUriIn;
  }

  public static class ExternalStorageNotAvailableException extends IllegalStateException {
  }

  public static class UnknownPictureSaveException extends IllegalStateException {
    public Uri pictureUri, homeUri;

    public UnknownPictureSaveException(Uri pictureUri, Uri homeUri, IOException e) {
      super(e);
      this.pictureUri = pictureUri;
      this.homeUri = homeUri;
    }
  }

  public static long findByUuid(String uuid) {
    String selection = KEY_UUID + " = ?";
    String[] selectionArgs = new String[]{uuid};

    Cursor mCursor = cr().query(CONTENT_URI,
        new String[]{KEY_ROWID}, selection, selectionArgs, null);
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return -1;
    } else {
      mCursor.moveToFirst();
      long result = mCursor.getLong(0);
      mCursor.close();
      return result;
    }
  }
}
