/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.template;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.items.ItemAdapter;
import com.android.setupwizardlib.items.ItemGroup;
import com.android.setupwizardlib.items.ItemInflater;
import com.android.setupwizardlib.util.DrawableLayoutDirectionHelper;

/**
 * A {@link Mixin} for interacting with ListViews.
 */
public class ListMixin implements Mixin {

    private TemplateLayout mTemplateLayout;

    @Nullable
    private ListView mListView;

    private Drawable mDivider;
    private Drawable mDefaultDivider;

    private int mDividerInsetStart;
    private int mDividerInsetEnd;

    /**
     * @param layout The layout this mixin belongs to.
     */
    public ListMixin(@NonNull TemplateLayout layout, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        mTemplateLayout = layout;

        final Context context = layout.getContext();
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SuwListMixin, defStyleAttr, 0);

        final int entries = a.getResourceId(R.styleable.SuwListMixin_android_entries, 0);
        if (entries != 0) {
            final ItemGroup inflated =
                    (ItemGroup) new ItemInflater(context).inflate(entries);
            setAdapter(new ItemAdapter(inflated));
        }
        int dividerInset =
                a.getDimensionPixelSize(R.styleable.SuwListMixin_suwDividerInset, -1);
        if (dividerInset != -1) {
            setDividerInset(dividerInset);
        } else {
            int dividerInsetStart =
                    a.getDimensionPixelSize(R.styleable.SuwListMixin_suwDividerInsetStart, 0);
            int dividerInsetEnd =
                    a.getDimensionPixelSize(R.styleable.SuwListMixin_suwDividerInsetEnd, 0);
            setDividerInsets(dividerInsetStart, dividerInsetEnd);
        }
        a.recycle();
    }

    /**
     * @return The list view contained in the layout, as marked by {@code @android:id/list}. This
     *         will return {@code null} if the list doesn't exist in the layout.
     */
    public ListView getListView() {
        return getListViewInternal();
    }

    // Client code can assume getListView() will not be null if they know their template contains
    // the list, but this mixin cannot. Any usages of getListView in this mixin needs null checks.
    @Nullable
    private ListView getListViewInternal() {
        if (mListView == null) {
            final View list = mTemplateLayout.findManagedViewById(android.R.id.list);
            if (list instanceof ListView) {
                mListView = (ListView) list;
            }
        }
        return mListView;
    }

    /**
     * List mixin needs to update the dividers if the layout direction has changed. This method
     * should be called when {@link View#onLayout(boolean, int, int, int, int)} of the template
     * is called.
     */
    public void onLayout() {
        if (mDivider == null) {
            // Update divider in case layout direction has just been resolved
            updateDivider();
        }
    }

    /**
     * Gets the adapter of the list view in this layout. If the adapter is a HeaderViewListAdapter,
     * this method will unwrap it and return the underlying adapter.
     *
     * @return The adapter, or {@code null} if there is no list, or if the list has no adapter.
     */
    public ListAdapter getAdapter() {
        final ListView listView = getListViewInternal();
        if (listView != null) {
            final ListAdapter adapter = listView.getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                return ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }
            return adapter;
        }
        return null;
    }

    /**
     * Sets the adapter on the list view in this layout.
     */
    public void setAdapter(ListAdapter adapter) {
        final ListView listView = getListViewInternal();
        if (listView != null) {
            listView.setAdapter(adapter);
        }
    }

    /**
     * @deprecated Use {@link #setDividerInsets(int, int)} instead.
     */
    @Deprecated
    public void setDividerInset(int inset) {
        setDividerInsets(inset, 0);
    }

    /**
     * Sets the start inset of the divider. This will use the default divider drawable set in the
     * theme and apply insets to it.
     *
     * @param start The number of pixels to inset on the "start" side of the list divider. Typically
     *              this will be either {@code @dimen/suw_items_glif_icon_divider_inset} or
     *              {@code @dimen/suw_items_glif_text_divider_inset}.
     * @param end The number of pixels to inset on the "end" side of the list divider.
     */
    public void setDividerInsets(int start, int end) {
        mDividerInsetStart = start;
        mDividerInsetEnd = end;
        updateDivider();
    }

    /**
     * @return The number of pixels inset on the start side of the divider.
     * @deprecated This is the same as {@link #getDividerInsetStart()}. Use that instead.
     */
    @Deprecated
    public int getDividerInset() {
        return getDividerInsetStart();
    }

    /**
     * @return The number of pixels inset on the start side of the divider.
     */
    public int getDividerInsetStart() {
        return mDividerInsetStart;
    }

    /**
     * @return The number of pixels inset on the end side of the divider.
     */
    public int getDividerInsetEnd() {
        return mDividerInsetEnd;
    }

    private void updateDivider() {
        final ListView listView = getListViewInternal();
        if (listView == null) {
            return;
        }
        boolean shouldUpdate = true;
        if (Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            shouldUpdate = mTemplateLayout.isLayoutDirectionResolved();
        }
        if (shouldUpdate) {
            if (mDefaultDivider == null) {
                mDefaultDivider = listView.getDivider();
            }
            mDivider = DrawableLayoutDirectionHelper.createRelativeInsetDrawable(
                    mDefaultDivider,
                    mDividerInsetStart /* start */,
                    0 /* top */,
                    mDividerInsetEnd /* end */,
                    0 /* bottom */,
                    mTemplateLayout);
            listView.setDivider(mDivider);
        }
    }

    /**
     * @return The drawable used as the divider.
     */
    public Drawable getDivider() {
        return mDivider;
    }
}
