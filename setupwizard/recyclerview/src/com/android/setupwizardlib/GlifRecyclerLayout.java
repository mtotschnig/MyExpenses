/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.setupwizardlib.template.RecyclerMixin;
import com.android.setupwizardlib.template.RecyclerViewScrollHandlingDelegate;
import com.android.setupwizardlib.template.RequireScrollMixin;

/**
 * A GLIF themed layout with a RecyclerView. {@code android:entries} can also be used to specify an
 * {@link com.android.setupwizardlib.items.ItemHierarchy} to be used with this layout in XML.
 */
public class GlifRecyclerLayout extends GlifLayout {

    protected RecyclerMixin mRecyclerMixin;

    public GlifRecyclerLayout(Context context) {
        this(context, 0, 0);
    }

    public GlifRecyclerLayout(Context context, int template) {
        this(context, template, 0);
    }

    public GlifRecyclerLayout(Context context, int template, int containerId) {
        super(context, template, containerId);
        init(context, null, 0);
    }

    public GlifRecyclerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public GlifRecyclerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mRecyclerMixin.parseAttributes(attrs, defStyleAttr);
        registerMixin(RecyclerMixin.class, mRecyclerMixin);

        final RequireScrollMixin requireScrollMixin = getMixin(RequireScrollMixin.class);
        requireScrollMixin.setScrollHandlingDelegate(
                new RecyclerViewScrollHandlingDelegate(requireScrollMixin, getRecyclerView()));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mRecyclerMixin.onLayout();
    }

    @Override
    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        if (template == 0) {
            template = R.layout.suw_glif_recycler_template;
        }
        return super.onInflateTemplate(inflater, template);
    }

    @Override
    protected void onTemplateInflated() {
        final View recyclerView = findViewById(R.id.suw_recycler_view);
        if (recyclerView instanceof RecyclerView) {
            mRecyclerMixin = new RecyclerMixin(this, (RecyclerView) recyclerView);
        } else {
            throw new IllegalStateException(
                    "GlifRecyclerLayout should use a template with recycler view");
        }
    }

    @Override
    protected ViewGroup findContainer(int containerId) {
        if (containerId == 0) {
            containerId = R.id.suw_recycler_view;
        }
        return super.findContainer(containerId);
    }

    @Override
    // Returning generic type is the common pattern used for findViewBy* methods
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T extends View> T findManagedViewById(int id) {
        final View header = mRecyclerMixin.getHeader();
        if (header != null) {
            final T view = header.findViewById(id);
            if (view != null) {
                return view;
            }
        }
        return super.findViewById(id);
    }

    /**
     * @see RecyclerMixin#setDividerItemDecoration(DividerItemDecoration)
     */
    public void setDividerItemDecoration(DividerItemDecoration decoration) {
        mRecyclerMixin.setDividerItemDecoration(decoration);
    }

    /**
     * @see RecyclerMixin#getRecyclerView()
     */
    public RecyclerView getRecyclerView() {
        return mRecyclerMixin.getRecyclerView();
    }

    /**
     * @see RecyclerMixin#setAdapter(Adapter)
     */
    public void setAdapter(Adapter<? extends ViewHolder> adapter) {
        mRecyclerMixin.setAdapter(adapter);
    }

    /**
     * @see RecyclerMixin#getAdapter()
     */
    public Adapter<? extends ViewHolder> getAdapter() {
        return mRecyclerMixin.getAdapter();
    }

    /**
     * @deprecated Use {@link #setDividerInsets(int, int)} instead.
     */
    @Deprecated
    public void setDividerInset(int inset) {
        mRecyclerMixin.setDividerInset(inset);
    }

    /**
     * @see RecyclerMixin#setDividerInset(int)
     */
    public void setDividerInsets(int start, int end) {
        mRecyclerMixin.setDividerInsets(start, end);
    }

    /**
     * @deprecated Use {@link #getDividerInsetStart()} instead.
     */
    @Deprecated
    public int getDividerInset() {
        return mRecyclerMixin.getDividerInset();
    }

    /**
     * @see RecyclerMixin#getDividerInsetStart()
     */
    public int getDividerInsetStart() {
        return mRecyclerMixin.getDividerInsetStart();
    }

    /**
     * @see RecyclerMixin#getDividerInsetEnd()
     */
    public int getDividerInsetEnd() {
        return mRecyclerMixin.getDividerInsetEnd();
    }

    /**
     * @see RecyclerMixin#getDivider()
     */
    public Drawable getDivider() {
        return mRecyclerMixin.getDivider();
    }
}
