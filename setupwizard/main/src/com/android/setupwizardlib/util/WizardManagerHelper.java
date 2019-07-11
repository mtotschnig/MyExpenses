/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.setupwizardlib.R;

import java.util.Arrays;

public class WizardManagerHelper {

    private static final String ACTION_NEXT = "com.android.wizard.NEXT";

    // EXTRA_SCRIPT_URI and EXTRA_ACTION_ID are used in setup wizard in versions before M and are
    // kept for backwards compatibility.
    @VisibleForTesting
    static final String EXTRA_SCRIPT_URI = "scriptUri";
    @VisibleForTesting
    static final String EXTRA_ACTION_ID = "actionId";

    @VisibleForTesting
    static final String EXTRA_WIZARD_BUNDLE = "wizardBundle";
    private static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    @VisibleForTesting
    static final String EXTRA_IS_FIRST_RUN = "firstRun";
    @VisibleForTesting
    static final String EXTRA_IS_DEFERRED_SETUP = "deferredSetup";
    @VisibleForTesting
    static final String EXTRA_IS_PRE_DEFERRED_SETUP = "preDeferredSetup";

    public static final String EXTRA_THEME = "theme";
    public static final String EXTRA_USE_IMMERSIVE_MODE = "useImmersiveMode";

    public static final String SETTINGS_GLOBAL_DEVICE_PROVISIONED = "device_provisioned";
    public static final String SETTINGS_SECURE_USER_SETUP_COMPLETE = "user_setup_complete";

    public static final String THEME_HOLO = "holo";
    public static final String THEME_HOLO_LIGHT = "holo_light";
    public static final String THEME_MATERIAL = "material";
    public static final String THEME_MATERIAL_LIGHT = "material_light";

    /**
     * Passed in a setup wizard intent as {@link #EXTRA_THEME}. This is the dark variant of the
     * theme used in setup wizard for Nougat MR1.
     */
    public static final String THEME_GLIF = "glif";

    /**
     * Passed in a setup wizard intent as {@link #EXTRA_THEME}. This is the default theme used in
     * setup wizard for Nougat MR1.
     */
    public static final String THEME_GLIF_LIGHT = "glif_light";

    /**
     * Passed in a setup wizard intent as {@link #EXTRA_THEME}. This is the dark variant of the
     * theme used in setup wizard for O DR.
     */
    public static final String THEME_GLIF_V2 = "glif_v2";

    /**
     * Passed in a setup wizard intent as {@link #EXTRA_THEME}. This is the default theme used in
     * setup wizard for O DR.
     */
    public static final String THEME_GLIF_V2_LIGHT = "glif_v2_light";

    /**
     * Passed in a setup wizard intent as {@link #EXTRA_THEME}. This is the dark variant of the
     * theme used in setup wizard for P.
     */
    public static final String THEME_GLIF_V3 = "glif_v3";

    /**
     * Passed in a setup wizard intent as {@link #EXTRA_THEME}. This is the default theme used in
     * setup wizard for P.
     */
    public static final String THEME_GLIF_V3_LIGHT = "glif_v3_light";

    /**
     * Get an intent that will invoke the next step of setup wizard.
     *
     * @param originalIntent The original intent that was used to start the step, usually via
     *                       {@link android.app.Activity#getIntent()}.
     * @param resultCode The result code of the step. See {@link ResultCodes}.
     * @return A new intent that can be used with
     *         {@link android.app.Activity#startActivityForResult(Intent, int)} to start the next
     *         step of the setup flow.
     */
    public static Intent getNextIntent(Intent originalIntent, int resultCode) {
        return getNextIntent(originalIntent, resultCode, null);
    }

    /**
     * Get an intent that will invoke the next step of setup wizard.
     *
     * @param originalIntent The original intent that was used to start the step, usually via
     *                       {@link android.app.Activity#getIntent()}.
     * @param resultCode The result code of the step. See {@link ResultCodes}.
     * @param data An intent containing extra result data.
     * @return A new intent that can be used with
     *         {@link android.app.Activity#startActivityForResult(Intent, int)} to start the next
     *         step of the setup flow.
     */
    public static Intent getNextIntent(Intent originalIntent, int resultCode, Intent data) {
        Intent intent = new Intent(ACTION_NEXT);
        copyWizardManagerExtras(originalIntent, intent);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        if (data != null && data.getExtras() != null) {
            intent.putExtras(data.getExtras());
        }
        intent.putExtra(EXTRA_THEME, originalIntent.getStringExtra(EXTRA_THEME));

        return intent;
    }

    /**
     * Copy the internal extras used by setup wizard from one intent to another. For low-level use
     * only, such as when using {@link Intent#FLAG_ACTIVITY_FORWARD_RESULT} to relay to another
     * intent.
     *
     * @param srcIntent Intent to get the wizard manager extras from.
     * @param dstIntent Intent to copy the wizard manager extras to.
     */
    public static void copyWizardManagerExtras(Intent srcIntent, Intent dstIntent) {
        dstIntent.putExtra(EXTRA_WIZARD_BUNDLE, srcIntent.getBundleExtra(EXTRA_WIZARD_BUNDLE));
        for (String key : Arrays.asList(
                EXTRA_IS_FIRST_RUN, EXTRA_IS_DEFERRED_SETUP, EXTRA_IS_PRE_DEFERRED_SETUP)) {
            dstIntent.putExtra(key, srcIntent.getBooleanExtra(key, false));
        }

        for (String key : Arrays.asList(EXTRA_THEME, EXTRA_SCRIPT_URI, EXTRA_ACTION_ID)) {
            dstIntent.putExtra(key, srcIntent.getStringExtra(key));
        }
    }

    /**
     * Check whether an intent is intended to be used within the setup wizard flow.
     *
     * @param intent The intent to be checked, usually from
     *               {@link android.app.Activity#getIntent()}.
     * @return true if the intent passed in was intended to be used with setup wizard.
     */
    public static boolean isSetupWizardIntent(Intent intent) {
        return intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false);
    }

    /**
     * Checks whether the current user has completed Setup Wizard. This is true if the current user
     * has gone through Setup Wizard. The current user may or may not be the device owner and the
     * device owner may have already completed setup wizard.
     *
     * @param context The context to retrieve the settings.
     * @return true if the current user has completed Setup Wizard.
     * @see #isDeviceProvisioned(android.content.Context)
     */
    public static boolean isUserSetupComplete(Context context) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.Secure.getInt(context.getContentResolver(),
                    SETTINGS_SECURE_USER_SETUP_COMPLETE, 0) == 1;
        } else {
            // For versions below JB MR1, there are no user profiles. Just return the global device
            // provisioned state.
            return Settings.Secure.getInt(context.getContentResolver(),
                    SETTINGS_GLOBAL_DEVICE_PROVISIONED, 0) == 1;
        }
    }

    /**
     * Checks whether the device is provisioned. This means that the device has gone through Setup
     * Wizard at least once. Note that the user can still be in Setup Wizard even if this is true,
     * for a secondary user profile triggered through Settings > Add account.
     *
     * @param context The context to retrieve the settings.
     * @return true if the device is provisioned.
     * @see #isUserSetupComplete(android.content.Context)
     */
    public static boolean isDeviceProvisioned(Context context) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.Global.getInt(context.getContentResolver(),
                    SETTINGS_GLOBAL_DEVICE_PROVISIONED, 0) == 1;
        } else {
            return Settings.Secure.getInt(context.getContentResolver(),
                    SETTINGS_GLOBAL_DEVICE_PROVISIONED, 0) == 1;
        }
    }

    /**
     * Checks whether an intent is running in the deferred setup wizard flow.
     *
     * @param originalIntent The original intent that was used to start the step, usually via
     *                       {@link android.app.Activity#getIntent()}.
     * @return true if the intent passed in was running in deferred setup wizard.
     */
    public static boolean isDeferredSetupWizard(Intent originalIntent) {
        return originalIntent != null
                && originalIntent.getBooleanExtra(EXTRA_IS_DEFERRED_SETUP, false);
    }

    /**
     * Checks whether an intent is running in "pre-deferred" setup wizard flow.
     *
     * @param originalIntent The original intent that was used to start the step, usually via
     *                       {@link android.app.Activity#getIntent()}.
     * @return true if the intent passed in was running in "pre-deferred" setup wizard.
     */
    public static boolean isPreDeferredSetupWizard(Intent originalIntent) {
        return originalIntent != null
                && originalIntent.getBooleanExtra(EXTRA_IS_PRE_DEFERRED_SETUP, false);
    }

    /**
     * Checks the intent whether the extra indicates that the light theme should be used or not. If
     * the theme is not specified in the intent, or the theme specified is unknown, the value def
     * will be returned.
     *
     * @param intent The intent used to start the activity, which the theme extra will be read from.
     * @param def The default value if the theme is not specified.
     * @return True if the activity started by the given intent should use light theme.
     */
    public static boolean isLightTheme(Intent intent, boolean def) {
        final String theme = intent.getStringExtra(EXTRA_THEME);
        return isLightTheme(theme, def);
    }

    /**
     * Checks whether {@code theme} represents a light or dark theme. If the theme specified is
     * unknown, the value def will be returned.
     *
     * @param theme The theme as specified from an intent sent from setup wizard.
     * @param def The default value if the theme is not known.
     * @return True if {@code theme} represents a light theme.
     */
    public static boolean isLightTheme(String theme, boolean def) {
        if (THEME_HOLO_LIGHT.equals(theme) || THEME_MATERIAL_LIGHT.equals(theme)
                || THEME_GLIF_LIGHT.equals(theme) || THEME_GLIF_V2_LIGHT.equals(theme)
                || THEME_GLIF_V3_LIGHT.equals(theme)) {
            return true;
        } else if (THEME_HOLO.equals(theme) || THEME_MATERIAL.equals(theme)
                || THEME_GLIF.equals(theme) || THEME_GLIF_V2.equals(theme)
                || THEME_GLIF_V3.equals(theme)) {
            return false;
        } else {
            return def;
        }
    }

    /**
     * Gets the theme style resource defined by this library for the theme specified in the given
     * intent. For example, for THEME_GLIF_LIGHT, the theme @style/SuwThemeGlif.Light is returned.
     *
     * @param intent The intent passed by setup wizard, or one with the theme propagated along using
     *               {@link #copyWizardManagerExtras(Intent, Intent)}.
     * @return The style corresponding to the theme in the given intent, or {@code defaultTheme} if
     *         the given theme is not recognized.
     *
     * @see #getThemeRes(String, int)
     */
    public static @StyleRes int getThemeRes(Intent intent, @StyleRes int defaultTheme) {
        final String theme = intent.getStringExtra(EXTRA_THEME);
        return getThemeRes(theme, defaultTheme);
    }

    /**
     * Gets the theme style resource defined by this library for the given theme name. For example,
     * for THEME_GLIF_LIGHT, the theme @style/SuwThemeGlif.Light is returned.
     *
     * <p>If you require extra theme attributes but want to ensure forward compatibility with new
     * themes added here, consider overriding {@link android.app.Activity#onApplyThemeResource} in
     * your activity and call {@link Theme#applyStyle(int, boolean)} using your theme overlay.
     *
     * <pre>{@code
     * protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
     *     super.onApplyThemeResource(theme, resid, first);
     *     theme.applyStyle(R.style.MyThemeOverlay, true);
     * }
     * }</pre>
     *
     * @param theme The string representation of the theme.
     * @return The style corresponding to the given {@code theme}, or {@code defaultTheme} if the
     *         given theme is not recognized.
     */
    public static @StyleRes int getThemeRes(String theme, @StyleRes int defaultTheme) {
        if (theme != null) {
            switch (theme) {
                case THEME_GLIF_V3_LIGHT:
                    return R.style.SuwThemeGlifV3_Light;
                case THEME_GLIF_V3:
                    return R.style.SuwThemeGlifV3;
                case THEME_GLIF_V2_LIGHT:
                    return R.style.SuwThemeGlifV2_Light;
                case THEME_GLIF_V2:
                    return R.style.SuwThemeGlifV2;
                case THEME_GLIF_LIGHT:
                    return R.style.SuwThemeGlif_Light;
                case THEME_GLIF:
                    return R.style.SuwThemeGlif;
                case THEME_MATERIAL_LIGHT:
                    return R.style.SuwThemeMaterial_Light;
                case THEME_MATERIAL:
                    return R.style.SuwThemeMaterial;
                default:
                    // fall through
            }
        }
        return defaultTheme;
    }
}
