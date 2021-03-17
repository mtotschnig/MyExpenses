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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.RemoteException;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.viewmodel.data.PlanInstance;
import org.totschnig.myexpenses.viewmodel.data.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.FULL_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_EXECUTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_EXECUTION_ADVANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.LABEL_SUB_TEMPLATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PLAN_INSTANCE_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DbUtils.getLongOrNull;

public class Template extends Transaction implements ITransfer, ISplit {
  public enum Action {
    SAVE, EDIT;
    public static final String JOIN;
    static {
      JOIN = TextUtils.joinEnum(Action.class);
    }
  }
  private static String PART_SELECT = "(" + KEY_PARENTID + "= ?)";
  private String title;
  public Long planId;
  private boolean planExecutionAutomatic = false;

  private Action defaultAction = Action.SAVE;

  private int planExecutionAdvance = 0;

  public Transaction getTemplate() {
    return template;
  }

  private final Transaction template;

  @Nullable
  public Plan getPlan() {
    return plan;
  }

  public void setPlan(Plan plan) {
    this.plan = plan;
  }

  private Plan plan;

  public static final Uri CONTENT_URI = TransactionProvider.TEMPLATES_URI;

  @Override
  public Uri linkedTagsUri() {
    return TransactionProvider.TEMPLATES_TAGS_URI;
  }

  @NonNull
  @Override
  public String linkColumn() {
    return KEY_TEMPLATEID;
  }

  public static final String[] PROJECTION_BASE, PROJECTION_EXTENDED;

  static {
    PROJECTION_BASE = new String[]{
        KEY_ROWID,
        KEY_AMOUNT,
        KEY_COMMENT,
        KEY_CATID,
        LABEL_MAIN,
        FULL_LABEL,
        LABEL_SUB_TEMPLATE,
        KEY_PAYEE_NAME,
        KEY_TRANSFER_ACCOUNT,
        KEY_ACCOUNTID,
        KEY_METHODID,
        KEY_TITLE,
        KEY_PLANID,
        KEY_PLAN_EXECUTION,
        KEY_UUID,
        KEY_PARENTID,
        KEY_PLAN_EXECUTION_ADVANCE,
        KEY_DEFAULT_ACTION
    };
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength + 3];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = KEY_COLOR;
    PROJECTION_EXTENDED[baseLength + 1] = KEY_CURRENCY;
    PROJECTION_EXTENDED[baseLength + 2] = KEY_METHOD_LABEL;
  }

  /**
   * derives a new template from an existing Transaction
   *
   * @param t     the transaction whose data (account, amount, category, comment, payment method, payee,
   *              populates the template
   * @param title identifies the template in the template list
   */
  public Template(Transaction t, String title) {
    super();
    setTitle(title);
    if (t instanceof Transfer) {
      template = new Transfer(t.getAccountId(), t.getAmount(), t.getTransferAccountId());
    } else if (t instanceof SplitTransaction) {
      template = new SplitTransaction(t.getAccountId(), t.getAmount());
    } else {
      template = new Transaction(t.getAccountId(), t.getAmount());
    }
    setCatId(t.getCatId());
    setLabel(t.getLabel());
    setComment(t.getComment());
    setMethodId(t.getMethodId());
    setMethodLabel(t.getMethodLabel());
    setPayee(t.getPayee());
    if (isSplit()) {
      persistForEdit();
      Cursor c = cr().query(Transaction.CONTENT_URI, new String[]{KEY_ROWID},
          KEY_PARENTID + " = ?", new String[]{String.valueOf(t.getId())}, null);
      if (c != null) {
        c.moveToFirst();
        while (!c.isAfterLast()) {
          Transaction splitPart = t.getSplitPart(c.getLong(0));
          if (splitPart != null) {
            Template part = new Template(splitPart, title);
            part.setStatus(STATUS_UNCOMMITTED);
            part.setParentId(getId());
            part.save();
          }
          c.moveToNext();
        }
        c.close();
      }
    }
  }

  @Override
  public void setCatId(Long catId) {
    template.setCatId(catId);
  }

  @Override
  public Long getCatId() {
    return template.getCatId();
  }

  @Override
  public void setComment(String comment) {
    template.setComment(comment);
  }

  @Override
  public String getComment() {
    return template.getComment();
  }

  @Override
  public void setMethodId(Long methodId) {
    template.setMethodId(methodId);
  }

  @Override
  public Long getMethodId() {
    return template.getMethodId();
  }

  @Override
  public void setMethodLabel(String methodLabel) {
    template.setMethodLabel(methodLabel);
  }

  @Override
  public String getMethodLabel() {
    return template.getMethodLabel();
  }

  @Override
  public void setPayeeId(Long payeeId) {
    template.setPayeeId(payeeId);
  }

  @Override
  public Long getPayeeId() {
    return template.getPayeeId();
  }

  @Override
  public void setPayee(String payee) {
    template.setPayee(payee);
  }

  @Override
  public String getPayee() {
    return template.getPayee();
  }

  @Override
  public void setAmount(Money amount) {
    if (template instanceof Transfer) {
      //transfer template only have one part set
      ((Transfer) template).setAmountAndTransferAmount(amount, null);
    } else {
      template.setAmount(amount);
    }
  }

  @Override
  public Money getAmount() {
    return template.getAmount();
  }

  @Override
  public void setAccountId(Long accountId) {
    template.setAccountId(accountId);
  }

  @Override
  public long getAccountId() {
    return template.getAccountId();
  }

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  /**
   * @param c
   */
  public Template(Cursor c) {
    super();
    CurrencyUnit currency;
    final CurrencyContext currencyContext = MyApplication.getInstance().getAppComponent().currencyContext();
    int currencyColumnIndex = c.getColumnIndex(KEY_CURRENCY);
    long accountId = c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID));
    //we allow the object to be instantiated without instantiation of
    //the account, because the latter triggers an error (getDatabase called recursively)
    //when we need a template instance in database onUpgrade
    if (currencyColumnIndex != -1) {
      currency = currencyContext.get(c.getString(currencyColumnIndex));
    } else {
      currency = Account.getInstanceFromDb(accountId).getCurrencyUnit();
    }
    Money amount = new Money(currency, c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT)));
    boolean isTransfer = !c.isNull(c.getColumnIndexOrThrow(KEY_TRANSFER_ACCOUNT));
    Long catId = getLongOrNull(c, KEY_CATID);
    if (isTransfer) {
      template = new Transfer(accountId, amount, DbUtils.getLongOrNull(c, KEY_TRANSFER_ACCOUNT));
    } else {
      if (DatabaseConstants.SPLIT_CATID.equals(catId)) {
        template = new SplitTransaction(accountId, amount);
      } else {
        template = new Transaction(accountId, amount);
        setCatId(catId);
      }
      setMethodId(DbUtils.getLongOrNull(c, KEY_METHODID));
      setPayee(DbUtils.getString(c, KEY_PAYEE_NAME));
      setMethodLabel(DbUtils.getString(c, KEY_METHOD_LABEL));
    }
    setId(c.getLong(c.getColumnIndexOrThrow(KEY_ROWID)));
    setComment(DbUtils.getString(c, KEY_COMMENT));
    setLabel(DbUtils.getString(c, KEY_LABEL));
    setTitle(DbUtils.getString(c, KEY_TITLE));
    planId = DbUtils.getLongOrNull(c, KEY_PLANID);
    setParentId(DbUtils.getLongOrNull(c, KEY_PARENTID));
    setPlanExecutionAutomatic(c.getInt(c.getColumnIndexOrThrow(KEY_PLAN_EXECUTION)) > 0);
    planExecutionAdvance = c.getInt(c.getColumnIndex(KEY_PLAN_EXECUTION_ADVANCE));
    int uuidColumnIndex = c.getColumnIndexOrThrow(KEY_UUID);
    if (c.isNull(uuidColumnIndex)) {//while upgrade to DB schema 47, uuid is still null
      setUuid(generateUuid());
    } else {
      setUuid(DbUtils.getString(c, KEY_UUID));
    }
    setSealed(c.getInt(c.getColumnIndexOrThrow(KEY_SEALED)) > 0);
    try {
      defaultAction = Action.valueOf(c.getString(c.getColumnIndex(KEY_DEFAULT_ACTION)));
    } catch (IllegalArgumentException ignored) {}
  }

  public Template(Account account, int operationType, Long parentId) {
    super();
    setTitle("");
    switch (operationType) {
      case TYPE_TRANSACTION:
        template = Transaction.getNewInstance(account.getId());
        break;
      case TYPE_TRANSFER:
        template = Transfer.getNewInstance(account.getId());
        break;
      case TYPE_SPLIT:
        template = SplitTransaction.getNewInstance(account, false);
        break;
      default:
        throw new UnsupportedOperationException(
            String.format(Locale.ROOT, "Unknown type %d", operationType));
    }
    setParentId(parentId);
  }

  @Nullable
  public static Template getTypedNewInstance(int operationType, Long accountId, boolean forEdit, Long parentId) {
    Account account = Account.getInstanceFromDbWithFallback(accountId);
    if (account == null) {
      return null;
    }
    Template t = new Template(account, operationType, parentId);
    if (forEdit && t.isSplit()) {
      if (!t.persistForEdit()) {
        return null;
      }
    }
    return t;
  }

  private boolean persistForEdit() {
    setStatus(STATUS_UNCOMMITTED);
    return save() != null;
  }

  /**
   * @param planId
   * @param instanceId
   * @return a template that is linked to the calendar event with id planId, but only if the instance instanceId
   * has not yet been dealt with
   */
  public static Template getInstanceForPlanIfInstanceIsOpen(long planId, long instanceId) {
    Cursor c = cr().query(
        CONTENT_URI,
        null,
        KEY_PLANID + "= ? AND NOT exists(SELECT 1 from " + TABLE_PLAN_INSTANCE_STATUS
            + " WHERE " + KEY_INSTANCEID + " = ? AND " + KEY_TEMPLATEID + " = " + KEY_ROWID + ")",
        new String[]{String.valueOf(planId), String.valueOf(instanceId)},
        null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    Template t = new Template(c);
    c.close();
    return t;
  }

  @Nullable
  public static PlanInstance getPlanInstance(long planId, long date) {
    PlanInstance planInstance = null;
    try (Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_INSTANCE, String.valueOf(CalendarProviderProxy.calculateId(date))).build(),
        null, KEY_PLANID + "= ?",
        new String[]{String.valueOf(planId)},
        null)) {
      if (c != null && c.moveToFirst()) {
        final Long instanceId = getLongOrNull(c, KEY_INSTANCEID);
        final Long transactionId = getLongOrNull(c, KEY_TRANSACTIONID);
        final long templateId = c.getLong(c.getColumnIndex(KEY_ROWID));
        final CurrencyContext currencyContext = MyApplication.getInstance().getAppComponent().currencyContext();
        CurrencyUnit currency = currencyContext.get(c.getString(c.getColumnIndex(KEY_CURRENCY)));
        Money amount = new Money(currency, c.getLong(c.getColumnIndex(KEY_AMOUNT)));
        planInstance = new PlanInstance(templateId, instanceId, transactionId, c.getString(c.getColumnIndex(KEY_TITLE)), date, c.getInt(c.getColumnIndex(KEY_COLOR)), amount,
            c.getInt(c.getColumnIndex(KEY_SEALED)) == 1);
      }
    }
    return planInstance;
  }

  @Nullable
  public static kotlin.Pair<Transaction, List<Tag>> getInstanceFromDbWithTags(long id) {
    Template t = getInstanceFromDb(id);
    return t == null ? null : new kotlin.Pair<>(t, t.loadTags());
  }

  @Nullable
  public static Template getInstanceFromDb(long id) {
    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null, null, null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    Template t = new Template(c);
    c.close();
    if (t.planId != null) {
      t.plan = Plan.getInstanceFromDb(t.planId);
    }
    return t;
  }

  public static Template getInstanceFromDbIfInstanceIsOpen(long id, long instanceId) {
    Cursor c = cr().query(
        CONTENT_URI,
        null,
        KEY_ROWID + "= ? AND NOT exists(SELECT 1 from " + TABLE_PLAN_INSTANCE_STATUS
            + " WHERE " + KEY_INSTANCEID + " = ? AND " + KEY_TEMPLATEID + " = " + KEY_ROWID + ")",
        new String[]{String.valueOf(id), String.valueOf(instanceId)},
        null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    Template t = new Template(c);
    c.close();
    return t;
  }

  @Override
  public Uri save(boolean withCommit) {
    return save(null);
  }

  /**
   * Saves the new template, or update an existing one
   *
   * @return the Uri of the template. Upon creation it is returned from the content provider, null if inserting fails on constraints
   */
  public Uri save(Long withLinkedTransaction) {
    if (plan != null) {
      Uri planUri = plan.save();
      if (planUri != null) {
        planId = ContentUris.parseId(planUri);
      }
    }
    Uri uri;
    Long payee_id = Payee.require(getPayee());
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, getComment());
    initialValues.put(KEY_AMOUNT, getAmount().getAmountMinor());
    if (isTransfer()) {
      initialValues.put(KEY_TRANSFER_ACCOUNT, template.getTransferAccountId());
    } else {
      initialValues.put(KEY_CATID, getCatId());
    }
    initialValues.put(KEY_PAYEEID, payee_id);
    initialValues.put(KEY_METHODID, getMethodId());
    initialValues.put(KEY_TITLE, getTitle());
    initialValues.put(KEY_PLANID, planId);
    initialValues.put(KEY_PLAN_EXECUTION, isPlanExecutionAutomatic());
    initialValues.put(KEY_PLAN_EXECUTION_ADVANCE, planExecutionAdvance);
    initialValues.put(KEY_DEFAULT_ACTION, defaultAction.name());
    initialValues.put(KEY_ACCOUNTID, getAccountId());
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    if (getId() == 0) {
      initialValues.put(KEY_UUID, requireUuid());
      initialValues.put(KEY_STATUS, getStatus());
      initialValues.put(KEY_PARENTID, getParentId());
      try {
        ops.add(ContentProviderOperation.newInsert(CONTENT_URI).withValues(initialValues).build());
        if (withLinkedTransaction != null) {
          ops.add(ContentProviderOperation.newInsert(TransactionProvider.PLAN_INSTANCE_STATUS_URI)
              .withValueBackReference(KEY_TEMPLATEID, 0)
              .withValue(KEY_INSTANCEID, CalendarProviderProxy.calculateId(plan.getDtStart()))
              .withValue(KEY_TRANSACTIONID, withLinkedTransaction)
              .build());
        }
        ContentProviderResult[] result = cr().applyBatch(TransactionProvider.AUTHORITY, ops);
        uri = result[0].uri;
      } catch (RemoteException | OperationApplicationException | SQLiteConstraintException e) {
        return null;
      }
      setId(ContentUris.parseId(uri));
      if (plan != null) {
        plan.updateCustomAppUri(buildCustomAppUri(getId()));
      }
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      ops.add(ContentProviderOperation.newUpdate(uri).withValues(initialValues).build());
      if (withLinkedTransaction != null) {
        ops.add(ContentProviderOperation.newInsert(TransactionProvider.PLAN_INSTANCE_STATUS_URI)
            .withValue(KEY_TEMPLATEID, getId())
            .withValue(KEY_INSTANCEID, CalendarProviderProxy.calculateId(plan.getDtStart()))
            .withValue(KEY_TRANSACTIONID, withLinkedTransaction)
            .build());
      }
      addCommitOperations(CONTENT_URI, ops);
      try {
        cr().applyBatch(TransactionProvider.AUTHORITY, ops);
      } catch (RemoteException | OperationApplicationException | SQLiteConstraintException e) {
        return null;
      }
    }
    updateNewPlanEnabled();
    return uri;
  }

  public static void delete(long id, boolean deletePlan) {
    Template t = getInstanceFromDb(id);
    if (t == null) {
      return;
    }
    if (t.planId != null) {
      if (deletePlan) {
        Plan.delete(t.planId);
      }
      cr().delete(
          TransactionProvider.PLAN_INSTANCE_STATUS_URI,
          KEY_TEMPLATEID + " = ?",
          new String[]{String.valueOf(id)});
    }
    cr().delete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null,
        null);
    updateNewPlanEnabled();
  }

  public static int countPerMethod(long methodId) {
    return countPerMethod(CONTENT_URI, methodId);
  }

  public static int countPerAccount(long accountId) {
    return countPerAccount(CONTENT_URI, accountId);
  }

  public static int countAll() {
    return countAll(CONTENT_URI);
  }

  public static String buildCustomAppUri(long id) {
    return ContentUris.withAppendedId(Template.CONTENT_URI, id).toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj)) {
      Timber.d("Template differs %d" , 1);
      return false;
    }
    if (getClass() != obj.getClass()) {
      Timber.d("Template differs %d" , 2);
      return false;
    }
    Template other = (Template) obj;
    if (isPlanExecutionAutomatic() != other.isPlanExecutionAutomatic()) {
      Timber.d("Template differs %d" , 3);
      return false;
    }
    if (planId == null) {
      if (other.planId != null) {
        Timber.d("Template differs %d" , 4);
        return false;
      }
    } else if (!planId.equals(other.planId)) {
      Timber.d("Template differs %d" , 5);
      return false;
    }
    if (getTitle() == null) {
      if (other.getTitle() != null) {
        Timber.d("Template differs %d" , 6);
        return false;
      }
    } else if (!getTitle().equals(other.getTitle())) {
      Timber.d("Template differs %d" , 7);
      return false;
    }
    if (getUuid() == null) {
      if (other.getUuid() != null) {
        Timber.d("Template differs %d" , 8);
        return false;
      }
    } else if (!getUuid().equals(other.getUuid())) {
      Timber.d("Template differs %d" , 9);
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.getTitle() != null ? this.getTitle().hashCode() : 0;
    result = 31 * result + (this.planId != null ? this.planId.hashCode() : 0);
    result = 31 * result + (this.isPlanExecutionAutomatic() ? 1 : 0);
    result = 31 * result + (this.getUuid() != null ? this.getUuid().hashCode() : 0);
    return result;
  }

  public static void updateNewPlanEnabled() {
    final AppComponent appComponent = MyApplication.getInstance().getAppComponent();
    LicenceHandler licenceHandler = appComponent.licenceHandler();
    PrefHandler prefHandler = appComponent.prefHandler();
    boolean newPlanEnabled = true, newSplitTemplateEnabled = true;
    if (!licenceHandler.hasAccessTo(ContribFeature.PLANS_UNLIMITED)) {
      if (count(Template.CONTENT_URI, KEY_PLANID + " is not null", null) >= ContribFeature.FREE_PLANS) {
        newPlanEnabled = false;
      }
    }
    prefHandler.putBoolean(PrefKey.NEW_PLAN_ENABLED, newPlanEnabled);

    if (!licenceHandler.hasAccessTo(ContribFeature.SPLIT_TEMPLATE)) {
      if (count(Template.CONTENT_URI, KEY_CATID + " = " + DatabaseConstants.SPLIT_CATID, null) >= ContribFeature.FREE_SPLIT_TEMPLATES) {
        newSplitTemplateEnabled = false;
      }
    }
    prefHandler.putBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, newSplitTemplateEnabled);
  }

  public boolean isPlanExecutionAutomatic() {
    return planExecutionAutomatic;
  }

  public void setPlanExecutionAutomatic(boolean planExecutionAutomatic) {
    this.planExecutionAutomatic = planExecutionAutomatic;
  }

  public int getPlanExecutionAdvance() {
    return planExecutionAdvance;
  }

  public void setPlanExecutionAdvance(int planExecutionAdvance) {
    this.planExecutionAdvance = planExecutionAdvance;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public int operationType() {
    return template.operationType();
  }

  @Override
  public Long getTransferAccountId() {
    if (isTransfer()) {
      return template.getTransferAccountId();
    }
    CrashHandler.report("Tried to get transfer account for a template that is no transfer");
    return null;
  }

  @Override
  public void setTransferAccountId(Long transferAccountId) {
    if (isTransfer()) {
      template.setTransferAccountId(transferAccountId);
    } else {
      CrashHandler.report("Tried to set transfer account for a template that is no transfer");
    }
  }

  public Uri getContentUri() {
    return CONTENT_URI;
  }

  @Override
  public String getUncommittedView() {
    return VIEW_TEMPLATES_UNCOMMITTED;
  }

  @Override
  protected String getPartOrPeerSelect() {
    return PART_SELECT;
  }

  @Override
  protected Transaction getSplitPart(long partId) {
    return Template.getInstanceFromDb(partId);
  }

  @Nullable
  @Override
  public Long getTransferPeer() {
    return null;
  }

  @Override
  public void setTransferPeer(@Nullable Long transferPeer) {
    throw new IllegalStateException("Transfer templates have no transferPeer");
  }

  public static void cleanupCanceledEdit(Long id) {
    cleanupCanceledEdit(id, CONTENT_URI, PART_SELECT);
  }

  public Action getDefaultAction() {
    return defaultAction;
  }

  public void setDefaultAction(Action defaultAction) {
    this.defaultAction = defaultAction;
  }
}