package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.database.Cursor;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;

public class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    private final MyExpenses myExpenses;

    public MyViewPagerAdapter(MyExpenses myExpenses, FragmentManager fm, Cursor cursor) {
        super(myExpenses, fm, cursor);
        this.myExpenses = myExpenses;
    }

    public String getFragmentName(int currentPosition) {
        return FragmentPagerAdapter.makeFragmentName(myExpenses.viewPager().getId(), getItemId(currentPosition));
    }

    @Override
    public Fragment getItem(Context context, Cursor cursor) {
        return TransactionList.newInstance(cursor.getLong(myExpenses.getColumnIndexRowId()));
    }
}
