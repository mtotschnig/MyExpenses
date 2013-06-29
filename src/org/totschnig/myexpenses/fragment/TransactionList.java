package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.SherlockFragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

//TODO: consider moving to ListFragment
public class TransactionList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  long accountId;
  SimpleCursorAdapter mAdapter;
  private int colorExpense;
  private int colorIncome;
  private TransactionsObserver tObserver;
  private AccountObserver aObserver;
  private Account mAccount;
  private TextView balanceTv;
  private View bottomLine;

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
      accountId = getArguments().getLong("account_id");
      mAccount = Account.getInstanceFromDb(getArguments().getLong("account_id"));
      tObserver = new TransactionsObserver(new Handler());
      aObserver = new AccountObserver(new Handler());
      ContentResolver cr= getActivity().getContentResolver();
      //we adjust the balance, when transactions have been added/deleted/updated
      cr.registerContentObserver(TransactionProvider.TRANSACTIONS_URI, true,tObserver);
      //when account has changed, we might have
      //1) to refresh the list (currency has changed),
      //2) update current balance(opening balance has changed),
      //3) update the bottombarcolor (color has changed
      cr.registerContentObserver(
          TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(accountId)).build(),
          true,aObserver);
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    try {
      ContentResolver cr = getActivity().getContentResolver();
      cr.unregisterContentObserver(tObserver);
      cr.unregisterContentObserver(aObserver);
    } catch (IllegalStateException ise) {
        // Do Nothing.  Observer has already been unregistered.
    }
  }

  @Override  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Resources.Theme theme = getActivity().getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
    
    View v = inflater.inflate(R.layout.expenses_list, null, false);
    //work around the problem that the view pager does not display its background correclty with Sherlock
    if (Build.VERSION.SDK_INT < 11) {
      v.setBackgroundColor(getActivity().getResources().getColor(
          MyApplication.getThemeId() == R.style.ThemeLight ? android.R.color.white : android.R.color.black));
    }
    balanceTv = (TextView) v.findViewById(R.id.end);
    updateBalance();
    balanceTv.setText(Utils.formatCurrency(mAccount.getCurrentBalance()));
    bottomLine = v.findViewById(R.id.BottomLine);
    updateColor();
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
          text = Utils.convAmount(text,mAccount.currency);
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
        col = c.getColumnIndex(KEY_TRANSFER_PEER);
        String catText = (String) tv2.getText();
        if (c.getLong(col) != 0) {
          catText = ((amount < 0) ? "=&gt; " : "&lt;= ") + catText;
        } else {
          col = c.getColumnIndex(KEY_LABEL_SUB);
          String label_sub = c.getString(col);
          if (label_sub != null && label_sub.length() > 0) {
            catText += categorySeparator + label_sub;
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
    lv.setEmptyView(v.findViewById(R.id.empty));
    lv.setOnItemClickListener(new OnItemClickListener()
    {
         @Override
         public void onItemClick(AdapterView<?> a, View v,int position, long id)
         {
           Intent i = new Intent(getActivity(), ExpenseEdit.class);
           i.putExtra(KEY_ROWID, id);
           //i.putExtra("operationType", operationType);
           startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
         }
    });
    registerForContextMenu(lv);
    return v;

  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.TRANSACTIONS_URI, null, "account_id = ?", new String[] { String.valueOf(accountId) }, null);
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
  class TransactionsObserver extends ContentObserver {
    public TransactionsObserver(Handler handler) {
       super(handler);
    }
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      updateBalance();
    }
 }
  class AccountObserver extends ContentObserver {
    public AccountObserver(Handler handler) {
       super(handler);
    }
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      updateBalance();
      updateColor();
      mAdapter.notifyDataSetChanged();
    }
  }
  private void updateBalance() {
      balanceTv.setText(Utils.formatCurrency(mAccount.getCurrentBalance()));
  }
  private void updateColor() {
    bottomLine.setBackgroundColor(mAccount.color);
  }
}
