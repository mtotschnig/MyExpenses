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

import static org.junit.Assert.assertEquals;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = Config.ALL_SDKS)
public class DimensionConsistencyTest {

    // Visual height of the framework switch widget
    private static final int SWTICH_HEIGHT_DP = 26;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = new ContextThemeWrapper(application, R.style.SuwThemeGlif_Light);
    }

    @Test
    public void testSwitchPaddingTop() {
        final Resources res = mContext.getResources();

        assertEquals(
                "Switch and divider should be aligned at center vertically: "
                        + "suw_switch_padding_top + SWITCH_HEIGHT / 2 = "
                        + "suw_switch_divider_padding_top + suw_switch_divider_height / 2",
                res.getDimensionPixelSize(R.dimen.suw_switch_divider_padding_top)
                        + (res.getDimensionPixelSize(R.dimen.suw_switch_divider_height) / 2),
                res.getDimensionPixelSize(R.dimen.suw_switch_padding_top)
                        + (dp2Px(SWTICH_HEIGHT_DP) / 2));
    }

    private int dp2Px(float dp) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }
}
