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

package com.android.setupwizardlib.test;

import static android.support.test.InstrumentationRegistry.getTargetContext;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.support.test.filters.SmallTest;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.android.setupwizardlib.R;
import com.android.setupwizardlib.items.Item;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Sanity test for all the item layouts to make sure they won't crash when being inflated in
 * different themes.
 */
@RunWith(Parameterized.class)
@SmallTest
public class ItemLayoutTest {

    @Parameters
    public static Iterable<Object[]> data() {
        int[] themes = new int[] {
                R.style.SuwThemeMaterial_Light,
                R.style.SuwThemeMaterial,
                R.style.SuwThemeGlif_Light,
                R.style.SuwThemeGlif,
                R.style.SuwThemeGlifV2_Light,
                R.style.SuwThemeGlifV2
        };
        int[] layouts = new int[] {
                R.layout.suw_items_default,
                R.layout.suw_items_verbose,
                R.layout.suw_items_description
        };

        // Test all the possible combinations of themes and layouts.
        List<Object[]> params = new ArrayList<>();
        for (int theme : themes) {
            for (int layout : layouts) {
                params.add(new Object[] { theme, layout });
            }
        }
        return params;
    }

    private final Context mContext;
    private final FrameLayout mParent;
    private final Item mItem;

    public ItemLayoutTest(int theme, int layout) {
        mContext = new ContextThemeWrapper(getTargetContext(), theme);
        mParent = new FrameLayout(mContext);
        mItem = new Item();
        mItem.setLayoutResource(layout);
    }

    @Test
    public void testInflateLayoutHasBasicViews() {
        LayoutInflater.from(mContext).inflate(mItem.getLayoutResource(), mParent, true);
        mItem.onBindView(mParent);

        assertNotNull("Title should exist", mParent.findViewById(R.id.suw_items_title));
        assertNotNull("Summary should exist", mParent.findViewById(R.id.suw_items_summary));
        assertNotNull("Icon should exist", mParent.findViewById(R.id.suw_items_icon));
    }
}
