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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.view.BottomScrollView;
import com.android.setupwizardlib.view.BottomScrollView.BottomScrollListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
@RunWith(SuwLibRobolectricTestRunner.class)
public class ScrollViewScrollHandlingDelegateTest {

    @Mock
    private RequireScrollMixin mRequireScrollMixin;

    private BottomScrollView mScrollView;
    private ScrollViewScrollHandlingDelegate mDelegate;
    private ArgumentCaptor<BottomScrollListener> mListenerCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mScrollView = spy(new BottomScrollView(application));
        mDelegate = new ScrollViewScrollHandlingDelegate(mRequireScrollMixin, mScrollView);

        mListenerCaptor = ArgumentCaptor.forClass(BottomScrollListener.class);
        doNothing().when(mScrollView).setBottomScrollListener(mListenerCaptor.capture());

        mScrollView.layout(0, 0, 50, 50);
    }

    @Test
    public void testRequireScroll() throws Throwable {
        mDelegate.startListening();

        mListenerCaptor.getValue().onRequiresScroll();
        verify(mRequireScrollMixin).notifyScrollabilityChange(true);
    }

    @Test
    public void testScrolledToBottom() throws Throwable {
        mDelegate.startListening();

        mListenerCaptor.getValue().onRequiresScroll();
        verify(mRequireScrollMixin).notifyScrollabilityChange(true);

        mListenerCaptor.getValue().onScrolledToBottom();

        verify(mRequireScrollMixin).notifyScrollabilityChange(false);
    }

    @Test
    public void testPageScrollDown() throws Throwable {
        mDelegate.pageScrollDown();
        verify(mScrollView).smoothScrollBy(anyInt(), eq(50));
    }
}
