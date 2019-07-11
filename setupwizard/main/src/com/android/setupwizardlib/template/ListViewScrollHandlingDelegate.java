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

import android.util.Log;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.setupwizardlib.template.RequireScrollMixin.ScrollHandlingDelegate;

/**
 * {@link ScrollHandlingDelegate} which analyzes scroll events from {@link ListView} and
 * notifies {@link RequireScrollMixin} about scrollability changes.
 */
public class ListViewScrollHandlingDelegate implements ScrollHandlingDelegate,
        AbsListView.OnScrollListener {

    private static final String TAG = "ListViewDelegate";

    private static final int SCROLL_DURATION = 500;

    @NonNull
    private final RequireScrollMixin mRequireScrollMixin;

    @Nullable
    private final ListView mListView;

    public ListViewScrollHandlingDelegate(
            @NonNull RequireScrollMixin requireScrollMixin,
            @Nullable ListView listView) {
        mRequireScrollMixin = requireScrollMixin;
        mListView = listView;
    }

    @Override
    public void startListening() {
        if (mListView != null) {
            mListView.setOnScrollListener(this);

            final ListAdapter adapter = mListView.getAdapter();
            if (mListView.getLastVisiblePosition() < adapter.getCount()) {
                mRequireScrollMixin.notifyScrollabilityChange(true);
            }
        } else {
            Log.w(TAG, "Cannot require scroll. List view is null");
        }
    }

    @Override
    public void pageScrollDown() {
        if (mListView != null) {
            final int height = mListView.getHeight();
            mListView.smoothScrollBy(height, SCROLL_DURATION);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (firstVisibleItem + visibleItemCount >= totalItemCount) {
            mRequireScrollMixin.notifyScrollabilityChange(false);
        } else {
            mRequireScrollMixin.notifyScrollabilityChange(true);
        }
    }
}
