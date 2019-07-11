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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
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
public class ColoredHeaderMixinTest {

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
    public void testSetColor() {
        ColoredHeaderMixin mixin = new ColoredHeaderMixin(mTemplateLayout, null, 0);
        mixin.setColor(ColorStateList.valueOf(Color.MAGENTA));

        assertEquals(ColorStateList.valueOf(Color.MAGENTA), mHeaderTextView.getTextColors());
    }

    @Test
    public void testGetColor() {
        ColoredHeaderMixin mixin = new ColoredHeaderMixin(mTemplateLayout, null, 0);
        mHeaderTextView.setTextColor(ColorStateList.valueOf(Color.GREEN));

        assertEquals(ColorStateList.valueOf(Color.GREEN), mixin.getColor());
    }

    @SuppressWarnings("ResourceType")  // Needed to create attribute set from layout XML.
    @Test
    public void testSetColorFromXml() throws IOException, XmlPullParserException {
        final XmlResourceParser parser =
                mContext.getResources().getXml(R.layout.test_mixin_attributes);
        while (!TemplateLayout.class.getName().equals(parser.getName())) {
            parser.next();
        }
        new ColoredHeaderMixin(mTemplateLayout, Xml.asAttributeSet(parser), 0);

        assertEquals(ColorStateList.valueOf(Color.RED), mHeaderTextView.getTextColors());
    }
}
