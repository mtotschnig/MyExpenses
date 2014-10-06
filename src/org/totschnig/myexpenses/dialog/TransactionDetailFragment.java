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


import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;

import java.text.DateFormat;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TransactionDetailFragment extends CommitSafeDialogFragment implements LoaderManager.LoaderCallbacks<Cursor>,OnClickListener {
  Transaction mTransaction;
  SimpleCursorAdapter mAdapter;
  View mLayout;
  
  public static final TransactionDetailFragment newInstance(Long id) {
    TransactionDetailFragment dialogFragment = new TransactionDetailFragment();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ROWID, id);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((MyExpenses) activity).startTaskExecution(
        TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION,
      new Long[] {getArguments().getLong(KEY_ROWID)},
      null,
      0);
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    mLayout = li.inflate(R.layout.transaction_detail, null);
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.progress_dialog_loading)
      .setView(mLayout)
      .setNegativeButton(android.R.string.ok,this)
      .setPositiveButton(R.string.menu_edit,this)
      .create();
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    if (getActivity()==null) {
      return null;
    }
    switch(id) {
      case MyExpenses.SPLIT_PART_CURSOR:
      CursorLoader cursorLoader = new CursorLoader(getActivity(), TransactionProvider.TRANSACTIONS_URI,null, "parent_id = ?",
          new String[] { String.valueOf(mTransaction.getId()) }, null);
      return cursorLoader;
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    int id = loader.getId();
    switch(id) {
      case MyExpenses.SPLIT_PART_CURSOR:
      mAdapter.swapCursor(cursor);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.swapCursor(null);
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (ctx == null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE) {
      if (mTransaction.transfer_peer != null && DbUtils.hasParent(mTransaction.transfer_peer)) {
        Toast.makeText(getActivity(), getString(R.string.warning_splitpartcategory_context), Toast.LENGTH_LONG).show();
        return;
      }
      Intent i = new Intent(ctx, ExpenseEdit.class);
      i.putExtra(KEY_ROWID, mTransaction.getId());
      i.putExtra(DatabaseConstants.KEY_TRANSFER_ENABLED,ctx.transferEnabled());
      //i.putExtra("operationType", operationType);
      ctx.startActivityForResult(i, MyExpenses.EDIT_TRANSACTION_REQUEST);
    } else {
      dismiss();
    }
  }
  public void fillData(Transaction o) {
    final MyExpenses ctx = (MyExpenses) getActivity();
    mLayout.findViewById(R.id.progress).setVisibility(View.GONE);
    mTransaction = o;
    if (mTransaction == null) {
      TextView error = (TextView) mLayout.findViewById(R.id.error);
      error.setVisibility(View.VISIBLE);
      error.setText("Transaction has been deleted");
      return;
    }
    mLayout.findViewById(R.id.Table).setVisibility(View.VISIBLE);
    int title;
    boolean type = mTransaction.amount.getAmountMinor() > 0 ? ExpenseEdit.INCOME : ExpenseEdit.EXPENSE;
    if (mTransaction instanceof SplitTransaction) {
      //TODO: refactor duplicated code with SplitPartList
      title = R.string.split_transaction;
      View emptyView = mLayout.findViewById(R.id.empty);
      Resources.Theme theme = ctx.getTheme();
      TypedValue color = new TypedValue();
      theme.resolveAttribute(R.attr.colorExpense, color, true);
      final int colorExpense = color.data;
      theme.resolveAttribute(R.attr.colorIncome,color, true);
      final int colorIncome = color.data;
      
      ListView lv = (ListView) mLayout.findViewById(R.id.list);
      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{KEY_LABEL_MAIN,KEY_AMOUNT};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.category,R.id.amount};

      final String categorySeparator, commentSeparator;
      categorySeparator = " : ";
      commentSeparator = " / ";
      // Now create a simple cursor adapter and set it to display
      mAdapter = new SimpleCursorAdapter(ctx, R.layout.split_part_row, null, from, to,0)  {
        /* (non-Javadoc)
         * calls {@link #convText for formatting the values retrieved from the cursor}
         * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
         */
        @Override
        public void setViewText(TextView v, String text) {
          switch (v.getId()) {
          case R.id.amount:
            text = Utils.convAmount(text,mTransaction.amount.getCurrency());
          }
          super.setViewText(v, text);
        }
        /* (non-Javadoc)
         * manipulates the view for amount (setting expenses to red) and
         * category (indicate transfer direction with => or <=
         * @see android.widget.CursorAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          View row=super.getView(position, convertView, parent);
          TextView tv1 = (TextView)row.findViewById(R.id.amount);
          Cursor c = getCursor();
          c.moveToPosition(position);
          int col = c.getColumnIndex(KEY_AMOUNT);
          long amount = c.getLong(col);
          if (amount < 0) {
            tv1.setTextColor(colorExpense);
            // Set the background color of the text.
          }
          else {
            tv1.setTextColor(colorIncome);
          }
          TextView tv2 = (TextView)row.findViewById(R.id.category);
          if (Build.VERSION.SDK_INT < 11)
            tv2.setTextColor(Color.WHITE);
          String catText = tv2.getText().toString();
          if (DbUtils.getLongOrNull(c,KEY_TRANSFER_PEER) != null) {
            catText = ((amount < 0) ? "=&gt; " : "&lt;= ") + catText;
          } else {
            Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
            if (catId == null) {
              catText = getString(R.string.no_category_assigned);
            }
            else {
              col = c.getColumnIndex(KEY_LABEL_SUB);
              String label_sub = c.getString(col);
              if (label_sub != null && label_sub.length() > 0) {
                catText += categorySeparator + label_sub;
              }
            }
          }
          col = c.getColumnIndex(KEY_COMMENT);
          String comment = c.getString(col);
          if (comment != null && comment.length() > 0) {
            catText += (catText.equals("") ? "" : commentSeparator) + "<i>" + comment + "</i>";
          }
          tv2.setText(Html.fromHtml(catText));
          return row;
        }
      };
      lv.setAdapter(mAdapter);
      lv.setEmptyView(emptyView);
      LoaderManager manager = ctx.getSupportLoaderManager();
      if (manager.getLoader(MyExpenses.SPLIT_PART_CURSOR) != null && !manager.getLoader(MyExpenses.SPLIT_PART_CURSOR).isReset())
        manager.restartLoader(MyExpenses.SPLIT_PART_CURSOR, null, this);
      else
        manager.initLoader(MyExpenses.SPLIT_PART_CURSOR, null, this);
    } else {
      mLayout.findViewById(R.id.SplitContainer).setVisibility(View.GONE);
      if (mTransaction instanceof Transfer) {
        title = R.string.transfer;
        ((TextView) mLayout.findViewById(R.id.AccountLabel)).setText(R.string.transfer_from_account);
        ((TextView) mLayout.findViewById(R.id.CategoryLabel)).setText(R.string.transfer_to_account);
      }
      else {
        title = type ? R.string.income : R.string.expense;
        ((TextView) mLayout.findViewById(R.id.PayeeLabel)).setText(type?R.string.payer:R.string.payee);
      }
    }
    String accountLabel = Account.getInstanceFromDb(mTransaction.accountId).label;
    if (mTransaction instanceof Transfer) {
      ((TextView) mLayout.findViewById(R.id.Account)).setText(type ? mTransaction.label : accountLabel);
      ((TextView) mLayout.findViewById(R.id.Category)).setText(type ? accountLabel : mTransaction.label);
    } else {
      ((TextView) mLayout.findViewById(R.id.Account)).setText(accountLabel);
      if ((mTransaction.getCatId() != null && mTransaction.getCatId() > 0)) {
        ((TextView) mLayout.findViewById(R.id.Category)).setText(mTransaction.label);
      } else {
        mLayout.findViewById(R.id.CategoryRow).setVisibility(View.GONE);
      }
    }
    ((TextView) mLayout.findViewById(R.id.Date)).setText(
        DateFormat.getDateInstance(DateFormat.FULL).format(mTransaction.getDate())
        + " "
        + DateFormat.getTimeInstance(DateFormat.SHORT).format(mTransaction.getDate()));
    ((TextView) mLayout.findViewById(R.id.Amount)).setText(Utils.formatCurrency(
        new Money(mTransaction.amount.getCurrency(),Math.abs(mTransaction.amount.getAmountMinor()))));
    if (!mTransaction.comment.equals(""))
      ((TextView) mLayout.findViewById(R.id.Comment)).setText(mTransaction.comment);
    else
      mLayout.findViewById(R.id.CommentRow).setVisibility(View.GONE);
    if (!mTransaction.referenceNumber.equals(""))
      ((TextView) mLayout.findViewById(R.id.Number)).setText(mTransaction.referenceNumber);
    else
      mLayout.findViewById(R.id.NumberRow).setVisibility(View.GONE);
    if (!mTransaction.payee.equals(""))
      ((TextView) mLayout.findViewById(R.id.Payee)).setText(mTransaction.payee);
    else
      mLayout.findViewById(R.id.PayeeRow).setVisibility(View.GONE);
    if (mTransaction.methodId != null)
      ((TextView) mLayout.findViewById(R.id.Method)).setText(PaymentMethod.getInstanceFromDb(mTransaction.methodId).getDisplayLabel());
    else
      mLayout.findViewById(R.id.MethodRow).setVisibility(View.GONE);
    if (Account.getInstanceFromDb(mTransaction.accountId).type.equals(Type.CASH))
      mLayout.findViewById(R.id.StatusRow).setVisibility(View.GONE);
    else {
      TextView tv = (TextView)mLayout.findViewById(R.id.Status);
      tv.setBackgroundColor(mTransaction.crStatus.color);
      tv.setText(mTransaction.crStatus.toString());
    }
    getDialog().setTitle(title);
  }
}
