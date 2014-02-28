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

import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import com.android.calendar.CalendarContractCompat.Events;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

public class Template extends Transaction {
  public String title;
  public boolean isTransfer;
  public Long planId;
  public boolean planExecutionAutomatic = false;

  public static final Uri CONTENT_URI = TransactionProvider.TEMPLATES_URI;
  public static String[] PROJECTION_BASE, PROJECTION_EXTENDED;
  static {
    PROJECTION_BASE = new String[] {
      KEY_ROWID,
      KEY_AMOUNT,
      KEY_COMMENT,
      KEY_CATID,
      LABEL_MAIN,
      SHORT_LABEL,
      "CASE" +
        " WHEN transfer_peer = 0 AND cat_id AND (SELECT parent_id FROM categories WHERE _id = cat_id)" +
        " THEN (SELECT label FROM categories WHERE _id = cat_id)" +
        "END AS label_sub",//different from Transaction, since transfer_peer is treated as boolean here
      KEY_PAYEE_NAME,
      KEY_TRANSFER_PEER,
      KEY_TRANSFER_ACCOUNT,
      KEY_ACCOUNTID,
      KEY_METHODID,
      KEY_TITLE,
      KEY_PLANID,
      KEY_PLAN_EXECUTION
    };
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength+2];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = KEY_COLOR;
    PROJECTION_EXTENDED[baseLength+1] = KEY_CURRENCY;
  }
  /**
   * derives a new template from an existing Transaction
   * @param t the transaction whose data (account, amount, category, comment, payment method, payee,
   * populates the template
   * @param title identifies the template in the template list
   */
  public Template(Transaction t, String title) {
    this.title = title;
    this.accountId = t.accountId;
    this.amount = t.amount;
    this.catId = t.catId;
    this.comment = t.comment;
    this.methodId = t.methodId;
    this.payee = t.payee;
    //we are not interested in which was the transfer_peer of the transfer
    //from which the template was derived;
    //we use KEY_TRANSFER_PEER as boolean
    this.isTransfer = t.transfer_peer != null;
    this.transfer_account = t.transfer_account;
  }
  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  /**
   * @param c
   */
  public Template (Cursor c) {
    this(c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID)),
        c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT))
        );
    if (isTransfer = c.getInt(c.getColumnIndexOrThrow(KEY_TRANSFER_PEER)) > 0) {
      transfer_account = DbUtils.getLongOrNull(c, KEY_TRANSFER_ACCOUNT);
    } else {
      methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
      catId = DbUtils.getLongOrNull(c, KEY_CATID);
      payee = DbUtils.getString(c,KEY_PAYEE_NAME);
    }
    id = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
    comment = DbUtils.getString(c,KEY_COMMENT);
    label =  DbUtils.getString(c,KEY_LABEL);
    title = DbUtils.getString(c,KEY_TITLE);
    planId = DbUtils.getLongOrNull(c, KEY_PLANID);
    planExecutionAutomatic = c.getInt(c.getColumnIndexOrThrow(KEY_PLAN_EXECUTION)) > 0;
  }
  public Template(long accountId,Long amount) {
    super(accountId,amount);
    title = "";
  }
  public Template(Account account, long amount) {
    super(account,amount);
    title = "";
  }
  public static Template getTypedNewInstance(int mOperationType, long accountId) {
    Account account = Account.getInstanceFromDb(accountId);
    if (account == null) {
      return null;
    }
    Template t = new Template(account,0L);
    t.isTransfer = mOperationType == MyExpenses.TYPE_TRANSFER;
    return t;
  }
  public void setDate(Date date){
    //templates have no date
  }
  /**
   * @param planId
   * @param instanceId
   * @return a template that is linked to the calendar event with id planId, but only if the instance instanceId
   * has not yet been dealt with
   */
  public static Template getInstanceForPlanIfInstanceIsOpen(long planId,long instanceId) {
    Cursor c = cr().query(
        CONTENT_URI,
        null,
        KEY_PLANID + "= ? AND NOT exists(SELECT 1 from planinstance_transaction WHERE "
            + KEY_INSTANCEID + " = ?)",
        new String[] {String.valueOf(planId),String.valueOf(instanceId)},
        null);
    if (c == null || c.getCount() == 0) {
      return null;
    }
    c.moveToFirst();
    Template t = new Template(c);
    c.close();
    return t;
  }
  public static Template getInstanceFromDb(long id) {
    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null,null,null, null);
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
  /**
   * Saves the new template, or updated an existing one
   * @return the Uri of the template. Upon creation it is returned from the content provider, null if inserting fails on constraints
   */
  public Uri save() {
    Uri uri;
    Long payee_id = (payee != null && !payee.equals("")) ?
        Payee.require(payee) : null;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_AMOUNT, amount.getAmountMinor());
    initialValues.put(KEY_CATID, catId);
    initialValues.put(KEY_TRANSFER_ACCOUNT, transfer_account);
    initialValues.put(KEY_PAYEEID, payee_id);
    initialValues.put(KEY_METHODID, methodId);
    initialValues.put(KEY_TITLE, title);
    initialValues.put(KEY_PLANID, planId);
    initialValues.put(KEY_PLAN_EXECUTION,planExecutionAutomatic);
    initialValues.put(KEY_ACCOUNTID, accountId);
    if (id == 0) {
      initialValues.put(KEY_TRANSFER_PEER, isTransfer);
      try {
        uri = cr().insert(CONTENT_URI, initialValues);
      } catch (SQLiteConstraintException e) {
        return null;
      }
      id = ContentUris.parseId(uri);
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      try {
        cr().update(uri, initialValues, null, null);
      } catch (SQLiteConstraintException e) {
        return null;
      }
    }
    //add callback to event
    if (planId != null) {
      initialValues.clear();
      if (android.os.Build.VERSION.SDK_INT>=16) {
        initialValues.put(
            //we encode both account and template into the CUSTOM URI
            Events.CUSTOM_APP_URI,
            ContentUris.withAppendedId(
                ContentUris.withAppendedId(Template.CONTENT_URI,accountId),
                id).toString());
        initialValues.put(Events.CUSTOM_APP_PACKAGE, "org.totschnig.myexpenses");
      }
      initialValues.put(Events.TITLE,title);
      initialValues.put(Events.DESCRIPTION, compileDescription(MyApplication.getInstance()));
      try {
        cr().update(
            ContentUris.withAppendedId(Events.CONTENT_URI, planId),
            initialValues,
            null,
            null);
      } catch (SQLiteException e) {
        //we have seen a bugy calendar provider implementation on Symphony phone
        //we try the insert again without the custom app columns
        initialValues.remove(Events.CUSTOM_APP_URI);
        initialValues.remove(Events.CUSTOM_APP_PACKAGE);
        cr().update(
            ContentUris.withAppendedId(Events.CONTENT_URI, planId),
            initialValues,
            null,
            null);
      }
    }
    return uri;
  }
  public static void delete(long id) {
    Template t = getInstanceFromDb(id);
    if (t.planId != null) {
      Plan.delete(t.planId);
      cr().delete(
          TransactionProvider.PLAN_INSTANCE_STATUS_URI,
          KEY_TEMPLATEID + " = ?",
          new String[]{String.valueOf(id)});
    }
    cr().delete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null,
        null);
  }
  public static int countPerMethod(long methodId) {
    return countPerMethod(CONTENT_URI,methodId);
  }
  public static int countPerAccount(long accountId) {
    return countPerAccount(CONTENT_URI,accountId);
  }
  public static int countAll() {
    return countAll(CONTENT_URI);
  }
  public String compileDescription(Context ctx) {
    StringBuilder sb = new StringBuilder();
    sb.append(ctx.getString(R.string.amount));
    sb.append(" : ");
    sb.append(Utils.formatCurrency(amount));
    sb.append("\n");
    if (catId != null && catId > 0) {
      sb.append(ctx.getString(R.string.category));
      sb.append(" : ");
      sb.append(label);
      sb.append("\n");
    }
    if (isTransfer) {
      sb.append(ctx.getString(R.string.account));
      sb.append(" : ");
      sb.append(label);
      sb.append("\n");
    }
    //comment
    if (!comment.equals("")) {
      sb.append(ctx.getString(R.string.comment));
      sb.append(" : ");
      sb.append(comment);
      sb.append("\n");
    }
    //comment
    if (!payee.equals("")) {
      sb.append(ctx.getString(
          amount.getAmountMajor().signum() == 1 ? R.string.payer : R.string.payee));
      sb.append(" : ");
      sb.append(payee);
      sb.append("\n");
    }
    //Method
    if (methodId != null) {
      sb.append(ctx.getString(R.string.method));
      sb.append(" : ");
      sb.append(PaymentMethod.getInstanceFromDb(methodId).getDisplayLabel());
    }
    return sb.toString();
  }
  public boolean applyInstance(long instanceId, long date) {
    Transaction t = Transaction.getInstanceFromTemplate(this);
    t.setDate(new Date(date));
    t.originPlanInstanceId = instanceId;
    return t.save() != null;
  }
}