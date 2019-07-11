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

package com.android.setupwizardlib.test;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.android.setupwizardlib.items.SimpleInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SimpleInflaterTest {

    @Test
    public void testInflateXml() {
        final Context context = InstrumentationRegistry.getContext();
        TestInflater inflater = new TestInflater(context.getResources());
        final StringBuilder result = inflater.inflate(R.xml.simple_inflater_test);

        assertEquals("Parent[null] > Child[foobar]", result.toString());
    }

    private static class TestInflater extends SimpleInflater<StringBuilder> {

        protected TestInflater(@NonNull Resources resources) {
            super(resources);
        }

        @Override
        protected StringBuilder onCreateItem(String tagName, AttributeSet attrs) {
            final String attribute = attrs.getAttributeValue(null, "myattribute");
            return new StringBuilder(tagName).append("[").append(attribute).append("]");
        }

        @Override
        protected void onAddChildItem(StringBuilder parent, StringBuilder child) {
            parent.append(" > ").append(child);
        }
    }
}
