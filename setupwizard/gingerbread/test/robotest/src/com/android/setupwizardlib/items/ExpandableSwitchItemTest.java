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

package com.android.setupwizardlib.items;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.view.CheckableLinearLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
public class ExpandableSwitchItemTest {

    private TextView mSummaryView;
    private ExpandableSwitchItem mItem;

    @Before
    public void setUp() {
        mItem = new ExpandableSwitchItem();
        mItem.setTitle("TestTitle");
        mItem.setCollapsedSummary("TestSummary");
        mItem.setExpandedSummary("TestSummaryExpanded");
    }

    @Test
    public void testInitialState() {
        View view = createLayout();
        mItem.onBindView(view);

        assertEquals("Collapsed summary should be TestSummary",
                "TestSummary", mItem.getCollapsedSummary());
        assertEquals("Expanded summary should be TestSummaryExpanded",
                "TestSummaryExpanded", mItem.getExpandedSummary());

        assertEquals("Should be collapsed initially", "TestSummary", mItem.getSummary());
        assertEquals("Summary view should display collapsed summary",
                "TestSummary", mSummaryView.getText());

        assertFalse("Expandable switch item itself should not be focusable", view.isFocusable());

        View switchContent = view.findViewById(R.id.suw_items_expandable_switch_content);
        assertThat(switchContent).isInstanceOf(CheckableLinearLayout.class);
        assertThat(switchContent.isFocusable())
                .named("expandable content focusable")
                .isTrue();
    }

    @Test
    public void testExpanded() {
        View view = createLayout();
        mItem.onBindView(view);

        mItem.setExpanded(true);

        assertEquals("Collapsed summary should be TestSummary",
                "TestSummary", mItem.getCollapsedSummary());
        assertEquals("Expanded summary should be TestSummaryExpanded",
                "TestSummaryExpanded", mItem.getExpandedSummary());

        assertTrue("Should be expanded", mItem.isExpanded());
        assertEquals("getSummary should be expanded summary",
                "TestSummaryExpanded", mItem.getSummary());
    }

    @Test
    public void testCollapsed() {
        View view = createLayout();
        mItem.onBindView(view);

        mItem.setExpanded(true);
        assertTrue("Should be expanded", mItem.isExpanded());

        mItem.setExpanded(false);

        assertEquals("Collapsed summary should be TestSummary",
                "TestSummary", mItem.getCollapsedSummary());
        assertEquals("Expanded summary should be TestSummaryExpanded",
                "TestSummaryExpanded", mItem.getExpandedSummary());

        assertFalse("Should be expanded", mItem.isExpanded());
        assertEquals("getSummary should be collapsed summary",
                "TestSummary", mItem.getSummary());
    }

    @Test
    public void testClick() {
        View view = createLayout();
        mItem.onBindView(view);

        assertFalse("Should not be expanded initially", mItem.isExpanded());

        final View content = view.findViewById(R.id.suw_items_expandable_switch_content);
        content.performClick();
        assertTrue("Should be expanded after clicking", mItem.isExpanded());

        content.performClick();
        assertFalse("Should be collapsed again after clicking", mItem.isExpanded());
    }

    @Test
    public void testDrawableState() {
        final View view =
                LayoutInflater.from(application).inflate(mItem.getLayoutResource(), null);
        mItem.onBindView(view);

        final View titleView = view.findViewById(R.id.suw_items_title);
        assertThat(titleView.getDrawableState()).asList().named("Drawable state")
                .doesNotContain(android.R.attr.state_checked);

        mItem.setExpanded(true);
        mItem.onBindView(view);
        assertThat(titleView.getDrawableState()).asList().named("Drawable state")
                .contains(android.R.attr.state_checked);

        mItem.setExpanded(false);
        mItem.onBindView(view);
        assertThat(titleView.getDrawableState()).asList().named("Drawable state")
                .doesNotContain(android.R.attr.state_checked);
    }

    private ViewGroup createLayout() {
        ViewGroup root =
                (ViewGroup) LayoutInflater.from(application)
                        .inflate(R.layout.suw_items_expandable_switch, null);
        mSummaryView = root.findViewById(R.id.suw_items_summary);

        return root;
    }
}
