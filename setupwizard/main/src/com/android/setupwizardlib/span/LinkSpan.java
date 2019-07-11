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

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * A clickable span that will listen for click events and send it back to the context. To use this
 * class, implement {@link OnLinkClickListener} in your TextView, or use
 * {@link com.android.setupwizardlib.view.RichTextView#setOnClickListener(View.OnClickListener)}.
 *
 * <p />Note on accessibility: For TalkBack to be able to traverse and interact with the links, you
 * should use {@code LinkAccessibilityHelper} in your {@code TextView} subclass. Optionally you can
 * also use {@code RichTextView}, which includes link support.
 */
public class LinkSpan extends ClickableSpan {

    /*
     * Implementation note: When the orientation changes, TextView retains a reference to this span
     * instead of writing it to a parcel (ClickableSpan is not Parcelable). If this class has any
     * reference to the containing Activity (i.e. the activity context, or any views in the
     * activity), it will cause memory leak.
     */

    /* static section */

    private static final String TAG = "LinkSpan";

    private static final Typeface TYPEFACE_MEDIUM =
            Typeface.create("sans-serif-medium", Typeface.NORMAL);

    /**
     * @deprecated Use {@link OnLinkClickListener}
     */
    @Deprecated
    public interface OnClickListener {
        void onClick(LinkSpan span);
    }

    /**
     * Listener that is invoked when a link span is clicked. If the containing view of this span
     * implements this interface, this will be invoked when the link is clicked.
     */
    public interface OnLinkClickListener {

        /**
         * Called when a link has been clicked.
         *
         * @param span The span that was clicked.
         * @return True if the click was handled, stopping further propagation of the click event.
         */
        boolean onLinkClick(LinkSpan span);
    }

    /* non-static section */

    private final String mId;

    public LinkSpan(String id) {
        mId = id;
    }

    @Override
    public void onClick(View view) {
        if (dispatchClick(view)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Prevent the touch event from bubbling up to the parent views.
                view.cancelPendingInputEvents();
            }
        } else {
            Log.w(TAG, "Dropping click event. No listener attached.");
        }
        if (view instanceof TextView) {
            // Remove the highlight effect when the click happens by clearing the selection
            CharSequence text = ((TextView) view).getText();
            if (text instanceof Spannable) {
                Selection.setSelection((Spannable) text, 0);
            }
        }
    }

    private boolean dispatchClick(View view) {
        boolean handled = false;
        if (view instanceof OnLinkClickListener) {
            handled = ((OnLinkClickListener) view).onLinkClick(this);
        }
        if (!handled) {
            final OnClickListener listener = getLegacyListenerFromContext(view.getContext());
            if (listener != null) {
                listener.onClick(this);
                handled = true;
            }
        }
        return handled;
    }

    /**
     * @deprecated Deprecated together with {@link OnClickListener}
     */
    @Nullable
    @Deprecated
    private OnClickListener getLegacyListenerFromContext(@Nullable Context context) {
        while (true) {
            if (context instanceof OnClickListener) {
                return (OnClickListener) context;
            } else if (context instanceof ContextWrapper) {
                // Unwrap any context wrapper, in base the base context implements onClickListener.
                // ContextWrappers cannot have circular base contexts, so at some point this will
                // reach the one of the other cases and return.
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        }
    }

    @Override
    public void updateDrawState(TextPaint drawState) {
        super.updateDrawState(drawState);
        drawState.setUnderlineText(false);
        drawState.setTypeface(TYPEFACE_MEDIUM);
    }

    public String getId() {
        return mId;
    }
}
