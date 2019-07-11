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

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/**
 * A {@link Mixin} for showing a progress bar.
 */
public class ProgressBarMixin implements Mixin {

    private TemplateLayout mTemplateLayout;

    @Nullable
    private ColorStateList mColor;

    /**
     * @param layout The layout this mixin belongs to.
     */
    public ProgressBarMixin(TemplateLayout layout) {
        mTemplateLayout = layout;
    }

    /**
     * @return True if the progress bar is currently shown.
     */
    public boolean isShown() {
        final View progressBar = mTemplateLayout.findManagedViewById(R.id.suw_layout_progress);
        return progressBar != null && progressBar.getVisibility() == View.VISIBLE;
    }

    /**
     * Sets whether the progress bar is shown. If the progress bar has not been inflated from the
     * stub, this method will inflate the progress bar.
     *
     * @param shown True to show the progress bar, false to hide it.
     */
    public void setShown(boolean shown) {
        if (shown) {
            View progressBar = getProgressBar();
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        } else {
            View progressBar = peekProgressBar();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Gets the progress bar in the layout. If the progress bar has not been used before, it will be
     * installed (i.e. inflated from its view stub).
     *
     * @return The progress bar of this layout. May be null only if the template used doesn't have a
     *         progress bar built-in.
     */
    private ProgressBar getProgressBar() {
        final View progressBar = peekProgressBar();
        if (progressBar == null) {
            final ViewStub progressBarStub =
                    (ViewStub) mTemplateLayout.findManagedViewById(R.id.suw_layout_progress_stub);
            if (progressBarStub != null) {
                progressBarStub.inflate();
            }
            setColor(mColor);
        }
        return peekProgressBar();
    }

    /**
     * Gets the progress bar in the layout only if it has been installed.
     * {@link #setShown(boolean)} should be called before this to ensure the progress bar
     * is set up correctly.
     *
     * @return The progress bar of this layout, or null if the progress bar is not installed. The
     *         null case can happen either if {@link #setShown(boolean)} with true was
     *         not called before this, or if the template does not contain a progress bar.
     */
    public ProgressBar peekProgressBar() {
        return (ProgressBar) mTemplateLayout.findManagedViewById(R.id.suw_layout_progress);
    }

    /**
     * Sets the color of the indeterminate progress bar. This method is a no-op on SDK < 21.
     */
    public void setColor(@Nullable ColorStateList color) {
        mColor = color;
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            final ProgressBar bar = peekProgressBar();
            if (bar != null) {
                bar.setIndeterminateTintList(color);
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M || color != null) {
                    // There is a bug in Lollipop where setting the progress tint color to null
                    // will crash with "java.lang.NullPointerException: Attempt to invoke virtual
                    // method 'int android.graphics.Paint.getAlpha()' on a null object reference"
                    // at android.graphics.drawable.NinePatchDrawable.draw(:250)
                    // The bug doesn't affect ProgressBar on M because it uses ShapeDrawable instead
                    // of NinePatchDrawable. (commit 6a8253fdc9f4574c28b4beeeed90580ffc93734a)
                    bar.setProgressBackgroundTintList(color);
                }
            }
        }
    }

    /**
     * @return The color previously set in {@link #setColor(ColorStateList)}, or null if the color
     * is not set. In case of null, the color of the progress bar will be inherited from the theme.
     */
    @Nullable
    public ColorStateList getColor() {
        return mColor;
    }
}
