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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.TransactionAdapter;
import org.totschnig.myexpenses.dialog.AmountFilterDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.SelectCrStatusDialogFragment;
import org.totschnig.myexpenses.dialog.SelectMethodDialogFragment;
import org.totschnig.myexpenses.dialog.SelectPayerDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.*;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView.OnHeaderClickListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

//TODO: consider moving to ListFragment
public class TransactionList extends ContextualActionBarFragment implements
    LoaderManager.LoaderCallbacks<Cursor>,OnHeaderClickListener {

  protected int getMenuResource() {
    return R.menu.transactionlist_context;
  }
  
  protected WhereFilter mFilter = WhereFilter.empty();

  private static final int TRANSACTION_CURSOR = 0;
  private static final int SUM_CURSOR = 1;
  private static final int GROUPING_CURSOR = 2;

  public static final String KEY_FILTER = "filter";
  public static final String CATEGORY_SEPARATOR = " : ",
      COMMENT_SEPARATOR = " / ";
  private MyGroupedAdapter mAdapter;
  private AccountObserver aObserver;
  Account mAccount;
  public boolean hasItems, mappedCategories, mappedPayees, mappedMethods;
  private Cursor mTransactionsCursor, mGroupingCursor;

  private ExpandableStickyListHeadersListView mListView;
  private LoaderManager mManager;
  private SparseBooleanArray mappedCategoriesPerGroup;
  /**
   * used to restore list selection when drawer is reopened
   */
  private SparseBooleanArray mCheckedListItems;

  private int columnIndexYear,                 columnIndexYearOfWeekStart, columnIndexMonth,
              columnIndexWeek,                 columnIndexDay,             columnIndexLabelSub,
              columnIndexPayee,                columnIndexCrStatus,        columnIndexGroupYear,
              columnIndexGroupMappedCategories,columnIndexGroupSumInterim, columnIndexGroupSumIncome,
              columnIndexGroupSumExpense,      columnIndexGroupSumTransfer,
              columnIndexLabelMain,            columnIndexGroupSecond;
  boolean indexesCalculated = false, indexesGroupingCalculated = false;
  //the following values are cached from the account object, so that we can react to changes in the observer
  private Grouping mGrouping;
  private Type mType;
  private String mCurrency;
  private Long mOpeningBalance;

  public static Fragment newInstance(long accountId) {
    TransactionList pageFragment = new TransactionList();
    Bundle bundle = new Bundle();
    bundle.putSerializable(KEY_ACCOUNTID, accountId);
    pageFragment.setArguments(bundle);
    return pageFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    mappedCategoriesPerGroup = new SparseBooleanArray();
    mAccount = Account.getInstanceFromDb(getArguments().getLong(KEY_ACCOUNTID));
    if (mAccount == null) {
      return;
    }
    mGrouping = mAccount.grouping;
    mType = mAccount.type;
    mCurrency = mAccount.currency.getCurrencyCode();
    mOpeningBalance = mAccount.openingBalance.getAmountMinor();
  }
  private void setAdapter() {
    Context ctx = getActivity();
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_LABEL_MAIN,KEY_DATE,KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category,R.id.date,R.id.amount};
    mAdapter = new MyGroupedAdapter(ctx, R.layout.expense_row, null, from, to,0);
    mListView.setAdapter(mAdapter);
  }
  private void setGrouping() {
    mAdapter.refreshDateFormat();
    restartGroupingLoader();
  }
  private void restartGroupingLoader() {
    mGroupingCursor = null;
    if (mManager == null) {
      //can happen after an orientation change in ExportDialogFragment, when resetting multiple accounts
      mManager = getLoaderManager();
    }
    if (mManager.getLoader(GROUPING_CURSOR) != null && !mManager.getLoader(GROUPING_CURSOR).isReset())
      mManager.restartLoader(GROUPING_CURSOR, null, this);
    else
      mManager.initLoader(GROUPING_CURSOR, null, this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (aObserver==null)
      return;
    try {
      ContentResolver cr = getActivity().getContentResolver();
      cr.unregisterContentObserver(aObserver);
    } catch (IllegalStateException ise) {
        // Do Nothing.  Observer has already been unregistered.
    }
  }

  @Override  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final MyExpenses ctx = (MyExpenses) getActivity();
    if (mAccount==null) {
      TextView tv = new TextView(ctx);
      tv.setText("Error loading transaction list for account "+getArguments().getLong(KEY_ACCOUNTID));
      return  tv;
    }
    mManager = getLoaderManager();
    //setGrouping();
    if (savedInstanceState != null) {
      mFilter =  new WhereFilter(savedInstanceState.getSparseParcelableArray(KEY_FILTER));
    } else {
      restoreFilterFromPreferences();
    }
    View v = inflater.inflate(R.layout.expenses_list, null, false);
    //TODO check if still needed with Appcompat
    //work around the problem that the view pager does not display its background correctly with Sherlock
    if (Build.VERSION.SDK_INT < 11) {
      v.setBackgroundColor(ctx.getResources().getColor(
         MyApplication.PrefKey.UI_THEME_KEY.getString("dark").equals("light")
          ? android.R.color.white : android.R.color.black));
    }
    mListView = (ExpandableStickyListHeadersListView) v.findViewById(R.id.list);
    setAdapter();
    mListView.setOnHeaderClickListener(this);
    mListView.setDrawingListUnderStickyHeader(false);
    mManager.initLoader(GROUPING_CURSOR, null, this);
    mManager.initLoader(TRANSACTION_CURSOR, null, this);
    mManager.initLoader(SUM_CURSOR, null, this);

    mListView.setEmptyView(v.findViewById(R.id.empty));
    mListView.setOnItemClickListener(new OnItemClickListener() {
       @Override
       public void onItemClick(AdapterView<?> a, View v,int position, long id) {
         FragmentManager fm = ctx.getSupportFragmentManager();
         DialogFragment f = (DialogFragment) fm.findFragmentByTag(TransactionDetailFragment.class.getName());
         if (f == null) {
           FragmentTransaction ft = getFragmentManager().beginTransaction();
           TransactionDetailFragment.newInstance(id).show(ft, TransactionDetailFragment.class.getName());
         }
       }
    });
    aObserver = new AccountObserver(new Handler());
    ContentResolver cr= getActivity().getContentResolver();
    //when account has changed, we might have
    //1) to refresh the list (currency has changed),
    //2) update current balance(opening balance has changed),
    //3) update the bottombarcolor (color has changed)
    //4) refetch grouping cursor (grouping has changed)
    cr.registerContentObserver(
        TransactionProvider.ACCOUNTS_URI,
        true,aObserver);

    registerForContextualActionBar(mListView.getWrappedList());
    return v;
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
      SparseBooleanArray positions,Long[]itemIds) {
    FragmentManager fm = getActivity().getSupportFragmentManager();
    switch(command) {
    case R.id.DELETE_COMMAND:
      boolean hasReconciled = false;
      if (mAccount.type != Type.CASH) {
        for (int i=0; i<positions.size(); i++) {
          mTransactionsCursor.moveToPosition(i);
          try {
            if (CrStatus.valueOf(mTransactionsCursor.getString(columnIndexCrStatus))==CrStatus.RECONCILED) {
              hasReconciled = true;
              break;
            }
          } catch (IllegalArgumentException ex) {
            continue;
          }
        }
      }
      String message = getResources().getQuantityString(R.plurals.warning_delete_transaction,itemIds.length,itemIds.length);
      if (hasReconciled) {
        message += " " + getString(R.string.warning_delete_reconciled);
      }
      MessageDialogFragment.newInstance(
          R.string.dialog_title_warning_delete_transaction,
          message,
          new MessageDialogFragment.Button(
              R.string.menu_delete,
              R.id.DELETE_COMMAND_DO,
              itemIds),
          null,
          new MessageDialogFragment.Button(android.R.string.no,R.id.CANCEL_CALLBACK_COMMAND,null))
        .show(fm,"DELETE_TRANSACTION");
      return true;
    case R.id.CLONE_TRANSACTION_COMMAND:
      ((ProtectedFragmentActivity) getActivity()).startTaskExecution(
          TaskExecutionFragment.TASK_CLONE,
          itemIds,
          null,
          0);
      break;
      //super is handling deactivation of mActionMode
    }
    return super.dispatchCommandMultiple(command, positions, itemIds);
  }
  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) info;
    MyExpenses ctx = (MyExpenses) getActivity();
    switch(command) {
    case R.id.EDIT_COMMAND:
      mTransactionsCursor.moveToPosition(acmi.position);
      if (DbUtils.getLongOrNull(mTransactionsCursor, "transfer_peer_parent") != null) {
        Toast.makeText(getActivity(), getString(R.string.warning_splitpartcategory_context), Toast.LENGTH_LONG).show();
      } else {
        Intent i = new Intent(ctx, ExpenseEdit.class);
        i.putExtra(KEY_ROWID, acmi.id);
        i.putExtra(DatabaseConstants.KEY_TRANSFER_ENABLED,ctx.transferEnabled());
        ctx.startActivityForResult(i, MyExpenses.EDIT_TRANSACTION_REQUEST);
      }
      //super is handling deactivation of mActionMode
      break;
    case R.id.CREATE_TEMPLATE_COMMAND:
      mTransactionsCursor.moveToPosition(acmi.position);
      String label = mTransactionsCursor.getString(columnIndexPayee);
      if (TextUtils.isEmpty(label))
        label = mTransactionsCursor.getString(columnIndexLabelSub);
      if (TextUtils.isEmpty(label))
        label = mTransactionsCursor.getString(columnIndexLabelMain);
      Bundle args = new Bundle();
      args.putLong(KEY_ROWID, acmi.id);
      args.putString(EditTextDialog.KEY_DIALOG_TITLE, getString(R.string.dialog_title_template_title));
      args.putString(EditTextDialog.KEY_VALUE,label);
      args.putInt(EditTextDialog.KEY_REQUEST_CODE, ProtectedFragmentActivity.TEMPLATE_TITLE_REQUEST);
      EditTextDialog.newInstance(args).show(ctx.getSupportFragmentManager(), "TEMPLATE_TITLE");
      return true;
    }
    return super.dispatchCommandSingle(command, info);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    CursorLoader cursorLoader = null;
    String selection;
    String[] selectionArgs;
    if (mAccount.getId() < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS	+	"=0)";
      selectionArgs = new String[] {mAccount.currency.getCurrencyCode()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[] { String.valueOf(mAccount.getId()) };
    }
    switch(id) {
    case TRANSACTION_CURSOR:
      if (!mFilter.isEmpty()) {
        selection += " AND " + mFilter.getSelectionForParents();
        selectionArgs = Utils.joinArrays(selectionArgs, mFilter.getSelectionArgs());
      }
      Uri uri = TransactionProvider.TRANSACTIONS_URI.buildUpon().appendQueryParameter("extended", "1").build();
      cursorLoader = new CursorLoader(getActivity(),
          uri, null, selection + " AND " + KEY_PARENTID + " is null",
          selectionArgs, null);
      break;
    //TODO: probably we can get rid of SUM_CURSOR, if we also aggregate unmapped transactions
    case SUM_CURSOR:
      cursorLoader = new CursorLoader(getActivity(),
          TransactionProvider.TRANSACTIONS_URI,
          new String[] {MAPPED_CATEGORIES,MAPPED_METHODS,MAPPED_PAYEES},
          selection,
          selectionArgs, null);
      break;
    case GROUPING_CURSOR:
      if (!mFilter.isEmpty()) {
        selection = mFilter.getSelectionForParts();
        selectionArgs = mFilter.getSelectionArgs();
      } else {
        selection = null;
        selectionArgs = null;
      }
      Builder builder = TransactionProvider.TRANSACTIONS_URI.buildUpon();
      builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
        .appendPath(mAccount.grouping.name());
      if (mAccount.getId() < 0) {
        builder.appendQueryParameter(KEY_CURRENCY, mAccount.currency.getCurrencyCode());
      } else {
        builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(mAccount.getId()));
      }
      cursorLoader = new CursorLoader(getActivity(),
          builder.build(),
          null,selection,selectionArgs, null);
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
        columnIndexYear = c.getColumnIndex(KEY_YEAR);
        columnIndexYearOfWeekStart = c.getColumnIndex(KEY_YEAR_OF_WEEK_START);
        columnIndexMonth = c.getColumnIndex(KEY_MONTH);
        columnIndexWeek = c.getColumnIndex(KEY_WEEK);
        columnIndexDay  = c.getColumnIndex(KEY_DAY);
        columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
        columnIndexLabelMain = c.getColumnIndex(KEY_LABEL_MAIN);
        columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
        columnIndexCrStatus = c.getColumnIndex(KEY_CR_STATUS);
        indexesCalculated = true;
      }
      ((SimpleCursorAdapter) mAdapter).swapCursor(c);
      invalidateCAB();
      break;
    case SUM_CURSOR:
      c.moveToFirst();
      mappedCategories = c.getInt(c.getColumnIndex(KEY_MAPPED_CATEGORIES)) > 0;
      mappedPayees = c.getInt(c.getColumnIndex(KEY_MAPPED_PAYEES)) > 0;
      mappedMethods = c.getInt(c.getColumnIndex(KEY_MAPPED_METHODS)) > 0;
      getActivity().supportInvalidateOptionsMenu();
      break;
    case GROUPING_CURSOR:
      mGroupingCursor = c;
      //if the transactionscursor has been loaded before the grouping cursor, we need to refresh
      //in order to have accurate grouping values
      if (!indexesGroupingCalculated) {
        columnIndexGroupYear = c.getColumnIndex(KEY_YEAR);
        columnIndexGroupSecond = c.getColumnIndex(KEY_SECOND_GROUP);
        columnIndexGroupSumIncome = c.getColumnIndex(KEY_SUM_INCOME);
        columnIndexGroupSumExpense = c.getColumnIndex(KEY_SUM_EXPENSES);
        columnIndexGroupSumTransfer = c.getColumnIndex(KEY_SUM_TRANSFERS);
        columnIndexGroupMappedCategories = c.getColumnIndex(KEY_MAPPED_CATEGORIES);
        columnIndexGroupSumInterim = c.getColumnIndex(KEY_INTERIM_BALANCE);
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
      mappedPayees = false;
      mappedMethods = false;
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
      if (getActivity()==null||getActivity().isFinishing()) {
        return;
      }
      //if grouping has changed
      if (mAccount.grouping != mGrouping) {
        mGrouping = mAccount.grouping;
        if (mAdapter != null) {
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
      if (!mAccount.openingBalance.getAmountMinor().equals(mOpeningBalance)) {
        restartGroupingLoader();
        mOpeningBalance = mAccount.openingBalance.getAmountMinor();
      }
    }
  }
  public class MyGroupedAdapter extends TransactionAdapter implements StickyListHeadersAdapter {
    LayoutInflater inflater;
    public MyGroupedAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(mAccount,context, layout, c, from, to, flags);
      inflater = LayoutInflater.from(getActivity());
    }
    @SuppressWarnings("incomplete-switch")
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      HeaderViewHolder holder;
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.header, parent, false);
        holder = new HeaderViewHolder();
        holder.text = (TextView) convertView.findViewById(R.id.text);
        holder.sumExpense = (TextView) convertView.findViewById(R.id.sum_expense);
        holder.sumIncome = (TextView) convertView.findViewById(R.id.sum_income);
        holder.sumTransfer = (TextView) convertView.findViewById(R.id.sum_transfer);
        holder.interimBalance = (TextView) convertView.findViewById(R.id.interim_balance);
        convertView.setTag(holder);
      } else {
        holder = (HeaderViewHolder) convertView.getTag();
      }

      Cursor c = getCursor();
      c.moveToPosition(position);
      int year = c.getInt(mAccount.grouping.equals(Grouping.WEEK)?columnIndexYearOfWeekStart:columnIndexYear);
      int second=-1;

      if (mGroupingCursor != null && mGroupingCursor.moveToFirst()) {
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
      Long sumExpense = DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumExpense);
      holder.sumExpense.setText("- " + Utils.convAmount(
          sumExpense,
          mAccount.currency));
      Long sumIncome = DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumIncome);
      holder.sumIncome.setText("+ " + Utils.convAmount(
          sumIncome,
          mAccount.currency));
      Long sumTransfer = DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumTransfer);
      holder.sumTransfer.setText("<-> " + Utils.convAmount(
          sumTransfer,
          mAccount.currency));
      Long delta = sumIncome - sumExpense +  sumTransfer;
      Long interimBalance = DbUtils.getLongOr0L(mGroupingCursor, columnIndexGroupSumInterim);
      Long previousBalance = interimBalance - delta;
      holder.interimBalance.setText(
          String.format("%s %s %s = %s",
              Utils.convAmount(previousBalance,mAccount.currency),
              Long.signum(delta) >-1 ? "+" : "-",
              Utils.convAmount(Math.abs(delta),mAccount.currency),
              Utils.convAmount(interimBalance,mAccount.currency)));
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
    if(mListView.isHeaderCollapsed(headerId)){
      mListView.expand(headerId);
    } else {
      mListView.collapse(headerId);
    }
  }

  @Override
  public boolean onHeaderLongClick(StickyListHeadersListView l, View header,
      int itemPosition, long headerId, boolean currentlySticky) {
    MyExpenses ctx = (MyExpenses) getActivity();
    if (mappedCategoriesPerGroup.get(itemPosition)) {
      if (MyApplication.getInstance().isContribEnabled()) {
        ctx.contribFeatureCalled(Feature.DISTRIBUTION, headerId);
      }
      else {
        CommonCommands.showContribDialog(ctx,Feature.DISTRIBUTION, headerId);
      }
    } else {
      Toast.makeText(ctx, getString(R.string.no_mapped_transactions), Toast.LENGTH_LONG).show();
    }
    return true;
  }

  @Override
  protected void configureMenuLegacy(Menu menu, ContextMenuInfo menuInfo) {
    super.configureMenuLegacy(menu, menuInfo);
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    configureMenuInternal(menu,info.position);
  }
  @Override
  protected void configureMenu11(Menu menu, int count) {
    super.configureMenu11(menu, count);
    if (count==1) {
      SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
      for (int i=0; i<checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i)) {
          configureMenuInternal(menu,checkedItemPositions.keyAt(i));
          break;
        }
      }
    }
  }
  private void configureMenuInternal(Menu menu, int position) {
    if (mTransactionsCursor != null) {
      //templates for splits is not yet implemented
      if (mTransactionsCursor.moveToPosition(position) &&
          SPLIT_CATID.equals(DbUtils.getLongOrNull(mTransactionsCursor, KEY_CATID)))
        menu.findItem(R.id.CREATE_TEMPLATE_COMMAND).setVisible(false);
    }
  }
  @SuppressLint("NewApi")
  public void onDrawerOpened() {
    if (mActionMode != null) {
      mCheckedListItems = mListView.getWrappedList().getCheckedItemPositions().clone();
      mActionMode.finish();
    }
  }
  public void onDrawerClosed() {
    if (mCheckedListItems!=null) {
      for (int i=0; i<mCheckedListItems.size(); i++) {
        if (mCheckedListItems.valueAt(i)) {
          mListView.getWrappedList().setItemChecked(mCheckedListItems.keyAt(i), true);
        }
      }
    }
    mCheckedListItems = null;
  }
  public void addFilterCriteria(Integer id, Criteria c) {
    mFilter.put(id, c);
    SharedPreferencesCompat.apply(
      MyApplication.getInstance().getSettings().edit().putString(
          KEY_FILTER + "_"+c.columnName+"_"+mAccount.getId(), c.toStringExtra()));
    mManager.restartLoader(TRANSACTION_CURSOR, null, this);
    mManager.restartLoader(GROUPING_CURSOR, null, this);
    getActivity().supportInvalidateOptionsMenu();
  }
  /**
   * Removes a given filter
   * @param column
   * @return true if the filter was set and succesfully removed, false otherwise
   */
  public boolean removeFilter(Integer id) {
    Criteria c = mFilter.get(id);
    boolean isFiltered = c != null;
    if (isFiltered) {
      SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings().edit().remove(
              KEY_FILTER + "_"+c.columnName+"_"+mAccount.getId()));
      mFilter.remove(id);
      mManager.restartLoader(TRANSACTION_CURSOR, null, this);
      mManager.restartLoader(GROUPING_CURSOR, null, this);
      getActivity().supportInvalidateOptionsMenu();
    }
    return isFiltered;
  }
  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (mAccount==null) {
      //seen in report 3331195c529454ca6b25a4c5d403beda
      return;
    }
    MenuItem searchMenu = menu.findItem(R.id.SEARCH_MENU);
    if (!mFilter.isEmpty()) {
      searchMenu.getIcon().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
      searchMenu.setChecked(true);
    } else {
      searchMenu.getIcon().setColorFilter(null);
      searchMenu.setChecked(false);
    }
    SubMenu filterMenu = searchMenu.getSubMenu();
    for (int i = 0; i < filterMenu.size(); i++) {
      MenuItem filterItem = filterMenu.getItem(i);
      boolean enabled = true;
      switch(filterItem.getItemId()) {
      case R.id.FILTER_CATEGORY_COMMAND:
        enabled = mappedCategories;
        break;
      case R.id.FILTER_STATUS_COMMAND:
        enabled = !mAccount.type.equals(Type.CASH);
        break;
      case R.id.FILTER_PAYEE_COMMAND:
        enabled = mappedPayees;
        break;
      case R.id.FILTER_METHOD_COMMAND:
        enabled = mappedMethods;
        break;
      }
      Criteria c = mFilter.get(filterItem.getItemId());
      Utils.menuItemSetEnabledAndVisible(filterItem, enabled || c!=null);
      if (c!=null) {
        filterItem.setChecked(true);
        filterItem.setTitle(c.prettyPrint());
      }
    }
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSparseParcelableArray(KEY_FILTER, mFilter.getCriteria());
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int command = item.getItemId();
    switch (command) {
    case R.id.FILTER_CATEGORY_COMMAND:
      if (!removeFilter(command)) {
        Intent i = new Intent(getActivity(), ManageCategories.class);
        i.setAction("myexpenses.intent.select_filter");
        startActivityForResult(i, ProtectedFragmentActivity.FILTER_CATEGORY_REQUEST);
      }
      return true;
    case R.id.FILTER_AMOUNT_COMMAND:
      if (!removeFilter(command)) {
        AmountFilterDialog.newInstance(mAccount.currency)
        .show(getActivity().getSupportFragmentManager(), "AMOUNT_FILTER");
      }
      return true;
    case R.id.FILTER_COMMENT_COMMAND:
      if (!removeFilter(command)) {
        Bundle args = new Bundle();
        args.putInt(EditTextDialog.KEY_REQUEST_CODE, ProtectedFragmentActivity.FILTER_COMMENT_REQUEST);
        args.putString(EditTextDialog.KEY_DIALOG_TITLE, getString(R.string.search_comment));
        EditTextDialog.newInstance(args).show(getActivity().getSupportFragmentManager(), "COMMENT_FILTER");
      }
      return true;
    case R.id.FILTER_STATUS_COMMAND:
      if (!removeFilter(command)) {
        SelectCrStatusDialogFragment.newInstance()
        .show(getActivity().getSupportFragmentManager(), "STATUS_FILTER");
      }
      return true;
    case R.id.FILTER_PAYEE_COMMAND:
      if (!removeFilter(command)) {
        SelectPayerDialogFragment.newInstance(mAccount.getId())
        .show(getActivity().getSupportFragmentManager(), "PAYER_FILTER");
      }
      return true;
    case R.id.FILTER_METHOD_COMMAND:
      if (!removeFilter(command)) {
        SelectMethodDialogFragment.newInstance(mAccount.getId())
        .show(getActivity().getSupportFragmentManager(), "METHOD_FILTER");
      }
      return true;
    case R.id.PRINT_COMMAND:
      MyExpenses ctx = (MyExpenses) getActivity();
      if (hasItems) {
        if (MyApplication.getInstance().isContribEnabled()) {
          ctx.contribFeatureCalled(Feature.PRINT, null);
        }
        else {
          CommonCommands.showContribDialog(ctx,Feature.PRINT, null);
        }
      } else {
        MessageDialogFragment.newInstance(
            0,
            R.string.dialog_command_disabled_reset_account,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(ctx.getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
  public SparseArray<Criteria> getFilterCriteria() {
    return mFilter.getCriteria();
  }
  private void restoreFilterFromPreferences() {
    SharedPreferences settings = MyApplication.getInstance().getSettings();
    String filter = settings.getString(KEY_FILTER + "_"+KEY_CATID+"_"+mAccount.getId(),null);
    if (filter!=null) {
      mFilter.put(R.id.FILTER_CATEGORY_COMMAND, SingleCategoryCriteria.fromStringExtra(filter));
    }
    filter = settings.getString(KEY_FILTER + "_"+KEY_AMOUNT+"_"+mAccount.getId(),null);
    if (filter!=null) {
      mFilter.put(R.id.FILTER_AMOUNT_COMMAND, AmountCriteria.fromStringExtra(filter));
    }
    filter = settings.getString(KEY_FILTER + "_"+KEY_COMMENT+"_"+mAccount.getId(),null);
    if (filter!=null) {
      mFilter.put(R.id.FILTER_COMMENT_COMMAND, CommentCriteria.fromStringExtra(filter));
    }
    filter = settings.getString(KEY_FILTER + "_"+KEY_CR_STATUS+"_"+mAccount.getId(),null);
    if (filter!=null) {
      mFilter.put(R.id.FILTER_STATUS_COMMAND, CrStatusCriteria.fromStringExtra(filter));
    }
    filter = settings.getString(KEY_FILTER + "_"+KEY_PAYEEID+"_"+mAccount.getId(),null);
    if (filter!=null) {
      mFilter.put(R.id.FILTER_PAYEE_COMMAND, PayeeCriteria.fromStringExtra(filter));
    }
    filter = settings.getString(KEY_FILTER + "_"+KEY_METHODID+"_"+mAccount.getId(),null);
    if (filter!=null) {
      mFilter.put(R.id.FILTER_METHOD_COMMAND, MethodCriteria.fromStringExtra(filter));
    }
  }
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == ProtectedFragmentActivity.FILTER_CATEGORY_REQUEST && resultCode == Activity.RESULT_OK) {
      long catId = intent.getLongExtra(KEY_CATID,0);
      String label = intent.getStringExtra(KEY_LABEL);
      addFilterCriteria(R.id.FILTER_CATEGORY_COMMAND,new SingleCategoryCriteria(catId, label));
    }
  }
}
