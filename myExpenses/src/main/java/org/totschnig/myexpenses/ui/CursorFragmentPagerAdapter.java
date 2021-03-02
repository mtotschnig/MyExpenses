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
//Credit: https://gist.github.com/peterkuterna/3144266

package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseIntArray;
import android.view.ViewGroup;

import java.util.HashMap;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public abstract class CursorFragmentPagerAdapter extends FragmentPagerAdapter {

    protected boolean mDataValid;
    protected Cursor mCursor;
    protected Context mContext;
    //TODO replace by storage class that handles longs instead of ints
    protected SparseIntArray mItemPositions;
    protected HashMap<Object, Integer> mObjectMap;
    protected int mRowIDColumn;

    public CursorFragmentPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
        super(fm);

        init(context, cursor);
    }

    void init(Context context, Cursor c) {
        mObjectMap = new HashMap<>();
        boolean cursorPresent = c != null;
        mCursor = c;
        mDataValid = cursorPresent;
        mContext = context;
        mRowIDColumn = cursorPresent ? c.getColumnIndexOrThrow("_id") : -1;
        setItemPositions();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemPosition(Object object) {
        Integer rowId = mObjectMap.get(object);
        if (rowId != null && mItemPositions != null) {
            return mItemPositions.get(rowId, POSITION_NONE);
        }
        return POSITION_NONE;
    }

    public void setItemPositions() {
        mItemPositions = null;

        if (mDataValid) {
            int count = mCursor.getCount();
            mItemPositions = new SparseIntArray(count);
            mCursor.moveToPosition(-1);
            while (mCursor.moveToNext()) {
                int rowId = mCursor.getInt(mRowIDColumn);
                int cursorPos = mCursor.getPosition();
                mItemPositions.append(rowId, cursorPos);
            }
        }
    }

    @Override
    public Fragment getItem(int position) {
        if (mDataValid) {
            mCursor.moveToPosition(position);
            return getItem(mContext, mCursor);
        } else {
            return null;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        boolean stillValid = getItemPosition(object) != POSITION_NONE;
        mObjectMap.remove(object);
        if (stillValid) {
          detachItem(container, position, object);
        } else {
          super.destroyItem(container, position, object);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        int rowId = mCursor.getInt(mRowIDColumn);
        Object obj = super.instantiateItem(container, position);
        mObjectMap.put(obj, rowId);

        return obj;
    }

    public abstract Fragment getItem(Context context, Cursor cursor);

    @Override
    public int getCount() {
        if (mDataValid) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        mCursor = newCursor;
        if (newCursor != null) {
            mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
        } else {
            mRowIDColumn = -1;
            mDataValid = false;
        }

        setItemPositions();
        if (newCursor != null)
          notifyDataSetChanged();

        return oldCursor;
    }

    @Override
    public long getItemId(int position) {
        if (!mDataValid || !mCursor.moveToPosition(position)) {
            return super.getItemId(position);
        }
        int rowId = mCursor.getInt(mRowIDColumn);
        return rowId;
    }

}
