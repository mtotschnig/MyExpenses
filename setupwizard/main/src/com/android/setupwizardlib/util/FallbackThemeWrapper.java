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

package com.android.setupwizardlib.util;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.view.ContextThemeWrapper;

import androidx.annotation.StyleRes;

/**
 * Same as {@link ContextThemeWrapper}, but the base context's theme attributes take precedence
 * over the wrapper context's. This is used to provide default values for theme attributes
 * referenced in layouts, to remove the risk of crashing the client because of using the wrong
 * theme.
 */
public class FallbackThemeWrapper extends ContextThemeWrapper {

    /**
     * Creates a new context wrapper with the specified theme.
     *
     * The specified theme will be applied as fallbacks to the base context's theme. Any attributes
     * defined in the base context's theme will retain their original values. Otherwise values in
     * {@code themeResId} will be used.
     *
     * @param base The base context.
     * @param themeResId The theme to use as fallback.
     */
    public FallbackThemeWrapper(Context base, @StyleRes int themeResId) {
        super(base, themeResId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onApplyThemeResource(Theme theme, int resId, boolean first) {
        theme.applyStyle(resId, false /* force */);
    }
}
