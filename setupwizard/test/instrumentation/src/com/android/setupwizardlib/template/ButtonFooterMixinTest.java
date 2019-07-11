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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ButtonFooterMixinTest {

    private Context mContext;
    private TemplateLayout mTemplateLayout;

    // The parent view to contain the view stub and views it inflates.
    private FrameLayout mStubParent;
    private ViewStub mFooterStub;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mTemplateLayout = spy(new TemplateLayout(mContext, R.layout.test_template,
                R.id.suw_layout_content));

        mFooterStub = new ViewStub(mContext, R.layout.suw_glif_footer_button_bar);
        mStubParent = new FrameLayout(mContext);
        mStubParent.addView(mFooterStub);
        doReturn(mFooterStub).when(mTemplateLayout).findManagedViewById(eq(R.id.suw_layout_footer));
    }

    @Test
    public void testAddButton() {
        ButtonFooterMixin mixin = new ButtonFooterMixin(mTemplateLayout);
        final Button button = mixin.addButton("foobar", R.style.SuwGlifButton_Primary);

        assertNotNull(button);
        @IdRes final int id = 12345;
        button.setId(id);
        assertNotNull(mStubParent.findViewById(id));

        assertEquals("foobar", button.getText());

        // Make sure the style is applied by checking the paddings
        assertEquals(dp2Px(16), button.getPaddingLeft());
        assertEquals(dp2Px(16), button.getPaddingRight());
    }

    @Test
    public void testAddButtonTextRes() {
        ButtonFooterMixin mixin = new ButtonFooterMixin(mTemplateLayout);
        final Button button = mixin.addButton(R.string.suw_next_button_label,
                R.style.SuwGlifButton_Primary);

        assertNotNull(button);
        button.setTag("button");
        assertNotNull(mStubParent.findViewWithTag("button"));

        assertEquals("Next", button.getText());

        // Make sure the style is applied by checking the paddings
        assertEquals(dp2Px(16), button.getPaddingLeft());
        assertEquals(dp2Px(16), button.getPaddingRight());
    }

    @Test
    public void testAddSpace() {
        ButtonFooterMixin mixin = new ButtonFooterMixin(mTemplateLayout);
        mixin.addButton("foo", R.style.SuwGlifButton_Secondary);
        final View space = mixin.addSpace();
        mixin.addButton("bar", R.style.SuwGlifButton_Primary);

        space.setTag("space");
        assertNotNull(mStubParent.findViewWithTag("space"));
        assertEquals("Space should have weight of 1",
                1f, ((LinearLayout.LayoutParams) space.getLayoutParams()).weight, 0.001);
    }

    @Test
    public void testRemoveButton() {
        ButtonFooterMixin mixin = new ButtonFooterMixin(mTemplateLayout);
        final Button fooButton = mixin.addButton("foo", R.style.SuwGlifButton_Secondary);
        final Button barButton = mixin.addButton("bar", R.style.SuwGlifButton_Secondary);

        fooButton.setTag("foo");
        barButton.setTag("bar");
        assertNotNull("Foo button should exist", mStubParent.findViewWithTag("foo"));
        assertNotNull("Bar button should exist", mStubParent.findViewWithTag("bar"));

        mixin.removeButton(fooButton);

        assertNull("Foo button should be removed", mStubParent.findViewWithTag("foo"));
        assertNotNull("Bar button should not be removed", mStubParent.findViewWithTag("bar"));
    }

    @Test
    public void testRemoveSpace() {
        ButtonFooterMixin mixin = new ButtonFooterMixin(mTemplateLayout);
        final Button fooButton = mixin.addButton("foo", R.style.SuwGlifButton_Secondary);
        final View space = mixin.addSpace();

        fooButton.setTag("foo");
        space.setTag("space");
        assertNotNull("Foo button should exist", mStubParent.findViewWithTag("foo"));
        assertNotNull("space should exist", mStubParent.findViewWithTag("space"));

        mixin.removeSpace(space);

        assertNotNull("Foo button should not be removed", mStubParent.findViewWithTag("foo"));
        assertNull("Space should be removed", mStubParent.findViewWithTag("space"));
    }

    @Test
    public void testRemoveAllViews() {
        ButtonFooterMixin mixin = new ButtonFooterMixin(mTemplateLayout);
        final Button fooButton = mixin.addButton("foo", R.style.SuwGlifButton_Secondary);
        final View space = mixin.addSpace();

        fooButton.setTag("foo");
        space.setTag("space");
        assertNotNull("Foo button should exist", mStubParent.findViewWithTag("foo"));
        assertNotNull("space should exist", mStubParent.findViewWithTag("space"));

        mixin.removeAllViews();

        assertNull("Foo button should be removed", mStubParent.findViewWithTag("foo"));
        assertNull("Space should be removed", mStubParent.findViewWithTag("space"));
    }

    private int dp2Px(float dp) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }
}
