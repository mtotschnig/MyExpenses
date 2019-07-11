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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ListMixinTest {

    private Context mContext;
    private TemplateLayout mTemplateLayout;

    private ListView mListView;

    @Mock
    private ListAdapter mAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = InstrumentationRegistry.getTargetContext();
        mTemplateLayout = spy(new TemplateLayout(mContext, R.layout.test_template,
                R.id.suw_layout_content));

        mListView = mock(ListView.class, delegatesTo(new ListView(mContext)));
        doReturn(1).when(mAdapter).getViewTypeCount();

        doReturn(mListView).when(mTemplateLayout)
                .findManagedViewById(eq(android.R.id.list));
        doReturn(true).when(mTemplateLayout).isLayoutDirectionResolved();
    }

    @Test
    public void testGetListView() {
        ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
        assertSame(mListView, mixin.getListView());
    }

    @Test
    public void testGetAdapter() {
        mListView.setAdapter(mAdapter);

        ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
        assertSame(mAdapter, mixin.getAdapter());
    }

    @Test
    public void testSetAdapter() {
        assertNull(mListView.getAdapter());

        ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
        mixin.setAdapter(mAdapter);

        assertSame(mAdapter, mListView.getAdapter());
    }

    @Test
    public void testDividerInsetLegacy() {
        ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
        mixin.setDividerInset(123);

        assertEquals(123, mixin.getDividerInset());

        final Drawable divider = mListView.getDivider();
        InsetDrawable insetDrawable = (InsetDrawable) divider;
        Rect rect = new Rect();
        insetDrawable.getPadding(rect);

        assertEquals(new Rect(123, 0, 0, 0), rect);
    }

    @Test
    public void testDividerInsets() {
        ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
        mixin.setDividerInsets(123, 456);

        assertEquals(123, mixin.getDividerInsetStart());
        assertEquals(456, mixin.getDividerInsetEnd());

        final Drawable divider = mListView.getDivider();
        InsetDrawable insetDrawable = (InsetDrawable) divider;
        Rect rect = new Rect();
        insetDrawable.getPadding(rect);

        assertEquals(new Rect(123, 0, 456, 0), rect);
    }

    @Test
    public void testDividerInsetLegacyRtl() {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            doReturn(View.LAYOUT_DIRECTION_RTL).when(mTemplateLayout).getLayoutDirection();

            ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
            mixin.setDividerInset(123);

            assertEquals(123, mixin.getDividerInset());

            final Drawable divider = mListView.getDivider();
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

            ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);
            mixin.setDividerInsets(123, 456);

            assertEquals(123, mixin.getDividerInsetStart());
            assertEquals(456, mixin.getDividerInsetEnd());

            final Drawable divider = mListView.getDivider();
            InsetDrawable insetDrawable = (InsetDrawable) divider;
            Rect rect = new Rect();
            insetDrawable.getPadding(rect);

            assertEquals(new Rect(456, 0, 123, 0), rect);
        }
        // else the test passes
    }

    @Test
    public void testNoList() {
        doReturn(null).when(mTemplateLayout).findManagedViewById(eq(android.R.id.list));

        ListMixin mixin = new ListMixin(mTemplateLayout, null, 0);

        mixin.setAdapter(mAdapter);
        mixin.setDividerInset(123);

        assertNull(mixin.getListView());
        assertNull(mixin.getAdapter());
        mixin.getDividerInset(); // Test that it doesn't crash. The return value is not significant.
        assertNull(mixin.getDivider());

        verifyNoMoreInteractions(mListView);
    }
}
