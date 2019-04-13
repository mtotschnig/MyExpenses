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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;


public class AccountWidget extends AbstractWidget<Account> {

  @Override
  String getPrefName() {
    return "org.totschnig.myexpenses.activity.AccountWidget";
  }

  @Override
  PrefKey getProtectionKey() {
    return PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET;
  }

  public static final Uri[] OBSERVED_URIS = new Uri[]{
      TransactionProvider.ACCOUNTS_URI,
      TransactionProvider.TRANSACTIONS_URI
  };
  private Money mCurrentBalance;

  @Override
  protected RemoteViews updateWidgetFrom(Context context,
                                         int widgetId, int layoutId, Account a) {
    RemoteViews updateViews = super.updateWidgetFrom(context, widgetId, layoutId, a);
    updateViews.setTextViewText(R.id.line1, a.getLabel());
    updateViews.setTextViewText(R.id.note,
        CurrencyFormatter.instance().formatCurrency(mCurrentBalance));
//    updateViews.setTextColor(R.id.note, context.getResources().getColor(
//        balance.getAmountMinor() < 0 ? R.color.colorExpenseDark : R.color.colorIncomeDark));
    setBackgroundColorSave(updateViews, R.id.divider3, a.color);
    addScrollOnClick(context, updateViews, widgetId);
    addTapOnClick(context, updateViews, widgetId, a.getId());
    addButtonsClick(context, updateViews, widgetId, a);
    saveForWidget(context, widgetId, a.getId());
    int multipleAccountsVisible = Account.count(null, null) < 2 ? View.GONE
        : View.VISIBLE;
    int transferEnabledVisible = !a.isSealed() && Account.getTransferEnabledGlobal() ? View.VISIBLE
        : View.GONE;
    updateViews.setViewVisibility(R.id.command1, a.isSealed()? View.GONE : View.VISIBLE);
    updateViews.setViewVisibility(R.id.navigation, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.divider1, transferEnabledVisible);
    updateViews.setViewVisibility(R.id.command2, transferEnabledVisible);
    return updateViews;
  }

  private void addTapOnClick(Context context, RemoteViews updateViews,
                             int widgetId, long accountId) {
    Intent intent = new Intent(context, MyExpenses.class);
    intent.putExtra(DatabaseConstants.KEY_ROWID, accountId);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.object_info, pendingIntent);
  }

  private Intent buildButtonIntent(Context context, Account account) {
    Intent intent = new Intent(context, ExpenseEdit.class);
    if (account.getId() < 0) {
      intent.putExtra(DatabaseConstants.KEY_CURRENCY, account.getCurrencyUnit().code());
    } else {
      intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, account.getId());
    }
    intent.putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, true);
    intent.putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return intent;
  }

  private void addButtonsClick(Context context, RemoteViews updateViews,
                               int widgetId, Account account) {
    Intent intent = buildButtonIntent(context, account);
    PendingIntent pendingIntent = PendingIntent.getActivity(
        context,
        2 * widgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.command1, pendingIntent);
    setImageViewVectorDrawable(context, updateViews, R.id.command1, R.drawable.ic_menu_add);
    updateViews.setContentDescription(R.id.command1,
        context.getString(R.string.menu_create_transaction));
    intent = buildButtonIntent(context, account);
    intent.putExtra(OPERATION_TYPE, TYPE_TRANSFER);
    pendingIntent = PendingIntent.getActivity(
        context,
        2 * widgetId + 1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.command2, pendingIntent);
    setImageViewVectorDrawable(context, updateViews, R.id.command2, R.drawable.ic_menu_forward);
    updateViews.setContentDescription(R.id.command2,
        context.getString(R.string.menu_create_transfer));
  }

  @Override
  Account getObject(Cursor c) {
    Account a = Account.fromCursor(c);
    mCurrentBalance = new Money(a.getCurrencyUnit(),
        c.getLong(c.getColumnIndexOrThrow(KEY_CURRENT_BALANCE)));
    return a;
  }

  @Override
  Cursor getCursor(Context c) {
    Uri.Builder builder = TransactionProvider.ACCOUNTS_URI.buildUpon();
    builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1");
    return c.getContentResolver().query(
        //TODO find out if we should implement an optimized provider method that only returns current balance
        builder.build(), null, null, null, null);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
  }
}
