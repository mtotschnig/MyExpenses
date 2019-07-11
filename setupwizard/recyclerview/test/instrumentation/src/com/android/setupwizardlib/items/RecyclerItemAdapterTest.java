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

package com.android.setupwizardlib.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import com.android.setupwizardlib.items.RecyclerItemAdapter.PatchedLayerDrawable;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RecyclerItemAdapterTest {

    private Item[] mItems = new Item[5];
    private ItemGroup mItemGroup = new ItemGroup();

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < 5; i++) {
            Item item = new Item();
            item.setTitle("TestTitle" + i);
            item.setId(i);
            // Layout resource: 0 -> 1, 1 -> 11, 2 -> 21, 3 -> 1, 4 -> 11.
            // (Resource IDs cannot be 0)
            item.setLayoutResource((i % 3) * 10 + 1);
            mItems[i] = item;
            mItemGroup.addChild(item);
        }
    }

    @Test
    public void testAdapter() {
        RecyclerItemAdapter adapter = new RecyclerItemAdapter(mItemGroup);
        assertEquals("Adapter should have 5 items", 5, adapter.getItemCount());
        assertEquals("Adapter should return the first item", mItems[0], adapter.getItem(0));
        assertEquals("ID should be same as position", 2, adapter.getItemId(2));

        // ViewType is same as layout resource for RecyclerItemAdapter
        assertEquals("Second item should have view type 21", 21, adapter.getItemViewType(2));
    }

    @Test
    public void testGetRootItemHierarchy() {
        RecyclerItemAdapter adapter = new RecyclerItemAdapter(mItemGroup);
        ItemHierarchy root = adapter.getRootItemHierarchy();
        assertSame("Root item hierarchy should be mItemGroup", mItemGroup, root);
    }

    @Test
    public void testPatchedLayerDrawableNoPadding() {
        ShapeDrawable child = new ShapeDrawable(new RectShape());
        child.setPadding(0, 0, 0, 0);
        PatchedLayerDrawable drawable = new PatchedLayerDrawable(new Drawable[] { child });

        Rect padding = new Rect();
        assertFalse("Patched layer drawable should not have padding", drawable.getPadding(padding));
        assertEquals(new Rect(0, 0, 0, 0), padding);
    }

    @Test
    public void testPatchedLayerDrawableWithPadding() {
        ShapeDrawable child = new ShapeDrawable(new RectShape());
        child.setPadding(10, 10, 10, 10);
        PatchedLayerDrawable drawable = new PatchedLayerDrawable(new Drawable[] { child });

        Rect padding = new Rect();
        assertTrue("Patched layer drawable should have padding", drawable.getPadding(padding));
        assertEquals(new Rect(10, 10, 10, 10), padding);
    }

    @Test
    public void testAdapterNotifications() {
        RecyclerItemAdapter adapter = new RecyclerItemAdapter(mItemGroup);
        final AdapterDataObserver observer = mock(AdapterDataObserver.class);
        adapter.registerAdapterDataObserver(observer);

        mItems[0].setTitle("Child 1");
        verify(observer).onItemRangeChanged(eq(0), eq(1), anyObject());

        mItemGroup.removeChild(mItems[1]);
        verify(observer).onItemRangeRemoved(eq(1), eq(1));

        mItemGroup.addChild(mItems[1]);
        verify(observer).onItemRangeInserted(eq(4), eq(1));
    }

    @Test
    public void testCreateViewHolder() {
        RecyclerItemAdapter adapter = new RecyclerItemAdapter(mItemGroup);
        FrameLayout parent = new FrameLayout(InstrumentationRegistry.getContext());

        final ItemViewHolder viewHolder =
                adapter.onCreateViewHolder(parent, R.layout.test_list_item);
        assertNotNull("Background should be set", viewHolder.itemView.getBackground());
        assertEquals("foobar", viewHolder.itemView.getTag());
    }

    @Test
    public void testCreateViewHolderNoBackground() {
        RecyclerItemAdapter adapter = new RecyclerItemAdapter(mItemGroup);
        FrameLayout parent = new FrameLayout(InstrumentationRegistry.getContext());

        final ItemViewHolder viewHolder =
                adapter.onCreateViewHolder(parent, R.layout.test_list_item_no_background);
        assertNull("Background should be null", viewHolder.itemView.getBackground());
    }

    @Test
    public void testCreateViewHolderWithExistingBackground() {
        RecyclerItemAdapter adapter = new RecyclerItemAdapter(mItemGroup);
        FrameLayout parent = new FrameLayout(InstrumentationRegistry.getContext());

        final ItemViewHolder viewHolder =
                adapter.onCreateViewHolder(parent, R.layout.test_existing_background);
        Drawable background = viewHolder.itemView.getBackground();
        assertTrue(background instanceof PatchedLayerDrawable);

        PatchedLayerDrawable layerDrawable = (PatchedLayerDrawable) background;
        assertTrue(layerDrawable.getDrawable(0) instanceof GradientDrawable);
    }
}
