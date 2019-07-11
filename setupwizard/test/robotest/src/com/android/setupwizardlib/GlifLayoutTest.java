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

package com.android.setupwizardlib;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.template.ColoredHeaderMixin;
import com.android.setupwizardlib.template.HeaderMixin;
import com.android.setupwizardlib.template.IconMixin;
import com.android.setupwizardlib.template.ProgressBarMixin;
import com.android.setupwizardlib.view.StatusBarBackgroundLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
public class GlifLayoutTest {

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = new ContextThemeWrapper(application, R.style.SuwThemeGlif_Light);
    }

    @Test
    public void testDefaultTemplate() {
        GlifLayout layout = new GlifLayout(mContext);
        assertDefaultTemplateInflated(layout);
    }

    @Test
    public void testSetHeaderText() {
        GlifLayout layout = new GlifLayout(mContext);
        TextView title = (TextView) layout.findViewById(R.id.suw_layout_title);
        layout.setHeaderText("Abracadabra");
        assertEquals("Header text should be \"Abracadabra\"", "Abracadabra", title.getText());
    }

    @Test
    public void testAddView() {
        @IdRes int testViewId = 123456;
        GlifLayout layout = new GlifLayout(mContext);
        TextView tv = new TextView(mContext);
        tv.setId(testViewId);
        layout.addView(tv);
        assertDefaultTemplateInflated(layout);
        View view = layout.findViewById(testViewId);
        assertSame("The view added should be the same text view", tv, view);
    }

    @Test
    public void testGetScrollView() {
        GlifLayout layout = new GlifLayout(mContext);
        assertNotNull("Get scroll view should not be null with default template",
                layout.getScrollView());
    }

    @Test
    public void testSetPrimaryColor() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setProgressBarShown(true);
        layout.setPrimaryColor(ColorStateList.valueOf(Color.RED));
        assertEquals("Primary color should be red",
                ColorStateList.valueOf(Color.RED), layout.getPrimaryColor());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.suw_layout_progress);
            assertEquals("Progress bar should be tinted red",
                    ColorStateList.valueOf(Color.RED), progressBar.getIndeterminateTintList());
            assertEquals("Determinate progress bar should also be tinted red",
                    ColorStateList.valueOf(Color.RED), progressBar.getProgressBackgroundTintList());
        }
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void testSetPrimaryColorTablet() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setProgressBarShown(true);
        layout.setPrimaryColor(ColorStateList.valueOf(Color.RED));
        assertEquals("Primary color should be red",
                ColorStateList.valueOf(Color.RED), layout.getPrimaryColor());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.suw_layout_progress);
            assertEquals("Progress bar should be tinted red",
                    ColorStateList.valueOf(Color.RED), progressBar.getIndeterminateTintList());
            assertEquals("Determinate progress bar should also be tinted red",
                    ColorStateList.valueOf(Color.RED), progressBar.getProgressBackgroundTintList());
        }

        assertEquals(Color.RED, ((GlifPatternDrawable) getTabletBackground(layout)).getColor());
    }

    @Test
    public void testSetBackgroundBaseColor() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setPrimaryColor(ColorStateList.valueOf(Color.BLUE));
        layout.setBackgroundBaseColor(ColorStateList.valueOf(Color.RED));

        assertEquals(Color.RED, ((GlifPatternDrawable) getPhoneBackground(layout)).getColor());
        assertEquals(Color.RED, layout.getBackgroundBaseColor().getDefaultColor());
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void testSetBackgroundBaseColorTablet() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setPrimaryColor(ColorStateList.valueOf(Color.BLUE));
        layout.setBackgroundBaseColor(ColorStateList.valueOf(Color.RED));

        assertEquals(Color.RED, ((GlifPatternDrawable) getTabletBackground(layout)).getColor());
        assertEquals(Color.RED, layout.getBackgroundBaseColor().getDefaultColor());
    }

    @Test
    public void testSetBackgroundPatternedTrue() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setBackgroundPatterned(true);

        assertThat(getPhoneBackground(layout), instanceOf(GlifPatternDrawable.class));
        assertTrue("Background should be patterned", layout.isBackgroundPatterned());
    }

    @Test
    public void testSetBackgroundPatternedFalse() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setBackgroundPatterned(false);

        assertThat(getPhoneBackground(layout), instanceOf(ColorDrawable.class));
        assertFalse("Background should not be patterned", layout.isBackgroundPatterned());
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void testSetBackgroundPatternedTrueTablet() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setBackgroundPatterned(true);

        assertThat(getTabletBackground(layout), instanceOf(GlifPatternDrawable.class));
        assertTrue("Background should be patterned", layout.isBackgroundPatterned());
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void testSetBackgroundPatternedFalseTablet() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setBackgroundPatterned(false);

        assertThat(getTabletBackground(layout), instanceOf(ColorDrawable.class));
        assertFalse("Background should not be patterned", layout.isBackgroundPatterned());
    }

    @Test
    public void testNonGlifTheme() {
        mContext = new ContextThemeWrapper(application, android.R.style.Theme);
        new GlifLayout(mContext);
        // Inflating with a non-GLIF theme should not crash
    }

    @Test
    public void testPeekProgressBarNull() {
        GlifLayout layout = new GlifLayout(mContext);
        assertNull("PeekProgressBar should return null initially", layout.peekProgressBar());
    }

    @Test
    public void testPeekProgressBar() {
        GlifLayout layout = new GlifLayout(mContext);
        layout.setProgressBarShown(true);
        assertNotNull("Peek progress bar should return the bar after setProgressBarShown(true)",
                layout.peekProgressBar());
    }

    @Test
    public void testMixins() {
        GlifLayout layout = new GlifLayout(mContext);
        final HeaderMixin header = layout.getMixin(HeaderMixin.class);
        assertTrue("Header should be instance of ColoredHeaderMixin. "
                + "Found " + header.getClass() + " instead.", header instanceof ColoredHeaderMixin);

        assertNotNull("GlifLayout should have icon mixin", layout.getMixin(IconMixin.class));
        assertNotNull("GlifLayout should have progress bar mixin",
                layout.getMixin(ProgressBarMixin.class));
    }

    @Test
    public void testInflateFooter() {
        GlifLayout layout = new GlifLayout(mContext);

        final View view = layout.inflateFooter(android.R.layout.simple_list_item_1);
        assertEquals(android.R.id.text1, view.getId());
        assertNotNull(layout.findViewById(android.R.id.text1));
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void testInflateFooterTablet() {
        testInflateFooter();
    }

    @Test
    public void testInflateFooterBlankTemplate() {
        GlifLayout layout = new GlifLayout(mContext, R.layout.suw_glif_blank_template);

        final View view = layout.inflateFooter(android.R.layout.simple_list_item_1);
        assertEquals(android.R.id.text1, view.getId());
        assertNotNull(layout.findViewById(android.R.id.text1));
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void testInflateFooterBlankTemplateTablet() {
        testInflateFooterBlankTemplate();
    }

    @Test
    public void testFooterXml() {
        GlifLayout layout = new GlifLayout(
                mContext,
                Robolectric.buildAttributeSet()
                        .addAttribute(R.attr.suwFooter, "@android:layout/simple_list_item_1")
                        .build());

        assertNotNull(layout.findViewById(android.R.id.text1));
    }

    @Test
    public void inflateStickyHeader_shouldAddViewToLayout() {
        GlifLayout layout = new GlifLayout(mContext);

        final View view = layout.inflateStickyHeader(android.R.layout.simple_list_item_1);
        assertEquals(android.R.id.text1, view.getId());
        assertNotNull(layout.findViewById(android.R.id.text1));
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void inflateStickyHeader_whenOnTablets_shouldAddViewToLayout() {
        inflateStickyHeader_shouldAddViewToLayout();
    }

    @Test
    public void inflateStickyHeader_whenInXml_shouldAddViewToLayout() {
        GlifLayout layout = new GlifLayout(
                mContext,
                Robolectric.buildAttributeSet()
                        .addAttribute(R.attr.suwStickyHeader, "@android:layout/simple_list_item_1")
                        .build());

        assertNotNull(layout.findViewById(android.R.id.text1));
    }

    @Test
    public void inflateStickyHeader_whenOnBlankTemplate_shouldAddViewToLayout() {
        GlifLayout layout = new GlifLayout(mContext, R.layout.suw_glif_blank_template);

        final View view = layout.inflateStickyHeader(android.R.layout.simple_list_item_1);
        assertEquals(android.R.id.text1, view.getId());
        assertNotNull(layout.findViewById(android.R.id.text1));
    }

    @Config(qualifiers = "sw600dp")
    @Test
    public void inflateStickyHeader_whenOnBlankTemplateTablet_shouldAddViewToLayout() {
        inflateStickyHeader_whenOnBlankTemplate_shouldAddViewToLayout();
    }

    @Config(minSdk = Config.OLDEST_SDK, maxSdk = Config.NEWEST_SDK)
    @Test
    public void createFromXml_shouldSetLayoutFullscreen_whenLayoutFullscreenIsNotSet() {
        GlifLayout layout = new GlifLayout(
                mContext,
                Robolectric.buildAttributeSet()
                        .build());
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            assertEquals(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN,
                    layout.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Test
    public void createFromXml_shouldNotSetLayoutFullscreen_whenLayoutFullscreenIsFalse() {
        GlifLayout layout = new GlifLayout(
                mContext,
                Robolectric.buildAttributeSet()
                        .addAttribute(R.attr.suwLayoutFullscreen, "false")
                        .build());

        assertEquals(
                0,
                layout.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private Drawable getPhoneBackground(GlifLayout layout) {
        final StatusBarBackgroundLayout patternBg =
                (StatusBarBackgroundLayout) layout.findManagedViewById(R.id.suw_pattern_bg);
        return patternBg.getStatusBarBackground();
    }

    private Drawable getTabletBackground(GlifLayout layout) {
        final View patternBg = layout.findManagedViewById(R.id.suw_pattern_bg);
        return patternBg.getBackground();
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
