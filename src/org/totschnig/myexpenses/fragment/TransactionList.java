package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.SelectFromCursorDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView.OnHeaderClickListener;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

//TODO: consider moving to ListFragment
public class TransactionList extends SherlockFragment implements
    LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener, OnHeaderClickListener {
  private static final int TRANSACTION_CURSOR = 0;
  private static final int SUM_CURSOR = 1;
  private static final int GROUPING_CURSOR = 2;
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
  private TransactionsGrouping mGrouping;
  public enum TransactionsGrouping {
    NONE,DAY,WEEK,MONTH,YEAR
  }
  DateFormat headerDateFormat;
  String headerPrefix;
  private StickyListHeadersListView mListView;
  private Cursor mGroupingCursor;
  private LoaderManager mManager;

  public static TransactionList newInstance(long accountId) {
    
    TransactionList pageFragment = new TransactionList();
    Bundle bundle = new Bundle();
    bundle.putLong("account_id", accountId);
    Log.i("DEBUG",String.format("creating framgent instance for account %d", accountId));
    pageFragment.setArguments(bundle);
    return pageFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      MyApplication.getInstance().getSettings().registerOnSharedPreferenceChangeListener(this);

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
  private void setAdapter() {
    Context ctx = getSherlockActivity();
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_LABEL_MAIN,KEY_DATE,KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category,R.id.date,R.id.amount};
    mAdapter = new MyGroupedAdapter(ctx, R.layout.expense_row, null, from, to,0);
    mListView.setAdapter(mAdapter);
  }
  private void setGrouping() {
    try {
      mGrouping = TransactionsGrouping.valueOf(
          MyApplication.getInstance().getSettings()
          .getString(MyApplication.PREFKEY_TRANSACTIONS_GROUPING, "WEEK"));
    } catch (IllegalArgumentException e) {
      mGrouping = TransactionsGrouping.WEEK;
    }
    switch (mGrouping) {
    case DAY:
      headerPrefix = "";
      headerDateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL);
      break;
    case MONTH:
      headerPrefix = "";
      headerDateFormat = new SimpleDateFormat("MMMM y");
      break;
    case WEEK:
      headerPrefix = "Week ";
      headerDateFormat = new SimpleDateFormat("ww, y");
      break;
    case YEAR:
      headerPrefix = "";
      headerDateFormat = new SimpleDateFormat("y");
    }
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
    mTransactionsCursor.moveToPosition(info.position);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    //templates for splits is not yet implemented
    if (! SPLIT_CATID.equals(DbUtils.getLongOrNull(mTransactionsCursor, KEY_CATID)))
      menu.add(0, R.id.CREATE_TEMPLATE_COMMAND, 0, R.string.menu_create_template);
    menu.add(0, R.id.CLONE_TRANSACTION_COMMAND, 0, R.string.menu_clone_transaction);
    //move transaction is disabled for transfers,
    //TODO we also would need to check for splits with transfer parts
    if (((MyExpenses) getSherlockActivity()).getCursor(MyExpenses.ACCOUNTS_CURSOR,null).getCount() > 1 &&
        DbUtils.getLongOrNull(mTransactionsCursor, KEY_TRANSFER_PEER) == null) {
      menu.add(0,R.id.MOVE_TRANSACTION_COMMAND,0,R.string.menu_move_transaction);
    }
  }


  @Override  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getSherlockActivity();
    setGrouping();
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
    mListView = (StickyListHeadersListView) v.findViewById(R.id.list);
    setAdapter();
    mListView.setOnHeaderClickListener(this);
    mManager = getLoaderManager();
    if (!mGrouping.equals(TransactionsGrouping.NONE))
      mManager.initLoader(GROUPING_CURSOR, null, this);
    mManager.initLoader(TRANSACTION_CURSOR, null, this);
    mManager.initLoader(SUM_CURSOR, null, this);
    // Now create a simple cursor adapter and set it to display

    mListView.setEmptyView(v.findViewById(R.id.empty));
    mListView.setOnItemClickListener(new OnItemClickListener()
    {
         @Override
         public void onItemClick(AdapterView<?> a, View v,int position, long id)
         {
           TransactionDetailFragment.newInstance(id)
           .show(ctx.getSupportFragmentManager(), "TRANSACTION_DETAIL");
         }
    });
    registerForContextMenu(mListView);
    return v;
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    //http://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager-receives-oncontextitemselected-call
    if (!getUserVisibleHint())
      return false;
    MyExpenses ctx = (MyExpenses) getActivity();
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Bundle args;
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      if (checkSplitPartTransfer(info.position)) {
        Transaction.delete(info.id);
        ctx.configButtons();
      }
      return true;
    case R.id.CLONE_TRANSACTION_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
        ctx.contribFeatureCalled(Feature.CLONE_TRANSACTION, info.id);
      }
      else {
        CommonCommands.showContribDialog(ctx,Feature.CLONE_TRANSACTION, info.id);
      }
      return true;
    case R.id.MOVE_TRANSACTION_COMMAND:
      args = new Bundle();
      args.putInt("id", R.id.MOVE_TRANSACTION_COMMAND);
      args.putString("dialogTitle",getString(R.string.dialog_title_select_account));
      //args.putString("selection",KEY_ROWID + " != " + mCurrentAccount.id);
      args.putString("column", KEY_LABEL);
      args.putLong("contextTransactionId",info.id);
      args.putInt("cursorId", MyExpenses.ACCOUNTS_OTHER_CURSOR);
      SelectFromCursorDialogFragment.newInstance(args)
        .show(ctx.getSupportFragmentManager(), "SELECT_ACCOUNT");
      return true;
    case R.id.CREATE_TEMPLATE_COMMAND:
      args = new Bundle();
      args.putLong("transactionId", info.id);
      args.putString("dialogTitle", getString(R.string.dialog_title_template_title));
      EditTextDialog.newInstance(args).show(ctx.getSupportFragmentManager(), "TEMPLATE_TITLE");
      return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    Log.i("DEBUG",String.format("creating loader account %d, loader %d", accountId, id));
    CursorLoader cursorLoader = null;
    switch(id) {
    case TRANSACTION_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI, null, "account_id = ? AND parent_id is null",
          new String[] { String.valueOf(accountId) }, null);
      break;
    case SUM_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI, new String[] {"sum(" + KEY_AMOUNT + ")"}, "account_id = ? AND parent_id is null",
          new String[] { String.valueOf(accountId) }, null);
      break;
    case GROUPING_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI.buildUpon().appendPath("groups").appendPath(mGrouping.name()).build(),
          null,"account_id = ?",new String[] { String.valueOf(accountId) }, null);
      break;
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
      break;
    case GROUPING_CURSOR:
      mGroupingCursor = c;
      //if the transactionscursor has been loaded before the grouping cursor, we need to refresh
      //in order to have accurate grouping values
      if (mTransactionsCursor != null)
        mAdapter.notifyDataSetChanged();
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
      break;
    case GROUPING_CURSOR:
      mGroupingCursor = null;
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
      if (mAdapter != null)
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
    if (bottomLine != null)
      bottomLine.setBackgroundColor(mAccount.color);
  }
  private boolean checkSplitPartTransfer(int position) {
    mTransactionsCursor.moveToPosition(position);
    Long transferPeer = DbUtils.getLongOrNull(mTransactionsCursor, KEY_TRANSFER_PEER);
    if (transferPeer != null && DbUtils.hasParent(transferPeer)) {
      Toast.makeText(getActivity(), getString(R.string.warning_splitpartcategory_context), Toast.LENGTH_LONG).show();
      return false;
    }
    return true;
  }
  public class MyGroupedAdapter extends MyAdapter implements StickyListHeadersAdapter {
    LayoutInflater inflater;
    public MyGroupedAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      inflater = LayoutInflater.from(getSherlockActivity());
      
    }
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      Log.i("DEBUG",String.format("Fetching header for position %d", position));
      if (mGrouping.equals(TransactionsGrouping.NONE))
        return null;
      HeaderViewHolder holder = new HeaderViewHolder();
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.header, parent, false);
        holder.text = (TextView) convertView.findViewById(R.id.text);
        holder.sumExpense = (TextView) convertView.findViewById(R.id.sum_expense);
        holder.sumIncome = (TextView) convertView.findViewById(R.id.sum_income);
        holder.sumTransfer = (TextView) convertView.findViewById(R.id.sum_transfer);
        convertView.setTag(holder);
      } else
        holder = (HeaderViewHolder) convertView.getTag();

      Cursor c = getCursor();
      c.moveToPosition(position);
      int col = c.getColumnIndex(KEY_DATE);
      String headerText = headerPrefix + Utils.convDate(c.getString(col),headerDateFormat);
      int year = c.getInt(c.getColumnIndex("year"));
      int month = c.getInt(c.getColumnIndex("month"));
      int week = c.getInt(c.getColumnIndex("week"));
      int day = c.getInt(c.getColumnIndex("day"));
      holder.text.setText(headerText);
      if (mGroupingCursor != null) {
        mGroupingCursor.moveToFirst();
        traverseCursor:
        while (!mGroupingCursor.isAfterLast()) {
          if (mGroupingCursor.getInt(mGroupingCursor.getColumnIndex("year")) != year)
            continue;
          switch (mGrouping) {
          case YEAR:
            fillSums(holder,mGroupingCursor);
            break traverseCursor;
          case DAY:
            if (mGroupingCursor.getInt(mGroupingCursor.getColumnIndex("day")) != day)
              break;
            else
              fillSums(holder,mGroupingCursor);
              break traverseCursor;
          case MONTH:
            if (mGroupingCursor.getInt(mGroupingCursor.getColumnIndex("month")) != month)
              break;
            else
              fillSums(holder,mGroupingCursor);
              break traverseCursor;
          case WEEK:
            if (mGroupingCursor.getInt(mGroupingCursor.getColumnIndex("week")) != week)
              break;
            else
              fillSums(holder,mGroupingCursor);
              break traverseCursor;
          }
          mGroupingCursor.moveToNext();
        }
      }
      return convertView;
    }
    private void fillSums(HeaderViewHolder holder, Cursor mGroupingCursor) {
      holder.sumExpense.setText(Utils.convAmount(
          mGroupingCursor.getString(mGroupingCursor.getColumnIndex("sum_expense")),
          mAccount.currency));
      holder.sumIncome.setText("+ " + Utils.convAmount(
          mGroupingCursor.getString(mGroupingCursor.getColumnIndex("sum_income")),
          mAccount.currency));
      holder.sumTransfer.setText("<-> " + Utils.convAmount(
          mGroupingCursor.getString(mGroupingCursor.getColumnIndex("sum_transfer")),
          mAccount.currency));
    }
    @Override
    public long getHeaderId(int position) {
      if (mGrouping.equals(TransactionsGrouping.NONE))
        return 0;
      Cursor c = getCursor();
      c.moveToPosition(position);
      int year = c.getInt(c.getColumnIndex("year"));
      int month = c.getInt(c.getColumnIndex("month"));
      int week = c.getInt(c.getColumnIndex("week"));
      int day = c.getInt(c.getColumnIndex("day"));
      switch(mGrouping) {
      case DAY:
        return (year-1900)*200+day;
      case WEEK:
        return (year-1900)*200+week;
      case MONTH:
        return (year-1900)*200+month;
      case YEAR:
        return (year-1900);
      default:
        return 0;
      }
    }
  }
  public class MyAdapter extends SimpleCursorAdapter {

    SimpleDateFormat dateFormat;
    String categorySeparator, commentSeparator;

    public MyAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        dateFormat =  new SimpleDateFormat("dd.MM HH:mm");
        categorySeparator = " : ";
        commentSeparator = " / ";
      } else {
        dateFormat = new SimpleDateFormat("dd.MM\nHH:mm");
        categorySeparator = " :\n";
        commentSeparator = "<br>";
      }

      // TODO Auto-generated constructor stub
    }
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
      String catText = (String) tv2.getText();
      if (DbUtils.getLongOrNull(c,KEY_TRANSFER_PEER) != null) {
        catText = ((amount < 0) ? "=&gt; " : "&lt;= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
        if (SPLIT_CATID.equals(catId))
          catText = getString(R.string.split_transaction);
        else if (catId == null) {
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
  }
  class HeaderViewHolder {
    TextView text;
    TextView sumIncome;
    TextView sumExpense;
    TextView sumTransfer;
}
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_TRANSACTIONS_GROUPING)) {
      setGrouping();
      TransactionsGrouping oldValue = mGrouping;
      if (!mGrouping.equals(TransactionsGrouping.NONE)) {
        if (oldValue.equals(TransactionsGrouping.NONE))
          mManager.initLoader(GROUPING_CURSOR, null, this);
        else
          mManager.restartLoader(GROUPING_CURSOR, null, this);
      } else {
        mManager.destroyLoader(GROUPING_CURSOR);
        mAdapter.notifyDataSetChanged();
      }
    }
  }
  @Override
  public void onHeaderClick(StickyListHeadersListView l, View header,
      int itemPosition, long headerId, boolean currentlySticky) {
    View sumLine = header.findViewById(R.id.sum_line);
    sumLine.setVisibility(sumLine.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
  }
}
