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

import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/**
 * A {@link Mixin} for setting and getting the header text.
 */
public class HeaderMixin implements Mixin {

    private TemplateLayout mTemplateLayout;

    /**
     * @param layout The layout this Mixin belongs to.
     * @param attrs XML attributes given to the layout.
     * @param defStyleAttr The default style attribute as given to the constructor of the layout.
     */
    public HeaderMixin(@NonNull TemplateLayout layout, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        mTemplateLayout = layout;

        final TypedArray a = layout.getContext().obtainStyledAttributes(
                attrs, R.styleable.SuwHeaderMixin, defStyleAttr, 0);

        // Set the header text
        final CharSequence headerText = a.getText(R.styleable.SuwHeaderMixin_suwHeaderText);
        if (headerText != null) {
            setText(headerText);
        }

        a.recycle();
    }

    /**
     * @return The TextView displaying the header.
     */
    public TextView getTextView() {
        return (TextView) mTemplateLayout.findManagedViewById(R.id.suw_layout_title);
    }

    /**
     * Sets the header text. This can also be set via the XML attribute {@code app:suwHeaderText}.
     *
     * @param title The resource ID of the text to be set as header.
     */
    public void setText(int title) {
        final TextView titleView = getTextView();
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    /**
     * Sets the header text. This can also be set via the XML attribute {@code app:suwHeaderText}.
     *
     * @param title The text to be set as header.
     */
    public void setText(CharSequence title) {
        final TextView titleView = getTextView();
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    /**
     * @return The current header text.
     */
    public CharSequence getText() {
        final TextView titleView = getTextView();
        return titleView != null ? titleView.getText() : null;
    }
}
