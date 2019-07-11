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

package com.android.setupwizardlib.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.items.ButtonItem.OnClickListener;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
public class ButtonItemTest {

    private ViewGroup mParent;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = new ContextThemeWrapper(application, R.style.SuwThemeGlif_Light);
        mParent = new LinearLayout(mContext);
    }

    @Test
    public void testDefaultItem() {
        ButtonItem item = new ButtonItem();

        assertTrue("ButtonItem should be enabled by default", item.isEnabled());
        assertEquals("ButtonItem should return count = 0", 0, item.getCount());
        assertEquals("ButtonItem should return layout resource = 0", 0, item.getLayoutResource());
        assertEquals("Default theme should be @style/SuwButtonItem", R.style.SuwButtonItem,
                item.getTheme());
        assertNull("Default text should be null", item.getText());
    }

    @Test
    public void testOnBindView() {
        ButtonItem item = new ButtonItem();

        try {
            item.onBindView(new View(mContext));
            fail("Calling onBindView on ButtonItem should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    @Test
    public void testCreateButton() {
        TestButtonItem item = new TestButtonItem();
        final Button button = item.createButton(mParent);

        assertTrue("Default button should be enabled", button.isEnabled());
        assertTrue("Default button text should be empty", TextUtils.isEmpty(button.getText()));
    }

    @Test
    public void testButtonItemSetsItsId() {
        TestButtonItem item = new TestButtonItem();
        final int id = 12345;
        item.setId(id);

        assertEquals("Button's id should be set", item.createButton(mParent).getId(), id);
    }

    @Test
    public void testCreateButtonTwice() {
        TestButtonItem item = new TestButtonItem();
        final Button button = item.createButton(mParent);

        FrameLayout frameLayout = new FrameLayout(mContext);
        frameLayout.addView(button);

        final Button button2 = item.createButton(mParent);
        assertSame("createButton should be reused", button, button2);
        assertNull("Should be removed from parent after createButton", button2.getParent());
    }

    @Test
    public void testSetEnabledTrue() {
        TestButtonItem item = new TestButtonItem();
        item.setEnabled(true);

        final Button button = item.createButton(mParent);
        assertTrue("ButtonItem should be enabled", item.isEnabled());
        assertTrue("Button should be enabled", button.isEnabled());
    }

    @Test
    public void testSetEnabledFalse() {
        TestButtonItem item = new TestButtonItem();
        item.setEnabled(false);

        final Button button = item.createButton(mParent);
        assertFalse("ButtonItem should be disabled", item.isEnabled());
        assertFalse("Button should be disabled", button.isEnabled());
    }

    @Test
    public void testSetText() {
        TestButtonItem item = new TestButtonItem();
        item.setText("lorem ipsum");

        final Button button = item.createButton(mParent);
        assertEquals("ButtonItem text should be \"lorem ipsum\"", "lorem ipsum", item.getText());
        assertEquals("Button text should be \"lorem ipsum\"", "lorem ipsum", button.getText());
    }

    @Test
    public void testSetTheme() {
        TestButtonItem item = new TestButtonItem();
        item.setTheme(R.style.SuwButtonItem_Colored);

        final Button button = item.createButton(mParent);
        assertEquals("ButtonItem theme should be SuwButtonItem.Colored",
                R.style.SuwButtonItem_Colored, item.getTheme());
        assertNotNull(button.getContext().getTheme());
    }

    @Test
    public void testOnClickListener() {
        TestButtonItem item = new TestButtonItem();
        final OnClickListener listener = mock(OnClickListener.class);
        item.setOnClickListener(listener);

        verify(listener, never()).onClick(any(ButtonItem.class));

        final Button button = item.createButton(mParent);
        button.performClick();

        verify(listener).onClick(same(item));
    }

    private static class TestButtonItem extends ButtonItem {

        @Override
        public Button createButton(ViewGroup parent) {
            // Make this method public for testing
            return super.createButton(parent);
        }
    }
}
