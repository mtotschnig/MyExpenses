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

package com.android.setupwizardlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.setupwizardlib.R;

/**
 * A layout that will measure its children size based on the space it is given, by using its
 * {@code android:minWidth}, {@code android:minHeight}, {@code android:maxWidth}, and
 * {@code android:maxHeight} values.
 *
 * <p>Typically this is used to show an illustration image or video on the screen. For optimal UX,
 * those assets typically want to occupy the remaining space available on screen within a certain
 * range, and then stop scaling beyond the min/max size attributes. Therefore this view is typically
 * used inside a ScrollView with {@code fillViewport} set to true, together with a linear layout
 * weight or relative layout to fill the remaining space visible on screen.
 *
 * <p>When measuring, this view ignores its children and simply layout according to the minWidth /
 * minHeight given. Therefore it is common for children of this layout to have width / height set to
 * {@code match_parent}. The maxWidth / maxHeight values will then be applied to the children to
 * make sure they are not too big.
 */
public class FillContentLayout extends FrameLayout {

    private int mMaxWidth;
    private int mMaxHeight;

    public FillContentLayout(Context context) {
        this(context, null);
    }

    public FillContentLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.suwFillContentLayoutStyle);
    }

    public FillContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.SuwFillContentLayout,
                defStyleAttr,
                0);

        mMaxHeight = a.getDimensionPixelSize(
                R.styleable.SuwFillContentLayout_android_maxHeight, -1);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.SuwFillContentLayout_android_maxWidth, -1);

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure this view with the minWidth and minHeight, without asking the children.
        // (Children size is the drawable's intrinsic size, and we don't want that to influence
        // the size of the illustration).
        setMeasuredDimension(
                getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            measureIllustrationChild(getChildAt(i), getMeasuredWidth(), getMeasuredHeight());
        }
    }

    private void measureIllustrationChild(View child, int parentWidth, int parentHeight) {
        // Modified from ViewGroup#measureChildWithMargins
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        // Create measure specs that are no bigger than min(parentSize, maxSize)

        int childWidthMeasureSpec = getMaxSizeMeasureSpec(
                Math.min(mMaxWidth, parentWidth),
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                lp.width);
        int childHeightMeasureSpec = getMaxSizeMeasureSpec(
                Math.min(mMaxHeight, parentHeight),
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private static int getMaxSizeMeasureSpec(int maxSize, int padding, int childDimension) {
        // Modified from ViewGroup#getChildMeasureSpec
        int size = Math.max(0, maxSize - padding);

        if (childDimension >= 0) {
            // Child wants a specific size... so be it
            return MeasureSpec.makeMeasureSpec(childDimension, MeasureSpec.EXACTLY);
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size. So be it.
            return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size. It can't be
            // bigger than us.
            return MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST);
        }
        return 0;
    }
}
