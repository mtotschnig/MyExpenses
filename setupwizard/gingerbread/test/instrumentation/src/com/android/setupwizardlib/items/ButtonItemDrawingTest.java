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

package com.android.setupwizardlib.items;

import static org.junit.Assert.assertTrue;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.StyleRes;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.test.util.DrawingTestHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ButtonItemDrawingTest {

    private static final int GLIF_ACCENT_COLOR = 0xff4285f4;
    private static final int GLIF_V3_ACCENT_COLOR = 0xff1a73e8;

    // These tests need to be run on UI thread because button uses ValueAnimator
    @Rule
    public UiThreadTestRule mUiThreadTestRule = new UiThreadTestRule();

    @Test
    @UiThreadTest
    public void drawButton_glif_shouldHaveAccentColoredButton()
            throws InstantiationException, IllegalAccessException {
        Button button = createButton(R.style.SuwThemeGlif_Light);

        DrawingTestHelper drawingTestHelper = new DrawingTestHelper(50, 50);
        drawingTestHelper.drawView(button);

        int accentPixelCount =
                countPixelsWithColor(drawingTestHelper.getPixels(), GLIF_ACCENT_COLOR);
        assertTrue("> 10 pixels should be #4285f4. Found " + accentPixelCount,
                accentPixelCount > 10);
    }

    @Test
    @UiThreadTest
    public void drawButton_glifV3_shouldHaveAccentColoredButton()
            throws InstantiationException, IllegalAccessException {
        Button button = createButton(R.style.SuwThemeGlifV3_Light);

        DrawingTestHelper drawingTestHelper = new DrawingTestHelper(50, 50);
        drawingTestHelper.drawView(button);

        int accentPixelCount =
                countPixelsWithColor(drawingTestHelper.getPixels(), GLIF_V3_ACCENT_COLOR);
        assertTrue("> 10 pixels should be #1a73e8. Found " + accentPixelCount,
                accentPixelCount > 10);
    }

    private Button createButton(@StyleRes int theme)
            throws InstantiationException, IllegalAccessException {
        final ViewGroup parent = new LinearLayout(DrawingTestHelper.createCanvasActivity(theme));
        TestButtonItem item = new TestButtonItem();
        item.setTheme(R.style.SuwButtonItem_Colored);
        item.setText("foobar");

        return item.createButton(parent);
    }

    private int countPixelsWithColor(int[] pixels, int color) {
        int count = 0;
        for (int pixel : pixels) {
            if (pixel == color) {
                count++;
            }
        }
        return count;
    }

    private static class TestButtonItem extends ButtonItem {

        @Override
        public Button createButton(ViewGroup parent) {
            // Make this method public for testing
            return super.createButton(parent);
        }
    }
}
