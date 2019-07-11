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

package com.android.setupwizardlib.gesture;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
@RunWith(SuwLibRobolectricTestRunner.class)
public class ConsecutiveTapsGestureDetectorTest {

    @Mock
    private ConsecutiveTapsGestureDetector.OnConsecutiveTapsListener mListener;

    private ConsecutiveTapsGestureDetector mDetector;
    private int mSlop;
    private int mTapTimeout;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        View view = new View(application);
        view.measure(500, 500);
        view.layout(0, 0, 500, 500);
        mDetector = new ConsecutiveTapsGestureDetector(mListener, view);

        mSlop = ViewConfiguration.get(application).getScaledDoubleTapSlop();
        mTapTimeout = ViewConfiguration.getDoubleTapTimeout();
    }

    @Test
    public void onTouchEvent_shouldTriggerCallbackOnFourTaps() {
        InOrder inOrder = inOrder(mListener);

        tap(0, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(1));

        tap(100, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(2));

        tap(200, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(3));

        tap(300, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(4));
    }

    @Test
    public void onTouchEvent_tapOnDifferentLocation_shouldResetCounter() {
        InOrder inOrder = inOrder(mListener);

        tap(0, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(1));

        tap(100, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(2));

        tap(200, 25f + mSlop * 2, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(1));

        tap(300, 25f + mSlop * 2, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(2));
    }

    @Test
    public void onTouchEvent_tapAfterTimeout_shouldResetCounter() {
        InOrder inOrder = inOrder(mListener);

        tap(0, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(1));

        tap(100, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(2));

        tap(200 + mTapTimeout, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(1));

        tap(300 + mTapTimeout, 25f, 25f);
        inOrder.verify(mListener).onConsecutiveTaps(eq(2));
    }

    private void tap(int timeMillis, float x, float y) {
        mDetector.onTouchEvent(
                MotionEvent.obtain(timeMillis, timeMillis, MotionEvent.ACTION_DOWN, x, y, 0));
        mDetector.onTouchEvent(
                MotionEvent.obtain(timeMillis, timeMillis + 10, MotionEvent.ACTION_UP, x, y, 0));
    }
}
