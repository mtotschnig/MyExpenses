package org.totschnig.myexpenses.dialog;


import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;

import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class TransactionDetailFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor>,OnClickListener {
  Transaction mTransaction;
  SimpleCursorAdapter mAdapter;
  
  public static final TransactionDetailFragment newInstance(Long id) {
    TransactionDetailFragment dialogFragment = new TransactionDetailFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable("id", id);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle bundle = getArguments();
    mTransaction = Transaction.getInstanceFromDb(bundle.getLong("id"));
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.transaction_detail, null);
    int title;
    if (mTransaction instanceof SplitTransaction) {
      //TODO: refactor duplicated code with SplitPartList
      title = R.string.split_transaction;
      View emptyView = view.findViewById(R.id.empty);
      Resources.Theme theme = ctx.getTheme();
      TypedValue color = new TypedValue();
      theme.resolveAttribute(R.attr.colorExpense, color, true);
      final int colorExpense = color.data;
      theme.resolveAttribute(R.attr.colorIncome,color, true);
      final int colorIncome = color.data;
      
      ListView lv = (ListView) view.findViewById(R.id.list);
      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{KEY_LABEL_MAIN,KEY_AMOUNT};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.category,R.id.amount};

      final String categorySeparator, commentSeparator;
      categorySeparator = " : ";
      commentSeparator = " / ";
      getLoaderManager().initLoader(0, null, this);
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
            //text = Utils.convAmount(text,ctx.mAccount.currency);
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
          String catText = (String) tv2.getText();
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
      view.findViewById(R.id.CategoryRow).setVisibility(View.GONE);
    } else {
      view.findViewById(R.id.SplitContainer).setVisibility(View.GONE);
      ((TextView) view.findViewById(R.id.Category)).setText(mTransaction.label);
      if (mTransaction instanceof Transfer)
          title = R.string.transfer;
      else
        title = R.string.transaction;
    }
    ((TextView) view.findViewById(R.id.Date)).setText(
        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(mTransaction.date)
        + " "
        + new SimpleDateFormat("HH:mm").format(mTransaction.date));
    ((TextView) view.findViewById(R.id.Amount)).setText(Utils.formatCurrency(mTransaction.amount));
    if (!mTransaction.comment.equals(""))
      ((TextView) view.findViewById(R.id.Comment)).setText(mTransaction.comment);
    else
      view.findViewById(R.id.CommentRow).setVisibility(View.GONE);
    if (!mTransaction.payee.equals(""))
      ((TextView) view.findViewById(R.id.Payee)).setText(mTransaction.payee);
    else
      view.findViewById(R.id.PayeeRow).setVisibility(View.GONE);
    if (mTransaction.methodId != null)
      ((TextView) view.findViewById(R.id.Method)).setText(PaymentMethod.getInstanceFromDb(mTransaction.methodId).getDisplayLabel());
    else
      view.findViewById(R.id.MethodRow).setVisibility(View.GONE);
    return new AlertDialog.Builder(getActivity())
      .setTitle(title)
      .setView(view)
      .setNegativeButton(android.R.string.ok,this)
      .setPositiveButton(R.string.menu_edit,this)
      .create();
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(), TransactionProvider.TRANSACTIONS_URI,null, "parent_id = ?",
        new String[] { String.valueOf(mTransaction.id) }, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
    mAdapter.swapCursor(cursor);
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_POSITIVE) {
      final MyExpenses ctx = (MyExpenses) getActivity();
      Intent i = new Intent(ctx, ExpenseEdit.class);
      i.putExtra(KEY_ROWID, mTransaction.id);
      i.putExtra("transferEnabled",ctx.mTransferEnabled);
      //i.putExtra("operationType", operationType);
      startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    //nothing to do
  }
}
