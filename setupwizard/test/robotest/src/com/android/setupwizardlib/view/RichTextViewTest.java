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

package com.android.setupwizardlib.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Annotation;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.MotionEvent;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.span.LinkSpan;
import com.android.setupwizardlib.span.LinkSpan.OnLinkClickListener;
import com.android.setupwizardlib.view.TouchableMovementMethod.TouchableLinkMovementMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
public class RichTextViewTest {

    @Test
    public void testLinkAnnotation() {
        Annotation link = new Annotation("link", "foobar");
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(link, 1, 2, 0 /* flags */);

        RichTextView textView = new RichTextView(application);
        textView.setText(ssb);

        final CharSequence text = textView.getText();
        assertTrue("Text should be spanned", text instanceof Spanned);

        assertThat(textView.getMovementMethod()).isInstanceOf(TouchableLinkMovementMethod.class);

        Object[] spans = ((Spanned) text).getSpans(0, text.length(), Annotation.class);
        assertEquals("Annotation should be removed " + Arrays.toString(spans), 0, spans.length);

        spans = ((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
        assertEquals("There should be one span " + Arrays.toString(spans), 1, spans.length);
        assertTrue("The span should be a LinkSpan", spans[0] instanceof LinkSpan);
        assertEquals("The LinkSpan should have id \"foobar\"",
                "foobar", ((LinkSpan) spans[0]).getId());
    }

    @Test
    public void testOnLinkClickListener() {
        Annotation link = new Annotation("link", "foobar");
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(link, 1, 2, 0 /* flags */);

        RichTextView textView = new RichTextView(application);
        textView.setText(ssb);

        OnLinkClickListener listener = mock(OnLinkClickListener.class);
        textView.setOnLinkClickListener(listener);

        assertSame(listener, textView.getOnLinkClickListener());

        CharSequence text = textView.getText();
        LinkSpan[] spans = ((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
        spans[0].onClick(textView);

        verify(listener).onLinkClick(eq(spans[0]));
    }

    @Test
    public void testLegacyContextOnClickListener() {
        // Click listener implemented by context should still be invoked for compatibility.
        Annotation link = new Annotation("link", "foobar");
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(link, 1, 2, 0 /* flags */);

        TestContext context = spy(new TestContext(application));
        RichTextView textView = new RichTextView(context);
        textView.setText(ssb);

        CharSequence text = textView.getText();
        LinkSpan[] spans = ((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
        spans[0].onClick(textView);

        verify(context).onClick(eq(spans[0]));
    }

    @Test
    public void onTouchEvent_clickOnLinks_shouldReturnTrue() {
        Annotation link = new Annotation("link", "foobar");
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(link, 0, 2, 0 /* flags */);

        RichTextView textView = new RichTextView(application);
        textView.setText(ssb);

        TouchableLinkMovementMethod mockMovementMethod = mock(TouchableLinkMovementMethod.class);
        textView.setMovementMethod(mockMovementMethod);

        MotionEvent motionEvent =
                MotionEvent.obtain(123, 22, MotionEvent.ACTION_DOWN, 0, 0, 0);
        doReturn(motionEvent).when(mockMovementMethod).getLastTouchEvent();
        doReturn(true).when(mockMovementMethod).isLastTouchEventHandled();
        assertThat(textView.onTouchEvent(motionEvent)).isTrue();
    }

    @Test
    public void onTouchEvent_clickOutsideLinks_shouldReturnFalse() {
        Annotation link = new Annotation("link", "foobar");
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(link, 0, 2, 0 /* flags */);

        RichTextView textView = new RichTextView(application);
        textView.setText(ssb);

        TouchableLinkMovementMethod mockMovementMethod = mock(TouchableLinkMovementMethod.class);
        textView.setMovementMethod(mockMovementMethod);

        MotionEvent motionEvent =
                MotionEvent.obtain(123, 22, MotionEvent.ACTION_DOWN, 0, 0, 0);
        doReturn(motionEvent).when(mockMovementMethod).getLastTouchEvent();
        doReturn(false).when(mockMovementMethod).isLastTouchEventHandled();
        assertThat(textView.onTouchEvent(motionEvent)).isFalse();
    }

    @Test
    public void testTextStyle() {
        Annotation link = new Annotation("textAppearance", "foobar");
        SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
        ssb.setSpan(link, 1, 2, 0 /* flags */);

        RichTextView textView = new RichTextView(application);
        textView.setText(ssb);

        final CharSequence text = textView.getText();
        assertTrue("Text should be spanned", text instanceof Spanned);

        Object[] spans = ((Spanned) text).getSpans(0, text.length(), Annotation.class);
        assertEquals("Annotation should be removed " + Arrays.toString(spans), 0, spans.length);

        spans = ((Spanned) text).getSpans(0, text.length(), TextAppearanceSpan.class);
        assertEquals("There should be one span " + Arrays.toString(spans), 1, spans.length);
        assertTrue("The span should be a TextAppearanceSpan",
                spans[0] instanceof TextAppearanceSpan);
    }

    @Test
    public void testTextContainingLinksAreFocusable() {
        Annotation testLink = new Annotation("link", "value");
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("Linked");
        spannableStringBuilder.setSpan(testLink, 0, 3, 0);

        RichTextView view = new RichTextView(application);
        view.setText(spannableStringBuilder);

        assertTrue("TextView should be focusable since it contains spans", view.isFocusable());
    }


    @SuppressLint("SetTextI18n")  // It's OK. This is just a test.
    @Test
    public void testTextContainingNoLinksAreNotFocusable() {
        RichTextView textView = new RichTextView(application);
        textView.setText("Thou shall not be focusable!");

        assertFalse("TextView should not be focusable since it does not contain any span",
                textView.isFocusable());
    }


    // Based on the text contents of the text view, the "focusable" property of the element
    // should also be automatically changed.
    @SuppressLint("SetTextI18n")  // It's OK. This is just a test.
    @Test
    public void testRichTextViewFocusChangesWithTextChange() {
        RichTextView textView = new RichTextView(application);
        textView.setText("Thou shall not be focusable!");

        assertFalse(textView.isFocusable());
        assertFalse(textView.isFocusableInTouchMode());

        SpannableStringBuilder spannableStringBuilder =
                new SpannableStringBuilder("I am focusable");
        spannableStringBuilder.setSpan(new Annotation("link", "focus:on_me"), 0, 1, 0);
        textView.setText(spannableStringBuilder);
        assertTrue(textView.isFocusable());
        if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            assertTrue(textView.isFocusableInTouchMode());
            assertFalse(textView.getRevealOnFocusHint());
        } else {
            assertFalse(textView.isFocusableInTouchMode());
        }
    }

    public static class TestContext extends ContextWrapper implements LinkSpan.OnClickListener {

        public TestContext(Context base) {
            super(base);
        }

        @Override
        public void onClick(LinkSpan span) {
            // Ignore. Can be verified using Mockito
        }
    }
}
