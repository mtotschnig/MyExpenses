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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

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
public class ListViewScrollHandlingDelegateTest {

    @Mock
    private RequireScrollMixin mRequireScrollMixin;

    private ListView mListView;
    private ListViewScrollHandlingDelegate mDelegate;
    private ArgumentCaptor<OnScrollListener> mListenerCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mListView = spy(new TestListView(application));
        mDelegate = new ListViewScrollHandlingDelegate(mRequireScrollMixin, mListView);

        mListenerCaptor = ArgumentCaptor.forClass(OnScrollListener.class);
        doNothing().when(mListView).setOnScrollListener(mListenerCaptor.capture());

        mListView.layout(0, 0, 50, 50);
    }

    @Test
    public void testRequireScroll() throws Throwable {
        mDelegate.startListening();

        verify(mRequireScrollMixin).notifyScrollabilityChange(true);
    }

    @Test
    public void testScrolledToBottom() throws Throwable {
        mDelegate.startListening();

        verify(mRequireScrollMixin).notifyScrollabilityChange(true);

        doReturn(20).when(mListView).getLastVisiblePosition();
        mListenerCaptor.getValue().onScroll(mListView, 2, 20, 20);

        verify(mRequireScrollMixin).notifyScrollabilityChange(false);
    }

    @Test
    public void testPageScrollDown() throws Throwable {
        mDelegate.pageScrollDown();
        verify(mListView).smoothScrollBy(eq(50), anyInt());
    }

    private static class TestListView extends ListView {

        TestListView(Context context) {
            super(context);
            setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return 20;
                }

                @Override
                public Object getItem(int position) {
                    return null;
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return new View(parent.getContext());
                }
            });
        }
    }
}
