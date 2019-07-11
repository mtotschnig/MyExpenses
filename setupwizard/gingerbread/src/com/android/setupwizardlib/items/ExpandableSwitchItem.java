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

package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.view.CheckableLinearLayout;

/**
 * A switch item which is divided into two parts: the start (left for LTR) side shows the title and
 * summary, and when that is clicked, will expand to show a longer summary. The end (right for LTR)
 * side is a switch which can be toggled by the user.
 *
 * Note: It is highly recommended to use this item with recycler view rather than list view, because
 * list view draws the touch ripple effect on top of the item, rather than letting the item handle
 * it. Therefore you might see a double-ripple, one for the expandable area and one for the entire
 * list item, when using this in list view.
 */
public class ExpandableSwitchItem extends SwitchItem
        implements OnCheckedChangeListener, OnClickListener {

    private CharSequence mCollapsedSummary;
    private CharSequence mExpandedSummary;
    private boolean mIsExpanded = false;

    public ExpandableSwitchItem() {
        super();
    }

    public ExpandableSwitchItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.SuwExpandableSwitchItem);
        mCollapsedSummary = a.getText(R.styleable.SuwExpandableSwitchItem_suwCollapsedSummary);
        mExpandedSummary = a.getText(R.styleable.SuwExpandableSwitchItem_suwExpandedSummary);
        a.recycle();
    }

    @Override
    protected int getDefaultLayoutResource() {
        return R.layout.suw_items_expandable_switch;
    }

    @Override
    public CharSequence getSummary() {
        return mIsExpanded ? getExpandedSummary() : getCollapsedSummary();
    }

    /**
     * @return True if the item is currently expanded.
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }

    /**
     * Sets whether the item should be expanded.
     */
    public void setExpanded(boolean expanded) {
        if (mIsExpanded == expanded) {
            return;
        }
        mIsExpanded = expanded;
        notifyItemChanged();
    }

    /**
     * @return The summary shown when in collapsed state.
     */
    public CharSequence getCollapsedSummary() {
        return mCollapsedSummary;
    }

    /**
     * Sets the summary text shown when the item is collapsed. Corresponds to the
     * {@code app:suwCollapsedSummary} XML attribute.
     */
    public void setCollapsedSummary(CharSequence collapsedSummary) {
        mCollapsedSummary = collapsedSummary;
        if (!isExpanded()) {
            notifyChanged();
        }
    }

    /**
     * @return The summary shown when in expanded state.
     */
    public CharSequence getExpandedSummary() {
        return mExpandedSummary;
    }

    /**
     * Sets the summary text shown when the item is expanded. Corresponds to the
     * {@code app:suwExpandedSummary} XML attribute.
     */
    public void setExpandedSummary(CharSequence expandedSummary) {
        mExpandedSummary = expandedSummary;
        if (isExpanded()) {
            notifyChanged();
        }
    }

    @Override
    public void onBindView(View view) {
        // TODO: If it is possible to detect, log a warning if this is being used with ListView.
        super.onBindView(view);
        View content = view.findViewById(R.id.suw_items_expandable_switch_content);
        content.setOnClickListener(this);

        if (content instanceof CheckableLinearLayout) {
            ((CheckableLinearLayout) content).setChecked(isExpanded());
        }

        tintCompoundDrawables(view);

        // Expandable switch item has focusability on the expandable layout on the left, and the
        // switch on the right, but not the item itself.
        view.setFocusable(false);
    }

    @Override
    public void onClick(View v) {
        setExpanded(!isExpanded());
    }

    // Tint the expand arrow with the text color
    private void tintCompoundDrawables(View view) {
        final TypedArray a = view.getContext()
                .obtainStyledAttributes(new int[] {android.R.attr.textColorPrimary});
        final ColorStateList tintColor = a.getColorStateList(0);
        a.recycle();

        if (tintColor != null) {
            TextView titleView = (TextView) view.findViewById(R.id.suw_items_title);
            for (Drawable drawable : titleView.getCompoundDrawables()) {
                if (drawable != null) {
                    drawable.setColorFilter(tintColor.getDefaultColor(), Mode.SRC_IN);
                }
            }
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                for (Drawable drawable : titleView.getCompoundDrawablesRelative()) {
                    if (drawable != null) {
                        drawable.setColorFilter(tintColor.getDefaultColor(), Mode.SRC_IN);
                    }
                }
            }

        }
    }
}
