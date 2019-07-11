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

package com.android.setupwizardlib.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.annotation.NonNull;

/**
 * Button for navigation bar, which includes tinting of its compound drawables to be used for dark
 * and light themes.
 */
@SuppressLint("AppCompatCustomView")
public class NavigationBarButton extends Button {

    public NavigationBarButton(Context context) {
        super(context);
        init();
    }

    public NavigationBarButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Unfortunately, drawableStart and drawableEnd set through XML does not call the setter,
        // so manually getting it and wrapping it in the compat drawable.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Drawable[] drawables = getCompoundDrawablesRelative();
            for (int i = 0; i < drawables.length; i++) {
                if (drawables[i] != null) {
                    drawables[i] = TintedDrawable.wrap(drawables[i]);
                }
            }
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawables[0], drawables[1],
                    drawables[2], drawables[3]);
        }
    }

    @Override
    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        if (left != null) left = TintedDrawable.wrap(left);
        if (top != null) top = TintedDrawable.wrap(top);
        if (right != null) right = TintedDrawable.wrap(right);
        if (bottom != null) bottom = TintedDrawable.wrap(bottom);
        super.setCompoundDrawables(left, top, right, bottom);
        tintDrawables();
    }

    @Override
    public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end,
            Drawable bottom) {
        if (start != null) start = TintedDrawable.wrap(start);
        if (top != null) top = TintedDrawable.wrap(top);
        if (end != null) end = TintedDrawable.wrap(end);
        if (bottom != null) bottom = TintedDrawable.wrap(bottom);
        super.setCompoundDrawablesRelative(start, top, end, bottom);
        tintDrawables();
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        super.setTextColor(colors);
        tintDrawables();
    }

    private void tintDrawables() {
        final ColorStateList textColors = getTextColors();
        if (textColors != null) {
            for (Drawable drawable : getAllCompoundDrawables()) {
                if (drawable instanceof TintedDrawable) {
                    ((TintedDrawable) drawable).setTintListCompat(textColors);
                }
            }
            invalidate();
        }
    }

    private Drawable[] getAllCompoundDrawables() {
        Drawable[] drawables = new Drawable[6];
        Drawable[] compoundDrawables = getCompoundDrawables();
        drawables[0] = compoundDrawables[0];  // left
        drawables[1] = compoundDrawables[1];  // top
        drawables[2] = compoundDrawables[2];  // right
        drawables[3] = compoundDrawables[3];  // bottom
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Drawable[] compoundDrawablesRelative = getCompoundDrawablesRelative();
            drawables[4] = compoundDrawablesRelative[0];  // start
            drawables[5] = compoundDrawablesRelative[2];  // end
        }
        return drawables;
    }

    // TODO: Remove this class and use DrawableCompat.wrap() once we can use support library 22.1.0
    // or above
    private static class TintedDrawable extends LayerDrawable {

        public static TintedDrawable wrap(Drawable drawable) {
            if (drawable instanceof TintedDrawable) {
                return (TintedDrawable) drawable;
            }
            return new TintedDrawable(drawable.mutate());
        }

        private ColorStateList mTintList = null;

        TintedDrawable(Drawable wrapped) {
            super(new Drawable[] { wrapped });
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        public boolean setState(@NonNull int[] stateSet) {
            boolean needsInvalidate = super.setState(stateSet);
            boolean needsInvalidateForState = updateState();
            return needsInvalidate || needsInvalidateForState;
        }

        public void setTintListCompat(ColorStateList colors) {
            mTintList = colors;
            if (updateState()) {
                invalidateSelf();
            }
        }

        private boolean updateState() {
            if (mTintList != null) {
                final int color = mTintList.getColorForState(getState(), 0);
                setColorFilter(color, PorterDuff.Mode.SRC_IN);
                return true;  // Needs invalidate
            }
            return false;
        }
    }
}
