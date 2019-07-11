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
import static org.mockito.Mockito.spy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Xml;
import android.widget.TextView;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class HeaderMixinTest {

    private Context mContext;
    private TemplateLayout mTemplateLayout;
    private TextView mHeaderTextView;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mTemplateLayout = spy(new TemplateLayout(mContext, R.layout.test_template,
                R.id.suw_layout_content));

        mHeaderTextView = new TextView(mContext);
        doReturn(mHeaderTextView).when(mTemplateLayout)
                .findManagedViewById(eq(R.id.suw_layout_title));
    }

    @Test
    public void testGetTextView() {
        HeaderMixin mixin = new HeaderMixin(mTemplateLayout, null, 0);
        assertSame(mHeaderTextView, mixin.getTextView());
    }

    @Test
    public void testSetTextId() {
        HeaderMixin mixin = new HeaderMixin(mTemplateLayout, null, 0);
        mixin.setText(R.string.suw_next_button_label);

        assertEquals("Next", mHeaderTextView.getText());
    }

    @Test
    public void testSetText() {
        HeaderMixin mixin = new HeaderMixin(mTemplateLayout, null, 0);
        mixin.setText("Foobar");

        assertEquals("Foobar", mHeaderTextView.getText());
    }

    @SuppressLint("SetTextI18n")  // It's OK, this is a test
    @Test
    public void testGetText() {
        mHeaderTextView.setText("Lorem ipsum");

        HeaderMixin mixin = new HeaderMixin(mTemplateLayout, null, 0);
        assertEquals("Lorem ipsum", mixin.getText());
    }

    @SuppressWarnings("ResourceType")  // Needed to create attribute set from layout XML.
    @Test
    public void testSetTextFromXml() throws IOException, XmlPullParserException {
        final XmlResourceParser parser =
                mContext.getResources().getXml(R.layout.test_mixin_attributes);
        while (!TemplateLayout.class.getName().equals(parser.getName())) {
            parser.next();
        }
        new HeaderMixin(mTemplateLayout, Xml.asAttributeSet(parser), 0);

        assertEquals("lorem ipsum", mHeaderTextView.getText());
    }
}
