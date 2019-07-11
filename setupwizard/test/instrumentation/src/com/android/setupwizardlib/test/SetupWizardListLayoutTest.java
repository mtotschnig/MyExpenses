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

package com.android.setupwizardlib.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.SetupWizardListLayout;
import com.android.setupwizardlib.view.NavigationBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SetupWizardListLayoutTest {

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeMaterial_Light);
    }

    @Test
    public void testDefaultTemplate() {
        SetupWizardListLayout layout = new SetupWizardListLayout(mContext);
        assertListTemplateInflated(layout);
    }

    @Test
    public void testAddView() {
        SetupWizardListLayout layout = new SetupWizardListLayout(mContext);
        TextView tv = new TextView(mContext);
        try {
            layout.addView(tv);
            fail("Adding view to ListLayout should throw");
        } catch (UnsupportedOperationException e) {
            // Expected exception
        }
    }

    @Test
    public void testInflateFromXml() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        SetupWizardListLayout layout = (SetupWizardListLayout)
                inflater.inflate(R.layout.test_list_layout, null);
        assertListTemplateInflated(layout);
    }

    @Test
    public void testShowProgressBar() {
        final SetupWizardListLayout layout = new SetupWizardListLayout(mContext);
        layout.showProgressBar();
        assertTrue("Progress bar should be shown", layout.isProgressBarShown());
        final View progressBar = layout.findViewById(R.id.suw_layout_progress);
        assertTrue("Progress bar view should be shown",
                progressBar instanceof ProgressBar && progressBar.getVisibility() == View.VISIBLE);
    }

    @Test
    public void testDividerInsetLegacy() {
        SetupWizardListLayout layout = new SetupWizardListLayout(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            layout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        assertListTemplateInflated(layout);

        layout.setDividerInset(10);
        assertEquals("Divider inset should be 10", 10, layout.getDividerInset());

        final Drawable divider = layout.getDivider();
        assertTrue("Divider should be instance of InsetDrawable", divider instanceof InsetDrawable);
    }

    @Test
    public void testDividerInsets() {
        SetupWizardListLayout layout = new SetupWizardListLayout(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            layout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        assertListTemplateInflated(layout);

        layout.setDividerInsets(10, 15);
        assertEquals("Divider inset start should be 10", 10, layout.getDividerInsetStart());
        assertEquals("Divider inset end should be 15", 15, layout.getDividerInsetEnd());

        final Drawable divider = layout.getDivider();
        assertTrue("Divider should be instance of InsetDrawable", divider instanceof InsetDrawable);
    }

    private void assertListTemplateInflated(SetupWizardLayout layout) {
        View decorView = layout.findViewById(R.id.suw_layout_decor);
        View navbar = layout.findViewById(R.id.suw_layout_navigation_bar);
        View title = layout.findViewById(R.id.suw_layout_title);
        View list = layout.findViewById(android.R.id.list);
        assertNotNull("@id/suw_layout_decor_view should not be null", decorView);
        assertTrue("@id/suw_layout_navigation_bar should be an instance of NavigationBar",
                navbar instanceof NavigationBar);
        assertNotNull("@id/suw_layout_title should not be null", title);
        assertTrue("@android:id/list should be an instance of ListView", list instanceof ListView);
    }
}
