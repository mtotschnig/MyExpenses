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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;
import com.android.setupwizardlib.util.Partner.ResourceEntry;
import com.android.setupwizardlib.util.PartnerTest.ShadowApplicationPackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowResources;

import java.util.Arrays;
import java.util.Collections;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(
        sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK },
        shadows = ShadowApplicationPackageManager.class)
public class PartnerTest {

    private static final String ACTION_PARTNER_CUSTOMIZATION =
            "com.android.setupwizard.action.PARTNER_CUSTOMIZATION";

    private Context mContext;
    private Resources mPartnerResources;

    private ShadowApplicationPackageManager mPackageManager;

    @Before
    public void setUp() throws Exception {
        Partner.resetForTesting();

        mContext = spy(application);
        mPartnerResources = spy(ShadowResources.getSystem());

        mPackageManager =
                (ShadowApplicationPackageManager) Shadows.shadowOf(application.getPackageManager());
        mPackageManager.partnerResources = mPartnerResources;
    }

    @Test
    public void testLoadPartner() {
        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Arrays.asList(
                        createResolveInfo("foo.bar", false, true),
                        createResolveInfo("test.partner.package", true, true))
        );

        Partner partner = Partner.get(mContext);
        assertNotNull("Partner should not be null", partner);
    }

    @Test
    public void testLoadNoPartner() {
        Partner partner = Partner.get(mContext);
        assertNull("Partner should be null", partner);
    }

    @Test
    public void testLoadNonSystemPartner() {
        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Arrays.asList(
                        createResolveInfo("foo.bar", false, true),
                        createResolveInfo("test.partner.package", false, true))
        );

        Partner partner = Partner.get(mContext);
        assertNull("Partner should be null", partner);
    }

    @Test
    public void testLoadPartnerValue() {
        doReturn(0x7f010000).when(mPartnerResources)
                .getIdentifier(eq("suwTransitionDuration"), eq("integer"), anyString());
        doReturn(5000).when(mPartnerResources).getInteger(eq(0x7f010000));

        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Arrays.asList(
                        createResolveInfo("foo.bar", false, true),
                        createResolveInfo("test.partner.package", true, true))
        );

        ResourceEntry entry = Partner.getResourceEntry(mContext, R.integer.suwTransitionDuration);
        int partnerValue = entry.resources.getInteger(entry.id);
        assertEquals("Partner value should be overlaid to 5000", 5000, partnerValue);
        assertTrue("Partner value should come from overlay", entry.isOverlay);
    }

    @Test
    public void getColor_shouldReturnPartnerValueIfPresent() {
        final int expectedPartnerColor = 1111;
        doReturn(12345).when(mPartnerResources)
                .getIdentifier(eq("suw_color_accent_dark"), eq("color"), anyString());
        doReturn(expectedPartnerColor).when(mPartnerResources).getColor(eq(12345));
        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Arrays.asList(createResolveInfo("test.partner.package", true, true)));
        final int foundColor = Partner.getColor(mContext, R.color.suw_color_accent_dark);
        assertEquals("Partner color should be overlayed to: " + expectedPartnerColor,
                expectedPartnerColor, foundColor);
    }

    @Test
    public void getText_shouldReturnPartnerValueIfPresent() {
        final CharSequence expectedPartnerText = "partner";
        doReturn(12345).when(mPartnerResources)
                .getIdentifier(eq("suw_next_button_label"), eq("string"), anyString());
        doReturn(expectedPartnerText).when(mPartnerResources).getText(eq(12345));
        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Collections.singletonList(createResolveInfo("test.partner.package", true, true)));
        final CharSequence partnerText = Partner.getText(mContext, R.string.suw_next_button_label);
        assertThat(partnerText).isEqualTo(expectedPartnerText);
    }

    @Test
    public void testLoadDefaultValue() {
        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Arrays.asList(
                        createResolveInfo("foo.bar", false, true),
                        createResolveInfo("test.partner.package", true, true))
        );

        ResourceEntry entry = Partner.getResourceEntry(mContext, R.color.suw_color_accent_dark);
        int partnerValue = entry.resources.getColor(entry.id);
        assertEquals("Partner value should default to 0xff448aff", 0xff448aff, partnerValue);
        assertFalse("Partner value should come from fallback", entry.isOverlay);
    }

    @Test
    public void testNotDirectBootAware() {
        mPackageManager.addResolveInfoForIntent(
                new Intent(ACTION_PARTNER_CUSTOMIZATION),
                Collections.singletonList(createResolveInfo("test.partner.package", true, false)));

        ResourceEntry entry = Partner.getResourceEntry(mContext, R.color.suw_color_accent_dark);
        int partnerValue = entry.resources.getColor(entry.id);
        assertEquals("Partner value should default to 0xff448aff", 0xff448aff, partnerValue);
        assertFalse("Partner value should come from fallback", entry.isOverlay);
    }

    private ResolveInfo createResolveInfo(
            String packageName,
            boolean isSystem,
            boolean directBootAware) {
        ResolveInfo info = new ResolveInfo();
        info.resolvePackageName = packageName;
        ActivityInfo activityInfo = new ActivityInfo();
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.flags = isSystem ? ApplicationInfo.FLAG_SYSTEM : 0;
        appInfo.packageName = packageName;
        activityInfo.applicationInfo = appInfo;
        activityInfo.packageName = packageName;
        activityInfo.name = packageName;
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            activityInfo.directBootAware = directBootAware;
        }
        info.activityInfo = activityInfo;
        return info;
    }

    @Implements(className = "android.app.ApplicationPackageManager")
    public static class ShadowApplicationPackageManager extends
            org.robolectric.shadows.ShadowApplicationPackageManager {

        public Resources partnerResources;

        @Implementation
        @Override
        public Resources getResourcesForApplication(ApplicationInfo app)
                throws NameNotFoundException {
            if (app != null && "test.partner.package".equals(app.packageName)) {
                return partnerResources;
            } else {
                return super.getResourcesForApplication(app);
            }
        }
    }
}
