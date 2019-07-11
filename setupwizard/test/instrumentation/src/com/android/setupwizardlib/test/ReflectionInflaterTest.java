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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import androidx.annotation.NonNull;

import com.android.setupwizardlib.items.ReflectionInflater;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ReflectionInflaterTest {

    @Test
    public void testInflateXml() {
        final Context context = InstrumentationRegistry.getContext();
        TestInflater inflater = new TestInflater(context);
        final Animation result = inflater.inflate(R.xml.reflection_inflater_test);

        assertTrue(result instanceof AnimationSet);
        final AnimationSet set = (AnimationSet) result;
        final List<Animation> animations = set.getAnimations();
        assertEquals(1, animations.size());
        assertTrue(animations.get(0) instanceof ScaleAnimation);
    }

    @Test
    public void testDefaultPackage() {
        final Context context = InstrumentationRegistry.getContext();
        TestInflater inflater = new TestInflater(context);
        inflater.setDefaultPackage("android.view.animation.");
        final Animation result =
                inflater.inflate(R.xml.reflection_inflater_test_with_default_package);

        assertTrue(result instanceof AnimationSet);
        final AnimationSet set = (AnimationSet) result;
        final List<Animation> animations = set.getAnimations();
        assertEquals(1, animations.size());
        assertTrue(animations.get(0) instanceof ScaleAnimation);
    }

    private static class TestInflater extends ReflectionInflater<Animation> {

        protected TestInflater(@NonNull Context context) {
            super(context);
        }

        @Override
        protected void onAddChildItem(Animation parent, Animation child) {
            final AnimationSet group = (AnimationSet) parent;
            group.addAnimation(child);
        }
    }
}
