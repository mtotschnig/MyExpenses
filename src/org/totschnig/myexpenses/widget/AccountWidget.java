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
// based on Financisto

package org.totschnig.myexpenses.widget;

import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;


public class AccountWidget extends AbstractWidget<Account> {
  
  @Override
  Uri getContentUri() {
    return Uri
        .parse("content://org.totschnig.myexpenses/accountwidget");
  }
  
  @Override
  String getPrefName() {
    // TODO Auto-generated method stub
    return"org.totschnig.myexpenses.activity.AccountWidget";
  }

  RemoteViews updateWidgetFrom(Context context,
      int widgetId, int layoutId, Account a) {
    Log.d("MyExpensesWidget", "updating account " + a.id);
    RemoteViews updateViews = new RemoteViews(context.getPackageName(),
        layoutId);
    updateViews.setTextViewText(R.id.line1, a.label);
    Account.Type type = a.type;
    // if (type.isCard && a.cardIssuer != null) {
    // CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
    // updateViews.setImageViewResource(R.id.account_icon, cardIssuer.iconId);
    // } else {
    // updateViews.setImageViewResource(R.id.account_icon, type.iconId);
    // }
    updateViews.setTextViewText(R.id.note,
        Utils.formatCurrency(a.getCurrentBalance()));
    // int amountColor = u.getAmountColor(amount);
    // updateViews.setTextColor(R.id.note, amountColor);
    addScrollOnClick(context, updateViews, widgetId);
    addTapOnClick(context, updateViews, a.id);
    addButtonsClick(context, updateViews, a.id);
    saveForWidget(context, widgetId, a.id);
    int multipleAccountsVisible = Account.count(null, null) < 2 ? View.GONE
        : View.VISIBLE;
    updateViews.setViewVisibility(R.id.navigation, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.divider3, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.divider1, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.add_transfer, multipleAccountsVisible);
    return updateViews;
  }

  private void addTapOnClick(Context context, RemoteViews updateViews,
      long accountId) {
    Intent intent = new Intent(context, MyExpenses.class);
    intent.putExtra(DatabaseConstants.KEY_ROWID, accountId);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.account_info, pendingIntent);
  }

  private void addButtonsClick(Context context, RemoteViews updateViews,
      long accountId) {
    Intent intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId);
    PendingIntent pendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_ADD_TRANSACTION,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.add_transaction, pendingIntent);
    intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSFER);
    intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId);
    pendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_ADD_TRANSFER,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.add_transfer, pendingIntent);
  }

  @Override
  Account getObject(long objectId) {
    return Account.getInstanceFromDb(objectId);
  }
  @Override
  Account getObject(Cursor c) {
    return new Account(c);
  }

  @Override
  Cursor getCursor(Context c) {
    // TODO Auto-generated method stub
    return c.getContentResolver().query(
        TransactionProvider.ACCOUNTS_URI, null, null, null, null);
  }
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("AccountWidget", "onReceive intent "+intent);
    super.onReceive(context, intent);
  }
}
