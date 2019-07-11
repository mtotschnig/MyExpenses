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

package com.android.setupwizardlib.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build.VERSION_CODES;
import android.view.Surface;
import android.view.View;

import androidx.annotation.RawRes;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.shadow.ShadowLog;
import com.android.setupwizardlib.shadow.ShadowLog.TerribleFailure;
import com.android.setupwizardlib.view.IllustrationVideoViewTest.ShadowMockMediaPlayer;
import com.android.setupwizardlib.view.IllustrationVideoViewTest.ShadowSurface;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(
        sdk = Config.NEWEST_SDK,
        shadows = {
                ShadowLog.class,
                ShadowMockMediaPlayer.class,
                ShadowSurface.class
        })
public class IllustrationVideoViewTest {

    @Mock
    private SurfaceTexture mSurfaceTexture;

    private IllustrationVideoView mView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        ShadowMockMediaPlayer.reset();
    }

    @Test
    public void nullMediaPlayer_shouldThrowWtf() {
        ShadowMockMediaPlayer.sMediaPlayer = null;
        try {
            createDefaultView();
            fail("WTF should be thrown for null media player");
        } catch (TerribleFailure e) {
            // pass
        }
    }

    @Test
    public void onVisibilityChanged_notVisible_shouldRelease() {
        createDefaultView();
        mView.onWindowVisibilityChanged(View.GONE);

        verify(ShadowMockMediaPlayer.sMediaPlayer).release();
        assertThat(mView.mSurface).isNull();
        assertThat(mView.mMediaPlayer).isNull();
    }

    @Test
    public void onVisibilityChanged_visible_shouldPlay() {
        createDefaultView();

        mView.onWindowVisibilityChanged(View.GONE);
        assertThat(mView.mSurface).isNull();
        assertThat(mView.mMediaPlayer).isNull();

        mView.onWindowVisibilityChanged(View.VISIBLE);

        assertThat(mView.mSurface).isNotNull();
        assertThat(mView.mMediaPlayer).isNotNull();
    }

    @Test
    public void testPausedWhenWindowFocusLost() {
        createDefaultView();
        mView.start();

        assertNotNull(mView.mMediaPlayer);
        assertNotNull(mView.mSurface);

        mView.onWindowFocusChanged(false);
        verify(ShadowMockMediaPlayer.getMock()).pause();
    }

    @Test
    public void testStartedWhenWindowFocusRegained() {
        testPausedWhenWindowFocusLost();

        // Clear verifications for calls in the other test
        reset(ShadowMockMediaPlayer.getMock());

        mView.onWindowFocusChanged(true);
        verify(ShadowMockMediaPlayer.getMock()).start();
    }

    @Test
    public void testSurfaceReleasedWhenTextureDestroyed() {
        createDefaultView();
        mView.start();

        assertNotNull(mView.mMediaPlayer);
        assertNotNull(mView.mSurface);

        mView.onSurfaceTextureDestroyed(mSurfaceTexture);
        verify(ShadowMockMediaPlayer.getMock()).release();
    }

    @Test
    public void testXmlSetVideoResId() {
        createDefaultView();
        assertEquals(android.R.color.white, ShadowMockMediaPlayer.sResId);
    }

    @Test
    public void testSetVideoResId() {
        createDefaultView();

        @RawRes int black = android.R.color.black;
        mView.setVideoResource(black);

        assertEquals(android.R.color.black, ShadowMockMediaPlayer.sResId);
    }

    private void createDefaultView() {
        mView = new IllustrationVideoView(
                application,
                Robolectric.buildAttributeSet()
                        // Any resource attribute should work, since the media player is mocked
                        .addAttribute(R.attr.suwVideo, "@android:color/white")
                        .build());
        mView.setSurfaceTexture(mock(SurfaceTexture.class));
        mView.onSurfaceTextureAvailable(mSurfaceTexture, 500, 500);
    }

    @Implements(MediaPlayer.class)
    public static class ShadowMockMediaPlayer extends ShadowMediaPlayer {

        private static MediaPlayer sMediaPlayer = mock(MediaPlayer.class);
        private static int sResId;

        public static void reset() {
            sMediaPlayer = mock(MediaPlayer.class);
            sResId = 0;
        }

        @Implementation
        public static MediaPlayer create(Context context, int resId) {
            sResId = resId;
            return sMediaPlayer;
        }

        public static MediaPlayer getMock() {
            return sMediaPlayer;
        }
    }

    @Implements(Surface.class)
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static class ShadowSurface extends org.robolectric.shadows.ShadowSurface {

        @RealObject
        private Surface mRealSurface;

        public void __constructor__(SurfaceTexture surfaceTexture) {
            // Call the constructor on the real object, so that critical fields such as mLock is
            // initialized properly.
            Shadow.invokeConstructor(Surface.class, mRealSurface,
                    ReflectionHelpers.ClassParameter.from(SurfaceTexture.class, surfaceTexture));
            super.__constructor__(surfaceTexture);
        }
    }
}
