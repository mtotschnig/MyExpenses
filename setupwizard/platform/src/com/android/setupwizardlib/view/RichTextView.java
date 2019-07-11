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

import android.content.Context;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.android.setupwizardlib.span.LinkSpan;
import com.android.setupwizardlib.span.LinkSpan.OnLinkClickListener;
import com.android.setupwizardlib.span.SpanHelper;
import com.android.setupwizardlib.view.TouchableMovementMethod.TouchableLinkMovementMethod;

/**
 * An extension of TextView that automatically replaces the annotation tags as specified in
 * {@link SpanHelper#replaceSpan(android.text.Spannable, Object, Object)}
 *
 * <p>Note: The accessibility interaction for ClickableSpans (and therefore LinkSpans) are built
 * into platform in O, although the interaction paradigm is different. (See b/17726921). In this
 * platform version, the links are exposed in the Local Context Menu of TalkBack instead of
 * accessible directly through swiping.
 */
public class RichTextView extends TextView implements OnLinkClickListener {

    /* static section */

    private static final String TAG = "RichTextView";

    private static final String ANNOTATION_LINK = "link";
    private static final String ANNOTATION_TEXT_APPEARANCE = "textAppearance";

    /**
     * Replace &lt;annotation&gt; tags in strings to become their respective types. Currently 2
     * types are supported:
     * <ol>
     *     <li>&lt;annotation link="foobar"&gt; will create a
     *     {@link com.android.setupwizardlib.span.LinkSpan} that broadcasts with the key
     *     "foobar"</li>
     *     <li>&lt;annotation textAppearance="TextAppearance.FooBar"&gt; will create a
     *     {@link android.text.style.TextAppearanceSpan} with @style/TextAppearance.FooBar</li>
     * </ol>
     */
    public static CharSequence getRichText(Context context, CharSequence text) {
        if (text instanceof Spanned) {
            final SpannableString spannable = new SpannableString(text);
            final Annotation[] spans = spannable.getSpans(0, spannable.length(), Annotation.class);
            for (Annotation span : spans) {
                final String key = span.getKey();
                if (ANNOTATION_TEXT_APPEARANCE.equals(key)) {
                    String textAppearance = span.getValue();
                    final int style = context.getResources()
                            .getIdentifier(textAppearance, "style", context.getPackageName());
                    if (style == 0) {
                        Log.w(TAG, "Cannot find resource: " + style);
                    }
                    final TextAppearanceSpan textAppearanceSpan =
                            new TextAppearanceSpan(context, style);
                    SpanHelper.replaceSpan(spannable, span, textAppearanceSpan);
                } else if (ANNOTATION_LINK.equals(key)) {
                    LinkSpan link = new LinkSpan(span.getValue());
                    SpanHelper.replaceSpan(spannable, span, link);
                }
            }
            return spannable;
        }
        return text;
    }

    /* non-static section */

    private OnLinkClickListener mOnLinkClickListener;

    public RichTextView(Context context) {
        super(context);
    }

    public RichTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        text = getRichText(getContext(), text);
        // Set text first before doing anything else because setMovementMethod internally calls
        // setText. This in turn ends up calling this method with mText as the first parameter
        super.setText(text, type);
        boolean hasLinks = hasLinks(text);

        if (hasLinks) {
            // When a TextView has a movement method, it will set the view to clickable. This makes
            // View.onTouchEvent always return true and consumes the touch event, essentially
            // nullifying any return values of MovementMethod.onTouchEvent.
            // To still allow propagating touch events to the parent when this view doesn't have
            // links, we only set the movement method here if the text contains links.
            setMovementMethod(TouchableLinkMovementMethod.getInstance());
        } else {
            setMovementMethod(null);
        }
        // ExploreByTouchHelper automatically enables focus for RichTextView
        // even though it may not have any links. Causes problems during talkback
        // as individual TextViews consume touch events and thereby reducing the focus window
        // shown by Talkback. Disable focus if there are no links
        setFocusable(hasLinks);
        // Do not "reveal" (i.e. scroll to) this view when this view is focused. Since this view is
        // focusable in touch mode, we may be focused when the screen is first shown, and starting
        // a screen halfway scrolled down is confusing to the user.
        setRevealOnFocusHint(false);
        setFocusableInTouchMode(hasLinks);
    }

    private boolean hasLinks(CharSequence text) {
        if (text instanceof Spanned) {
            final ClickableSpan[] spans =
                    ((Spanned) text).getSpans(0, text.length(), ClickableSpan.class);
            return spans.length > 0;
        }
        return false;
    }

    @Override
    @SuppressWarnings("ClickableViewAccessibility")  // super.onTouchEvent is called
    public boolean onTouchEvent(MotionEvent event) {
        // Since View#onTouchEvent always return true if the view is clickable (which is the case
        // when a TextView has a movement method), override the implementation to allow the movement
        // method, if it implements TouchableMovementMethod, to say that the touch is not handled,
        // allowing the event to bubble up to the parent view.
        boolean superResult = super.onTouchEvent(event);
        MovementMethod movementMethod = getMovementMethod();
        if (movementMethod instanceof TouchableMovementMethod) {
            TouchableMovementMethod touchableMovementMethod =
                    (TouchableMovementMethod) movementMethod;
            if (touchableMovementMethod.getLastTouchEvent() == event) {
                return touchableMovementMethod.isLastTouchEventHandled();
            }
        }
        return superResult;
    }

    public void setOnLinkClickListener(OnLinkClickListener listener) {
        mOnLinkClickListener = listener;
    }

    public OnLinkClickListener getOnLinkClickListener() {
        return mOnLinkClickListener;
    }

    @Override
    public boolean onLinkClick(LinkSpan span) {
        if (mOnLinkClickListener != null) {
            return mOnLinkClickListener.onLinkClick(span);
        }
        return false;
    }
}
