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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.setupwizardlib.DividerItemDecoration;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.items.ItemHierarchy;
import com.android.setupwizardlib.items.ItemInflater;
import com.android.setupwizardlib.items.RecyclerItemAdapter;
import com.android.setupwizardlib.util.DrawableLayoutDirectionHelper;
import com.android.setupwizardlib.view.HeaderRecyclerView;
import com.android.setupwizardlib.view.HeaderRecyclerView.HeaderAdapter;

/**
 * A {@link Mixin} for interacting with templates with recycler views. This mixin constructor takes
 * the instance of the recycler view to allow it to be instantiated dynamically, as in the case for
 * preference fragments.
 *
 * <p>Unlike typical mixins, this mixin is designed to be created in onTemplateInflated, which is
 * called by the super constructor, and then parse the XML attributes later in the constructor.
 */
public class RecyclerMixin implements Mixin {

    private TemplateLayout mTemplateLayout;

    @NonNull
    private final RecyclerView mRecyclerView;

    @Nullable
    private View mHeader;

    @NonNull
    private DividerItemDecoration mDividerDecoration;

    private Drawable mDefaultDivider;
    private Drawable mDivider;

    private int mDividerInsetStart;
    private int mDividerInsetEnd;

    /**
     * Creates the RecyclerMixin. Unlike typical mixins which are created in the constructor, this
     * mixin should be called in {@link TemplateLayout#onTemplateInflated()}, which is called by
     * the super constructor, because the recycler view and the header needs to be made available
     * before other mixins from the super class.
     *
     * @param layout The layout this mixin belongs to.
     */
    public RecyclerMixin(@NonNull TemplateLayout layout, @NonNull RecyclerView recyclerView) {
        mTemplateLayout = layout;

        mDividerDecoration = new DividerItemDecoration(mTemplateLayout.getContext());

        // The recycler view needs to be available
        mRecyclerView = recyclerView;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mTemplateLayout.getContext()));

        if (recyclerView instanceof HeaderRecyclerView) {
            mHeader = ((HeaderRecyclerView) recyclerView).getHeader();
        }

        mRecyclerView.addItemDecoration(mDividerDecoration);
    }

    /**
     * Parse XML attributes and configures this mixin and the recycler view accordingly. This should
     * be called from the constructor of the layout.
     *
     * @param attrs The {@link AttributeSet} as passed into the constructor. Can be null if the
     *              layout was not created from XML.
     * @param defStyleAttr The default style attribute as passed into the layout constructor. Can be
     *                     0 if it is not needed.
     */
    public void parseAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        final Context context = mTemplateLayout.getContext();
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SuwRecyclerMixin, defStyleAttr, 0);

        final int entries = a.getResourceId(R.styleable.SuwRecyclerMixin_android_entries, 0);
        if (entries != 0) {
            final ItemHierarchy inflated = new ItemInflater(context).inflate(entries);
            final RecyclerItemAdapter adapter = new RecyclerItemAdapter(inflated);
            adapter.setHasStableIds(a.getBoolean(
                    R.styleable.SuwRecyclerMixin_suwHasStableIds, false));
            setAdapter(adapter);
        }
        int dividerInset =
                a.getDimensionPixelSize(R.styleable.SuwRecyclerMixin_suwDividerInset, -1);
        if (dividerInset != -1) {
            setDividerInset(dividerInset);
        } else {
            int dividerInsetStart =
                    a.getDimensionPixelSize(R.styleable.SuwRecyclerMixin_suwDividerInsetStart, 0);
            int dividerInsetEnd =
                    a.getDimensionPixelSize(R.styleable.SuwRecyclerMixin_suwDividerInsetEnd, 0);
            setDividerInsets(dividerInsetStart, dividerInsetEnd);
        }

        a.recycle();
    }

    /**
     * @return The recycler view contained in the layout, as marked by
     *         {@code @id/suw_recycler_view}. This will return {@code null} if the recycler view
     *         doesn't exist in the layout.
     */
    @SuppressWarnings("NullableProblems") // If clients guarantee that the template has a recycler
                                          // view, and call this after the template is inflated,
                                          // this will not return null.
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * Gets the header view of the recycler layout. This is useful for other mixins if they need to
     * access views within the header, usually via {@link TemplateLayout#findManagedViewById(int)}.
     */
    @SuppressWarnings("NullableProblems") // If clients guarantee that the template has a header,
                                          // this call will not return null.
    public View getHeader() {
        return mHeader;
    }

    /**
     * Recycler mixin needs to update the dividers if the layout direction has changed. This method
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
     * Gets the adapter of the recycler view in this layout. If the adapter includes a header,
     * this method will unwrap it and return the underlying adapter.
     *
     * @return The adapter, or {@code null} if the recycler view has no adapter.
     */
    public Adapter<? extends ViewHolder> getAdapter() {
        @SuppressWarnings("unchecked") // RecyclerView.getAdapter returns raw type :(
        final RecyclerView.Adapter<? extends ViewHolder> adapter = mRecyclerView.getAdapter();
        if (adapter instanceof HeaderAdapter) {
            return ((HeaderAdapter<? extends ViewHolder>) adapter).getWrappedAdapter();
        }
        return adapter;
    }

    /**
     * Sets the adapter on the recycler view in this layout.
     */
    public void setAdapter(Adapter<? extends ViewHolder> adapter) {
        mRecyclerView.setAdapter(adapter);
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
        boolean shouldUpdate = true;
        if (Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            shouldUpdate = mTemplateLayout.isLayoutDirectionResolved();
        }
        if (shouldUpdate) {
            if (mDefaultDivider == null) {
                mDefaultDivider = mDividerDecoration.getDivider();
            }
            mDivider = DrawableLayoutDirectionHelper.createRelativeInsetDrawable(
                    mDefaultDivider,
                    mDividerInsetStart /* start */,
                    0 /* top */,
                    mDividerInsetEnd /* end */,
                    0 /* bottom */,
                    mTemplateLayout);
            mDividerDecoration.setDivider(mDivider);
        }
    }

    /**
     * @return The drawable used as the divider.
     */
    public Drawable getDivider() {
        return mDivider;
    }

    /**
     * Sets the divider item decoration directly. This is a low level method which should be used
     * only if custom divider behavior is needed, for example if the divider should be shown /
     * hidden in some specific cases for view holders that cannot implement
     * {@link com.android.setupwizardlib.DividerItemDecoration.DividedViewHolder}.
     */
    public void setDividerItemDecoration(@NonNull DividerItemDecoration decoration) {
        mRecyclerView.removeItemDecoration(mDividerDecoration);
        mDividerDecoration = decoration;
        mRecyclerView.addItemDecoration(mDividerDecoration);
        updateDivider();
    }
}
