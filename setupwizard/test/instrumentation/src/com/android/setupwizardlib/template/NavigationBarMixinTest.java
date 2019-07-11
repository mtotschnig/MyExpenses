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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NavigationBarMixinTest {

    private Context mContext;
    private TemplateLayout mTemplateLayout;
    private NavigationBar mNavigationBar;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
        mTemplateLayout = spy(new TemplateLayout(mContext, R.layout.test_template,
                R.id.suw_layout_content));

        mNavigationBar = new NavigationBar(mContext);
        doReturn(mNavigationBar).when(mTemplateLayout)
                .findManagedViewById(eq(R.id.suw_layout_navigation_bar));
    }

    @Test
    public void testGetNavigationBar() {
        NavigationBarMixin mixin = new NavigationBarMixin(mTemplateLayout);
        assertSame(mNavigationBar, mixin.getNavigationBar());
    }

    @Test
    public void testSetNextButtonText() {
        NavigationBarMixin mixin = new NavigationBarMixin(mTemplateLayout);
        mixin.setNextButtonText(R.string.suw_more_button_label);
        assertEquals("More", mNavigationBar.getNextButton().getText());

        mixin.setNextButtonText("Foobar");
        assertEquals("Foobar", mNavigationBar.getNextButton().getText());
    }

    @SuppressLint("SetTextI18n")  // It's OK, this is just a test
    @Test
    public void testGetNextButtonText() {
        mNavigationBar.getNextButton().setText("lorem ipsum");

        NavigationBarMixin mixin = new NavigationBarMixin(mTemplateLayout);
        assertSame("lorem ipsum", mixin.getNextButtonText());
    }

    @Test
    public void testSetNavigationBarListener() {
        final NavigationBarListener listener = mock(NavigationBarListener.class);
        NavigationBarMixin mixin = new NavigationBarMixin(mTemplateLayout);
        mixin.setNavigationBarListener(listener);

        mNavigationBar.getNextButton().performClick();
        verify(listener).onNavigateNext();

        mNavigationBar.getBackButton().performClick();
        verify(listener).onNavigateBack();
    }
}
