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

package org.totschnig.myexpenses.dialog;

import java.util.Calendar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.util.Utils;

import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendar.CalendarContractCompat.Instances;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class TemplateDetailFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {
  Template mTemplate;
  SimpleCursorAdapter mAdapter;
  
  public static final TemplateDetailFragment newInstance(Long id) {
    TemplateDetailFragment dialogFragment = new TemplateDetailFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable("id", id);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle bundle = getArguments();
    //TODO strict mode violation
    mTemplate = Template.getInstanceFromDb(bundle.getLong("id"));
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean type = mTemplate.amount.getAmountMinor() > 0 ? ExpenseEdit.INCOME : ExpenseEdit.EXPENSE;
    final ManageTemplates ctx = (ManageTemplates) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.template_detail, null);
    //title
    ((TextView) view.findViewById(R.id.Title)).setText(mTemplate.title);
    String accountLabel = Account.getInstanceFromDb(mTemplate.accountId).label;
    if (mTemplate.isTransfer) {
      ((TextView) view.findViewById(R.id.Account)).setText(type ? mTemplate.label : accountLabel);
      ((TextView) view.findViewById(R.id.Category)).setText(type ? accountLabel : mTemplate.label);
      ((TextView) view.findViewById(R.id.AccountLabel)).setText(R.string.transfer_from_account);
      ((TextView) view.findViewById(R.id.CategoryLabel)).setText(R.string.transfer_to_account);
    } else {
      ((TextView) view.findViewById(R.id.Account)).setText(accountLabel);
      if (mTemplate.catId != null && mTemplate.catId > 0) {
      ((TextView) view.findViewById(R.id.Category)).setText(mTemplate.label);
      } else {
        view.findViewById(R.id.CategoryRow).setVisibility(View.GONE);
      }
    }
    //amount
    ((TextView) view.findViewById(R.id.Amount)).setText(Utils.formatCurrency(mTemplate.amount));
    //comment
    if (!mTemplate.comment.equals(""))
      ((TextView) view.findViewById(R.id.Comment)).setText(mTemplate.comment);
    else
      view.findViewById(R.id.CommentRow).setVisibility(View.GONE);
    //payee
    if (!mTemplate.payee.equals(""))
      ((TextView) view.findViewById(R.id.Payee)).setText(mTemplate.payee);
    else
      view.findViewById(R.id.PayeeRow).setVisibility(View.GONE);
    //Method
    if (mTemplate.methodId != null)
      ((TextView) view.findViewById(R.id.Method)).setText(PaymentMethod.getInstanceFromDb(mTemplate.methodId).getDisplayLabel());
    else
      view.findViewById(R.id.MethodRow).setVisibility(View.GONE);
    Cursor c;
    if (mTemplate.planId != null &&
      (c = ctx.getContentResolver().query(
          ContentUris.withAppendedId(Events.CONTENT_URI, mTemplate.planId),
          new String[]{
            Events.DTSTART,
            Events.RRULE,
          },
          null,
          null,
          null
          )) !=null) {
        if (c.moveToFirst()) {
          ((TextView) view.findViewById(R.id.Plan)).setText(
              Plan.prettyTimeInfo(ctx, c.getString(1), c.getLong(0)));
          ListView lv = (ListView) view.findViewById(R.id.list);
          View emptyView = view.findViewById(R.id.empty);
          // Create an array to specify the fields we want to display in the list
          String[] from = new String[]{Instances.BEGIN};
          // and an array of the fields we want to bind those fields to
          int[] to = new int[]{R.id.date};
          mAdapter = new SimpleCursorAdapter(ctx, R.layout.plan_instance_row, null, from, to,0) {
            Calendar calendar = Calendar.getInstance();
            java.text.DateFormat dateFormat = java.text.DateFormat.
                getDateInstance(java.text.DateFormat.FULL);
            @Override
            public void setViewText(TextView v, String text) {
              switch (v.getId()) {
              case R.id.date:
                calendar.setTimeInMillis(Long.valueOf(text));
                text = dateFormat.format(calendar.getTime());
              }
              super.setViewText(v, text);
            }
          };
          lv.setAdapter(mAdapter);
          lv.setEmptyView(emptyView);
          LoaderManager manager = ctx.getSupportLoaderManager();
          if (manager.getLoader(ManageTemplates.PLAN_INSTANCES_CURSOR) != null && !manager.getLoader(ManageTemplates.PLAN_INSTANCES_CURSOR).isReset())
            manager.restartLoader(ManageTemplates.PLAN_INSTANCES_CURSOR, null, this);
          else
            manager.initLoader(ManageTemplates.PLAN_INSTANCES_CURSOR, null, this);
        } else {
          view.findViewById(R.id.PlanRow).setVisibility(View.GONE);
        }
        c.close();
    } else {
      view.findViewById(R.id.PlanRow).setVisibility(View.GONE);
      view.findViewById(R.id.InstancesContainer).setVisibility(View.GONE);
    }
    return new AlertDialog.Builder(ctx)
      .setTitle(R.string.template)
      .setView(view)
      .setNegativeButton(android.R.string.ok,this)
      .setPositiveButton(R.string.menu_edit,this)
      .setNeutralButton(R.string.menu_apply, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    ManageTemplates ctx = (ManageTemplates) getActivity();
    switch(which) {
    case AlertDialog.BUTTON_POSITIVE:
      Intent i = new Intent(ctx, ExpenseEdit.class);
      i.putExtra("template_id", mTemplate.id);
      ctx.startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
      break;
    case AlertDialog.BUTTON_NEUTRAL:
      ctx.applyTemplate(mTemplate.id);
    case AlertDialog.BUTTON_NEGATIVE:
      if (ctx.calledFromCalendar) {
        ctx.setResult(Activity.RESULT_OK);
        ctx.finish();
      }
    }
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch(id) {
    case ManageTemplates.PLAN_INSTANCES_CURSOR:
      // The ID of the recurring event whose instances you are searching
      // for in the Instances table
      String selection = Instances.EVENT_ID + " = " + mTemplate.planId;
      // Construct the query with the desired date range.
      Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
      long now = System.currentTimeMillis();
      ContentUris.appendId(builder, now);
      ContentUris.appendId(builder, now + 7776000000L); //90 days
      return new CursorLoader(
          getActivity(),
          builder.build(),
          new String[]{
            Instances._ID,
            Instances.BEGIN
          },
          selection,
          null,
          null);
    }
    return null;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    int id = loader.getId();
    switch(id) {
      case ManageTemplates.PLAN_INSTANCES_CURSOR:
      mAdapter.swapCursor(data);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.swapCursor(null);
  }
}
