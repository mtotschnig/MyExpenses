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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.db2.RepositoryTagsKt;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.PlannerUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.UriExtKt;
import org.totschnig.myexpenses.ui.DisplayParty;
import org.totschnig.myexpenses.util.ICurrencyFormatter;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.viewmodel.data.Tag;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Pair;
import kotlin.Triple;

import static android.text.TextUtils.isEmpty;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.CursorExtKt.getLongOrNull;
import static org.totschnig.myexpenses.provider.CursorExtKt.getString;
import static org.totschnig.myexpenses.provider.CursorExtKt.getStringOrNull;
import static org.totschnig.myexpenses.provider.DatabaseConstants.CATEGORY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.TransactionProvider.UNCOMMITTED_URI;
import static org.totschnig.myexpenses.util.CurrencyFormatterKt.formatMoney;

/**
 * Domain class for transactions
 *
 * @author Michael Totschnig
 */
public class Transaction extends Model implements ITransaction {
  private String comment = "";
  private DisplayParty party = null;
  private String referenceNumber = "";
  private String categoryPath = "";
  /**
   * seconds since epoch
   */
  private long date;
  private long valueDate;
  private Money amount;
  private Money transferAmount;
  private Money originalAmount;
  private Money equivalentAmount;
  private Long catId;
  private long accountId;
  private Long methodId;
  private String methodLabel = null;
  private Long parentId = null;
  private Long debtId = null;
  private String categoryIcon = null;
  private boolean isSealed = false;

  transient private Triple<String, ? extends Plan.Recurrence, LocalDate> initialPlan;

  public void setInitialPlan(@Nullable Triple<String, ? extends Plan.Recurrence, LocalDate> initialPlan) {
    this.initialPlan = initialPlan;
  }

  /**
   * template which defines the plan for which this transaction has been created
   */
  private Long originTemplateId = null;
  /**
   * the id of the calendar plan for which this transaction has been created
   */
  private Long originPlanId = null;
  /**
   * id of an instance of the event (plan) for which this transaction has been created
   */
  private Long originPlanInstanceId = null;
  /**
   * 0 = is normal, special states are
   * {@link org.totschnig.myexpenses.provider.DatabaseConstants#STATUS_EXPORTED} and
   * {@link org.totschnig.myexpenses.provider.DatabaseConstants#STATUS_UNCOMMITTED}
   */
  private int status = 0;

  public static final Uri CONTENT_URI = TransactionProvider.TRANSACTIONS_URI;
  public static final Uri CALLER_IS_SYNC_ADAPTER_URI = UriExtKt.fromSyncAdapter(CONTENT_URI);

  @NonNull
  public Money getAmount() {
    return amount;
  }

  public void setAmount(@NonNull Money amount) {
    this.amount = amount;
  }

  public Money getTransferAmount() {
    return transferAmount;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = StringUtils.strip(comment);
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = StringUtils.strip(referenceNumber);
  }

  public Long getMethodId() {
    return methodId;
  }

  public void setMethodId(Long methodId) {
    this.methodId = methodId;
  }

  public String getMethodLabel() {
    return methodLabel;
  }

  public void setMethodLabel(String methodLabel) {
    this.methodLabel = methodLabel;
  }

  @Nullable
  public DisplayParty getParty() {
    return party;
  }

  @Override
  public void setParty(@Nullable DisplayParty party) {
    this.party = party;
  }

  public Long getDebtId() {
    return debtId;
  }

  public void setDebtId(Long debtId) {
    this.debtId = debtId;
  }

  /**
   * stores the full path of the category
   */
  @Nullable
  public String getCategoryPath() {
    return categoryPath;
  }

  public void setCategoryPath(String label) {
    this.categoryPath = label;
  }

  public void setTransferAmount(Money transferAmount) {
    this.transferAmount = transferAmount;
  }

  public long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public void setTransferAccountId(Long transferAccountId) {
    //noop, convenience that allows to set transfer account on template and transfer without cast
  }

  public Long getTransferAccountId() {
    return null; //convenience that allows to set transfer account on template and transfer without cast
  }

  public void setOriginalAmount(Money originalAmount) {
    this.originalAmount = originalAmount;
  }

  public Money getOriginalAmount() {
    return originalAmount;
  }

  public Money getEquivalentAmount() {
    return equivalentAmount;
  }

  public void setEquivalentAmount(Money equivalentAmount) {
    this.equivalentAmount = equivalentAmount;
  }

  @NonNull
  public CrStatus getCrStatus() {
    return crStatus;
  }

  public void setCrStatus(@NonNull CrStatus crStatus) {
    this.crStatus = crStatus;
  }

  public String getCategoryIcon() {
    return categoryIcon;
  }

  public void setCategoryIcon(String categoryIcon) {
    this.categoryIcon = categoryIcon;
  }

  public boolean isSealed() {
    return isSealed;
  }

  public void setSealed(boolean sealed) {
    isSealed = sealed;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public void setStatus(int status) {
    this.status = status;
  }

  @Nullable
  @Override
  public Long getOriginTemplateId() {
    return originTemplateId;
  }

  @Override
  public void setOriginTemplateId(Long originTemplateId) {
    this.originTemplateId = originTemplateId;
  }

  @Override
  public void setAccountId(long accountId) {
    this.accountId = accountId;
  }

  @Nullable
  @Override
  public Long getOriginPlanInstanceId() {
    return originPlanInstanceId;
  }

  @Override
  public void setOriginPlanInstanceId(@Nullable Long originPlanInstanceId) {
    this.originPlanInstanceId = originPlanInstanceId;
  }

  @Nullable
  @Override
  public Long getOriginPlanId() {
    return originPlanId;
  }

  @Override
  public void setOriginPlanId(@Nullable Long originPlanId) {
    this.originPlanId = originPlanId;
  }

  @NonNull
  private CrStatus crStatus = CrStatus.UNRECONCILED;

  /**
   * factory method for retrieving an instance from the db with the given id
   *
   * @param homeCurrency May be null in case of split part
   * @return instance of {@link Transaction} or {@link Transfer} or null if not found
   */
  public static Transaction getInstanceFromDb(ContentResolver contentResolver, long id, @Nullable CurrencyUnit homeCurrency) {
    Transaction t;
    final CurrencyContext currencyContext = MyApplication.Companion.getInstance().getAppComponent().currencyContext();
    String[] projection = new String[]{KEY_ROWID, KEY_DATE, KEY_VALUE_DATE, KEY_AMOUNT, KEY_COMMENT, KEY_CATID,
        KEY_PATH, KEY_PAYEEID, KEY_PAYEE_NAME, KEY_SHORT_NAME, KEY_TRANSFER_PEER, KEY_TRANSFER_ACCOUNT, TRANSFER_CURRENCY, KEY_DEBT_ID,
        KEY_ACCOUNTID, KEY_METHODID, KEY_PARENTID, KEY_CR_STATUS, KEY_REFERENCE_NUMBER, KEY_CURRENCY,
        KEY_METHOD_LABEL, KEY_STATUS, KEY_TRANSFER_AMOUNT, KEY_TEMPLATEID, KEY_UUID, KEY_ORIGINAL_AMOUNT, KEY_ORIGINAL_CURRENCY,
        KEY_EQUIVALENT_AMOUNT, CATEGORY_ICON, KEY_SEALED};

    Cursor c = contentResolver.query(
            TransactionProvider.EXTENDED_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection, null, null, null);
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
    Money money = new Money(currencyContext.get(c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY))), amount);
    Long parent_id = getLongOrNull(c, KEY_PARENTID);
    Long catId = getLongOrNull(c, KEY_CATID);
    if (transfer_peer != null) {
      Long transferAccountId = getLongOrNull(c, KEY_TRANSFER_ACCOUNT);
      Transfer transfer = new Transfer(account_id, money, transferAccountId, parent_id);
      transfer.setTransferPeer(transfer_peer);
      transfer.setTransferAmount(new Money(currencyContext.get(c.getString(c.getColumnIndexOrThrow(KEY_TRANSFER_CURRENCY))),
          c.getLong(c.getColumnIndexOrThrow(KEY_TRANSFER_AMOUNT))));
      t = transfer;
    } else {
      if (DatabaseConstants.SPLIT_CATID.equals(catId)) {
        t = new SplitTransaction(account_id, money);
      } else {
        t = new Transaction(account_id, money, parent_id);
        t.setCategoryIcon(c.getString(c.getColumnIndexOrThrow(KEY_ICON)));
      }
    }
    try {
      t.setCrStatus(CrStatus.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_CR_STATUS))));
    } catch (IllegalArgumentException ex) {
      t.setCrStatus(CrStatus.UNRECONCILED);
    }
    t.setMethodId(getLongOrNull(c, KEY_METHODID));
    t.setMethodLabel(getStringOrNull(c, KEY_METHOD_LABEL, false));
    t.setCatId(catId);
    t.setParty(DisplayParty.Companion.fromCursor(c));
    t.setDebtId(getLongOrNull(c, KEY_DEBT_ID));
    t.setId(id);
    final long date = c.getLong(c.getColumnIndexOrThrow(KEY_DATE));
    t.setDate(date);
    final Long valueDate = getLongOrNull(c, KEY_VALUE_DATE);
    t.setValueDate(valueDate == null ? date : valueDate);
    t.setComment(getString(c, KEY_COMMENT));
    t.setReferenceNumber(getString(c, KEY_REFERENCE_NUMBER));
    t.setCategoryPath(getString(c, KEY_PATH));

    Long originalAmount = getLongOrNull(c, KEY_ORIGINAL_AMOUNT);
    if (originalAmount != null) {
      t.setOriginalAmount(new Money(currencyContext.get(c.getString(c.getColumnIndexOrThrow(KEY_ORIGINAL_CURRENCY))), originalAmount));
    }
    if (homeCurrency != null) {
      Long equivalentAmount = getLongOrNull(c, KEY_EQUIVALENT_AMOUNT);
      if (equivalentAmount != null) {
        t.setEquivalentAmount(new Money(homeCurrency, equivalentAmount));
      }
    }

    t.status = c.getInt(c.getColumnIndexOrThrow(KEY_STATUS));
    t.originTemplateId = getLongOrNull(c, KEY_TEMPLATEID);
    t.setUuid(getString(c, KEY_UUID));
    t.setSealed(c.getInt(c.getColumnIndexOrThrow(KEY_SEALED)) > 0);
    c.close();
    return t;
  }

  /**
   *
   * @param homeCurrency may be null in case of split part
   */
  @Nullable
  public static kotlin.Pair<Transaction, List<Tag>> getInstanceFromDbWithTags(ContentResolver contentResolver, long id, @Nullable CurrencyUnit homeCurrency) {
    Transaction t = getInstanceFromDb(contentResolver, id, homeCurrency);
    return t == null ? null : new kotlin.Pair<>(t, t.loadTags(contentResolver));
  }

  @Nullable
  public static kotlin.Triple<Transaction, List<Tag>, Boolean> getInstanceFromTemplateWithTags(ContentResolver contentResolver, long id) {
    Template te = Template.getInstanceFromDb(contentResolver, id);
    return te == null ? null : getInstanceFromTemplateWithTags(contentResolver, te);
  }

  public static Transaction getInstanceFromTemplate(ContentResolver contentResolver, long id) {
    Template te = Template.getInstanceFromDb(contentResolver, id);
    return te == null ? null : getInstanceFromTemplate(contentResolver, te);
  }

  @Nullable
  public static kotlin.Triple<Transaction, List<Tag>, Boolean> getInstanceFromTemplateIfOpen(ContentResolver contentResolver, long id, long instanceId) {
    Template te = Template.getInstanceFromDbIfInstanceIsOpen(contentResolver, id, instanceId);
    return te == null ? null : getInstanceFromTemplateWithTags(contentResolver, te);
  }

  public static Transaction getInstanceFromTemplate(ContentResolver contentResolver, Template te) {
    Transaction tr;
    switch (te.operationType()) {
      case TYPE_TRANSACTION -> {
        tr = new Transaction(te.getAccountId(), te.getAmount());
        tr.setMethodId(te.getMethodId());
        tr.setMethodLabel(te.getMethodLabel());
        tr.setCatId(te.getCatId());
        tr.setDebtId(te.getDebtId());
      }
      case TYPE_TRANSFER -> {
        tr = new Transfer(te.getAccountId(), te.getAmount());
        tr.setTransferAccountId(te.getTransferAccountId());
        tr.setCatId(te.getCatId());
      }
      case TYPE_SPLIT -> {
        tr = new SplitTransaction(te.getAccountId(), te.getAmount());
        tr.setStatus(STATUS_UNCOMMITTED);
        tr.setMethodId(te.getMethodId());
        tr.setMethodLabel(te.getMethodLabel());
        tr.setDebtId(te.getDebtId());
      }
      default -> throw new IllegalStateException(
              String.format(Locale.ROOT, "Unknown type %d", te.operationType()));
    }
    if (te.getOriginalAmount() != null) {
      tr.setOriginalAmount(te.getOriginalAmount());
    }
    tr.setComment(te.getComment());
    tr.setParty(te.getParty());
    tr.setCategoryPath(te.getCategoryPath());
    tr.originTemplateId = te.getId();
    if (tr instanceof SplitTransaction) {
      tr.save(contentResolver);
      Cursor c = contentResolver.query(
              uriForParts(Template.CONTENT_URI, te.getId()),
              new String[]{KEY_ROWID}, null, null, null
      );
      if (c != null) {
        c.moveToFirst();
        while (!c.isAfterLast()) {
          Triple<Transaction, List<Tag>, Boolean> part = Transaction.getInstanceFromTemplateWithTags(
                  contentResolver,
                  c.getLong(c.getColumnIndexOrThrow(KEY_ROWID))
          );
          if (part != null) {
            Transaction t = part.getFirst();
            t.status = STATUS_UNCOMMITTED;
            t.setParentId(tr.getId());
            t.saveAsNew(contentResolver);
            t.saveTags(contentResolver, part.getSecond());
          }
          c.moveToNext();
        }
        c.close();
      }
    }
    contentResolver.update(
            ContentUris.appendId(TransactionProvider.TEMPLATES_URI.buildUpon(), te.getId())
            .appendPath(TransactionProvider.URI_SEGMENT_INCREASE_USAGE)
            .build(),
        null, null, null);
    return tr;
  }

  public static Uri uriForParts(Uri contentUri, long mainId) {
    return contentUri.buildUpon().appendQueryParameter(KEY_PARENTID, String.valueOf(mainId)).build();
  }

  public static kotlin.Triple<Transaction, List<Tag>, Boolean> getInstanceFromTemplateWithTags(ContentResolver contentResolver, Template te) {
    return new kotlin.Triple<>(getInstanceFromTemplate(contentResolver, te), te.loadTags(contentResolver), te.isDynamic);
  }

  @Nullable
  public List<Tag> loadTags(ContentResolver contentResolver) {
    return RepositoryTagsKt.loadTagsForTransaction(contentResolver, getId());
  }

  @Override
  public void saveTags(@NonNull ContentResolver contentResolver, @NonNull  List<Tag> tags) {
    RepositoryTagsKt.saveTagsForTransaction(contentResolver, tags.stream().mapToLong(Tag::getId).toArray(), getId());
    if (initialPlan != null) {
      RepositoryTagsKt.saveTagsForTemplate(contentResolver, tags, originTemplateId);
    }
  }

  /**
   * factory method for creating an object of the correct type and linked to a given account
   * @return instance of {@link Transaction} or {@link Transfer} or {@link SplitTransaction} with date initialized to current date
   */
  public static Transaction getNewInstance(long accountId, CurrencyUnit currencyUnit) {
    return getNewInstance(accountId, currencyUnit, null);
  }

  public static Transaction getNewInstance(long accountId, CurrencyUnit currencyUnit, Long parentId) {
    return new Transaction(accountId, new Money(currencyUnit, 0L), parentId);
  }

  public static int undelete(ContentResolver contentResolver, long id) {
    Uri uri = ContentUris.appendId(CONTENT_URI.buildUpon(), id)
        .appendPath(TransactionProvider.URI_SEGMENT_UNDELETE).build();
    return contentResolver.update(uri, null, null, null);
  }

  public Transaction() {
    final ZonedDateTime now = ZonedDateTime.now();
    setDate(now);
    setValueDate(now);
  }

  public Transaction(long accountId, Money amount) {
    this();
    this.setAccountId(accountId);
    this.setAmount(amount);
  }

  public Transaction(long accountId, Long parentId) {
    this.setAccountId(accountId);
    setParentId(parentId);
  }

  public Transaction(long accountId, Money amount, Long parentId) {
    this(accountId, amount);
    setParentId(parentId);
  }

  public Long getCatId() {
    return catId;
  }

  public void setCatId(Long catId) {
    this.catId = catId;
  }

  @Deprecated
  public void setDate(Date date) {
    setDate(date.getTime() / 1000);
  }

  public void setDate(ZonedDateTime zonedDateTime) {
    setDate(zonedDateTime.toEpochSecond());
  }

  public void setDate(long unixEpoch) {
    this.date = unixEpoch;
  }

  public long getDate() {
    return date;
  }


  public void setValueDate(ZonedDateTime zonedDateTime) {
    setValueDate(zonedDateTime.toEpochSecond());
  }

  public void setValueDate(long unixEpoch) {
    this.valueDate = unixEpoch;
  }

  @Deprecated
  public void setValueDate(Date date) {
    setValueDate(date.getTime() / 1000);
  }


  public long getValueDate() {
    return valueDate;
  }

  @Override
  public Uri save(ContentResolver contentResolver) {
    return save(contentResolver, false);
  }

  @NonNull
  public Uri save(ContentResolver contentResolver, boolean withCommit) {
    return save(contentResolver, null, withCommit);
  }

  @NonNull
  public Uri save(ContentResolver contentResolver, @Nullable PlannerUtils plannerUtils, boolean withCommit) {
    Uri uri;
    try {
      ContentProviderResult[] result = contentResolver.applyBatch(TransactionProvider.AUTHORITY,
          buildSaveOperations(contentResolver, withCommit));
      if (getId() == 0) {
        //we need to find a uri, otherwise we would crash. Need to handle?
        uri = result[0].uri;
        updateFromResult(result);
      } else {
        uri = Uri.parse(CONTENT_URI + "/" + getId());
      }
    } catch (RemoteException | OperationApplicationException e) {
      throw new RuntimeException(e);
    }
    if (initialPlan != null) {
      String title = initialPlan.getFirst() != null ? initialPlan.getFirst() :
              (getParty() != null ? getParty().getName() :
                      (!isSplit() && !isEmpty(getCategoryPath()) ? getCategoryPath() :
                              (!isEmpty(getComment()) ? getComment() :
                                      MyApplication.Companion.getInstance().getString(R.string.menu_create_template)
                              )
                      )
              );
      Template originTemplate = new Template(contentResolver, this, title);
      String description = originTemplate.compileDescription(MyApplication.Companion.getInstance()); //TODO proper context
      originTemplate.setPlanExecutionAutomatic(true);
      Long withLinkedTransaction = null;
      if (initialPlan.getSecond() != Plan.Recurrence.NONE) {
        originTemplate.setPlan(new Plan(initialPlan.getThird(), initialPlan.getSecond(), title, description));
        withLinkedTransaction = getId();
      }
      originTemplate.save(contentResolver, plannerUtils, withLinkedTransaction);
      originTemplateId = originTemplate.getId();
      originPlanId = originTemplate.planId;
    }
    return uri;
  }

  public void updateFromResult(ContentProviderResult[] result) {
    if (getId() == 0) {
        setId(ContentUris.parseId(result[0].uri));
    }
  }

  void addCommitOperations(Uri uri, ArrayList<ContentProviderOperation> ops) {
    if (isSplit()) {
      String idStr = String.valueOf(getId());
      ContentValues statusValues = new ContentValues();
      String statusUncommitted = String.valueOf(STATUS_UNCOMMITTED);
      final String partOrPeerSelect = getPartOrPeerSelect();
      String[] uncommitedPartOrPeerSelectArgs = getPartOrPeerSelectArgs(partOrPeerSelect, statusUncommitted, idStr);
      ops.add(ContentProviderOperation.newDelete(uri).withSelection(
          partOrPeerSelect + "  AND " + KEY_STATUS + " != ?", uncommitedPartOrPeerSelectArgs).build());
      statusValues.put(KEY_STATUS, STATUS_NONE);
      //for a new split, both the parent and the parts are in state uncommitted
      //when we edit a split only the parts are in state uncommitted,
      //in any case we only update the state for rows that are uncommitted, to
      //prevent altering the state of a parent (e.g. from exported to non-exported)
      ops.add(ContentProviderOperation.newUpdate(uri).withValues(statusValues).withSelection(
          KEY_STATUS + " = ? AND " + KEY_ROWID + " = ?",
          new String[]{statusUncommitted, idStr}).build());
      ops.add(ContentProviderOperation.newUpdate(uri).withValues(statusValues).withSelection(
          partOrPeerSelect + "  AND " + KEY_STATUS + " = ?",
          uncommitedPartOrPeerSelectArgs).build());
    }
  }

  protected String getPartOrPeerSelect() {
    return null;
  }

  static String[] getPartOrPeerSelectArgs(String partOrPeerSelect, String extra, String id) {
    int count = StringUtils.countMatches(partOrPeerSelect, '?');
    List<String> args = new ArrayList<>(Collections.nCopies(count, id));
    if (extra != null) {
      args.add(extra);
    }
    return args.toArray(new String[0]);
  }

  /**
   * all Split Parts are cloned and we work with the uncommitted clones
   * @param clone  if true an uncommited clone of the instance is prepared
   */
  public void prepareForEdit(ContentResolver contentResolver, boolean clone, boolean withCurrentDate) {
    if (withCurrentDate) {
      final ZonedDateTime now = ZonedDateTime.now();
      setDate(now);
      setValueDate(now);
    }
    //TODO this needs to be moved into a transaction
    if (isSplit()) {
      Long oldId = getId();
      if (clone) {
        status = STATUS_UNCOMMITTED;
        saveAsNew(contentResolver);
      }
      String idStr = String.valueOf(oldId);
      //we only create uncommited clones if none exist yet
      Cursor c = contentResolver.query(
              uriForParts(getContentUri(), oldId),
              new String[]{KEY_ROWID},
              "NOT EXISTS (SELECT 1 from " + getUncommittedView()
              + " WHERE " + KEY_PARENTID + " = ?)", new String[]{idStr}, null);
      if (c != null) {
        c.moveToFirst();
        while (!c.isAfterLast()) {
          Pair<Transaction, List<Tag>> part = getSplitPart(contentResolver, c.getLong(0));
          if (part != null) {
            Transaction t = part.getFirst();
            t.status = STATUS_UNCOMMITTED;
            t.setParentId(getId());
            t.saveAsNew(contentResolver);
            t.saveTags(contentResolver, part.getSecond());
          }
          c.moveToNext();
        }
        c.close();
      }
    } else if (clone) {
      setId(0);
      setUuid(null);
    }
  }

  protected Pair<Transaction, List<Tag>> getSplitPart(ContentResolver contentResolver, long partId) {
    return Transaction.getInstanceFromDbWithTags(contentResolver, partId, null);
  }

  public Uri getContentUri() {
    return CONTENT_URI;
  }

  public String getUncommittedView() {
    return VIEW_UNCOMMITTED;
  }

  public ArrayList<ContentProviderOperation> buildSaveOperations(ContentResolver contentResolver, boolean withCommit) {
    return buildSaveOperations(contentResolver, 0, -1, false, withCommit);
  }

  /**
   * Constructs the {@link ArrayList} of {@link ContentProviderOperation}s necessary for saving
   * the transaction
   * as a side effect creates payee in database
   *
   * @param offset              Number of operations that are already added to the batch, needed for calculating back references
   * @param parentOffset        if not -1, it indicates at which position in the batch the parent of a new split transaction is situated.
   *                            Is used from SyncAdapter for creating split transactions
   * @param callerIsSyncAdapter true if called from sync adapter
   * @param withCommit          change state from uncommitted to committed
   * @return the URI of the transaction. Upon creation it is returned from the content provider
   */
  public ArrayList<ContentProviderOperation> buildSaveOperations(
          ContentResolver contentResolver,
          int offset, int parentOffset, boolean callerIsSyncAdapter, boolean withCommit) {
    Uri uri = getUriForSave(callerIsSyncAdapter);
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ContentValues initialValues = buildInitialValues(contentResolver);
    if (getId() == 0) {
      ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri).withValues(initialValues);
      if (parentOffset != -1) {
        builder.withValueBackReference(KEY_PARENTID, parentOffset);
      }
      ops.add(builder.build());
    } else {
      ops.add(ContentProviderOperation
          .newUpdate(uri.buildUpon().appendPath(String.valueOf(getId())).build())
          .withValues(initialValues).build());
    }
    addOriginPlanInstance(ops);
    return ops;
  }

  protected Uri getUriForSave(boolean callerIsSyncAdapter) {
    if (getStatus() == STATUS_UNCOMMITTED) return UNCOMMITTED_URI;
    if (callerIsSyncAdapter) return CALLER_IS_SYNC_ADAPTER_URI;
    return CONTENT_URI;
  }

  protected void addOriginPlanInstance(ArrayList<ContentProviderOperation> ops) {
    if (originPlanInstanceId != null) {
      if (originTemplateId != null) {
        ContentValues values = new ContentValues();
        values.put(KEY_TEMPLATEID, originTemplateId);
        values.put(KEY_INSTANCEID, originPlanInstanceId);
        final ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(TransactionProvider.PLAN_INSTANCE_STATUS_URI);
        if (getId() == 0) {
          builder.withValueBackReference(KEY_TRANSACTIONID, 0);
        } else {
          values.put(KEY_TRANSACTIONID, getId());
        }
        ops.add(builder.withValues(values).build());
      } else {
        CrashHandler.report(new IllegalStateException("No originTemplateId provided"));
      }
    }
  }

  public ContentValues buildInitialValues(ContentResolver contentResolver) {
    ContentValues initialValues = new ContentValues();

    Long payeeStore;
    if (getParty() != null) {
      payeeStore = getParty().getId();
    } else {
      payeeStore = null;
    }
    initialValues.put(KEY_COMMENT, getComment());
    initialValues.put(KEY_REFERENCE_NUMBER, getReferenceNumber());
    initialValues.put(KEY_DATE, getDate());
    initialValues.put(KEY_VALUE_DATE, getValueDate());

    initialValues.put(KEY_AMOUNT, getAmount().getAmountMinor());
    initialValues.put(KEY_CATID, getCatId());
    initialValues.put(KEY_PAYEEID, payeeStore);
    initialValues.put(KEY_METHODID, getMethodId());
    initialValues.put(KEY_CR_STATUS, crStatus.name());
    initialValues.put(KEY_ACCOUNTID, getAccountId());
    initialValues.put(KEY_DEBT_ID, getDebtId());

    initialValues.put(KEY_ORIGINAL_AMOUNT, originalAmount == null ? null : originalAmount.getAmountMinor());
    initialValues.put(KEY_ORIGINAL_CURRENCY, originalAmount == null ? null : originalAmount.getCurrencyUnit().getCode());
    initialValues.put(KEY_EQUIVALENT_AMOUNT, equivalentAmount == null ? null : equivalentAmount.getAmountMinor());

    if (getId() == 0) {
      initialValues.put(KEY_PARENTID, getParentId());
      initialValues.put(KEY_STATUS, status);
      initialValues.put(KEY_UUID, requireUuid());
    }
    return initialValues;
  }

  public Uri saveAsNew(ContentResolver contentResolver) {
    setId(0L);
    setUuid(null);
    return save(contentResolver);
  }

  public String compileDescription(MyApplication ctx) {
    ICurrencyFormatter currencyFormatter = ctx.getAppComponent().currencyFormatter();
    StringBuilder sb = new StringBuilder();
    sb.append(ctx.getString(R.string.amount));
    sb.append(" : ");
    sb.append(formatMoney(currencyFormatter, getAmount()));
    sb.append("\n");
    if (getCatId() != null && getCatId() > 0) {
      sb.append(ctx.getString(R.string.category));
      sb.append(" : ");
      sb.append(getCategoryPath());
      sb.append("\n");
    }
    if (isTransfer()) {
      sb.append(ctx.getString(R.string.account));
      sb.append(" : ");
      sb.append(getCategoryPath());
      sb.append("\n");
    }
    //comment
    if (!getComment().equals("")) {
      sb.append(ctx.getString(R.string.notes));
      sb.append(" : ");
      sb.append(getComment());
      sb.append("\n");
    }
    //payee
    if (getParty() != null) {
      sb.append(ctx.getString(
          getAmount().getAmountMajor().signum() == 1 ? R.string.payer : R.string.payee));
      sb.append(" : ");
      sb.append(getParty().getName());
      sb.append("\n");
    }
    //Method
    if (getMethodId() != null) {
      sb.append(ctx.getString(R.string.method));
      sb.append(" : ");
      sb.append(getMethodLabel());
      sb.append("\n");
    }
    sb.append("UUID : ");
    sb.append(requireUuid());
    return sb.toString();
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
    if (getAccountId() != other.getAccountId())
      return false;
    if (!getAmount().equals(other.getAmount()))
      return false;
    if (getCatId() == null) {
      if (other.getCatId() != null)
        return false;
    } else if (!getCatId().equals(other.getCatId()))
      return false;
    if (getComment() == null) {
      if (other.getComment() != null)
        return false;
    } else if (!getComment().equals(other.getComment()))
      return false;
    if (date != other.date)
      return false;
    if (getId() != other.getId())
      return false;
    //label is constructed on hoc by database as a consquence of transfer_account and category
    //and is not yet set when transaction is not saved, hence we do not consider it relevant
    //here for equality
/*    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;*/
    if (getMethodId() == null) {
      if (other.getMethodId() != null)
        return false;
    } else if (!getMethodId().equals(other.getMethodId()))
      return false;
    if (getParty() == null) {
      if (other.getParty() != null)
        return false;
    } else if (!getParty().getId().equals(other.getParty().getId()))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.getComment() != null ? this.getComment().hashCode() : 0;
    result = 31 * result + (this.getReferenceNumber() != null ? this.getReferenceNumber().hashCode() : 0);
    result = 31 * result + (this.getCategoryPath() != null ? this.getCategoryPath().hashCode() : 0);
    result = 31 * result + Long.valueOf(getDate()).hashCode();
    result = 31 * result + this.getAmount().hashCode();
    result = 31 * result + (this.getTransferAmount() != null ? this.getTransferAmount().hashCode() : 0);
    result = 31 * result + (this.catId != null ? this.catId.hashCode() : 0);
    result = 31 * result + Long.valueOf(getAccountId()).hashCode();
    result = 31 * result + (this.getMethodId() != null ? this.getMethodId().hashCode() : 0);
    result = 31 * result + (this.getMethodLabel() != null ? this.getMethodLabel().hashCode() : 0);
    result = 31 * result + (this.getParentId() != null ? this.getParentId().hashCode() : 0);
    result = 31 * result + (this.getParty() != null ? this.getParty().hashCode() : 0);
    result = 31 * result + (this.getOriginTemplateId() != null ? this.getOriginTemplateId().hashCode() : 0);
    result = 31 * result + (this.originPlanInstanceId != null ? this.originPlanInstanceId.hashCode() : 0);
    result = 31 * result + this.status;
    result = 31 * result + this.crStatus.hashCode();
    return result;
  }

  static void cleanupCanceledEdit(ContentResolver contentResolver, Long id, Uri contentUri, String partOrPeerSelect) {
    String idStr = String.valueOf(id);
    String statusUncommitted = String.valueOf(STATUS_UNCOMMITTED);
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    String[] partOrPeerSelectArgs = getPartOrPeerSelectArgs(partOrPeerSelect, statusUncommitted, idStr);
    ops.add(ContentProviderOperation.newDelete(contentUri)
        .withSelection(partOrPeerSelect + "  AND " + KEY_STATUS + " = ?", partOrPeerSelectArgs)
        .build());
    ops.add(ContentProviderOperation.newDelete(contentUri)
        .withSelection(KEY_STATUS + " = ? AND " + KEY_ROWID + " = ?", new String[]{statusUncommitted, idStr})
        .build());
    try {
      contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops);
    } catch (OperationApplicationException | RemoteException e) {
      CrashHandler.report(e);
    }
  }

  public boolean isTransfer() {
    return operationType() == TYPE_TRANSFER;
  }

  public boolean isSplit() {
    return operationType() == TYPE_SPLIT;
  }

  public int operationType() {
    return TYPE_TRANSACTION;
  }
}
