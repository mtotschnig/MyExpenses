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

package com.android.setupwizardlib.span;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertSame;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SuwLibRobolectricTestRunner.class)
public class LinkSpanTest {

    @Test
    public void onClick_shouldCallListenerOnContext() {
        final TestContext context = new TestContext(application);
        final TextView textView = new TextView(context);
        final LinkSpan linkSpan = new LinkSpan("test_id");

        linkSpan.onClick(textView);

        assertSame("Clicked LinkSpan should be passed to setup", linkSpan, context.clickedSpan);
    }

    @Test
    public void onClick_contextDoesNotImplementOnClickListener_shouldBeNoOp() {
        final TextView textView = new TextView(application);
        final LinkSpan linkSpan = new LinkSpan("test_id");

        linkSpan.onClick(textView);

        // This would be no-op, because the context doesn't implement LinkSpan.OnClickListener.
        // Just check that no uncaught exception here.
    }

    @Test
    public void onClick_contextWrapsOnClickListener_shouldCallWrappedListener() {
        final TestContext context = new TestContext(application);
        final Context wrapperContext = new ContextWrapper(context);
        final TextView textView = new TextView(wrapperContext);
        final LinkSpan linkSpan = new LinkSpan("test_id");


        linkSpan.onClick(textView);
        assertSame("Clicked LinkSpan should be passed to setup", linkSpan, context.clickedSpan);
    }

    @Test
    public void onClick_shouldClearSelection() {
        final TestContext context = new TestContext(application);
        final TextView textView = new TextView(context);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        final LinkSpan linkSpan = new LinkSpan("test_id");

        SpannableStringBuilder text = new SpannableStringBuilder("Lorem ipsum dolor sit");
        textView.setText(text);
        text.setSpan(linkSpan, /* start= */ 0, /* end= */ 5, /* flags= */ 0);
        // Simulate the touch effect set by TextView when touched.
        Selection.setSelection(text, /* start= */ 0, /* end= */ 5);

        linkSpan.onClick(textView);

        assertThat(Selection.getSelectionStart(textView.getText())).isEqualTo(0);
        assertThat(Selection.getSelectionEnd(textView.getText())).isEqualTo(0);
    }

    @SuppressWarnings("deprecation")
    private static class TestContext extends ContextWrapper implements LinkSpan.OnClickListener {

        public LinkSpan clickedSpan = null;

        TestContext(Context base) {
            super(base);
        }

        @Override
        public void onClick(LinkSpan span) {
            clickedSpan = span;
        }
    }
}
