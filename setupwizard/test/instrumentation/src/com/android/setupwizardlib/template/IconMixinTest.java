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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Xml;
import android.view.View;
import android.widget.ImageView;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class IconMixinTest {

    private Context mContext;
    private TemplateLayout mTemplateLayout;
    private ImageView mIconView;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
        mTemplateLayout = spy(new TemplateLayout(mContext, R.layout.test_template,
                R.id.suw_layout_content));

        mIconView = new ImageView(mContext);
        doReturn(mIconView).when(mTemplateLayout).findManagedViewById(eq(R.id.suw_layout_icon));
    }

    @Test
    public void testGetIconView() {
        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        assertSame(mIconView, mixin.getView());
    }

    @Test
    public void testSetIcon() {
        final ColorDrawable drawable = new ColorDrawable(Color.CYAN);
        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        mixin.setIcon(drawable);

        assertSame(drawable, mIconView.getDrawable());
        assertEquals(View.VISIBLE, mIconView.getVisibility());
    }

    @Test
    public void setIcon_resourceId_shouldSetIcon() {
        int icon = android.R.drawable.ic_menu_add;
        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        mixin.setIcon(icon);

        Drawable drawable = mIconView.getDrawable();
        assertThat(drawable).isInstanceOf(BitmapDrawable.class);
        assertEquals(View.VISIBLE, mIconView.getVisibility());
    }

    @Test
    public void setIcon_shouldSetVisibilityToGone_whenIconIsNull() {
        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        mixin.setIcon(null);

        assertEquals(View.GONE, mIconView.getVisibility());
    }

    @Test
    public void testGetIcon() {
        final ColorDrawable drawable = new ColorDrawable(Color.BLUE);
        mIconView.setImageDrawable(drawable);

        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        assertSame(drawable, mixin.getIcon());
    }

    @SuppressWarnings("ResourceType")  // Needed to create attribute set from layout XML.
    @Test
    public void testSetIconFromXml() throws IOException, XmlPullParserException {
        final XmlResourceParser parser =
                mContext.getResources().getXml(R.layout.test_mixin_attributes);
        while (!TemplateLayout.class.getName().equals(parser.getName())) {
            parser.next();
        }
        new IconMixin(mTemplateLayout, Xml.asAttributeSet(parser), 0);

        // Check that the bitmaps themselves are equal because BitmapDrawable does not implement
        // equals()
        final BitmapDrawable expected = (BitmapDrawable) mContext.getResources()
                .getDrawable(android.R.drawable.ic_menu_add);
        final BitmapDrawable actual = (BitmapDrawable) mIconView.getDrawable();
        assertEquals(expected.getBitmap(), actual.getBitmap());
        assertEquals(View.VISIBLE, mIconView.getVisibility());
    }

    @Test
    public void setContentDescription_shouldSetContentDescriptionOnIconView() {
        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        mixin.setContentDescription("hello world");
        assertThat(mIconView.getContentDescription()).isEqualTo("hello world");
    }

    @Test
    public void getContentDescription_shouldReturnContentDescriptionFromView() {
        IconMixin mixin = new IconMixin(mTemplateLayout, null, 0);
        mIconView.setContentDescription("aloha");
        assertThat(mixin.getContentDescription()).isEqualTo("aloha");
    }
}
