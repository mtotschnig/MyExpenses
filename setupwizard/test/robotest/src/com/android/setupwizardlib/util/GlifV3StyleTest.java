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

package com.android.setupwizardlib.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(minSdk = Config.OLDEST_SDK, maxSdk = Config.NEWEST_SDK)
public class GlifV3StyleTest {

    @Test
    public void activityWithGlifV3Theme_shouldUseLightNavBarOnV27OrAbove() {
        GlifThemeActivity activity = Robolectric.setupActivity(GlifThemeActivity.class);
        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            assertEquals(
                    activity.getWindow().getNavigationBarColor(),
                    Color.WHITE);
            int vis = activity.getWindow().getDecorView().getSystemUiVisibility();
            assertTrue((vis & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0);
        } else if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            assertEquals(
                    activity.getWindow().getNavigationBarColor(),
                    Color.BLACK);
        }
        // Nav bar color is not customizable pre-L
    }

    @Test
    public void buttonWithGlifV3_shouldBeGoogleSans() {
        GlifThemeActivity activity = Robolectric.setupActivity(GlifThemeActivity.class);
        Button button = new Button(
                activity,
                Robolectric.buildAttributeSet()
                        .setStyleAttribute("@style/SuwGlifButton.Primary")
                        .build());
        assertThat(button.getTypeface()).isEqualTo(Typeface.create("google-sans", 0));
        // Button should not be all caps
        assertThat(button.getTransformationMethod()).isNull();
    }

    private static class GlifThemeActivity extends Activity {

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            setTheme(R.style.SuwThemeGlifV3_Light);
            super.onCreate(savedInstanceState);
        }
    }
}
