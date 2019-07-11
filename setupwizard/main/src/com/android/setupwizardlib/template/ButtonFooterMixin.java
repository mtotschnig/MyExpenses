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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/**
 * A {@link Mixin} for managing buttons. By default, the button bar follows the GLIF design and
 * expects that buttons on the start (left for LTR) are "secondary" borderless buttons, while
 * buttons on the end (right for LTR) are "primary" accent-colored buttons.
 */
public class ButtonFooterMixin implements Mixin {

    private final Context mContext;

    @Nullable
    private final ViewStub mFooterStub;

    private LinearLayout mButtonContainer;

    /**
     * Create a mixin for managing buttons on the footer.
     *
     * @param layout The {@link TemplateLayout} containing this mixin.
     */
    public ButtonFooterMixin(TemplateLayout layout) {
        mContext = layout.getContext();
        mFooterStub = (ViewStub) layout.findManagedViewById(R.id.suw_layout_footer);
    }

    /**
     * Add a button with the given text and style. Common style for GLIF are
     * {@code SuwGlifButton.Primary} and {@code SuwGlifButton.Secondary}.
     *
     * @param text The label for the button.
     * @param theme Theme resource to be used for this button. Since this is applied as a theme,
     *              the resource will typically apply {@code android:buttonStyle} so it will be
     *              applied to the button as a style as well.
     *
     * @return The button that was created.
     */
    public Button addButton(CharSequence text, @StyleRes int theme) {
        Button button = createThemedButton(mContext, theme);
        button.setText(text);
        return addButton(button);
    }

    /**
     * Add a button with the given text and style. Common style for GLIF are
     * {@code SuwGlifButton.Primary} and {@code SuwGlifButton.Secondary}.
     *
     * @param text The label for the button.
     * @param theme Theme resource to be used for this button. Since this is applied as a theme,
     *              the resource will typically apply {@code android:buttonStyle} so it will be
     *              applied to the button as a style as well.
     *
     * @return The button that was created.
     */
    public Button addButton(@StringRes int text, @StyleRes int theme) {
        Button button = createThemedButton(mContext, theme);
        button.setText(text);
        return addButton(button);
    }

    /**
     * Add a button to the footer.
     *
     * @param button The button to be added to the footer.
     * @return The button that was added.
     */
    public Button addButton(Button button) {
        final LinearLayout buttonContainer = ensureFooterInflated();
        buttonContainer.addView(button);
        return button;
    }

    /**
     * Add a space to the footer. Spaces will share the remaining space of footer, so for example,
     * [Button] [space] [Button] [space] [Button] will give you 3 buttons, left, center, and right
     * aligned.
     *
     * @return The view that was used as space.
     */
    public View addSpace() {
        final LinearLayout buttonContainer = ensureFooterInflated();
        View space = new View(buttonContainer.getContext());
        space.setLayoutParams(new LayoutParams(0, 0, 1.0f));
        space.setVisibility(View.INVISIBLE);
        buttonContainer.addView(space);
        return space;
    }

    /**
     * Remove a previously added button.
     *
     * @param button The button to be removed.
     */
    public void removeButton(Button button) {
        if (mButtonContainer != null) {
            mButtonContainer.removeView(button);
        }
    }

    /**
     * Remove a previously added space.
     *
     * @param space The space to be removed.
     */
    public void removeSpace(View space) {
        if (mButtonContainer != null) {
            mButtonContainer.removeView(space);
        }
    }

    /**
     * Remove all views, including spaces, from the footer. Note that if the footer container is
     * already inflated, this will not remove the container itself.
     */
    public void removeAllViews() {
        if (mButtonContainer != null) {
            mButtonContainer.removeAllViews();
        }
    }

    @NonNull
    private LinearLayout ensureFooterInflated() {
        if (mButtonContainer == null) {
            if (mFooterStub == null) {
                throw new IllegalStateException("Footer stub is not found in this template");
            }
            mFooterStub.setLayoutResource(R.layout.suw_glif_footer_button_bar);
            mButtonContainer = (LinearLayout) mFooterStub.inflate();
        }
        return mButtonContainer;
    }

    @SuppressLint("InflateParams")
    private Button createThemedButton(Context context, @StyleRes int theme) {
        // Inflate a single button from XML, which when using support lib, will take advantage of
        // the injected layout inflater and give us AppCompatButton instead.
        LayoutInflater inflater = LayoutInflater.from(new ContextThemeWrapper(context, theme));
        return (Button) inflater.inflate(R.layout.suw_button, null, false);
    }
}
