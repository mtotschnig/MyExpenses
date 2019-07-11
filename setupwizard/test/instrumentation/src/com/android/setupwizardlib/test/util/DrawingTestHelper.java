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

package com.android.setupwizardlib.test.util;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.test.InstrumentationRegistry;
import android.view.View;
import android.view.View.MeasureSpec;

import androidx.annotation.StyleRes;

public class DrawingTestHelper {

    /**
     * Creates an activity of which to inflate views and drawables for drawing tests. This method
     * will return an instance of AppCompatActivity which allows testing of drawing behavior
     * injected by support libraries (like drawable tinting) as well.
     */
    public static Activity createCanvasActivity(@StyleRes int theme)
            throws IllegalAccessException, InstantiationException {
        final Context context = InstrumentationRegistry.getTargetContext();
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        final Intent intent = new Intent(context, DrawingTestActivity.class);
        final Activity activity = instrumentation.newActivity(
                DrawingTestActivity.class,
                context,
                null, /* token */
                new Application(),
                intent,
                new ActivityInfo(),
                "", /* title */
                null, /* parent */
                null, /* id */
                null /* lastNonConfigurationInstance */);
        instrumentation.callActivityOnCreate(activity, null);
        activity.setTheme(theme);
        return activity;
    }

    private final int mWidth;
    private final int mHeight;
    private final Canvas mCanvas;
    private final Bitmap mBitmap;

    public DrawingTestHelper(int width, int height) {
        mWidth = width;
        mHeight = height;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    public void drawView(View view) {
        view.measure(
                MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
        view.layout(0, 0, mWidth, mHeight);
        view.draw(mCanvas);
    }

    public int[] getPixels() {
        int[] out = new int[mWidth * mHeight];
        mBitmap.getPixels(out, 0, mWidth, 0, 0, mWidth, mHeight);
        return out;
    }

    public int getPixel(int x, int y) {
        return mBitmap.getPixel(x, y);
    }
}
