package org.totschnig.myexpenses;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseIntArray;
import android.view.ViewGroup;

import java.util.HashMap;

public abstract class CursorFragmentPagerAdapter extends FragmentPagerAdapter {

    protected boolean mDataValid;
    protected Cursor mCursor;
    protected Context mContext;
    protected SparseIntArray mItemPositions;
    protected HashMap<Object, Integer> mObjectMap;
    protected int mRowIDColumn;

    public CursorFragmentPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
        super(fm);

        init(context, cursor);
    }

    void init(Context context, Cursor c) {
        mObjectMap = new HashMap<Object, Integer>();
        boolean cursorPresent = c != null;
        mCursor = c;
        mDataValid = cursorPresent;
        mContext = context;
        mRowIDColumn = cursorPresent ? c.getColumnIndexOrThrow("_id") : -1;
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
        mObjectMap.remove(object);

        super.destroyItem(container, position, object);
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
        mObjectMap.put(obj, Integer.valueOf(rowId));

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

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
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
