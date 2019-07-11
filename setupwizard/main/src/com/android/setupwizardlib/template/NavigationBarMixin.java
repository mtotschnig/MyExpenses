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

import android.view.View;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

/**
 * A {@link Mixin} for interacting with a {@link NavigationBar}.
 */
public class NavigationBarMixin implements Mixin {

    private TemplateLayout mTemplateLayout;

    /**
     * @param layout The layout this mixin belongs to.
     */
    public NavigationBarMixin(TemplateLayout layout) {
        mTemplateLayout = layout;
    }

    /**
     * @return The navigation bar instance in the layout, or null if the layout does not have a
     *         navigation bar.
     */
    public NavigationBar getNavigationBar() {
        final View view = mTemplateLayout.findManagedViewById(R.id.suw_layout_navigation_bar);
        return view instanceof NavigationBar ? (NavigationBar) view : null;
    }

    /**
     * Sets the label of the next button.
     *
     * @param text Label of the next button.
     */
    public void setNextButtonText(int text) {
        getNavigationBar().getNextButton().setText(text);
    }

    /**
     * Sets the label of the next button.
     *
     * @param text Label of the next button.
     */
    public void setNextButtonText(CharSequence text) {
        getNavigationBar().getNextButton().setText(text);
    }

    /**
     * @return The current label of the next button.
     */
    public CharSequence getNextButtonText() {
        return getNavigationBar().getNextButton().getText();
    }

    /**
     * Sets the listener to handle back and next button clicks in the navigation bar.
     *
     * @see NavigationBar#setNavigationBarListener(NavigationBarListener)
     * @see NavigationBarListener
     */
    public void setNavigationBarListener(NavigationBarListener listener) {
        getNavigationBar().setNavigationBarListener(listener);
    }
}
