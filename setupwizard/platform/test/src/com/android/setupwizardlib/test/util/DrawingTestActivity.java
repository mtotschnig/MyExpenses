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

/**
 * Activity to test view and drawable drawing behaviors. This is used to make sure that the drawing
 * behavior tested is the same as it would when inflated as part of an activity, including any
 * injected layout inflater factories and custom themes etc.
 *
 * @see DrawingTestHelper
 */
public class DrawingTestActivity extends Activity {
}
