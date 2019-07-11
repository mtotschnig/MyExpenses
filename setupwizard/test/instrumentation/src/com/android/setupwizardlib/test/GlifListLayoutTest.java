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
import static org.junit.Assert.assertSame;
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
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.setupwizardlib.GlifListLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GlifListLayoutTest {

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = new ContextThemeWrapper(InstrumentationRegistry.getContext(),
                R.style.SuwThemeGlif_Light);
    }

    @Test
    public void testDefaultTemplate() {
        GlifListLayout layout = new GlifListLayout(mContext);
        assertListTemplateInflated(layout);
    }

    @Test
    public void testAddView() {
        GlifListLayout layout = new GlifListLayout(mContext);
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
        GlifListLayout layout = (GlifListLayout)
                inflater.inflate(R.layout.test_glif_list_layout, null);
        assertListTemplateInflated(layout);
    }

    @Test
    public void testGetListView() {
        GlifListLayout layout = new GlifListLayout(mContext);
        assertListTemplateInflated(layout);
        assertNotNull("getListView should not be null", layout.getListView());
    }

    @Test
    public void testAdapter() {
        GlifListLayout layout = new GlifListLayout(mContext);
        assertListTemplateInflated(layout);

        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1);
        adapter.add("Abracadabra");
        layout.setAdapter(adapter);

        final ListAdapter gotAdapter = layout.getAdapter();
        // Note: the wrapped adapter should be returned directly, not the HeaderViewListAdapter.
        assertSame("Adapter got from GlifListLayout should be same as set",
                adapter, gotAdapter);
    }

    @Test
    public void testDividerInsetLegacy() {
        GlifListLayout layout = new GlifListLayout(mContext);
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
        GlifListLayout layout = new GlifListLayout(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            layout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        assertListTemplateInflated(layout);

        layout.setDividerInsets(10, 15);
        assertEquals("Divider inset should be 10", 10, layout.getDividerInsetStart());
        assertEquals("Divider inset should be 15", 15, layout.getDividerInsetEnd());

        final Drawable divider = layout.getDivider();
        assertTrue("Divider should be instance of InsetDrawable", divider instanceof InsetDrawable);
    }

    private void assertListTemplateInflated(GlifListLayout layout) {
        View title = layout.findViewById(R.id.suw_layout_title);
        assertNotNull("@id/suw_layout_title should not be null", title);

        View icon = layout.findViewById(R.id.suw_layout_icon);
        assertNotNull("@id/suw_layout_icon should not be null", icon);

        View listView = layout.findViewById(android.R.id.list);
        assertTrue("@android:id/list should be a ListView", listView instanceof ListView);
    }
}
