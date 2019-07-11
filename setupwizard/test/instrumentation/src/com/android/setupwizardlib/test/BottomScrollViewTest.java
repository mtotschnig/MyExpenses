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
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import com.android.setupwizardlib.view.BottomScrollView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BottomScrollViewTest {

    private TestBottomScrollListener mListener;

    @Before
    public void setUp() throws Exception {
        mListener = new TestBottomScrollListener();
    }

    @Test
    public void testNoNeedScroll() {
        createScrollView(20);
        assertTrue("Scroll should not be required", mListener.mScrolledToBottom);
    }

    @Test
    public void testNeedScroll() {
        createScrollView(110);
        assertFalse("Scroll should be required", mListener.mScrolledToBottom);
    }

    @Test
    public void testScrollToBottom() {
        final BottomScrollView bottomScrollView = createScrollView(110);

        assertFalse("Scroll should be required", mListener.mScrolledToBottom);

        bottomScrollView.scrollTo(0, 10);
        assertTrue("Should already be scrolled to bottom", mListener.mScrolledToBottom);
    }

    @Test
    public void testScrollThreshold() {
        final BottomScrollView bottomScrollView = createScrollView(110);
        assertEquals("Scroll threshold should be 10", 10, bottomScrollView.getScrollThreshold());
    }

    private BottomScrollView createScrollView(final int childHeight) {
        final Context context = InstrumentationRegistry.getContext();
        final BottomScrollView bottomScrollView = new TestBottomScrollView(context);
        bottomScrollView.setBottomScrollListener(mListener);

        final View child = new TestChildView(context, childHeight);

        child.measure(0, 0); // TestChildView's measured dimensions doesn't depend on the arguments
        bottomScrollView.addView(child);
        bottomScrollView.layout(0, 0, 100, 100);

        return bottomScrollView;
    }

    private static class TestChildView extends View {

        private static final int WIDTH = 10;
        private int mHeight;

        TestChildView(Context context, int height) {
            super(context);
            mHeight = height;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(WIDTH, mHeight);
        }

        public void setHeight(int height) {
            mHeight = height;
        }
    }

    private static class TestBottomScrollView extends BottomScrollView {

        TestBottomScrollView(Context context) {
            super(context);
        }

        @Override
        public boolean post(Runnable action) {
            // Post all runnables synchronously so that tests can check the callbacks.
            action.run();
            return true;
        }
    }

    private static class TestBottomScrollListener implements BottomScrollView.BottomScrollListener {

        boolean mScrolledToBottom = true;

        @Override
        public void onScrolledToBottom() {
            mScrolledToBottom = true;
        }

        @Override
        public void onRequiresScroll() {
            mScrolledToBottom = false;
        }
    }
}
