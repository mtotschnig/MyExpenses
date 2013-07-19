package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

//TODO: consider moving to ListFragment
public class TransactionList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final int TRANSACTION_CURSOR = 0;
  private static final int SUM_CURSOR = 1;
  long accountId;
  SimpleCursorAdapter mAdapter;
  private int colorExpense;
  private int colorIncome;
  private AccountObserver aObserver;
  private Account mAccount;
  private TextView balanceTv;
  private View bottomLine;
  private boolean hasItems;
  private long transactionSum = 0;
  private Cursor mTransactionsCursor;

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
      setHasOptionsMenu(true);
      accountId = getArguments().getLong("account_id");
      mAccount = Account.getInstanceFromDb(getArguments().getLong("account_id"));
      aObserver = new AccountObserver(new Handler());
      ContentResolver cr= getSherlockActivity().getContentResolver();
      //when account has changed, we might have
      //1) to refresh the list (currency has changed),
      //2) update current balance(opening balance has changed),
      //3) update the bottombarcolor (color has changed
      cr.registerContentObserver(
          TransactionProvider.ACCOUNTS_URI,
          true,aObserver);
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    try {
      ContentResolver cr = getSherlockActivity().getContentResolver();
      cr.unregisterContentObserver(aObserver);
    } catch (IllegalStateException ise) {
        // Do Nothing.  Observer has already been unregistered.
    }
  }
  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (isVisible())
      menu.findItem(R.id.RESET_ACCOUNT_COMMAND).setVisible(hasItems);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    menu.add(0, R.id.SHOW_DETAIL_COMMAND, 0, R.string.menu_show_detail);
    menu.add(0, R.id.CREATE_TEMPLATE_COMMAND, 0, R.string.menu_create_template);
    menu.add(0, R.id.CLONE_TRANSACTION_COMMAND, 0, R.string.menu_clone_transaction);
    mTransactionsCursor.moveToPosition(info.position);
    //move transaction is disabled for transfers,
    if (((MyExpenses) getSherlockActivity()).getCursor(MyExpenses.ACCOUNTS_CURSOR,null).getCount() > 1 &&
        DbUtils.getLongOrNull(mTransactionsCursor, KEY_TRANSFER_PEER) == null) {
      menu.add(0,R.id.MOVE_TRANSACTION_COMMAND,0,R.string.menu_move_transaction);
    }
  }


  @Override  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getSherlockActivity();
    Resources.Theme theme = ctx.getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
    
    View v = inflater.inflate(R.layout.expenses_list, null, false);
    //work around the problem that the view pager does not display its background correclty with Sherlock
    if (Build.VERSION.SDK_INT < 11) {
      v.setBackgroundColor(ctx.getResources().getColor(
          MyApplication.getThemeId() == R.style.ThemeLight ? android.R.color.white : android.R.color.black));
    }
    balanceTv = (TextView) v.findViewById(R.id.end);
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
    getLoaderManager().initLoader(TRANSACTION_CURSOR, null, this);
    getLoaderManager().initLoader(SUM_CURSOR, null, this);
    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(ctx, R.layout.expense_row, null, from, to,0)  {
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
           Intent i = new Intent(ctx, ExpenseEdit.class);
           i.putExtra(KEY_ROWID, id);
           i.putExtra("transferEnabled",ctx.mTransferEnabled);
           //i.putExtra("operationType", operationType);
           startActivityForResult(i, MyExpenses.ACTIVITY_EDIT);
         }
    });
    registerForContextMenu(lv);
    return v;

  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    CursorLoader cursorLoader = null;
    switch(id) {
    case TRANSACTION_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI, null, "account_id = ? AND parent_id is null",
          new String[] { String.valueOf(accountId) }, null);
      return cursorLoader;
    case SUM_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI, new String[] {"sum(" + KEY_AMOUNT + ")"}, "account_id = ? AND parent_id is null",
          new String[] { String.valueOf(accountId) }, null);
    }
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    switch(arg0.getId()) {
    case TRANSACTION_CURSOR:
      mTransactionsCursor = c;
      mAdapter.swapCursor(c);
      hasItems = c.getCount()>0;
      if (isVisible())
        getSherlockActivity().supportInvalidateOptionsMenu();
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
      mTransactionsCursor = null;
      mAdapter.swapCursor(null);
      hasItems = false;
      if (isVisible())
        getSherlockActivity().supportInvalidateOptionsMenu();
      break;
    case SUM_CURSOR:
      transactionSum=0;
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
    if (balanceTv != null)
      balanceTv.setText(Utils.formatCurrency(
          new Money(mAccount.currency,
              mAccount.openingBalance.getAmountMinor() + transactionSum)));
  }
  private void updateColor() {
    bottomLine.setBackgroundColor(mAccount.color);
  }
}
