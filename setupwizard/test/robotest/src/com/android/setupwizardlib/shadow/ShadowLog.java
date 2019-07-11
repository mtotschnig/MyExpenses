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

package com.android.setupwizardlib.shadow;

import android.util.Log;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Log.class)
public class ShadowLog extends org.robolectric.shadows.ShadowLog {

    public static boolean sWtfIsFatal = true;

    public static class TerribleFailure extends RuntimeException {

        public TerribleFailure(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    @Implementation
    public static void wtf(String tag, String msg) {
        org.robolectric.shadows.ShadowLog.wtf(tag, msg);
        if (sWtfIsFatal) {
            throw new TerribleFailure(msg, null);
        }
    }

    @Implementation
    public static void wtf(String tag, String msg, Throwable throwable) {
        org.robolectric.shadows.ShadowLog.wtf(tag, msg, throwable);
        if (sWtfIsFatal) {
            throw new TerribleFailure(msg, throwable);
        }
    }
}
