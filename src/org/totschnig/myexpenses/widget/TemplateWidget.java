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
import android.widget.RemoteViews;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

public class TemplateWidget extends AbstractWidget<Template> {

  private static final Uri CONTENT_URI = Uri
      .parse("content://org.totschnig.myexpenses/templatewidget");

  @Override
  String getPrefName() {
    return "org.totschnig.myexpenses.activity.TemplateWidget";
  }

  private static void addScrollOnClick(Context context,
      RemoteViews updateViews, int widgetId) {
    Uri widgetUri = ContentUris.withAppendedId(CONTENT_URI, widgetId);
    Intent intent = new Intent(WIDGET_NEXT_ACTION, widgetUri, context,
        TemplateWidget.class);
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
        intent, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.down_icon, pendingIntent);
    intent = new Intent(WIDGET_PREVIOUS_ACTION, widgetUri, context,
        TemplateWidget.class);
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.up_icon, pendingIntent);
  }

  private static void addTapOnClick(Context context, RemoteViews updateViews,
      long accountId) {
    Intent intent = new Intent(context, MyExpenses.class);
    intent.putExtra(DatabaseConstants.KEY_ROWID, accountId);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.account_info, pendingIntent);
  }

  private static void addButtonsClick(Context context, RemoteViews updateViews,
      long accountId) {
    Intent intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.add_transaction, pendingIntent);
    intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSFER);
    intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId);
    pendingIntent = PendingIntent.getActivity(context, 1, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.add_transfer, pendingIntent);
  }

  @Override
  Template getObject(long objectId) {
    return Template.getInstanceFromDb(objectId);
  }

  @Override
  Template getObject(Cursor c) {
    return new Template(c);
  }

  @Override
  Cursor getCursor(Context c) {
    return c.getContentResolver().query(
        TransactionProvider.TEMPLATES_URI, null, null, null, null);
  }

  @Override
  RemoteViews updateWidgetFrom(Context context, int widgetId, int layoutId,
      Template t) {
    Log.d("MyExpensesWidget", "updating template " + t.id);
    RemoteViews updateViews = new RemoteViews(context.getPackageName(),
        layoutId);
    return updateViews;
  }
}
