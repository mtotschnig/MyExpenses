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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.setupwizardlib.template.RequireScrollMixin.ScrollHandlingDelegate;

/**
 * {@link ScrollHandlingDelegate} which analyzes scroll events from {@link RecyclerView} and
 * notifies {@link RequireScrollMixin} about scrollability changes.
 */
public class RecyclerViewScrollHandlingDelegate implements ScrollHandlingDelegate {

    private static final String TAG = "RVRequireScrollMixin";

    @Nullable
    private final RecyclerView mRecyclerView;

    @NonNull
    private final RequireScrollMixin mRequireScrollMixin;

    public RecyclerViewScrollHandlingDelegate(
            @NonNull RequireScrollMixin requireScrollMixin,
            @Nullable RecyclerView recyclerView) {
        mRequireScrollMixin = requireScrollMixin;
        mRecyclerView = recyclerView;
    }

    private boolean canScrollDown() {
        if (mRecyclerView != null) {
            // Compatibility implementation of View#canScrollVertically
            final int offset = mRecyclerView.computeVerticalScrollOffset();
            final int range = mRecyclerView.computeVerticalScrollRange()
                    - mRecyclerView.computeVerticalScrollExtent();
            return range != 0 && offset < range - 1;
        }
        return false;
    }

    @Override
    public void startListening() {
        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    mRequireScrollMixin.notifyScrollabilityChange(canScrollDown());
                }
            });

            if (canScrollDown()) {
                mRequireScrollMixin.notifyScrollabilityChange(true);
            }
        } else {
            Log.w(TAG, "Cannot require scroll. Recycler view is null.");
        }
    }

    @Override
    public void pageScrollDown() {
        if (mRecyclerView != null) {
            final int height = mRecyclerView.getHeight();
            mRecyclerView.smoothScrollBy(0, height);
        }
    }
}
