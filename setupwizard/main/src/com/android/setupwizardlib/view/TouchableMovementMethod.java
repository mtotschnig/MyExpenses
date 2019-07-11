/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * A movement method that tracks the last result of whether touch events are handled. This is
 * used to patch the return value of {@link TextView#onTouchEvent} so that it consumes the touch
 * events only when the movement method says the event is consumed.
 */
public interface TouchableMovementMethod {

    /**
     * @return The last touch event received in {@link MovementMethod#onTouchEvent}
     */
    MotionEvent getLastTouchEvent();

    /**
     * @return The return value of the last {@link MovementMethod#onTouchEvent}, or whether the
     * last touch event should be considered handled by the text view
     */
    boolean isLastTouchEventHandled();

    /**
     * An extension of LinkMovementMethod that tracks whether the event is handled when it is
     * touched.
     */
    class TouchableLinkMovementMethod extends LinkMovementMethod
            implements TouchableMovementMethod {

        public static TouchableLinkMovementMethod getInstance() {
            return new TouchableLinkMovementMethod();
        }

        boolean mLastEventResult = false;
        MotionEvent mLastEvent;

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            mLastEvent = event;
            boolean result = super.onTouchEvent(widget, buffer, event);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Unfortunately, LinkMovementMethod extends ScrollMovementMethod, and it always
                // consume the down event. So here we use the selection instead as a hint of whether
                // the down event landed on a link.
                mLastEventResult = Selection.getSelectionStart(buffer) != -1;
            } else {
                mLastEventResult = result;
            }
            return result;
        }

        @Override
        public MotionEvent getLastTouchEvent() {
            return mLastEvent;
        }

        @Override
        public boolean isLastTouchEventHandled() {
            return mLastEventResult;
        }
    }
}
