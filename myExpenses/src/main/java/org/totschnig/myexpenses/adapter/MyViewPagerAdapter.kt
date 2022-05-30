package org.totschnig.myexpenses.adapter

import android.content.Context
import android.database.Cursor
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.fragment.BaseTransactionList
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter

class MyViewPagerAdapter(
    private val myExpenses: MyExpenses,
    fm: FragmentManager?,
    cursor: Cursor?
) : CursorFragmentPagerAdapter(
    myExpenses, fm, cursor
) {
    fun getFragmentName(currentPosition: Int): String {
        return makeFragmentName(myExpenses.viewPager().id, getItemId(currentPosition))
    }

    override fun getItem(context: Context, cursor: Cursor): Fragment {
        return BaseTransactionList.newInstance(cursor.getLong(DatabaseConstants.KEY_ROWID))
    }
}