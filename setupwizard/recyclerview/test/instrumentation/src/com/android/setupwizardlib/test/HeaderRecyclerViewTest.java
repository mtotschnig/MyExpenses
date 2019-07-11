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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.android.setupwizardlib.view.HeaderRecyclerView.HeaderAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test for {@link com.android.setupwizardlib.view.HeaderRecyclerView}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class HeaderRecyclerViewTest {

    private TestAdapter mWrappedAdapter;
    private HeaderAdapter mHeaderAdapter;

    @Mock
    private RecyclerView.AdapterDataObserver mObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mWrappedAdapter = new TestAdapter();

        mHeaderAdapter = new HeaderAdapter(mWrappedAdapter);
        mHeaderAdapter.registerAdapterDataObserver(mObserver);
    }

    /**
     * Test that notifyDataSetChanged gets propagated by HeaderRecyclerView's adapter.
     */
    @Test
    public void testNotifyChanged() {
        mWrappedAdapter.notifyDataSetChanged();

        verify(mObserver).onChanged();
    }

    /**
     * Test that notifyItemChanged gets propagated by HeaderRecyclerView's adapter.
     */
    @Test
    public void testNotifyItemChangedNoHeader() {
        mWrappedAdapter.notifyItemChanged(12);

        verify(mObserver).onItemRangeChanged(eq(12), eq(1), eq(null));
    }

    /**
     * Test that notifyItemChanged gets propagated by HeaderRecyclerView's adapter and adds 1 to the
     * position for the extra header items.
     */
    @Test
    public void testNotifyItemChangedWithHeader() {
        mHeaderAdapter.setHeader(new View(InstrumentationRegistry.getTargetContext()));
        mWrappedAdapter.notifyItemChanged(12);

        verify(mObserver).onItemRangeChanged(eq(13), eq(1), eq(null));
    }

    /**
     * Test that notifyItemInserted gets propagated by HeaderRecyclerView's adapter.
     */
    @Test
    public void testNotifyItemInsertedNoHeader() {
        mWrappedAdapter.notifyItemInserted(12);

        verify(mObserver).onItemRangeInserted(eq(12), eq(1));
    }

    /**
     * Test that notifyItemInserted gets propagated by HeaderRecyclerView's adapter and adds 1 to
     * the position for the extra header item.
     */
    @Test
    public void testNotifyItemInsertedWithHeader() {
        mHeaderAdapter.setHeader(new View(InstrumentationRegistry.getTargetContext()));
        mWrappedAdapter.notifyItemInserted(12);

        verify(mObserver).onItemRangeInserted(eq(13), eq(1));
    }

    /**
     * Test that notifyItemRemoved gets propagated by HeaderRecyclerView's adapter.
     */
    @Test
    public void testNotifyItemRemovedNoHeader() {
        mWrappedAdapter.notifyItemRemoved(12);

        verify(mObserver).onItemRangeRemoved(eq(12), eq(1));
    }

    /**
     * Test that notifyItemRemoved gets propagated by HeaderRecyclerView's adapter and adds 1 to
     * the position for the extra header item.
     */
    @Test
    public void testNotifyItemRemovedWithHeader() {
        mHeaderAdapter.setHeader(new View(InstrumentationRegistry.getTargetContext()));
        mWrappedAdapter.notifyItemRemoved(12);

        verify(mObserver).onItemRangeRemoved(eq(13), eq(1));
    }

    /**
     * Test that notifyItemMoved gets propagated by HeaderRecyclerView's adapter.
     */
    @Test
    public void testNotifyItemMovedNoHeader() {
        mWrappedAdapter.notifyItemMoved(12, 18);

        verify(mObserver).onItemRangeMoved(eq(12), eq(18), eq(1));
    }

    /**
     * Test that notifyItemMoved gets propagated by HeaderRecyclerView's adapter and adds 1 to
     * the position for the extra header item.
     */
    @Test
    public void testNotifyItemMovedWithHeader() {
        mHeaderAdapter.setHeader(new View(InstrumentationRegistry.getTargetContext()));
        mWrappedAdapter.notifyItemMoved(12, 18);

        verify(mObserver).onItemRangeMoved(eq(13), eq(19), eq(1));
    }

    /**
     * Test adapter to be wrapped inside {@link HeaderAdapter} to to send item change notifications.
     */
    public static class TestAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }
}
