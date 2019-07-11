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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.items.ItemAdapter;

/**
 * @deprecated Use {@link SetupWizardListLayout} instead.
 */
@Deprecated
public class SetupWizardItemsLayout extends SetupWizardListLayout {

    public SetupWizardItemsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SetupWizardItemsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    @Nullable
    public ItemAdapter getAdapter() {
        final ListAdapter adapter = super.getAdapter();
        if (adapter instanceof ItemAdapter) {
            return (ItemAdapter) adapter;
        }
        return null;
    }
}
