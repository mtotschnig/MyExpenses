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

package com.android.setupwizardlib.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class GlifStyleTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = new ContextThemeWrapper(application, R.style.SuwThemeGlif_Light);
    }

    @Test
    public void testSuwGlifButtonTertiary() {
        Button button = new Button(
                mContext,
                Robolectric.buildAttributeSet()
                        .setStyleAttribute("@style/SuwGlifButton.Tertiary")
                        .build());
        assertThat(button.getBackground()).named("background").isNotNull();
        assertThat(button.getTransformationMethod()).named("transformation method").isNull();
        if (VERSION.SDK_INT < VERSION_CODES.M) {
            // Robolectric resolved the wrong theme attribute on versions >= M
            // https://github.com/robolectric/robolectric/issues/2940
            assertEquals("ff4285f4", Integer.toHexString(button.getTextColors().getDefaultColor()));
        }
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    @Config(sdk = Config.NEWEST_SDK)
    @Test
    public void glifThemeLight_statusBarColorShouldBeTransparent() {
        GlifThemeActivity activity = Robolectric.setupActivity(GlifThemeActivity.class);
        assertEquals(0x00000000, activity.getWindow().getStatusBarColor());
    }

    @Test
    public void glifLoadingScreen_shouldHaveProgressBar() {
        GlifThemeActivity activity = Robolectric.setupActivity(GlifThemeActivity.class);
        activity.setContentView(R.layout.suw_glif_loading_screen);

        assertTrue("Progress bar should exist",
                activity.findViewById(R.id.suw_large_progress_bar) instanceof ProgressBar);
    }

    private static class GlifThemeActivity extends Activity {

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            setTheme(R.style.SuwThemeGlif_Light);
            super.onCreate(savedInstanceState);
        }
    }
}
