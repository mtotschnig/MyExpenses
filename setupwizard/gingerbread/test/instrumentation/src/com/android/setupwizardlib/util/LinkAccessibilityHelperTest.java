/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.setupwizardlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.text.BidiFormatter;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.android.setupwizardlib.span.LinkSpan;
import com.android.setupwizardlib.util.LinkAccessibilityHelper.PreOLinkAccessibilityHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LinkAccessibilityHelperTest {

    private static final LinkSpan LINK_SPAN = new LinkSpan("foobar");

    private TextView mTextView;
    private TestPreOLinkAccessibilityHelper mHelper;

    private DisplayMetrics mDisplayMetrics;

    @Test
    public void testGetVirtualViewAt() {
        initTextView();
        final int virtualViewId = mHelper.getVirtualViewAt(dp2Px(15), dp2Px(10));
        assertEquals("Virtual view ID should be 1", 1, virtualViewId);
    }

    @Test
    public void testGetVirtualViewAtHost() {
        initTextView();
        final int virtualViewId = mHelper.getVirtualViewAt(dp2Px(100), dp2Px(100));
        assertEquals("Virtual view ID should be INVALID_ID",
                ExploreByTouchHelper.INVALID_ID, virtualViewId);
    }

    @Test
    public void testGetVisibleVirtualViews() {
        initTextView();
        List<Integer> virtualViewIds = new ArrayList<>();
        mHelper.getVisibleVirtualViews(virtualViewIds);

        assertEquals("VisibleVirtualViews should be [1]",
                Collections.singletonList(1), virtualViewIds);
    }

    @Test
    public void testOnPopulateEventForVirtualView() {
        initTextView();
        AccessibilityEvent event = AccessibilityEvent.obtain();
        mHelper.onPopulateEventForVirtualView(1, event);

        // LinkSpan is set on substring(1, 2) of "Hello world" --> "e"
        assertEquals("LinkSpan description should be \"e\"",
                "e", event.getContentDescription().toString());

        event.recycle();
    }

    @Test
    public void testOnPopulateEventForVirtualViewHost() {
        initTextView();
        AccessibilityEvent event = AccessibilityEvent.obtain();
        mHelper.onPopulateEventForVirtualView(ExploreByTouchHelper.INVALID_ID, event);

        assertEquals("Host view description should be \"Hello world\"", "Hello world",
                event.getContentDescription().toString());

        event.recycle();
    }

    @Test
    public void testOnPopulateNodeForVirtualView() {
        initTextView();
        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mHelper.onPopulateNodeForVirtualView(1, info);

        assertEquals("LinkSpan description should be \"e\"",
                "e", info.getContentDescription().toString());
        assertTrue("LinkSpan should be focusable", info.isFocusable());
        assertTrue("LinkSpan should be clickable", info.isClickable());
        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertEquals("LinkSpan bounds should be (10.5dp, 0dp, 18.5dp, 20.5dp)",
                new Rect(dp2Px(10.5f), dp2Px(0f), dp2Px(18.5f), dp2Px(20.5f)), bounds);

        info.recycle();
    }

    @Test
    public void testNullLayout() {
        initTextView();
        // Setting the padding will cause the layout to be null-ed out.
        mTextView.setPadding(1, 1, 1, 1);

        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mHelper.onPopulateNodeForVirtualView(0, info);

        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertEquals("LinkSpan bounds should be (0, 0, 1, 1)",
                new Rect(0, 0, 1, 1), bounds);

        info.recycle();
    }

    @Test
    public void testRtlLayout() {
        SpannableStringBuilder ssb = new SpannableStringBuilder("מכונה בתרגום");
        ssb.setSpan(LINK_SPAN, 1, 2, 0 /* flags */);
        initTextView(ssb);

        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mHelper.onPopulateNodeForVirtualView(1, info);

        assertEquals("LinkSpan description should be \"כ\"",
                "כ", info.getContentDescription().toString());
        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertEquals("LinkSpan bounds should be (481.5dp, 0dp, 489.5dp, 20.5dp)",
                new Rect(dp2Px(481.5f), dp2Px(0f), dp2Px(489.5f), dp2Px(20.5f)), bounds);

        info.recycle();
    }

    @Test
    public void testMultilineLink() {
        SpannableStringBuilder ssb = new SpannableStringBuilder(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                + "Praesent accumsan efficitur eros eu porttitor.");
        ssb.setSpan(LINK_SPAN, 51, 74, 0 /* flags */);
        initTextView(ssb);

        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mHelper.onPopulateNodeForVirtualView(51, info);

        assertEquals("LinkSpan description should match the span",
                "elit. Praesent accumsan", info.getContentDescription().toString());
        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertEquals("LinkSpan bounds should match first line of the span",
                new Rect(dp2Px(343f), dp2Px(0f), dp2Px(500f), dp2Px(19.5f)), bounds);

        info.recycle();
    }

    @Test
    public void testRtlMultilineLink() {
        String iwLoremIpsum = "אחר על רביעי אקטואליה. לוח דת אחרות המקובל רומנית, מיזמים מועמדים "
                + "האנציקלופדיה בה צ'ט. מתן מה שנורו לערוך ייִדיש, בקר או החול אנתרופולוגיה, עוד "
                + "דפים המחשב מיזמים ב.";
        SpannableStringBuilder ssb = new SpannableStringBuilder(iwLoremIpsum);
        ssb.setSpan(LINK_SPAN, 50, 100, 0 /* flags */);
        initTextView(ssb);

        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mHelper.onPopulateNodeForVirtualView(50, info);

        assertEquals("LinkSpan description should match the span",
                iwLoremIpsum.substring(50, 100),
                info.getContentDescription().toString());
        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertEquals("LinkSpan bounds should match the first line of the span",
                new Rect(dp2Px(0f), dp2Px(0f), dp2Px(150f), dp2Px(19.5f)), bounds);

        info.recycle();
    }

    @Test
    public void testBidiMultilineLink() {
        String iwLoremIpsum = "אחר על רביעי אקטואליה. לוח דת אחרות המקובל רומנית, מיזמים מועמדים "
                + "האנציקלופדיה בה צ'ט. מתן מה שנורו לערוך ייִדיש, בקר או החול אנתרופולוגיה, עוד "
                + "דפים המחשב מיזמים ב.";
        BidiFormatter formatter = BidiFormatter.getInstance(false /* rtlContext */);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append("hello ").append(formatter.unicodeWrap(iwLoremIpsum)).append(" world");
        ssb.setSpan(LINK_SPAN,
                "hello ".length() + 2, // Add two for the characters added by BidiFormatter
                "hello ".length() + 2 + iwLoremIpsum.length(),
                0 /* flags */);
        initTextView(ssb);

        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mHelper.onPopulateNodeForVirtualView("hello ".length() + 2, info);

        assertEquals("LinkSpan description should match the span",
                iwLoremIpsum,
                info.getContentDescription().toString());
        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertEquals("LinkSpan bounds should match the first line of the span",
                new Rect(dp2Px(491.5f), dp2Px(0f), dp2Px(500f), dp2Px(19.5f)), bounds);

        info.recycle();
    }

    @Test
    public void testMethodDelegation() {
        initTextView();
        ExploreByTouchHelper delegate = mock(TestPreOLinkAccessibilityHelper.class);
        LinkAccessibilityHelper helper = new LinkAccessibilityHelper(delegate);

        AccessibilityEvent accessibilityEvent =
                AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED);

        helper.sendAccessibilityEvent(mTextView, AccessibilityEvent.TYPE_VIEW_CLICKED);
        verify(delegate).sendAccessibilityEvent(
                same(mTextView),
                eq(AccessibilityEvent.TYPE_VIEW_CLICKED));

        helper.sendAccessibilityEventUnchecked(mTextView, accessibilityEvent);
        verify(delegate).sendAccessibilityEventUnchecked(same(mTextView), same(accessibilityEvent));

        helper.performAccessibilityAction(
                mTextView,
                AccessibilityActionCompat.ACTION_CLICK.getId(),
                Bundle.EMPTY);
        verify(delegate).performAccessibilityAction(
                same(mTextView),
                eq(AccessibilityActionCompat.ACTION_CLICK.getId()),
                eq(Bundle.EMPTY));

        helper.dispatchPopulateAccessibilityEvent(
                mTextView,
                accessibilityEvent);
        verify(delegate).dispatchPopulateAccessibilityEvent(
                same(mTextView),
                same(accessibilityEvent));

        MotionEvent motionEvent = MotionEvent.obtain(0, 0, 0, 0, 0, 0);
        helper.dispatchHoverEvent(motionEvent);
        verify(delegate).dispatchHoverEvent(eq(motionEvent));

        helper.getAccessibilityNodeProvider(mTextView);
        verify(delegate).getAccessibilityNodeProvider(same(mTextView));

        helper.onInitializeAccessibilityEvent(mTextView, accessibilityEvent);
        verify(delegate).onInitializeAccessibilityEvent(
                same(mTextView),
                eq(accessibilityEvent));

        AccessibilityNodeInfoCompat accessibilityNodeInfo = AccessibilityNodeInfoCompat.obtain();
        helper.onInitializeAccessibilityNodeInfo(mTextView, accessibilityNodeInfo);
        verify(delegate).onInitializeAccessibilityNodeInfo(
                same(mTextView),
                same(accessibilityNodeInfo));

        helper.onPopulateAccessibilityEvent(mTextView, accessibilityEvent);
        verify(delegate).onPopulateAccessibilityEvent(
                same(mTextView),
                same(accessibilityEvent));

        FrameLayout parent = new FrameLayout(InstrumentationRegistry.getTargetContext());
        helper.onRequestSendAccessibilityEvent(parent, mTextView, accessibilityEvent);
        verify(delegate).onRequestSendAccessibilityEvent(
                same(parent),
                same(mTextView),
                same(accessibilityEvent));
    }

    private void initTextView() {
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(LINK_SPAN, 1, 2, 0 /* flags */);
        initTextView(ssb);
    }

    private void initTextView(CharSequence text) {
        mTextView = new TextView(InstrumentationRegistry.getContext());
        mTextView.setSingleLine(false);
        mTextView.setText(text);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        mHelper = new TestPreOLinkAccessibilityHelper(mTextView);

        int measureExactly500dp = View.MeasureSpec.makeMeasureSpec(dp2Px(500),
                View.MeasureSpec.EXACTLY);
        mTextView.measure(measureExactly500dp, measureExactly500dp);
        mTextView.layout(dp2Px(0), dp2Px(0), dp2Px(500), dp2Px(500));
    }

    private int dp2Px(float dp) {
        if (mDisplayMetrics == null) {
            mDisplayMetrics =
                    InstrumentationRegistry.getContext().getResources().getDisplayMetrics();
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDisplayMetrics);
    }

    public static class TestPreOLinkAccessibilityHelper extends PreOLinkAccessibilityHelper {

        TestPreOLinkAccessibilityHelper(TextView view) {
            super(view);
        }

        @Override
        public int getVirtualViewAt(float x, float y) {
            return super.getVirtualViewAt(x, y);
        }

        @Override
        public void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            super.getVisibleVirtualViews(virtualViewIds);
        }

        @Override
        public void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            super.onPopulateEventForVirtualView(virtualViewId, event);
        }

        @Override
        public void onPopulateNodeForVirtualView(int virtualViewId,
                AccessibilityNodeInfoCompat info) {
            super.onPopulateNodeForVirtualView(virtualViewId, info);
        }

        @Override
        public boolean onPerformActionForVirtualView(int virtualViewId, int action,
                Bundle arguments) {
            return super.onPerformActionForVirtualView(virtualViewId, action, arguments);
        }
    }
}
