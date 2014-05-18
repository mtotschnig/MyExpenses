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

import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;

import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

public class TemplateWidget extends AbstractWidget<Template> {
  
  private static final String WIDGET_INSTANCE_SAVE_ACTION = "org.totschnig.myexpenses.INSTANCE_SAVE";

  @Override
  Uri getContentUri() {
    return Uri
        .parse("content://org.totschnig.myexpenses/templatewidget");
  }

  @Override
  String getPrefName() {
    return "org.totschnig.myexpenses.activity.TemplateWidget";
  }

  private void addButtonsClick(Context context, RemoteViews updateViews,
      int widgetId, long templateId) {
    Uri widgetUri = ContentUris.withAppendedId(getContentUri(), widgetId);
    Intent intent = new Intent(WIDGET_INSTANCE_SAVE_ACTION, widgetUri, context,
        TemplateWidget.class);
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.command1, pendingIntent);
    updateViews.setImageViewResource(R.id.command1, R.drawable.create_instance_save_icon);
    intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(DatabaseConstants.KEY_TEMPLATEID, templateId);
    intent.putExtra(DatabaseConstants.KEY_INSTANCEID, -1L);
    pendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_INSTANCE_EDIT,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.command2, pendingIntent);
    updateViews.setImageViewResource(R.id.command2, R.drawable.create_instance_edit_icon);
  }

  private void addTapOnClick(Context context, RemoteViews updateViews) {
    Intent intent = new Intent(context, ManageTemplates.class);
    intent.putExtra(DatabaseConstants.KEY_TRANSFER_ENABLED, Account.getTransferEnabledGlobal());
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.object_info, pendingIntent);
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
        TransactionProvider.TEMPLATES_URI, null, KEY_PLANID + " is null", null, null);
  }

  @Override
  RemoteViews updateWidgetFrom(Context context, int widgetId, int layoutId,
      Template t) {
    Log.d("MyExpensesWidget", "updating template " + t.id);
    RemoteViews updateViews = new RemoteViews(context.getPackageName(),
        layoutId);
    updateViews.setTextViewText(R.id.line1, t.title);
    // if (type.isCard && a.cardIssuer != null) {
    // CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
    // updateViews.setImageViewResource(R.id.account_icon, cardIssuer.iconId);
    // } else {
    // updateViews.setImageViewResource(R.id.account_icon, type.iconId);
    // }
    updateViews.setTextViewText(R.id.note,
        t.label);
    // int amountColor = u.getAmountColor(amount);
    // updateViews.setTextColor(R.id.note, amountColor);
    addScrollOnClick(context, updateViews, widgetId);
    addTapOnClick(context, updateViews);
    addButtonsClick(context, updateViews, widgetId, t.id);
    saveForWidget(context, widgetId, t.id);
    int multipleTemplatesVisible = 
        Transaction.count(Template.CONTENT_URI, KEY_PLANID + " is null", null) < 2 ?
            View.GONE : 
            View.VISIBLE;
    updateViews.setViewVisibility(R.id.navigation, multipleTemplatesVisible);
    updateViews.setViewVisibility(R.id.divider3, multipleTemplatesVisible);
    return updateViews;
  }
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("TemplateWidget", "onReceive intent "+intent);
    String action = intent.getAction();
    if (WIDGET_INSTANCE_SAVE_ACTION.equals(action)) {
      int widgetId = intent.getIntExtra(WIDGET_ID, INVALID_APPWIDGET_ID);
      if (widgetId != INVALID_APPWIDGET_ID) {
        long objectId = loadForWidget(context, widgetId);
        Transaction t = Transaction.getInstanceFromTemplate(objectId);
        if (t != null) {
          if (t.save() != null) {
            Toast.makeText(context,
                context.getResources().getQuantityString(R.plurals.save_transaction_from_template_success, 1, 1),
                Toast.LENGTH_LONG).show();
          }
        }
      }
    } else {
      super.onReceive(context, intent);
    }
  }
  @Override
  protected RemoteViews noDataUpdate(Context context) {
    RemoteViews updateViews = super.noDataUpdate(context);
    updateViews.setTextViewText(R.id.object_info, context.getString(R.string.no_templates));
    addTapOnClick(context, updateViews);
    return updateViews;
  }
}
