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

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.db2.Repository;
import org.totschnig.myexpenses.db2.RepositoryTagsKt;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.PlannerUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.ui.DisplayParty;
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
import androidx.annotation.RequiresPermission;
import kotlin.Pair;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.CursorExtKt.getBoolean;
import static org.totschnig.myexpenses.provider.CursorExtKt.getLongOrNull;
import static org.totschnig.myexpenses.provider.CursorExtKt.getString;
import static org.totschnig.myexpenses.provider.CursorExtKt.getStringOrNull;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PLAN_INSTANCE_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_TEMPLATES_UNCOMMITTED;

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

  public boolean isDynamic = false;

  public static final Uri CONTENT_URI = TransactionProvider.TEMPLATES_URI;

  @Nullable
  @Override
  public List<Tag> loadTags(ContentResolver contentResolver) {
    return RepositoryTagsKt.loadTagsForTemplate(contentResolver, getId());
  }

  @Override
  public void saveTags(@NonNull ContentResolver contentResolver, @NonNull List<Tag> tags) {
    RepositoryTagsKt.saveTagsForTemplate(contentResolver, tags, getId());
  }

  /**
   * derives a new template from an existing Transaction
   *
   * @param t     the transaction whose data (account, amount, category, comment, payment method, payee,
   *              populates the template
   * @param title identifies the template in the template list
   */
  public Template(ContentResolver contentResolver, Transaction t, String title) {
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
    setCategoryPath(t.getCategoryPath());
    setComment(t.getComment());
    setMethodId(t.getMethodId());
    setMethodLabel(t.getMethodLabel());
    setParty(t.getParty());
    if (isSplit()) {
      persistForEdit(contentResolver);
      Cursor c = contentResolver.query(Transaction.CONTENT_URI, new String[]{KEY_ROWID},
          KEY_PARENTID + " = ?", new String[]{String.valueOf(t.getId())}, null);
      if (c != null) {
        c.moveToFirst();
        while (!c.isAfterLast()) {
          Pair<Transaction, List<Tag>> splitPart = t.getSplitPart(contentResolver, c.getLong(0));
          if (splitPart != null) {
            Template part = new Template(contentResolver, splitPart.getFirst(), title);
            part.setStatus(STATUS_UNCOMMITTED);
            part.setParentId(getId());
            part.save(contentResolver);
            part.saveTags(contentResolver, splitPart.getSecond());
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
  public void setParty(@Nullable DisplayParty displayParty) {
    template.setParty(displayParty);
  }

  @Override
  public DisplayParty getParty() {
    return template.getParty();
  }

  @Override
  public void setAmount(@NonNull Money amount) {
    if (template instanceof Transfer) {
      //transfer template only have one part set
      ((Transfer) template).setAmountAndTransferAmount(amount, null);
    } else {
      template.setAmount(amount);
    }
  }

  @NonNull
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

  @Override
  public void setDate(long unixEpoch) {
    //Templates do not have a date
  }

  @Override
  public long getDate() {
    if (plan != null) {
      return plan.getDtStart() / 1000;
    } else
      return template.getDate();
  }

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public Template(Cursor c) {
    super();
    CurrencyUnit currency;
    final CurrencyContext currencyContext = MyApplication.Companion.getInstance().getAppComponent().currencyContext();
    int currencyColumnIndex = c.getColumnIndex(KEY_CURRENCY);
    long accountId = c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID));
    currency = currencyContext.get(c.getString(currencyColumnIndex));
    Money amount = new Money(currency, c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT)));
    boolean isTransfer = !c.isNull(c.getColumnIndexOrThrow(KEY_TRANSFER_ACCOUNT));
    Long catId = getLongOrNull(c, KEY_CATID);
    if (isTransfer) {
      template = new Transfer(accountId, amount, getLongOrNull(c, KEY_TRANSFER_ACCOUNT));
      setCatId(catId);
    } else {
      if (DatabaseConstants.SPLIT_CATID.equals(catId)) {
        template = new SplitTransaction(accountId, amount);
      } else {
        template = new Transaction(accountId, amount);
        setCatId(catId);
      }
      setMethodId(getLongOrNull(c, KEY_METHODID));
      setParty(DisplayParty.Companion.fromCursor(c));
      setMethodLabel(getString(c, KEY_METHOD_LABEL));
    }
    setId(c.getLong(c.getColumnIndexOrThrow(KEY_ROWID)));
    setComment(getString(c, KEY_COMMENT));
    setCategoryPath(getString(c, KEY_PATH));
    setTitle(getString(c, KEY_TITLE));
    planId = getLongOrNull(c, KEY_PLANID);
    setParentId(getLongOrNull(c, KEY_PARENTID));
    setPlanExecutionAutomatic(c.getInt(c.getColumnIndexOrThrow(KEY_PLAN_EXECUTION)) > 0);
    planExecutionAdvance = c.getInt(c.getColumnIndexOrThrow(KEY_PLAN_EXECUTION_ADVANCE));
    int uuidColumnIndex = c.getColumnIndexOrThrow(KEY_UUID);
    if (c.isNull(uuidColumnIndex)) {//while upgrade to DB schema 47, uuid is still null
      setUuid(generateUuid());
    } else {
      setUuid(getString(c, KEY_UUID));
    }
    setSealed(c.getInt(c.getColumnIndexOrThrow(KEY_SEALED)) > 0);
    try {
      defaultAction = Action.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_DEFAULT_ACTION)));
    } catch (IllegalArgumentException ignored) {}
    setDebtId(getLongOrNull(c, KEY_DEBT_ID));
    String originalCurrency = getStringOrNull(c, KEY_ORIGINAL_CURRENCY, false);
    if (originalCurrency != null) {
      setOriginalAmount(
        new Money(currencyContext.get(originalCurrency),
        c.getLong(c.getColumnIndexOrThrow(KEY_ORIGINAL_AMOUNT)))
      );
    }
    isDynamic = getBoolean(c, KEY_DYNAMIC);
  }

  public Template(
          ContentResolver contentResolver,
          long id,
          CurrencyUnit currencyUnit,
          int operationType,
          Long parentId
  ) {
    super();
    setTitle("");
    switch (operationType) {
      case TYPE_TRANSACTION -> template = Transaction.getNewInstance(id, currencyUnit);
      case TYPE_TRANSFER -> template = Transfer.getNewInstance(id, currencyUnit, 0L, null);
      case TYPE_SPLIT -> template = SplitTransaction.getNewInstance(contentResolver, id, currencyUnit, false);
      default -> throw new UnsupportedOperationException(
              String.format(Locale.ROOT, "Unknown type %d", operationType));
    }
    setParentId(parentId);
  }

  @Nullable
  public static Template getTypedNewInstance(ContentResolver contentResolver, int operationType, long accountId, @NonNull CurrencyUnit currencyUnit,  boolean forEdit, Long parentId) {
    Template t = new Template(contentResolver, accountId, currencyUnit, operationType, parentId);
    if (forEdit && t.isSplit()) {
      if (!t.persistForEdit(contentResolver)) {
        return null;
      }
    }
    return t;
  }

  private boolean persistForEdit(ContentResolver contentResolver) {
    setStatus(STATUS_UNCOMMITTED);
    return save(contentResolver) != null;
  }

  /**
   * @return a template that is linked to the calendar event with id planId, but only if the instance instanceId
   * has not yet been dealt with
   */
  public static Template getInstanceForPlanIfInstanceIsOpen(ContentResolver contentResolver, long planId, long instanceId) {
    Cursor c = contentResolver.query(
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
  public static PlanInstance getPlanInstance(ContentResolver contentResolver, CurrencyContext currencyContext, long planId, long date) {
    PlanInstance planInstance = null;
    try (Cursor c = contentResolver.query(
        CONTENT_URI.buildUpon().appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_INSTANCE, String.valueOf(CalendarProviderProxy.calculateId(date))).build(),
        null, KEY_PLANID + "= ?",
        new String[]{String.valueOf(planId)},
        null)) {
      if (c != null && c.moveToFirst()) {
        final Long instanceId = getLongOrNull(c, KEY_INSTANCEID);
        final Long transactionId = getLongOrNull(c, KEY_TRANSACTIONID);
        final long templateId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
        CurrencyUnit currency = currencyContext.get(c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY)));
        Money amount = new Money(currency, c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT)));
        planInstance = new PlanInstance(templateId, instanceId, transactionId, c.getString(c.getColumnIndexOrThrow(KEY_TITLE)), date, c.getInt(c.getColumnIndexOrThrow(KEY_COLOR)), amount,
            c.getInt(c.getColumnIndexOrThrow(KEY_SEALED)) == 1);
      }
    }
    return planInstance;
  }

  @Nullable
  public static kotlin.Pair<Transaction, List<Tag>> getInstanceFromDbWithTags(ContentResolver contentResolver, long id) {
    Template t = getInstanceFromDb(contentResolver, id);
    return t == null ? null : new kotlin.Pair<>(t, t.loadTags(contentResolver));
  }

  @Nullable
  public static Template getInstanceFromDb(ContentResolver contentResolver, long id) {
    Cursor c = contentResolver.query(
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
      t.plan = Plan.getInstanceFromDb(contentResolver, t.planId);
    }
    return t;
  }

  public static Template getInstanceFromDbIfInstanceIsOpen(ContentResolver contentResolver, long id, long instanceId) {
    Cursor c = contentResolver.query(
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
  @NonNull
  public Uri save(@NonNull ContentResolver contentResolver, @Nullable PlannerUtils plannerUtils, boolean withCommit) {
    return save(contentResolver, plannerUtils, null);
  }

  /**
   * Saves the new template, or update an existing one
   *
   * @return the Uri of the template. Upon creation it is returned from the content provider, null if inserting fails on constraints
   */

  @RequiresPermission(allOf = {Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, conditional = true)
  public Uri save(ContentResolver contentResolver, PlannerUtils plannerUtils, Long withLinkedTransaction) {
    boolean runPlanner = false;
    if (plan != null) {
       if (plan.getId() == 0) {
         runPlanner = true;
       }
      Uri planUri = plan.save(contentResolver, plannerUtils);
      if (planUri != null) {
        planId = ContentUris.parseId(planUri);
      }
    }
    Uri uri;
    Long payeeStore;
    if (getParty() != null) {
      payeeStore = getParty().getId();
    } else {
      payeeStore = null;
    }
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, getComment());
    initialValues.put(KEY_AMOUNT, getAmount().getAmountMinor());
    if (isTransfer()) {
      initialValues.put(KEY_TRANSFER_ACCOUNT, template.getTransferAccountId());
    }
    initialValues.put(KEY_CATID, getCatId());
    initialValues.put(KEY_PAYEEID, payeeStore);
    initialValues.put(KEY_METHODID, getMethodId());
    initialValues.put(KEY_TITLE, getTitle());
    initialValues.put(KEY_PLANID, planId);
    initialValues.put(KEY_PLAN_EXECUTION, isPlanExecutionAutomatic());
    initialValues.put(KEY_PLAN_EXECUTION_ADVANCE, planExecutionAdvance);
    initialValues.put(KEY_DEFAULT_ACTION, defaultAction.name());
    initialValues.put(KEY_ACCOUNTID, getAccountId());
    initialValues.put(KEY_DEBT_ID, getDebtId());
    initialValues.put(KEY_ORIGINAL_AMOUNT, getOriginalAmount() == null ? null : getOriginalAmount().getAmountMinor());
    initialValues.put(KEY_ORIGINAL_CURRENCY, getOriginalAmount() == null ? null : getOriginalAmount().getCurrencyUnit().getCode());
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
        ContentProviderResult[] result = contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops);
        uri = result[0].uri;
      } catch (RemoteException | OperationApplicationException e) {
        return null;
      }
      setId(ContentUris.parseId(uri));
      if (plan != null) {
        plan.updateCustomAppUri(contentResolver, buildCustomAppUri(getId()));
      }
    } else {
      String idStr = String.valueOf(getId());
      uri = CONTENT_URI.buildUpon().appendPath(idStr).build();
      ops.add(ContentProviderOperation.newUpdate(uri).withValues(initialValues).build());
      if (withLinkedTransaction != null) {
        ops.add(ContentProviderOperation.newInsert(TransactionProvider.PLAN_INSTANCE_STATUS_URI)
            .withValue(KEY_TEMPLATEID, getId())
            .withValue(KEY_INSTANCEID, CalendarProviderProxy.calculateId(plan.getDtStart()))
            .withValue(KEY_TRANSACTIONID, withLinkedTransaction)
            .build());
      }
      addCommitOperations(CONTENT_URI, ops);
      ops.add(ContentProviderOperation.newAssertQuery(TransactionProvider.TEMPLATES_URI)
              .withSelection(KEY_PARENTID + " = ? AND " + KEY_ACCOUNTID + " != ?",
                      new String[] {idStr, String.valueOf(getAccountId())})
              .withExpectedCount(0) .build());
      try {
        contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops);
      } catch (RemoteException | OperationApplicationException e) {
        return null;
      }
    }
    updateNewPlanEnabled();
    if (runPlanner) {
      PlanExecutor.Companion.enqueueSelf(MyApplication.Companion.getInstance(), MyApplication.Companion.getInstance().getAppComponent().prefHandler(), true);
    }
    return uri;
  }

  public static void delete(ContentResolver contentResolver, long id, boolean deletePlan) {
    Template t = getInstanceFromDb(contentResolver, id);
    if (t == null) {
      return;
    }
    if (t.planId != null) {
      if (deletePlan) {
        Plan.delete(contentResolver, t.planId);
      }
      contentResolver.delete(
          TransactionProvider.PLAN_INSTANCE_STATUS_URI,
          KEY_TEMPLATEID + " = ?",
          new String[]{String.valueOf(id)});
    }
    contentResolver.delete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null,
        null);
    updateNewPlanEnabled();
  }

  public static String buildCustomAppUri(long id) {
    return ContentUris.withAppendedId(Template.CONTENT_URI, id).toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Template other = (Template) obj;
    if (isPlanExecutionAutomatic() != other.isPlanExecutionAutomatic()) {
      return false;
    }
    if (planId == null) {
      if (other.planId != null) {
        return false;
      }
    } else if (!planId.equals(other.planId)) {
      return false;
    }
    if (getTitle() == null) {
      if (other.getTitle() != null) {
        return false;
      }
    } else if (!getTitle().equals(other.getTitle())) {
      return false;
    }
    if (getUuid() == null) {
      if (other.getUuid() != null) {
        return false;
      }
    } else if (!getUuid().equals(other.getUuid())) {
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
    final AppComponent appComponent = MyApplication.Companion.getInstance().getAppComponent();
    LicenceHandler licenceHandler = appComponent.licenceHandler();
    PrefHandler prefHandler = appComponent.prefHandler();
    Repository repository = appComponent.repository();
    boolean newPlanEnabled = true, newSplitTemplateEnabled = true;
    if (!licenceHandler.hasAccessTo(ContribFeature.PLANS_UNLIMITED)) {
      if (repository.count(Template.CONTENT_URI, KEY_PLANID + " is not null", null) >= ContribFeature.FREE_PLANS) {
        newPlanEnabled = false;
      }
    }
    prefHandler.putBoolean(PrefKey.NEW_PLAN_ENABLED, newPlanEnabled);

    if (!licenceHandler.hasAccessTo(ContribFeature.SPLIT_TEMPLATE)) {
      if (repository.count(Template.CONTENT_URI, KEY_CATID + " = " + DatabaseConstants.SPLIT_CATID, null) >= ContribFeature.FREE_SPLIT_TEMPLATES) {
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
    CrashHandler.report(new Exception("Tried to get transfer account for a template that is no transfer"));
    return null;
  }

  @Override
  public void setTransferAccountId(Long transferAccountId) {
    if (isTransfer()) {
      template.setTransferAccountId(transferAccountId);
    } else {
      CrashHandler.report(new Exception("Tried to set transfer account for a template that is no transfer"));
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
  protected Pair<Transaction, List<Tag>> getSplitPart(ContentResolver contentResolver, long partId) {
    return Template.getInstanceFromDbWithTags(contentResolver, partId);
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

  public static void cleanupCanceledEdit(ContentResolver contentResolver, Long id) {
    cleanupCanceledEdit(contentResolver, id, CONTENT_URI, PART_SELECT);
  }

  public Action getDefaultAction() {
    return defaultAction;
  }

  public void setDefaultAction(Action defaultAction) {
    this.defaultAction = defaultAction;
  }
}