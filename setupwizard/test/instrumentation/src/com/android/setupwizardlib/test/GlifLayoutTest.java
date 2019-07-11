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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.setupwizardlib.GlifLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GlifLayoutTest {

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeGlif_Light);
    }

    @Test
    public void testInflateFromXml() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        GlifLayout layout = (GlifLayout) inflater.inflate(R.layout.test_glif_layout, null);
        assertDefaultTemplateInflated(layout);
        View content = layout.findViewById(R.id.test_content);
        assertTrue("@id/test_content should be a TextView", content instanceof TextView);
    }

    @Test
    public void testPrimaryColorFromXml() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        GlifLayout layout =
                (GlifLayout) inflater.inflate(R.layout.test_glif_layout_primary_color, null);
        assertDefaultTemplateInflated(layout);

        assertEquals(ColorStateList.valueOf(Color.RED), layout.getPrimaryColor());
    }

    @Test
    public void testSetProgressBarShownInvalid() {
        GlifLayout layout = new GlifLayout(mContext, R.layout.test_template);
        layout.setProgressBarShown(true);
        // This is a no-op because there is no progress bar stub
    }

    @Test
    public void testGlifTheme() {
        mContext = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeGlif_Light);
        final GlifLayout glifLayout = new GlifLayout(mContext);

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            // Scroll indicators are only available on versions >= M
            assertEquals(View.SCROLL_INDICATOR_BOTTOM,
                    glifLayout.getScrollView().getScrollIndicators());
        }
    }

    @Test
    public void testGlifV2Theme() {
        mContext = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeGlifV2_Light);
        final GlifLayout glifLayout = new GlifLayout(mContext);
        final TextView titleView = (TextView) glifLayout.findManagedViewById(R.id.suw_layout_title);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            assertEquals(View.TEXT_ALIGNMENT_GRAVITY, titleView.getTextAlignment());
        }
        assertEquals("Title text should be center aligned on GLIF v2 theme",
                Gravity.CENTER_HORIZONTAL, titleView.getGravity() & Gravity.CENTER_HORIZONTAL);

        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            // LinearLayout.getGravity is only available on versions >= N
            final View iconView = glifLayout.findManagedViewById(R.id.suw_layout_icon);
            final LinearLayout parent = (LinearLayout) iconView.getParent();
            assertEquals("Icon should be center aligned on GLIF v2 theme",
                    Gravity.CENTER_HORIZONTAL, parent.getGravity() & Gravity.CENTER_HORIZONTAL);
        }

        assertEquals("Status bar color should be white in GLIF v2 theme",
                "ffffffff",
                Integer.toHexString(glifLayout.getBackgroundBaseColor().getDefaultColor()));
        assertFalse("GLIF v2 theme shuold not have patterned background",
                glifLayout.isBackgroundPatterned());

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            // Scroll indicators are only available on versions >= M
            assertEquals(View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM,
                    glifLayout.getScrollView().getScrollIndicators());
        }
    }

    private void assertDefaultTemplateInflated(GlifLayout layout) {
        View title = layout.findViewById(R.id.suw_layout_title);
        assertNotNull("@id/suw_layout_title should not be null", title);

        View icon = layout.findViewById(R.id.suw_layout_icon);
        assertNotNull("@id/suw_layout_icon should not be null", icon);

        View scrollView = layout.findViewById(R.id.suw_scroll_view);
        assertTrue("@id/suw_scroll_view should be a ScrollView", scrollView instanceof ScrollView);
    }
}
