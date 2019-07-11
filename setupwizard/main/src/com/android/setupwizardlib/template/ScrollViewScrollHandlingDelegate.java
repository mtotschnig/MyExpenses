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
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.setupwizardlib.template.RequireScrollMixin.ScrollHandlingDelegate;
import com.android.setupwizardlib.view.BottomScrollView;
import com.android.setupwizardlib.view.BottomScrollView.BottomScrollListener;

/**
 * {@link ScrollHandlingDelegate} which analyzes scroll events from {@link BottomScrollView} and
 * notifies {@link RequireScrollMixin} about scrollability changes.
 */
public class ScrollViewScrollHandlingDelegate
        implements ScrollHandlingDelegate, BottomScrollListener {

    private static final String TAG = "ScrollViewDelegate";

    @NonNull
    private final RequireScrollMixin mRequireScrollMixin;

    @Nullable
    private final BottomScrollView mScrollView;

    public ScrollViewScrollHandlingDelegate(
            @NonNull RequireScrollMixin requireScrollMixin,
            @Nullable ScrollView scrollView) {
        mRequireScrollMixin = requireScrollMixin;
        if (scrollView instanceof BottomScrollView) {
            mScrollView = (BottomScrollView) scrollView;
        } else {
            Log.w(TAG, "Cannot set non-BottomScrollView. Found=" + scrollView);
            mScrollView = null;
        }
    }

    @Override
    public void onScrolledToBottom() {
        mRequireScrollMixin.notifyScrollabilityChange(false);
    }

    @Override
    public void onRequiresScroll() {
        mRequireScrollMixin.notifyScrollabilityChange(true);
    }

    @Override
    public void startListening() {
        if (mScrollView != null) {
            mScrollView.setBottomScrollListener(this);
        } else {
            Log.w(TAG, "Cannot require scroll. Scroll view is null.");
        }
    }

    @Override
    public void pageScrollDown() {
        if (mScrollView != null) {
            mScrollView.pageScroll(ScrollView.FOCUS_DOWN);
        }
    }
}
