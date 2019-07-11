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
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.android.setupwizardlib.SetupWizardPreferenceLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SetupWizardPreferenceLayoutTest {

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeMaterial_Light);
    }

    @Test
    public void testDefaultTemplate() {
        SetupWizardPreferenceLayout layout = new SetupWizardPreferenceLayout(mContext);
        assertPreferenceTemplateInflated(layout);
    }

    @Test
    public void testGetRecyclerView() {
        SetupWizardPreferenceLayout layout = new SetupWizardPreferenceLayout(mContext);
        assertPreferenceTemplateInflated(layout);
        assertNotNull("getRecyclerView should not be null", layout.getRecyclerView());
    }

    @Test
    public void testOnCreateRecyclerView() {
        SetupWizardPreferenceLayout layout = new SetupWizardPreferenceLayout(mContext);
        assertPreferenceTemplateInflated(layout);
        final RecyclerView recyclerView = layout.onCreateRecyclerView(LayoutInflater.from(mContext),
                layout, null /* savedInstanceState */);
        assertNotNull("RecyclerView created should not be null", recyclerView);
    }

    @Test
    public void testDividerInset() {
        SetupWizardPreferenceLayout layout = new SetupWizardPreferenceLayout(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            layout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        assertPreferenceTemplateInflated(layout);

        layout.addView(layout.onCreateRecyclerView(LayoutInflater.from(mContext), layout,
                null /* savedInstanceState */));

        layout.setDividerInset(10);
        assertEquals("Divider inset should be 10", 10, layout.getDividerInset());

        final Drawable divider = layout.getDivider();
        assertTrue("Divider should be instance of InsetDrawable", divider instanceof InsetDrawable);
    }

    private void assertPreferenceTemplateInflated(SetupWizardPreferenceLayout layout) {
        View contentContainer = layout.findViewById(R.id.suw_layout_content);
        assertTrue("@id/suw_layout_content should be a ViewGroup",
                contentContainer instanceof ViewGroup);

        assertNotNull("Header text view should not be null",
                layout.findManagedViewById(R.id.suw_layout_title));
        assertNotNull("Decoration view should not be null",
                layout.findManagedViewById(R.id.suw_layout_decor));
    }
}
