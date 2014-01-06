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

import java.util.ArrayList;
import java.util.HashMap;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.TemplateDetailFragment;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.android.calendar.CalendarContractCompat.Events;
public class TemplatesList extends BudgetListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  public static final int TEMPLATES_CURSOR=1;
  public static final int PLANS_CURSOR=2;
  Cursor mTemplatesCursor;
  private HashMap<Long,String> mPlanTimeInfo;
  private StickyListHeadersAdapter mAdapter;
  //private SimpleCursorAdapter mAdapter;
  //private StickyListHeadersListView mListView;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  
  private int columnIndexPlanId, columnIndexAmount, columnIndexLabelSub, columnIndexComment,
    columnIndexPayee, columnIndexTitle, columnIndexColor,columnIndexTransferPeer,
    columnIndexCurrency;
  boolean indexesCalculated = false;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setColors();
    View v = inflater.inflate(R.layout.templates_list, null, false);
    StickyListHeadersListView lv = (StickyListHeadersListView) v.findViewById(R.id.list);
    lv.setDrawingListUnderStickyHeader(false);
    mManager = getLoaderManager();
    mManager.initLoader(TEMPLATES_CURSOR, null, this);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_TITLE,KEY_LABEL_MAIN,KEY_AMOUNT};
    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.title,R.id.category,R.id.amount};
    mAdapter = new MyGroupedAdapter(
        getActivity(), 
        R.layout.template_row,
        null,
        from,
        to,
        0);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    //requires using activity (ManageTemplates) to implement OnChildClickListener
    //lv.setOnChildClickListener((OnChildClickListener) getActivity());
    lv.setOnItemClickListener(new OnItemClickListener()
    {
         @Override
         public void onItemClick(AdapterView<?> a, View v,int position, long id)
         {
           TemplateDetailFragment.newInstance(id)
           .show(getActivity().getSupportFragmentManager(), "TEMPLATE_DETAIL");
         }
    });
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch(id) {
    case TEMPLATES_CURSOR:
      return new CursorLoader(getActivity(),
        TransactionProvider.TEMPLATES_URI,
        null,
        null,
        null,
        null);
    case PLANS_CURSOR:
      return new CursorLoader(getActivity(),
        Events.CONTENT_URI,
        new String[]{
          Events._ID,
          Events.DTSTART,
          Events.RRULE,
        },
        Events._ID + " IN (" + TextUtils.join(",",(ArrayList<Long>) bundle.getSerializable("plans"))  + ")",
        null,
        null);
    }
    return null;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    switch (loader.getId()) {
    case TEMPLATES_CURSOR:
      mTemplatesCursor = c;
      if (!indexesCalculated) {
        columnIndexPlanId = c.getColumnIndex(KEY_PLANID);
        columnIndexAmount = c.getColumnIndex(KEY_AMOUNT);
        columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
        columnIndexComment = c.getColumnIndex(KEY_COMMENT);
        columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
        columnIndexTitle = c.getColumnIndex(KEY_TITLE);
        columnIndexColor = c.getColumnIndex(KEY_COLOR);
        columnIndexTransferPeer = c.getColumnIndex(KEY_TRANSFER_PEER);
        columnIndexCurrency = c.getColumnIndex(KEY_CURRENCY);
        indexesCalculated = true;
      }
      mTemplatesCursor.moveToFirst();
      ArrayList<Long> plans = new ArrayList<Long>();
      long planId;
      Bundle bundle = new Bundle();
      while (mTemplatesCursor.isAfterLast() == false) {
        if ((planId = mTemplatesCursor.getLong(columnIndexPlanId)) != 0L) {
          plans.add(planId);
        }
        mTemplatesCursor.moveToNext();
      }
      if (plans.size()>0) {
        bundle.putSerializable("plans", plans);
        mManager.initLoader(PLANS_CURSOR, bundle, this);
      } else {
        ((SimpleCursorAdapter) mAdapter).swapCursor(mTemplatesCursor);
      }
      break;
    case PLANS_CURSOR:
      mPlanTimeInfo = new HashMap<Long, String>();
      c.moveToFirst();
      while (c.isAfterLast() == false) {
        mPlanTimeInfo.put(
            c.getLong(c.getColumnIndex(Events._ID)),
            Plan.prettyTimeInfo(
                getActivity(),
                c.getString(c.getColumnIndex(Events.RRULE)),
                c.getLong(c.getColumnIndex(Events.DTSTART))));
        c.moveToNext();
      }
      ((SimpleCursorAdapter) mAdapter).swapCursor(mTemplatesCursor);
      break;
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
      ((SimpleCursorAdapter) mAdapter).swapCursor(null);
  }
  public class MyAdapter extends SimpleCursorAdapter {
    String categorySeparator = " : ",
        commentSeparator = " / ";
    public MyAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      convertView=super.getView(position, convertView, parent);
      ImageView button = (ImageView) convertView.findViewById(R.id.handleTemplateOrPlan);
      button.setTag(position);
      Cursor c = getCursor();
      c.moveToPosition(position);
      TextView tv1 = (TextView)convertView.findViewById(R.id.amount);
      long amount = c.getLong(columnIndexAmount);
      if (amount < 0) {
        tv1.setTextColor(colorExpense);
        // Set the background color of the text.
      }
      else {
        tv1.setTextColor(colorIncome);
      }
      tv1.setText(Utils.convAmount(amount,Utils.getSaveInstance(c.getString(columnIndexCurrency))));
      Long planId = DbUtils.getLongOrNull(c, KEY_PLANID);
      if (planId != null) {
        String planInfo = mPlanTimeInfo.get(planId);
        if (planInfo == null) {
          planInfo = "Event deleted from Calendar";
          button.setVisibility(View.GONE);
        } else {
          button.setImageResource(android.R.drawable.ic_menu_my_calendar);
          button.setVisibility(View.VISIBLE);
        }
        ((TextView) convertView.findViewById(R.id.title)).setText(
            c.getString(columnIndexTitle)
            +" (" + planInfo + ")");
      } else {
        button.setImageResource(R.drawable.manage_plans_icon);
        button.setVisibility(View.VISIBLE);
      }
      int color = c.getInt(columnIndexColor);
      convertView.findViewById(R.id.colorAccount).setBackgroundColor(color);
      TextView tv2 = (TextView)convertView.findViewById(R.id.category);
      CharSequence catText = tv2.getText();
      if (c.getInt(columnIndexTransferPeer) > 0) {
        catText = ((amount < 0) ? "=> " : "<= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
        if (catId == null) {
          catText = getString(R.string.no_category_assigned);
        } else {
          String label_sub = c.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + categorySeparator + label_sub;
          }
        }
      }
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
      return convertView;
    }
  }
  public class MyGroupedAdapter extends MyAdapter implements StickyListHeadersAdapter {
    LayoutInflater inflater;
    public MyGroupedAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      inflater = LayoutInflater.from(getSherlockActivity());
    }
    @Override
    public long getHeaderId(int position) {
      Cursor c = getCursor();
      c.moveToPosition(position);
      return DbUtils.getLongOrNull(c, KEY_PLANID) == null ?
          0 : 1;
    }
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.template_plan_header, parent, false);
      }
      Cursor c = getCursor();
      c.moveToPosition(position);
      ((TextView) convertView.findViewById(R.id.text)).setText(
          DbUtils.getLongOrNull(c, KEY_PLANID) == null ?
              "Templates" : "Plans");
      return convertView;
    }
  }
  public void handleTemplateOrPlan(View v) {
    mTemplatesCursor.moveToPosition((Integer) v.getTag());
    if (DbUtils.getLongOrNull(mTemplatesCursor, KEY_PLANID) == null) {
    //TODO Strict mode
      if (Transaction.getInstanceFromTemplate(
              mTemplatesCursor.getLong(mTemplatesCursor.getColumnIndex(KEY_ROWID)))
            .save() == null)
        Toast.makeText(getActivity(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
      else
        Toast.makeText(getActivity(),getString(R.string.save_transaction_from_template_success), Toast.LENGTH_LONG).show();
      getActivity().finish();
    } else {
        Toast.makeText(getActivity(),"TODO: show instance list", Toast.LENGTH_LONG).show();
    }
  }
}
