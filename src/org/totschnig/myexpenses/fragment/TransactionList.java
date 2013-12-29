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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.SelectFromCursorDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView.OnHeaderClickListener;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.LinearLayout.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

//TODO: consider moving to ListFragment
public class TransactionList extends BudgetListFragment implements
    LoaderManager.LoaderCallbacks<Cursor>,OnHeaderClickListener {
  private static final int TRANSACTION_CURSOR = 0;
  private static final int SUM_CURSOR = 1;
  private static final int GROUPING_CURSOR = 2;
  private StickyListHeadersAdapter mAdapter;
  private AccountObserver aObserver;
  private Account mAccount;
  public boolean hasItems, mappedCategories;
  private Cursor mTransactionsCursor, mGroupingCursor;
  private DateFormat itemDateFormat;
  private StickyListHeadersListView mListView;
  private LoaderManager mManager;
  private SparseBooleanArray mappedCategoriesPerGroup;

  private int columnIndexYear, columnIndexYearOfWeekStart,columnIndexMonth, columnIndexWeek, columnIndexDay,
    columnIndexAmount, columnIndexLabelSub, columnIndexComment, columnIndexPayee, columnIndexCrStatus, columnIndexReferenceNumber,
    columnIndexGroupYear, columnIndexGroupSecond, columnIndexGroupMappedCategories, columIndexGroupSumInterim,
    columnIndexGroupSumIncome, columnIndexGroupSumExpense, columnIndexGroupSumTransfer;
  boolean indexesCalculated, indexesGroupingCalculated = false;
  //the following values are cached from the account object, so that we can react to changes in the observer
  private Grouping mGrouping;
  private Type mType;
  private String mCurrency;
  private Long mOpeningBalance;

  public static Fragment newInstance(Account account) {
    TransactionList pageFragment = new TransactionList();
    Bundle bundle = new Bundle();
    bundle.putSerializable("account", account);
    pageFragment.setArguments(bundle);
    return pageFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    mappedCategoriesPerGroup = new SparseBooleanArray();
    mAccount = (Account) getArguments().getSerializable("account");
    mGrouping = mAccount.grouping;
    mType = mAccount.type;
    mCurrency = mAccount.currency.getCurrencyCode();
    mOpeningBalance = mAccount.openingBalance.getAmountMinor();
    aObserver = new AccountObserver(new Handler());
    ContentResolver cr= getSherlockActivity().getContentResolver();
    //when account has changed, we might have
    //1) to refresh the list (currency has changed),
    //2) update current balance(opening balance has changed),
    //3) update the bottombarcolor (color has changed)
    //4) refetch grouping cursor (grouping has changed)
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
    switch (mAccount.grouping) {
    case DAY:
      itemDateFormat = new SimpleDateFormat("HH:mm");
      break;
    case MONTH:
      itemDateFormat = new SimpleDateFormat("dd");
      break;
    case WEEK:
      itemDateFormat = new SimpleDateFormat("EEE");
      break;
    case YEAR:
    case NONE:
      itemDateFormat = Utils.localizedYearlessDateFormat();
    }
    restartGroupingLoader();
  }
  private void restartGroupingLoader() {
    mGroupingCursor = null;
    if (mManager.getLoader(GROUPING_CURSOR) != null && !mManager.getLoader(GROUPING_CURSOR).isReset())
      mManager.restartLoader(GROUPING_CURSOR, null, this);
    else
      mManager.initLoader(GROUPING_CURSOR, null, this);
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
    mManager = getLoaderManager();
    setGrouping();
    setColors();
    
    View v = inflater.inflate(R.layout.expenses_list, null, false);
    //work around the problem that the view pager does not display its background correclty with Sherlock
    if (Build.VERSION.SDK_INT < 11) {
      v.setBackgroundColor(ctx.getResources().getColor(
         MyApplication.getInstance().getSettings().getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("light")
          ? android.R.color.white : android.R.color.black));
    }
    mListView = (StickyListHeadersListView) v.findViewById(R.id.list);
    setAdapter();
    mListView.setOnHeaderClickListener(this);
    mListView.setDrawingListUnderStickyHeader(false);
    mManager.initLoader(GROUPING_CURSOR, null, this);
    mManager.initLoader(TRANSACTION_CURSOR, null, this);
    mManager.initLoader(SUM_CURSOR, null, this);
    // Now create a simple cursor adapter and set it to display

    mListView.setEmptyView(v.findViewById(R.id.empty));
    mListView.setOnItemClickListener(new OnItemClickListener() {
       @Override
       public void onItemClick(AdapterView<?> a, View v,int position, long id) {
         FragmentManager fm = ctx.getSupportFragmentManager();
         DialogFragment f = (DialogFragment) fm.findFragmentByTag("TRANSACTION_DETAIL");
         if (f == null) {
           f = TransactionDetailFragment.newInstance(id);
         }
         f.show(fm, "TRANSACTION_DETAIL");
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
    FragmentManager fm = ctx.getSupportFragmentManager();
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      if (checkSplitPartTransfer(info.position)) {
        MessageDialogFragment.newInstance(
            R.string.dialog_title_warning_delete_transaction,
            R.string.warning_delete_transaction,
            new MessageDialogFragment.Button(android.R.string.yes, R.id.DELETE_COMMAND_DO, info.id),
            null,
            MessageDialogFragment.Button.noButton())
          .show(ctx.getSupportFragmentManager(),"DELETE_TRANSACTION");
      }
      return true;
    case R.id.CLONE_TRANSACTION_COMMAND:
      fm.beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_CLONE,info.id, null), "ASYNC_TASK")
        .commit();
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
    CursorLoader cursorLoader = null;
    String selection;
    String[] selectionArgs;
    if (mAccount.id < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT _id from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[] {mAccount.currency.getCurrencyCode()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[] { String.valueOf(mAccount.id) };
    }
    switch(id) {
    case TRANSACTION_CURSOR:
      Uri uri = (mAccount.id < 0) ?
          TransactionProvider.TRANSACTIONS_URI.buildUpon().appendQueryParameter("extended", "1").build() :
          TransactionProvider.TRANSACTIONS_URI;
      cursorLoader = new CursorLoader(getSherlockActivity(),
          uri, null, selection + " AND parent_id is null",
          selectionArgs, null);
      break;
    //TODO: probably we can get rid of SUM_CURSOR, if we also aggregate unmapped transactions
    case SUM_CURSOR:
      cursorLoader = new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI,
          new String[] {MAPPED_CATEGORIES},
          selection + " AND (cat_id IS null OR cat_id != " + SPLIT_CATID + ")",
          selectionArgs, null);
      break;
    case GROUPING_CURSOR:
      Builder builder = TransactionProvider.TRANSACTIONS_URI.buildUpon();
      builder.appendPath("groups")
        .appendPath(mAccount.grouping.name());
      //the selectionArg is used in a subquery used by the content provider
      //this will change once filters are implemented
      if (mAccount.id < 0) {
        builder.appendQueryParameter(KEY_CURRENCY, mAccount.currency.getCurrencyCode());
      } else {
        builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(mAccount.id));
      }
      cursorLoader = new CursorLoader(getSherlockActivity(),
          builder.build(),
          null,null,null, null);
      break;
    }
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    switch(arg0.getId()) {
    case TRANSACTION_CURSOR:
      mTransactionsCursor = c;
      hasItems = c.getCount()>0;
      if (!indexesCalculated) {
        columnIndexYear = c.getColumnIndex("year");
        columnIndexYearOfWeekStart = c.getColumnIndex("year_of_week_start");
        columnIndexMonth = c.getColumnIndex("month");
        columnIndexWeek = c.getColumnIndex("week");
        columnIndexDay  = c.getColumnIndex("day");
        columnIndexAmount = c.getColumnIndex(KEY_AMOUNT);
        columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
        columnIndexComment = c.getColumnIndex(KEY_COMMENT);
        columnIndexReferenceNumber= c.getColumnIndex(KEY_REFERENCE_NUMBER);
        columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
        columnIndexCrStatus = c.getColumnIndex(KEY_CR_STATUS);
        indexesCalculated = true;
      }
      ((SimpleCursorAdapter) mAdapter).swapCursor(c);
      break;
    case SUM_CURSOR:
      c.moveToFirst();
      mappedCategories = c.getInt(c.getColumnIndex("mapped_categories")) >0;
      break;
    case GROUPING_CURSOR:
      mGroupingCursor = c;
      //if the transactionscursor has been loaded before the grouping cursor, we need to refresh
      //in order to have accurate grouping values
      if (!indexesGroupingCalculated) {
        columnIndexGroupYear = c.getColumnIndex("year");
        columnIndexGroupSecond = c.getColumnIndex("second");
        columnIndexGroupSumIncome = c.getColumnIndex("sum_income");
        columnIndexGroupSumExpense = c.getColumnIndex("sum_expense");
        columnIndexGroupSumTransfer = c.getColumnIndex("sum_transfer");
        columnIndexGroupMappedCategories = c.getColumnIndex("mapped_categories");
        columIndexGroupSumInterim = c.getColumnIndex("interim_balance");
        indexesGroupingCalculated = true;
      }
      if (mTransactionsCursor != null)
        ((BaseAdapter) mAdapter).notifyDataSetChanged();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    switch(arg0.getId()) {
    case TRANSACTION_CURSOR:
      mTransactionsCursor = null;
      ((SimpleCursorAdapter) mAdapter).swapCursor(null);
      hasItems = false;
      break;
    case SUM_CURSOR:
      mappedCategories = false;
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
      //if grouping has changed
      if (mAccount.grouping != mGrouping) {
        if (mAdapter != null) {
          mGrouping = mAccount.grouping;
          setGrouping();
          //we should not need to notify here, since setGrouping restarts
          //the loader and in onLoadFinished we notify
          //mAdapter.notifyDataSetChanged();
        }
        return;
      }
      if (mAccount.type != mType ||
          mAccount.currency.getCurrencyCode() != mCurrency) {
        mListView.setAdapter(mAdapter);
        mType = mAccount.type;
        mCurrency = mAccount.currency.getCurrencyCode();
      }
      if (mAccount.openingBalance.getAmountMinor() != mOpeningBalance) {
        restartGroupingLoader();
        mOpeningBalance = mAccount.openingBalance.getAmountMinor();
      }
    }
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
    @SuppressWarnings("incomplete-switch")
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      HeaderViewHolder holder = new HeaderViewHolder();
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.header, parent, false);
        holder.text = (TextView) convertView.findViewById(R.id.text);
        holder.sumExpense = (TextView) convertView.findViewById(R.id.sum_expense);
        holder.sumIncome = (TextView) convertView.findViewById(R.id.sum_income);
        holder.sumTransfer = (TextView) convertView.findViewById(R.id.sum_transfer);
        holder.interimBalance = (TextView) convertView.findViewById(R.id.interim_balance);
        convertView.setTag(holder);
      } else
        holder = (HeaderViewHolder) convertView.getTag();

      Cursor c = getCursor();
      c.moveToPosition(position);
      int year = c.getInt(mAccount.grouping.equals(Grouping.WEEK)?columnIndexYearOfWeekStart:columnIndexYear);
      int second=-1;

      if (mGroupingCursor != null) {
        mGroupingCursor.moveToFirst();
        //no grouping, we need the first and only row
        if (mAccount.grouping.equals(Grouping.NONE)) {
          fillSums(holder,mGroupingCursor);
        } else {
          traverseCursor:
          while (!mGroupingCursor.isAfterLast()) {
            if (mGroupingCursor.getInt(columnIndexGroupYear) == year) {
              switch (mAccount.grouping) {
              case YEAR:
                fillSums(holder,mGroupingCursor);
                break traverseCursor;
              case DAY:
                second = c.getInt(columnIndexDay);
                if (mGroupingCursor.getInt(columnIndexGroupSecond) != second)
                  break;
                else {
                  fillSums(holder,mGroupingCursor);
                  break traverseCursor;
                }
              case MONTH:
                second = c.getInt(columnIndexMonth);
                if (mGroupingCursor.getInt(columnIndexGroupSecond) != second)
                  break;
                else {
                  fillSums(holder,mGroupingCursor);
                  break traverseCursor;
                }
              case WEEK:
                second = c.getInt(columnIndexWeek);
                if (mGroupingCursor.getInt(columnIndexGroupSecond) != second)
                  break;
                else {
                  fillSums(holder,mGroupingCursor);
                  break traverseCursor;
                }
              }
            }
            mGroupingCursor.moveToNext();
          }
        }
        if (!mGroupingCursor.isAfterLast())
          mappedCategoriesPerGroup.put(position, mGroupingCursor.getInt(columnIndexGroupMappedCategories)>0);
      }
      holder.text.setText(mAccount.grouping.getDisplayTitle(getActivity(),year,second,c));
      //holder.text.setText(mAccount.grouping.getDisplayTitle(getActivity(), year, second, mAccount.grouping.equals(Grouping.WEEK)?this_year_of_week_start:this_year, this_week,this_day));
      return convertView;
    }
    private void fillSums(HeaderViewHolder holder, Cursor mGroupingCursor) {
      holder.sumExpense.setText("- " + Utils.convAmount(
          DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumExpense),
          mAccount.currency));
      holder.sumIncome.setText("+ " + Utils.convAmount(
          DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumIncome),
          mAccount.currency));
      holder.sumTransfer.setText("<-> " + Utils.convAmount(
          DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumTransfer),
          mAccount.currency));
      holder.interimBalance.setText("= " + Utils.convAmount(
          DbUtils.getLongOr0L(mGroupingCursor, columIndexGroupSumInterim),
          mAccount.currency));
    }
    @Override
    public long getHeaderId(int position) {
      if (mAccount.grouping.equals(Account.Grouping.NONE))
        return 1;
      Cursor c = getCursor();
      c.moveToPosition(position);
      int year = c.getInt(mAccount.grouping.equals(Grouping.WEEK)?columnIndexYearOfWeekStart:columnIndexYear);
      int month = c.getInt(columnIndexMonth);
      int week = c.getInt(columnIndexWeek);
      int day = c.getInt(columnIndexDay);
      switch(mAccount.grouping) {
      case DAY:
        return year*1000+day;
      case WEEK:
        return year*1000+week;
      case MONTH:
        return year*1000+month;
      case YEAR:
        return year*1000;
      default:
        return 0;
      }
    }
  }
  public class MyAdapter extends SimpleCursorAdapter {
    String categorySeparator = " : ",
        commentSeparator = " / ";

    public MyAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      View v= super.newView(context, cursor, parent);
      if (mAccount.type.equals(Type.CASH))
        v.findViewById(R.id.colorContainer).setVisibility(View.GONE);
      if (mAccount.id < 0)
        v.findViewById(R.id.colorAccount).setLayoutParams(
            new LayoutParams(4, LayoutParams.FILL_PARENT));
      return v;
  }
    /* (non-Javadoc)
     * calls {@link #convText for formatting the values retrieved from the cursor}
     * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
     */
    @Override
    public void setViewText(TextView v, String text) {
      switch (v.getId()) {
      case R.id.date:
        text = Utils.convDateTime(text,itemDateFormat);
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
      convertView=super.getView(position, convertView, parent);

      TextView tv1 = (TextView)convertView.findViewById(R.id.amount);
      Cursor c = getCursor();
      c.moveToPosition(position);
      if (mAccount.id <0) {
        int color = c.getInt(c.getColumnIndex("color"));
        convertView.findViewById(R.id.colorAccount).setBackgroundColor(color);
      }
      long amount = c.getLong(columnIndexAmount);
      if (amount < 0) {
        tv1.setTextColor(colorExpense);
        // Set the background color of the text.
      }
      else {
        tv1.setTextColor(colorIncome);
      }
      TextView tv2 = (TextView)convertView.findViewById(R.id.category);
      CharSequence catText = tv2.getText();
      if (DbUtils.getLongOrNull(c,KEY_TRANSFER_PEER) != null) {
        catText = ((amount < 0) ? "=> " : "<= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
        if (SPLIT_CATID.equals(catId))
          catText = getString(R.string.split_transaction);
        else if (catId == null) {
          catText = getString(R.string.no_category_assigned);
        } else {
          String label_sub = c.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + categorySeparator + label_sub;
          }
        }
      }
      String referenceNumber= c.getString(columnIndexReferenceNumber);
      if (referenceNumber != null && referenceNumber.length() > 0)
        catText = "(" + referenceNumber + ") " + catText;
      SpannableStringBuilder ssb;
      String comment = c.getString(columnIndexComment);
      if (comment != null && comment.length() > 0) {
        ssb = new SpannableStringBuilder(comment);
        ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, comment.length(), 0);
        catText = TextUtils.concat(catText,commentSeparator,ssb);
      }
      String payee = c.getString(columnIndexPayee);
      if (payee != null && payee.length() > 0) {
        ssb = new SpannableStringBuilder(payee);
        ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
        catText = TextUtils.concat(catText,commentSeparator,ssb);
      }
      tv2.setText(catText);
      
      if (!mAccount.type.equals(Type.CASH)) {
        CrStatus status;
        try {
          status = CrStatus.valueOf(c.getString(columnIndexCrStatus));
        } catch (IllegalArgumentException ex) {
          status = CrStatus.UNRECONCILED;
        }
        convertView.findViewById(R.id.color1).setBackgroundColor(status.color);
        convertView.findViewById(R.id.colorContainer).setTag(getItemId(position));
      }
      return convertView;
    }
  }
  class HeaderViewHolder {
    TextView interimBalance;
    TextView text;
    TextView sumIncome;
    TextView sumExpense;
    TextView sumTransfer;
  }
  @Override
  public void onHeaderClick(StickyListHeadersListView l, View header,
      int itemPosition, long headerId, boolean currentlySticky) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (mappedCategoriesPerGroup.get(itemPosition)) {
      if (MyApplication.getInstance().isContribEnabled) {
        ctx.contribFeatureCalled(Feature.DISTRIBUTION, headerId);
      }
      else {
        CommonCommands.showContribDialog(ctx,Feature.DISTRIBUTION, headerId);
      }
    } else {
      Toast.makeText(ctx, getString(R.string.no_mapped_transactions), Toast.LENGTH_LONG).show();
    }
  }
}
