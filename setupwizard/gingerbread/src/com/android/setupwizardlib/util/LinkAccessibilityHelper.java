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

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.util.List;

/**
 * An accessibility delegate that allows {@link android.text.style.ClickableSpan} to be focused and
 * clicked by accessibility services.
 *
 * <p><strong>Note:</strong> This class is a no-op on Android O or above since there is native
 * support for ClickableSpan accessibility.
 *
 * <p>Sample usage:
 * <pre>
 * LinkAccessibilityHelper mAccessibilityHelper;
 *
 * private void init() {
 *     mAccessibilityHelper = new LinkAccessibilityHelper(myTextView);
 *     ViewCompat.setAccessibilityDelegate(myTextView, mLinkHelper);
 * }
 *
 * {@literal @}Override
 * protected boolean dispatchHoverEvent({@literal @}NonNull MotionEvent event) {
 *     if (mAccessibilityHelper != null && mAccessibilityHelper.dispatchHoverEvent(event)) {
 *         return true;
 *     }
 *     return super.dispatchHoverEvent(event);
 * }
 * </pre>
 *
 * @see com.android.setupwizardlib.view.RichTextView
 * @see androidx.customview.widget.ExploreByTouchHelper
 */
public class LinkAccessibilityHelper extends AccessibilityDelegateCompat {

    private static final String TAG = "LinkAccessibilityHelper";

    private final AccessibilityDelegateCompat mDelegate;

    public LinkAccessibilityHelper(TextView view) {
        this(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                // Platform support was added in O. This helper will be no-op
                ? new AccessibilityDelegateCompat()
                // Pre-O, we extend ExploreByTouchHelper to expose a virtual view hierarchy
                : new PreOLinkAccessibilityHelper(view));
    }

    @VisibleForTesting
    LinkAccessibilityHelper(@NonNull AccessibilityDelegateCompat delegate) {
        mDelegate = delegate;
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
        mDelegate.sendAccessibilityEvent(host, eventType);
    }

    @Override
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
        mDelegate.sendAccessibilityEventUnchecked(host, event);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        return mDelegate.dispatchPopulateAccessibilityEvent(host, event);
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        mDelegate.onPopulateAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        mDelegate.onInitializeAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        mDelegate.onInitializeAccessibilityNodeInfo(host, info);
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
            AccessibilityEvent event) {
        return mDelegate.onRequestSendAccessibilityEvent(host, child, event);
    }

    @Override
    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
        return mDelegate.getAccessibilityNodeProvider(host);
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        return mDelegate.performAccessibilityAction(host, action, args);
    }

    /**
     * Dispatches hover event to the virtual view hierarchy. This method should be called in
     * {@link View#dispatchHoverEvent(MotionEvent)}.
     *
     * @see ExploreByTouchHelper#dispatchHoverEvent(MotionEvent)
     */
    public final boolean dispatchHoverEvent(MotionEvent event) {
        return mDelegate instanceof ExploreByTouchHelper
                && ((ExploreByTouchHelper) mDelegate).dispatchHoverEvent(event);
    }

    @VisibleForTesting
    static class PreOLinkAccessibilityHelper extends ExploreByTouchHelper {

        private final Rect mTempRect = new Rect();
        private final TextView mView;

        PreOLinkAccessibilityHelper(TextView view) {
            super(view);
            mView = view;
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            final CharSequence text = mView.getText();
            if (text instanceof Spanned) {
                final Spanned spannedText = (Spanned) text;
                final int offset = getOffsetForPosition(mView, x, y);
                ClickableSpan[] linkSpans =
                        spannedText.getSpans(offset, offset, ClickableSpan.class);
                if (linkSpans.length == 1) {
                    ClickableSpan linkSpan = linkSpans[0];
                    return spannedText.getSpanStart(linkSpan);
                }
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            final CharSequence text = mView.getText();
            if (text instanceof Spanned) {
                final Spanned spannedText = (Spanned) text;
                ClickableSpan[] linkSpans = spannedText.getSpans(0, spannedText.length(),
                        ClickableSpan.class);
                for (ClickableSpan span : linkSpans) {
                    virtualViewIds.add(spannedText.getSpanStart(span));
                }
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            final ClickableSpan span = getSpanForOffset(virtualViewId);
            if (span != null) {
                event.setContentDescription(getTextForSpan(span));
            } else {
                Log.e(TAG, "LinkSpan is null for offset: " + virtualViewId);
                event.setContentDescription(mView.getText());
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId,
                AccessibilityNodeInfoCompat info) {
            final ClickableSpan span = getSpanForOffset(virtualViewId);
            if (span != null) {
                info.setContentDescription(getTextForSpan(span));
            } else {
                Log.e(TAG, "LinkSpan is null for offset: " + virtualViewId);
                info.setContentDescription(mView.getText());
            }
            info.setFocusable(true);
            info.setClickable(true);
            getBoundsForSpan(span, mTempRect);
            if (mTempRect.isEmpty()) {
                Log.e(TAG, "LinkSpan bounds is empty for: " + virtualViewId);
                mTempRect.set(0, 0, 1, 1);
            }
            info.setBoundsInParent(mTempRect);
            info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }

        @Override
        protected boolean onPerformActionForVirtualView(
                int virtualViewId,
                int action,
                Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                ClickableSpan span = getSpanForOffset(virtualViewId);
                if (span != null) {
                    span.onClick(mView);
                    return true;
                } else {
                    Log.e(TAG, "LinkSpan is null for offset: " + virtualViewId);
                }
            }
            return false;
        }

        private ClickableSpan getSpanForOffset(int offset) {
            CharSequence text = mView.getText();
            if (text instanceof Spanned) {
                Spanned spannedText = (Spanned) text;
                ClickableSpan[] spans = spannedText.getSpans(offset, offset, ClickableSpan.class);
                if (spans.length == 1) {
                    return spans[0];
                }
            }
            return null;
        }

        private CharSequence getTextForSpan(ClickableSpan span) {
            CharSequence text = mView.getText();
            if (text instanceof Spanned) {
                Spanned spannedText = (Spanned) text;
                return spannedText.subSequence(
                        spannedText.getSpanStart(span),
                        spannedText.getSpanEnd(span));
            }
            return text;
        }

        // Find the bounds of a span. If it spans multiple lines, it will only return the bounds for
        // the section on the first line.
        private Rect getBoundsForSpan(ClickableSpan span, Rect outRect) {
            CharSequence text = mView.getText();
            outRect.setEmpty();
            if (text instanceof Spanned) {
                final Layout layout = mView.getLayout();
                if (layout != null) {
                    Spanned spannedText = (Spanned) text;
                    final int spanStart = spannedText.getSpanStart(span);
                    final int spanEnd = spannedText.getSpanEnd(span);
                    final float xStart = layout.getPrimaryHorizontal(spanStart);
                    final float xEnd = layout.getPrimaryHorizontal(spanEnd);
                    final int lineStart = layout.getLineForOffset(spanStart);
                    final int lineEnd = layout.getLineForOffset(spanEnd);
                    layout.getLineBounds(lineStart, outRect);
                    if (lineEnd == lineStart) {
                        // If the span is on a single line, adjust both the left and right bounds
                        // so outrect is exactly bounding the span.
                        outRect.left = (int) Math.min(xStart, xEnd);
                        outRect.right = (int) Math.max(xStart, xEnd);
                    } else {
                        // If the span wraps across multiple lines, only use the first line (as
                        // returned by layout.getLineBounds above), and adjust the "start" of
                        // outrect to where the span starts, leaving the "end" of outrect at the end
                        // of the line. ("start" being left for LTR, and right for RTL)
                        if (layout.getParagraphDirection(lineStart) == Layout.DIR_RIGHT_TO_LEFT) {
                            outRect.right = (int) xStart;
                        } else {
                            outRect.left = (int) xStart;
                        }
                    }

                    // Offset for padding
                    outRect.offset(mView.getTotalPaddingLeft(), mView.getTotalPaddingTop());
                }
            }
            return outRect;
        }

        // Compat implementation of TextView#getOffsetForPosition().

        private static int getOffsetForPosition(TextView view, float x, float y) {
            if (view.getLayout() == null) return -1;
            final int line = getLineAtCoordinate(view, y);
            return getOffsetAtCoordinate(view, line, x);
        }

        private static float convertToLocalHorizontalCoordinate(TextView view, float x) {
            x -= view.getTotalPaddingLeft();
            // Clamp the position to inside of the view.
            x = Math.max(0.0f, x);
            x = Math.min(view.getWidth() - view.getTotalPaddingRight() - 1, x);
            x += view.getScrollX();
            return x;
        }

        private static int getLineAtCoordinate(TextView view, float y) {
            y -= view.getTotalPaddingTop();
            // Clamp the position to inside of the view.
            y = Math.max(0.0f, y);
            y = Math.min(view.getHeight() - view.getTotalPaddingBottom() - 1, y);
            y += view.getScrollY();
            return view.getLayout().getLineForVertical((int) y);
        }

        private static int getOffsetAtCoordinate(TextView view, int line, float x) {
            x = convertToLocalHorizontalCoordinate(view, x);
            return view.getLayout().getOffsetForHorizontal(line, x);
        }
    }
}
