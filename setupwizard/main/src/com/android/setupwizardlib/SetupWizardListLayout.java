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
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.setupwizardlib.template.ListMixin;
import com.android.setupwizardlib.template.ListViewScrollHandlingDelegate;
import com.android.setupwizardlib.template.RequireScrollMixin;

public class SetupWizardListLayout extends SetupWizardLayout {

    private static final String TAG = "SetupWizardListLayout";

    private ListMixin mListMixin;

    public SetupWizardListLayout(Context context) {
        this(context, 0, 0);
    }

    public SetupWizardListLayout(Context context, int template) {
        this(context, template, 0);
    }

    public SetupWizardListLayout(Context context, int template, int containerId) {
        super(context, template, containerId);
        init(context, null, 0);
    }

    public SetupWizardListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public SetupWizardListLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mListMixin = new ListMixin(this, attrs, defStyleAttr);
        registerMixin(ListMixin.class, mListMixin);

        final RequireScrollMixin requireScrollMixin = getMixin(RequireScrollMixin.class);
        requireScrollMixin.setScrollHandlingDelegate(
                new ListViewScrollHandlingDelegate(requireScrollMixin, getListView()));
    }

    @Override
    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        if (template == 0) {
            template = R.layout.suw_list_template;
        }
        return super.onInflateTemplate(inflater, template);
    }

    @Override
    protected ViewGroup findContainer(int containerId) {
        if (containerId == 0) {
            containerId = android.R.id.list;
        }
        return super.findContainer(containerId);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mListMixin.onLayout();
    }

    public ListView getListView() {
        return mListMixin.getListView();
    }

    public void setAdapter(ListAdapter adapter) {
        mListMixin.setAdapter(adapter);
    }

    public ListAdapter getAdapter() {
        return mListMixin.getAdapter();
    }

    /**
     * Sets the start inset of the divider. This will use the default divider drawable set in the
     * theme and inset it {@code inset} pixels to the right (or left in RTL layouts).
     *
     * @param inset The number of pixels to inset on the "start" side of the list divider. Typically
     *              this will be either {@code @dimen/suw_items_icon_divider_inset} or
     *              {@code @dimen/suw_items_text_divider_inset}.
     *
     * @see ListMixin#setDividerInset(int)
     * @deprecated Use {@link #setDividerInsets(int, int)} instead.
     */
    @Deprecated
    public void setDividerInset(int inset) {
        mListMixin.setDividerInset(inset);
    }

    /**
     * Sets the start inset of the divider. This will use the default divider drawable set in the
     * theme and apply insets to it.
     *
     * @param start The number of pixels to inset on the "start" side of the list divider. Typically
     *              this will be either {@code @dimen/suw_items_icon_divider_inset} or
     *              {@code @dimen/suw_items_text_divider_inset}.
     * @param end The number of pixels to inset on the "end" side of the list divider.
     *
     * @see ListMixin#setDividerInsets(int, int)
     */
    public void setDividerInsets(int start, int end) {
        mListMixin.setDividerInsets(start, end);
    }

    /**
     * @deprecated Use {@link #getDividerInsetStart()} instead.
     */
    @Deprecated
    public int getDividerInset() {
        return mListMixin.getDividerInset();
    }

    /**
     * @see ListMixin#getDividerInsetStart()
     */
    public int getDividerInsetStart() {
        return mListMixin.getDividerInsetStart();
    }

    /**
     * @see ListMixin#getDividerInsetEnd()
     */
    public int getDividerInsetEnd() {
        return mListMixin.getDividerInsetEnd();
    }

    /**
     * @see ListMixin#getDivider()
     */
    public Drawable getDivider() {
        return mListMixin.getDivider();
    }
}
