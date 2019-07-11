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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
@RunWith(SuwLibRobolectricTestRunner.class)
public class RecyclerViewScrollHandlingDelegateTest {

    @Mock
    private RequireScrollMixin mRequireScrollMixin;

    private RecyclerView mRecyclerView;
    private RecyclerViewScrollHandlingDelegate mDelegate;
    private ArgumentCaptor<OnScrollListener> mListenerCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mRecyclerView = spy(new RecyclerView(application));
        doReturn(20).when(mRecyclerView).computeVerticalScrollRange();
        doReturn(0).when(mRecyclerView).computeVerticalScrollExtent();
        doReturn(0).when(mRecyclerView).computeVerticalScrollOffset();
        mListenerCaptor = ArgumentCaptor.forClass(OnScrollListener.class);
        doNothing().when(mRecyclerView).addOnScrollListener(mListenerCaptor.capture());

        mDelegate = new RecyclerViewScrollHandlingDelegate(mRequireScrollMixin, mRecyclerView);
        mRecyclerView.layout(0, 0, 50, 50);
    }

    @Test
    public void testRequireScroll() {
        mDelegate.startListening();
        verify(mRequireScrollMixin).notifyScrollabilityChange(true);
    }

    @Test
    public void testScrolledToBottom() {
        mDelegate.startListening();
        verify(mRequireScrollMixin).notifyScrollabilityChange(true);

        doReturn(20).when(mRecyclerView).computeVerticalScrollOffset();
        mListenerCaptor.getValue().onScrolled(mRecyclerView, 0, 20);

        verify(mRequireScrollMixin).notifyScrollabilityChange(false);
    }

    @Test
    public void testClickScrollButton() {
        mDelegate.pageScrollDown();
        verify(mRecyclerView).smoothScrollBy(anyInt(), eq(50));
    }
}
