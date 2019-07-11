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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;

import androidx.annotation.StyleRes;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = Config.NEWEST_SDK)
public class WizardManagerHelperTest {

    @Test
    public void testGetNextIntent() {
        final Intent intent = new Intent("test.intent.ACTION");
        intent.putExtra("scriptUri", "android-resource://test-script");
        intent.putExtra("actionId", "test_action_id");
        intent.putExtra("theme", "test_theme");
        intent.putExtra("ignoreExtra", "poof"); // extra is ignored because it's not known

        final Intent data = new Intent();
        data.putExtra("extraData", "shazam");

        final Intent nextIntent =
                WizardManagerHelper.getNextIntent(intent, Activity.RESULT_OK, data);
        assertEquals("Next intent action should be NEXT", "com.android.wizard.NEXT",
                nextIntent.getAction());
        assertEquals("Script URI should be the same as original intent",
                "android-resource://test-script", nextIntent.getStringExtra("scriptUri"));
        assertEquals("Action ID should be the same as original intent", "test_action_id",
                nextIntent.getStringExtra("actionId"));
        assertEquals("Theme extra should be the same as original intent", "test_theme",
                nextIntent.getStringExtra("theme"));
        assertFalse("ignoreExtra should not be in nextIntent", nextIntent.hasExtra("ignoreExtra"));
        assertEquals("Result code extra should be RESULT_OK", Activity.RESULT_OK,
                nextIntent.getIntExtra("com.android.setupwizard.ResultCode", 0));
        assertEquals("Extra data should surface as extra in nextIntent", "shazam",
                nextIntent.getStringExtra("extraData"));
    }

    @Test
    public void testIsSetupWizardTrue() {
        final Intent intent = new Intent();
        intent.putExtra("firstRun", true);
        assertTrue("Is setup wizard should be true",
                WizardManagerHelper.isSetupWizardIntent(intent));
    }

    @Test
    public void testIsDeferredSetupTrue() {
        final Intent intent = new Intent();
        intent.putExtra("deferredSetup", true);
        assertTrue("Is deferred setup wizard should be true",
                WizardManagerHelper.isDeferredSetupWizard(intent));
    }

    @Test
    public void testIsPreDeferredSetupTrue() {
        final Intent intent = new Intent();
        intent.putExtra("preDeferredSetup", true);
        assertTrue("Is pre-deferred setup wizard should be true",
                WizardManagerHelper.isPreDeferredSetupWizard(intent));
    }

    @Test
    public void testIsSetupWizardFalse() {
        final Intent intent = new Intent();
        intent.putExtra("firstRun", false);
        assertFalse("Is setup wizard should be true",
                WizardManagerHelper.isSetupWizardIntent(intent));
    }

    @Test
    public void isLightTheme_shouldReturnTrue_whenThemeIsLight() {
        List<String> lightThemes = Arrays.asList(
                "holo_light",
                "material_light",
                "glif_light",
                "glif_v2_light",
                "glif_v3_light"
        );
        ArrayList<String> unexpectedIntentThemes = new ArrayList<>();
        ArrayList<String> unexpectedStringThemes = new ArrayList<>();
        for (final String theme : lightThemes) {
            Intent intent = new Intent();
            intent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
            if (!WizardManagerHelper.isLightTheme(intent, false)) {
                unexpectedIntentThemes.add(theme);
            }
            if (!WizardManagerHelper.isLightTheme(theme, false)) {
                unexpectedStringThemes.add(theme);
            }
        }
        assertTrue("Intent themes " + unexpectedIntentThemes + " should be light",
                unexpectedIntentThemes.isEmpty());
        assertTrue("String themes " + unexpectedStringThemes + " should be light",
                unexpectedStringThemes.isEmpty());
    }

    @Test
    public void isLightTheme_shouldReturnFalse_whenThemeIsNotLight() {
        List<String> lightThemes = Arrays.asList(
                "holo",
                "material",
                "glif",
                "glif_v2",
                "glif_v3"
        );
        ArrayList<String> unexpectedIntentThemes = new ArrayList<>();
        ArrayList<String> unexpectedStringThemes = new ArrayList<>();
        for (final String theme : lightThemes) {
            Intent intent = new Intent();
            intent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
            if (WizardManagerHelper.isLightTheme(intent, true)) {
                unexpectedIntentThemes.add(theme);
            }
            if (WizardManagerHelper.isLightTheme(theme, true)) {
                unexpectedStringThemes.add(theme);
            }
        }
        assertTrue("Intent themes " + unexpectedIntentThemes + " should not be light",
                unexpectedIntentThemes.isEmpty());
        assertTrue("String themes " + unexpectedStringThemes + " should not be light",
                unexpectedStringThemes.isEmpty());
    }

    @Test
    public void testIsLightThemeDefault() {
        final Intent intent = new Intent();
        intent.putExtra("theme", "abracadabra");
        assertTrue("isLightTheme should return default value true",
                WizardManagerHelper.isLightTheme(intent, true));
        assertFalse("isLightTheme should return default value false",
                WizardManagerHelper.isLightTheme(intent, false));
    }

    @Test
    public void testIsLightThemeUnspecified() {
        final Intent intent = new Intent();
        assertTrue("isLightTheme should return default value true",
                WizardManagerHelper.isLightTheme(intent, true));
        assertFalse("isLightTheme should return default value false",
                WizardManagerHelper.isLightTheme(intent, false));
    }

    @Test
    public void testGetThemeResGlifV3Light() {
        assertEquals(R.style.SuwThemeGlifV3_Light,
                WizardManagerHelper.getThemeRes("glif_v3_light", 0));
    }

    @Test
    public void testGetThemeResGlifV3() {
        assertEquals(R.style.SuwThemeGlifV3,
                WizardManagerHelper.getThemeRes("glif_v3", 0));
    }

    @Test
    public void testGetThemeResGlifV2Light() {
        assertEquals(R.style.SuwThemeGlifV2_Light,
                WizardManagerHelper.getThemeRes("glif_v2_light", 0));
    }

    @Test
    public void testGetThemeResGlifV2() {
        assertEquals(R.style.SuwThemeGlifV2,
                WizardManagerHelper.getThemeRes("glif_v2", 0));
    }

    @Test
    public void testGetThemeResGlifLight() {
        assertEquals(R.style.SuwThemeGlif_Light,
                WizardManagerHelper.getThemeRes("glif_light", 0));
    }

    @Test
    public void testGetThemeResGlif() {
        assertEquals(R.style.SuwThemeGlif,
                WizardManagerHelper.getThemeRes("glif", 0));
    }

    @Test
    public void testGetThemeResMaterialLight() {
        assertEquals(R.style.SuwThemeMaterial_Light,
                WizardManagerHelper.getThemeRes("material_light", 0));
    }

    @Test
    public void testGetThemeResMaterial() {
        assertEquals(R.style.SuwThemeMaterial,
                WizardManagerHelper.getThemeRes("material", 0));
    }

    @Test
    public void testGetThemeResDefault() {
        @StyleRes int def = 123;
        assertEquals(def, WizardManagerHelper.getThemeRes("abracadabra", def));
    }

    @Test
    public void testGetThemeResNull() {
        @StyleRes int def = 123;
        assertEquals(def, WizardManagerHelper.getThemeRes((String) null, def));
    }

    @Test
    public void testGetThemeResFromIntent() {
        Intent intent = new Intent();
        intent.putExtra(WizardManagerHelper.EXTRA_THEME, "material");
        assertEquals(R.style.SuwThemeMaterial, WizardManagerHelper.getThemeRes(intent, 0));
    }

    @Test
    public void testCopyWizardManagerIntent() {
        Bundle wizardBundle = new Bundle();
        wizardBundle.putString("foo", "bar");
        Intent originalIntent = new Intent()
                .putExtra(WizardManagerHelper.EXTRA_THEME, "test_theme")
                .putExtra(WizardManagerHelper.EXTRA_WIZARD_BUNDLE, wizardBundle)
                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                .putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, true)
                .putExtra(WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP, true)
                // Script URI and Action ID are kept for backwards compatibility
                .putExtra(WizardManagerHelper.EXTRA_SCRIPT_URI, "test_script_uri")
                .putExtra(WizardManagerHelper.EXTRA_ACTION_ID, "test_action_id");

        Intent intent = new Intent("test.intent.action");
        WizardManagerHelper.copyWizardManagerExtras(originalIntent, intent);

        assertEquals("Intent action should be kept", "test.intent.action", intent.getAction());
        assertEquals("EXTRA_THEME should be copied",
                "test_theme", intent.getStringExtra(WizardManagerHelper.EXTRA_THEME));
        Bundle copiedWizardBundle =
                intent.getParcelableExtra(WizardManagerHelper.EXTRA_WIZARD_BUNDLE);
        assertEquals("Wizard bundle should be copied", "bar", copiedWizardBundle.getString("foo"));

        assertTrue("EXTRA_IS_FIRST_RUN should be copied",
                intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, false));
        assertTrue("EXTRA_IS_DEFERRED_SETUP should be copied",
                intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, false));
        assertTrue("EXTRA_IS_PRE_DEFERRED_SETUP should be copied",
                intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP, false));

        // Script URI and Action ID are replaced by Wizard Bundle in M, but are kept for backwards
        // compatibility
        assertEquals("EXTRA_SCRIPT_URI should be copied",
                "test_script_uri", intent.getStringExtra(WizardManagerHelper.EXTRA_SCRIPT_URI));
        assertEquals("EXTRA_ACTION_ID should be copied",
                "test_action_id", intent.getStringExtra(WizardManagerHelper.EXTRA_ACTION_ID));
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void testIsUserSetupComplete() {
        Settings.Secure.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(application.getContentResolver(), "user_setup_complete", 1);
        assertTrue(WizardManagerHelper.isUserSetupComplete(application));

        Settings.Secure.putInt(application.getContentResolver(), "user_setup_complete", 0);
        assertFalse(WizardManagerHelper.isUserSetupComplete(application));
    }

    @Test
    @Config(sdk = VERSION_CODES.JELLY_BEAN)
    public void testIsUserSetupCompleteCompat() {
        Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 1);
        assertTrue(WizardManagerHelper.isUserSetupComplete(application));

        Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 0);
        assertFalse(WizardManagerHelper.isUserSetupComplete(application));
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void testIsDeviceProvisioned() {
        Settings.Secure.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        assertTrue(WizardManagerHelper.isDeviceProvisioned(application));
        Settings.Secure.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        assertFalse(WizardManagerHelper.isDeviceProvisioned(application));
    }

    @Test
    @Config(sdk = VERSION_CODES.JELLY_BEAN)
    public void testIsDeviceProvisionedCompat() {
        Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 1);
        assertTrue(WizardManagerHelper.isDeviceProvisioned(application));
        Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 0);
        assertFalse(WizardManagerHelper.isDeviceProvisioned(application));
    }
}
