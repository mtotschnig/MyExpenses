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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TemplateLayoutMixinTest {

    private TestTemplateLayout mLayout;

    @Before
    public void setUp() throws Exception {
        mLayout = new TestTemplateLayout(InstrumentationRegistry.getContext());
    }

    @Test
    public void testGetMixin() {
        final TestMixin mixin = mLayout.getMixin(TestMixin.class);
        assertNotNull("TestMixin should not be null", mixin);
        assertTrue("TestMixin should be an instance of TestMixinSubclass. "
                + "Found " + mixin.getClass() + " instead.",
                mixin instanceof TestMixinSubclass);

        // Mixin must be retrieved using the interface it's registered with, not the concrete class,
        // although they are often the same.
        assertNull("TestMixinSubclass should be null", mLayout.getMixin(TestMixinSubclass.class));
    }

    private static class TestTemplateLayout extends TemplateLayout {

        TestTemplateLayout(Context context) {
            super(context, R.layout.test_template, R.id.suw_layout_content);
            registerMixin(TestMixin.class, new TestMixinSubclass());
        }
    }

    private static class TestMixin implements Mixin {}

    private static class TestMixinSubclass extends TestMixin {}
}
