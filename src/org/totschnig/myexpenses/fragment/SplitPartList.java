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

package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

//TODO: consider moving to ListFragment
public class SplitPartList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final int TRANSACTION_CURSOR = 0;
  private static final int SUM_CURSOR = 1;
  SimpleCursorAdapter mAdapter;
  private int colorExpense;
  private int colorIncome;
  private TextView balanceTv;
  private long transactionSum = 0;
  private Money unsplitAmount;

  public static SplitPartList newInstance(Long parentId, Long accountId) {
    SplitPartList f = new SplitPartList(); 
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_PARENTID,parentId);
    bundle.putLong(KEY_ACCOUNTID,accountId);
    f.setArguments(bundle);
    return f;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      setRetainInstance(true);
  }
  @Override  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final Activity ctx = getSherlockActivity();
    View v = inflater.inflate(R.layout.split_parts_list, container, false);
    View emptyView = v.findViewById(R.id.empty);
    Resources.Theme theme = ctx.getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
    balanceTv = (TextView) v.findViewById(R.id.end);
    
    ListView lv = (ListView) v.findViewById(R.id.list);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_LABEL_MAIN,KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category,R.id.amount};

    final String categorySeparator, commentSeparator;
    categorySeparator = " : ";
    commentSeparator = " / ";
    getLoaderManager().initLoader(TRANSACTION_CURSOR, getArguments(), this);
    getLoaderManager().initLoader(SUM_CURSOR, getArguments(), this);
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
          text = Utils.convAmount(text,Account.getInstanceFromDb(getArguments().getLong(KEY_ACCOUNTID)).currency);
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
    lv.setOnItemClickListener(new OnItemClickListener()
    {
         @Override
         public void onItemClick(AdapterView<?> a, View v,int position, long id)
         {
           Intent i = new Intent(ctx, ExpenseEdit.class);
           i.putExtra(KEY_ROWID, id);
           //i.putExtra("operationType", operationType);
           startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
         }
    });
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
  }
  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      Transaction.delete(info.id);
      return true;
    }
    return super.onContextItemSelected(item);
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] selectionArgs = new String[] {String.valueOf(args.getLong(KEY_PARENTID))};
    CursorLoader cursorLoader = null;
    Uri uri = TransactionProvider.UNCOMMITTED_URI;
    switch(id) {
    case TRANSACTION_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(), uri,null, "parent_id = ?",
          selectionArgs, null);
      return cursorLoader;
    case SUM_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),uri,
          new String[] {"sum(" + KEY_AMOUNT + ")"}, "parent_id = ?",
          selectionArgs, null);
    }
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    switch(arg0.getId()) {
    case TRANSACTION_CURSOR:
      mAdapter.swapCursor(c);
      break;
    case SUM_CURSOR:
      c.moveToFirst();
      transactionSum = c.getLong(0);
      updateBalance();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    switch(arg0.getId()) {
    case TRANSACTION_CURSOR:
      mAdapter.swapCursor(null);
      break;
    case SUM_CURSOR:
      transactionSum=0;
      updateBalance();
    }
  }
  public void updateBalance() {
    ExpenseEdit ctx = (ExpenseEdit) getSherlockActivity();
    unsplitAmount = ctx.getAmount();
    //when we are called before transaction is loaded in parent activity
    if (unsplitAmount == null)
      return;
    unsplitAmount.setAmountMinor(unsplitAmount.getAmountMinor()-transactionSum);
    if (balanceTv != null)
      balanceTv.setText(Utils.formatCurrency(unsplitAmount));
  }

  public boolean splitComplete() {
    return unsplitAmount.getAmountMinor() == 0L;
  }
}
