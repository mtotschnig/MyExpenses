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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ProgressBarMixinTest {

    private TemplateLayout mTemplateLayout;

    @Before
    public void setUp() {
        Context context = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeMaterial_Light);
        mTemplateLayout = new TemplateLayout(
                context,
                R.layout.test_progress_bar_template, R.id.suw_layout_content);
    }

    @Test
    public void testSetShown() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);
        mixin.setShown(true);

        ProgressBar progressBar = (ProgressBar) mTemplateLayout.findViewById(
                R.id.suw_layout_progress);
        assertNotNull("Progress bar should be available after setting to shown", progressBar);
        assertEquals(View.VISIBLE, progressBar.getVisibility());
    }

    @Test
    public void testNotShown() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);
        mixin.setShown(true);
        mixin.setShown(false);

        ProgressBar progressBar = (ProgressBar) mTemplateLayout.findViewById(
                R.id.suw_layout_progress);
        assertNotEquals(View.VISIBLE, progressBar.getVisibility());
    }

    @Test
    public void testIsShown() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);

        mixin.setShown(true);
        assertTrue(mixin.isShown());

        mixin.setShown(false);
        assertFalse(mixin.isShown());
    }

    @Test
    public void testPeekProgressBar() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);
        assertNull("PeekProgressBar should return null when stub not inflated yet",
                mixin.peekProgressBar());

        mixin.setShown(true);
        assertNotNull("PeekProgressBar should be available after setting to shown",
                mixin.peekProgressBar());
    }

    @Test
    public void testSetColorBeforeSetShown() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);
        mixin.setColor(ColorStateList.valueOf(Color.MAGENTA));

        mixin.setShown(true);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = (ProgressBar) mTemplateLayout.findViewById(
                    R.id.suw_layout_progress);
            assertEquals(ColorStateList.valueOf(Color.MAGENTA),
                    progressBar.getIndeterminateTintList());
            assertEquals(ColorStateList.valueOf(Color.MAGENTA),
                    progressBar.getProgressBackgroundTintList());
        }
        // this method is a no-op on versions < lollipop. Just check that it doesn't crash.
    }

    @Test
    public void testSetColorAfterSetShown() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);
        mixin.setShown(true);

        mixin.setColor(ColorStateList.valueOf(Color.YELLOW));

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = (ProgressBar) mTemplateLayout.findViewById(
                    R.id.suw_layout_progress);
            assertEquals(ColorStateList.valueOf(Color.YELLOW),
                    progressBar.getIndeterminateTintList());
            assertEquals(ColorStateList.valueOf(Color.YELLOW),
                    progressBar.getProgressBackgroundTintList());
        }
        // this method is a no-op on versions < lollipop. Just check that it doesn't crash.
    }

    @Test
    public void testDeterminateProgressBarNullTint() {
        ProgressBarMixin mixin = new ProgressBarMixin(mTemplateLayout);
        mixin.setShown(true);
        mixin.peekProgressBar().setIndeterminate(false);

        mixin.setColor(null);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = (ProgressBar) mTemplateLayout.findViewById(
                    R.id.suw_layout_progress);
            assertEquals(null, progressBar.getProgressBackgroundTintList());
            progressBar.draw(new Canvas());
        }
        // setColor is a no-op on versions < lollipop. Just check that it doesn't crash.
    }
}
