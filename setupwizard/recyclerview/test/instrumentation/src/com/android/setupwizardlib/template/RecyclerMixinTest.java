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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RecyclerMixinTest {

    private Context mContext;
    private TemplateLayout mTemplateLayout;

    private RecyclerView mRecyclerView;

    @Mock
    private Adapter mAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = InstrumentationRegistry.getTargetContext();
        mTemplateLayout = spy(new TemplateLayout(mContext, R.layout.test_template,
                R.id.suw_layout_content));

        mRecyclerView = mock(RecyclerView.class, delegatesTo(new RecyclerView(mContext)));

        doReturn(true).when(mTemplateLayout).isLayoutDirectionResolved();
    }

    @Test
    public void testGetRecyclerView() {
        RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
        assertSame(mRecyclerView, mixin.getRecyclerView());
    }

    @Test
    public void testGetAdapter() {
        mRecyclerView.setAdapter(mAdapter);

        RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
        assertSame(mAdapter, mixin.getAdapter());
    }

    @Test
    public void testSetAdapter() {
        assertNull(mRecyclerView.getAdapter());

        RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
        mixin.setAdapter(mAdapter);

        assertSame(mAdapter, mRecyclerView.getAdapter());
    }

    @Test
    public void testDividerLegacyInset() {
        RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
        mixin.setDividerInset(123);

        assertEquals(123, mixin.getDividerInset());

        final Drawable divider = mixin.getDivider();
        InsetDrawable insetDrawable = (InsetDrawable) divider;
        Rect rect = new Rect();
        insetDrawable.getPadding(rect);

        assertEquals(new Rect(123, 0, 0, 0), rect);
    }

    @Test
    public void testDividerInsets() {
        RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
        mixin.setDividerInsets(123, 456);

        assertEquals(123, mixin.getDividerInsetStart());
        assertEquals(456, mixin.getDividerInsetEnd());

        final Drawable divider = mixin.getDivider();
        InsetDrawable insetDrawable = (InsetDrawable) divider;
        Rect rect = new Rect();
        insetDrawable.getPadding(rect);

        assertEquals(new Rect(123, 0, 456, 0), rect);
    }

    @Test
    public void testDividerInsetLegacyRtl() {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            doReturn(View.LAYOUT_DIRECTION_RTL).when(mTemplateLayout).getLayoutDirection();

            RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
            mixin.setDividerInset(123);

            assertEquals(123, mixin.getDividerInset());

            final Drawable divider = mixin.getDivider();
            InsetDrawable insetDrawable = (InsetDrawable) divider;
            Rect rect = new Rect();
            insetDrawable.getPadding(rect);

            assertEquals(new Rect(0, 0, 123, 0), rect);
        }
        // else the test passes
    }

    @Test
    public void testDividerInsetsRtl() {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            doReturn(View.LAYOUT_DIRECTION_RTL).when(mTemplateLayout).getLayoutDirection();

            RecyclerMixin mixin = new RecyclerMixin(mTemplateLayout, mRecyclerView);
            mixin.setDividerInsets(123, 456);

            assertEquals(123, mixin.getDividerInsetStart());
            assertEquals(456, mixin.getDividerInsetEnd());

            final Drawable divider = mixin.getDivider();
            InsetDrawable insetDrawable = (InsetDrawable) divider;
            Rect rect = new Rect();
            insetDrawable.getPadding(rect);

            assertEquals(new Rect(456, 0, 123, 0), rect);
        }
        // else the test passes
    }
}
