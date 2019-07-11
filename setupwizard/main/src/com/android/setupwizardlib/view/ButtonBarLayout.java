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
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.setupwizardlib.R;

/**
 * An extension of LinearLayout that automatically switches to vertical orientation when it can't
 * fit its child views horizontally.
 *
 * Modified from {@code com.android.internal.widget.ButtonBarLayout}
 */
public class ButtonBarLayout extends LinearLayout {

    private boolean mStacked = false;
    private int mOriginalPaddingLeft;
    private int mOriginalPaddingRight;

    public ButtonBarLayout(Context context) {
        super(context);
    }

    public ButtonBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        setStacked(false);

        boolean needsRemeasure = false;

        int initialWidthMeasureSpec = widthMeasureSpec;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            // Measure with WRAP_CONTENT, so that we can compare the measured size with the
            // available size to see if we need to stack.
            initialWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

            // We'll need to remeasure again to fill excess space.
            needsRemeasure = true;
        }

        super.onMeasure(initialWidthMeasureSpec, heightMeasureSpec);

        if (getMeasuredWidth() > widthSize) {
            setStacked(true);

            // Measure again in the new orientation.
            needsRemeasure = true;
        }

        if (needsRemeasure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void setStacked(boolean stacked) {
        if (mStacked == stacked) {
            return;
        }
        mStacked = stacked;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams childParams = (LayoutParams) child.getLayoutParams();
            if (stacked) {
                child.setTag(R.id.suw_original_weight, childParams.weight);
                childParams.weight = 0;
            } else {
                Float weight = (Float) child.getTag(R.id.suw_original_weight);
                if (weight != null) {
                    childParams.weight = weight;
                }
            }
            child.setLayoutParams(childParams);
        }

        setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        // Reverse the child order, so that the primary button is towards the top when vertical
        for (int i = childCount - 1; i >= 0; i--) {
            bringChildToFront(getChildAt(i));
        }

        if (stacked) {
            // HACK: In the default button bar style, the left and right paddings are not
            // balanced to compensate for different alignment for borderless (left) button and
            // the raised (right) button. When it's stacked, we want the buttons to be centered,
            // so we balance out the paddings here.
            mOriginalPaddingLeft = getPaddingLeft();
            mOriginalPaddingRight = getPaddingRight();
            int paddingHorizontal = Math.max(mOriginalPaddingLeft, mOriginalPaddingRight);
            setPadding(
                    paddingHorizontal, getPaddingTop(), paddingHorizontal, getPaddingBottom());
        } else {
            setPadding(
                    mOriginalPaddingLeft,
                    getPaddingTop(),
                    mOriginalPaddingRight,
                    getPaddingBottom());
        }
    }
}
