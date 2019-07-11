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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.template.RequireScrollMixin.OnRequireScrollStateChangedListener;
import com.android.setupwizardlib.template.RequireScrollMixin.ScrollHandlingDelegate;
import com.android.setupwizardlib.view.NavigationBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
@RunWith(SuwLibRobolectricTestRunner.class)
public class RequireScrollMixinTest {

    @Mock
    private TemplateLayout mTemplateLayout;

    @Mock
    private ScrollHandlingDelegate mDelegate;

    private RequireScrollMixin mRequireScrollMixin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(application).when(mTemplateLayout).getContext();
        mRequireScrollMixin = new RequireScrollMixin(mTemplateLayout);
        mRequireScrollMixin.setScrollHandlingDelegate(mDelegate);
    }

    @Test
    public void testRequireScroll() {
        mRequireScrollMixin.requireScroll();

        verify(mDelegate).startListening();
    }

    @Test
    public void testScrollStateChangedListener() {
        OnRequireScrollStateChangedListener listener =
                mock(OnRequireScrollStateChangedListener.class);
        mRequireScrollMixin.setOnRequireScrollStateChangedListener(listener);
        assertFalse("Scrolling should not be required initially",
                mRequireScrollMixin.isScrollingRequired());

        mRequireScrollMixin.notifyScrollabilityChange(true);
        verify(listener).onRequireScrollStateChanged(true);
        assertTrue("Scrolling should be required when there is more content below the fold",
                mRequireScrollMixin.isScrollingRequired());

        mRequireScrollMixin.notifyScrollabilityChange(false);
        verify(listener).onRequireScrollStateChanged(false);
        assertFalse("Scrolling should not be required after scrolling to bottom",
                mRequireScrollMixin.isScrollingRequired());

        // Once the user has scrolled to the bottom, they should not be forced to scroll down again
        mRequireScrollMixin.notifyScrollabilityChange(true);
        verifyNoMoreInteractions(listener);

        assertFalse("Scrolling should not be required after scrolling to bottom once",
                mRequireScrollMixin.isScrollingRequired());

        assertSame(listener, mRequireScrollMixin.getOnRequireScrollStateChangedListener());
    }

    @Test
    public void testCreateOnClickListener() {
        OnClickListener wrappedListener = mock(OnClickListener.class);
        final OnClickListener onClickListener =
                mRequireScrollMixin.createOnClickListener(wrappedListener);

        mRequireScrollMixin.notifyScrollabilityChange(true);
        onClickListener.onClick(null);

        verify(wrappedListener, never()).onClick(any(View.class));
        verify(mDelegate).pageScrollDown();

        mRequireScrollMixin.notifyScrollabilityChange(false);
        onClickListener.onClick(null);

        verify(wrappedListener).onClick(any(View.class));
    }

    @Test
    public void testRequireScrollWithNavigationBar() {
        final NavigationBar navigationBar = new NavigationBar(application);
        mRequireScrollMixin.requireScrollWithNavigationBar(navigationBar);

        mRequireScrollMixin.notifyScrollabilityChange(true);
        assertEquals("More button should be visible",
                View.VISIBLE, navigationBar.getMoreButton().getVisibility());
        assertEquals("Next button should be hidden",
                View.GONE, navigationBar.getNextButton().getVisibility());

        navigationBar.getMoreButton().performClick();
        verify(mDelegate).pageScrollDown();

        mRequireScrollMixin.notifyScrollabilityChange(false);
        assertEquals("More button should be hidden",
                View.GONE, navigationBar.getMoreButton().getVisibility());
        assertEquals("Next button should be visible",
                View.VISIBLE, navigationBar.getNextButton().getVisibility());
    }

    @SuppressLint("SetTextI18n") // It's OK for testing
    @Test
    public void testRequireScrollWithButton() {
        final Button button = new Button(application);
        button.setText("OriginalLabel");
        OnClickListener wrappedListener = mock(OnClickListener.class);
        mRequireScrollMixin.requireScrollWithButton(
                button, "TestMoreLabel", wrappedListener);

        assertEquals("Button label should be kept initially", "OriginalLabel", button.getText());

        mRequireScrollMixin.notifyScrollabilityChange(true);
        assertEquals("TestMoreLabel", button.getText());
        button.performClick();
        verify(wrappedListener, never()).onClick(eq(button));
        verify(mDelegate).pageScrollDown();

        mRequireScrollMixin.notifyScrollabilityChange(false);
        assertEquals("OriginalLabel", button.getText());
        button.performClick();
        verify(wrappedListener).onClick(eq(button));
    }
}
