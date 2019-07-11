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

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/**
 * A {@link Mixin} displaying a header text that can be set to different colors. This Mixin is
 * registered to the tempalte using HeaderMixin.class, and can be retrieved using:
 * {@code (ColoredHeaderMixin) templateLayout.getMixin(HeaderMixin.class}.
 */
public class ColoredHeaderMixin extends HeaderMixin {

    /**
     * {@inheritDoc}
     */
    public ColoredHeaderMixin(TemplateLayout layout, AttributeSet attrs, int defStyleAttr) {
        super(layout, attrs, defStyleAttr);

        final TypedArray a = layout.getContext().obtainStyledAttributes(
                attrs, R.styleable.SuwColoredHeaderMixin, defStyleAttr, 0);

        // Set the header color
        final ColorStateList headerColor =
                a.getColorStateList(R.styleable.SuwColoredHeaderMixin_suwHeaderColor);
        if (headerColor != null) {
            setColor(headerColor);
        }

        a.recycle();
    }

    /**
     * Sets the color of the header text. This can also be set via XML using
     * {@code app:suwHeaderColor}.
     *
     * @param color The text color of the header.
     */
    public void setColor(ColorStateList color) {
        final TextView titleView = getTextView();
        if (titleView != null) {
            titleView.setTextColor(color);
        }
    }

    /**
     * @return The current text color of the header.
     */
    public ColorStateList getColor() {
        final TextView titleView = getTextView();
        return titleView != null ? titleView.getTextColors() : null;
    }
}
