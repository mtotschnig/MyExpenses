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

package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

/**
 * A LinearLayout which is checkable. This will set the checked state when
 * {@link #onCreateDrawableState(int)} is called, and can be used with
 * {@code android:duplicateParentState} to propagate the drawable state to child views.
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {

    private boolean mChecked = false;

    public CheckableLinearLayout(Context context) {
        super(context);
    }

    public CheckableLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public CheckableLinearLayout(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public CheckableLinearLayout(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        setFocusable(true);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (mChecked) {
            final int[] superStates = super.onCreateDrawableState(extraSpace + 1);
            final int[] checked = new int[] { android.R.attr.state_checked };
            return mergeDrawableStates(superStates, checked);
        } else {
            return super.onCreateDrawableState(extraSpace);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }
}
