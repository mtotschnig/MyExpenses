package org.totschnig.myexpenses;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class TransactionList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  long accountId;
  SimpleCursorAdapter mAdapter;
  private int colorExpense;
  private int colorIncome;
  public static TransactionList newInstance(long accountId) {
    
    TransactionList pageFragment = new TransactionList();
    Bundle bundle = new Bundle();
    bundle.putLong("account_id", accountId);
    pageFragment.setArguments(bundle);
    return pageFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      accountId = getArguments() != null ? getArguments().getLong("account_id") : 1;
  }

  @Override  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Resources.Theme theme = getActivity().getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
    final Account account;
    try {
      account = Account.getInstanceFromDb(getArguments().getLong("account_id"));
    } catch (DataObjectNotFoundException e) {
      // this should not happen, since we got the account_id from db
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    
    View v = inflater.inflate(R.layout.expenses_list, null, false);
    int textColor = Utils.getTextColorForBackground(account.color);
    TextView tv = (TextView) v.findViewById(R.id.label);
    tv.setText(account.label);
    tv.setTextColor(textColor);
    tv = (TextView) v.findViewById(R.id.end);
    tv.setText(Utils.formatCurrency(account.getCurrentBalance()));
    tv.setTextColor(textColor);
    v.findViewById(R.id.heading).setBackgroundColor(account.color);
    ListView lv = (ListView) v.findViewById(R.id.list);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_LABEL_MAIN,KEY_DATE,KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category,R.id.date,R.id.amount};

    final SimpleDateFormat dateFormat;
    final String categorySeparator, commentSeparator;
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      dateFormat =  new SimpleDateFormat("dd.MM HH:mm");
      categorySeparator = " : ";
      commentSeparator = " / ";
    } else {
      dateFormat = new SimpleDateFormat("dd.MM\nHH:mm");
      categorySeparator = " :\n";
      commentSeparator = "<br>";
    }
    getLoaderManager().initLoader(0, null, this);
    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.expense_row, null, from, to,0)  {
      /* (non-Javadoc)
       * calls {@link #convText for formatting the values retrieved from the cursor}
       * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
       */
      @Override
      public void setViewText(TextView v, String text) {
        switch (v.getId()) {
        case R.id.date:
          text = Utils.convDate(text,dateFormat);
          break;
        case R.id.amount:
          text = Utils.convAmount(text,account.currency);
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
        int col = c.getColumnIndex(ExpensesDbAdapter.KEY_AMOUNT);
        long amount = c.getLong(col);
        if (amount < 0) {
          tv1.setTextColor(colorExpense);
          // Set the background color of the text.
        }
        else {
          tv1.setTextColor(colorIncome);
        }
        TextView tv2 = (TextView)row.findViewById(R.id.category);
        col = c.getColumnIndex(ExpensesDbAdapter.KEY_TRANSFER_PEER);
        String catText = (String) tv2.getText();
        if (c.getLong(col) != 0) {
          catText = ((amount < 0) ? "=&gt; " : "&lt;= ") + catText;
        } else {
          col = c.getColumnIndex(ExpensesDbAdapter.KEY_LABEL_SUB);
          String label_sub = c.getString(col);
          if (label_sub != null && label_sub.length() > 0) {
            catText += categorySeparator + label_sub;
          }
        }
        col = c.getColumnIndex(ExpensesDbAdapter.KEY_COMMENT);
        String comment = c.getString(col);
        if (comment != null && comment.length() > 0) {
          catText += (catText.equals("") ? "" : commentSeparator) + "<i>" + comment + "</i>";
        }
        tv2.setText(Html.fromHtml(catText));
        return row;
      }
    };
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    lv.setOnItemClickListener(new OnItemClickListener()
    {
         @Override
         public void onItemClick(AdapterView<?> a, View v,int position, long id)
         {
           Intent i = new Intent(getActivity(), ExpenseEdit.class);
           i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
           //i.putExtra("operationType", operationType);
           startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
         }
    });
    registerForContextMenu(lv);
    return v;

  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    String[] projection = new String[]{KEY_ROWID,KEY_DATE,KEY_AMOUNT, KEY_COMMENT,
        KEY_CATID,LABEL_MAIN,LABEL_SUB,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_METHODID};
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.TRANSACTIONS_URI, projection, "account_id = ?", new String[] { String.valueOf(accountId) }, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mAdapter.swapCursor(c);
    
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mAdapter.swapCursor(null);
  }
}
