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

package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.Keep;
import androidx.annotation.LayoutRes;
import androidx.annotation.StyleRes;

import com.android.setupwizardlib.template.Mixin;
import com.android.setupwizardlib.util.FallbackThemeWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic template class that inflates a template, provided in the constructor or in
 * {@code android:layout} through XML, and adds its children to a "container" in the template. When
 * inflating this layout from XML, the {@code android:layout} and {@code suwContainer} attributes
 * are required.
 */
public class TemplateLayout extends FrameLayout {

    /**
     * The container of the actual content. This will be a view in the template, which child views
     * will be added to when {@link #addView(View)} is called.
     */
    private ViewGroup mContainer;

    private Map<Class<? extends Mixin>, Mixin> mMixins = new HashMap<>();

    public TemplateLayout(Context context, int template, int containerId) {
        super(context);
        init(template, containerId, null, R.attr.suwLayoutTheme);
    }

    public TemplateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(0, 0, attrs, R.attr.suwLayoutTheme);
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public TemplateLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(0, 0, attrs, defStyleAttr);
    }

    // All the constructors delegate to this init method. The 3-argument constructor is not
    // available in LinearLayout before v11, so call super with the exact same arguments.
    private void init(int template, int containerId, AttributeSet attrs, int defStyleAttr) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.SuwTemplateLayout, defStyleAttr, 0);
        if (template == 0) {
            template = a.getResourceId(R.styleable.SuwTemplateLayout_android_layout, 0);
        }
        if (containerId == 0) {
            containerId = a.getResourceId(R.styleable.SuwTemplateLayout_suwContainer, 0);
        }
        inflateTemplate(template, containerId);

        a.recycle();
    }

    /**
     * Registers a mixin with a given class. This method should be called in the constructor.
     *
     * @param cls The class to register the mixin. In most cases, {@code cls} is the same as
     *            {@code mixin.getClass()}, but {@code cls} can also be a super class of that. In
     *            the latter case the the mixin must be retrieved using {@code cls} in
     *            {@link #getMixin(Class)}, not the subclass.
     * @param mixin The mixin to be registered.
     * @param <M> The class of the mixin to register. This is the same as {@code cls}
     */
    protected <M extends Mixin> void registerMixin(Class<M> cls, M mixin) {
        mMixins.put(cls, mixin);
    }

    /**
     * Same as {@link android.view.View#findViewById(int)}, but may include views that are managed
     * by this view but not currently added to the view hierarchy. e.g. recycler view or list view
     * headers that are not currently shown.
     */
    // Returning generic type is the common pattern used for findViewBy* methods
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T extends View> T findManagedViewById(int id) {
        return findViewById(id);
    }

    /**
     * Get a {@link Mixin} from this template registered earlier in
     * {@link #registerMixin(Class, Mixin)}.
     *
     * @param cls The class marker of Mixin being requested. The actual Mixin returned may be a
     *            subclass of this marker. Note that this must be the same class as registered in
     *            {@link #registerMixin(Class, Mixin)}, which is not necessarily the
     *            same as the concrete class of the instance returned by this method.
     * @param <M> The type of the class marker.
     * @return The mixin marked by {@code cls}, or null if the template does not have a matching
     *         mixin.
     */
    @SuppressWarnings("unchecked")
    public <M extends Mixin> M getMixin(Class<M> cls) {
        return (M) mMixins.get(cls);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        mContainer.addView(child, index, params);
    }

    private void addViewInternal(View child) {
        super.addView(child, -1, generateDefaultLayoutParams());
    }

    private void inflateTemplate(int templateResource, int containerId) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View templateRoot = onInflateTemplate(inflater, templateResource);
        addViewInternal(templateRoot);

        mContainer = findContainer(containerId);
        if (mContainer == null) {
            throw new IllegalArgumentException("Container cannot be null in TemplateLayout");
        }
        onTemplateInflated();
    }

    /**
     * This method inflates the template. Subclasses can override this method to customize the
     * template inflation, or change to a different default template. The root of the inflated
     * layout should be returned, and not added to the view hierarchy.
     *
     * @param inflater A LayoutInflater to inflate the template.
     * @param template The resource ID of the template to be inflated, or 0 if no template is
     *                 specified.
     * @return Root of the inflated layout.
     */
    protected View onInflateTemplate(LayoutInflater inflater, @LayoutRes int template) {
        return inflateTemplate(inflater, 0, template);
    }

    /**
     * Inflate the template using the given inflater and theme. The fallback theme will be applied
     * to the theme without overriding the values already defined in the theme, but simply providing
     * default values for values which have not been defined. This allows templates to add
     * additional required theme attributes without breaking existing clients.
     *
     * <p>In general, clients should still set the activity theme to the corresponding theme in
     * setup wizard lib, so that the content area gets the correct styles as well.
     *
     * @param inflater A LayoutInflater to inflate the template.
     * @param fallbackTheme A fallback theme to apply to the template. If the values defined in the
     *                      fallback theme is already defined in the original theme, the value in
     *                      the original theme takes precedence.
     * @param template The layout template to be inflated.
     * @return Root of the inflated layout.
     *
     * @see FallbackThemeWrapper
     */
    protected final View inflateTemplate(LayoutInflater inflater, @StyleRes int fallbackTheme,
            @LayoutRes int template) {
        if (template == 0) {
            throw new IllegalArgumentException("android:layout not specified for TemplateLayout");
        }
        if (fallbackTheme != 0) {
            inflater = LayoutInflater.from(
                    new FallbackThemeWrapper(inflater.getContext(), fallbackTheme));
        }
        return inflater.inflate(template, this, false);
    }

    protected ViewGroup findContainer(int containerId) {
        if (containerId == 0) {
            // Maintain compatibility with the deprecated way of specifying container ID.
            containerId = getContainerId();
        }
        return (ViewGroup) findViewById(containerId);
    }

    /**
     * This is called after the template has been inflated and added to the view hierarchy.
     * Subclasses can implement this method to modify the template as necessary, such as caching
     * views retrieved from findViewById, or other view operations that need to be done in code.
     * You can think of this as {@link View#onFinishInflate()} but for inflation of the
     * template instead of for child views.
     */
    protected void onTemplateInflated() {
    }

    /**
     * @return ID of the default container for this layout. This will be used to find the container
     * ViewGroup, which all children views of this layout will be placed in.
     * @deprecated Override {@link #findContainer(int)} instead.
     */
    @Deprecated
    protected int getContainerId() {
        return 0;
    }

    /* Animator support */

    private float mXFraction;
    private ViewTreeObserver.OnPreDrawListener mPreDrawListener;

    /**
     * Set the X translation as a fraction of the width of this view. Make sure this method is not
     * stripped out by proguard when using this with {@link android.animation.ObjectAnimator}. You
     * may need to add
     * <code>
     *     -keep @androidx.annotation.Keep class *
     * </code>
     * to your proguard configuration if you are seeing mysterious {@link NoSuchMethodError} at
     * runtime.
     */
    @Keep
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void setXFraction(float fraction) {
        mXFraction = fraction;
        final int width = getWidth();
        if (width != 0) {
            setTranslationX(width * fraction);
        } else {
            // If we haven't done a layout pass yet, wait for one and then set the fraction before
            // the draw occurs using an OnPreDrawListener. Don't call translationX until we know
            // getWidth() has a reliable, non-zero value or else we will see the fragment flicker on
            // screen.
            if (mPreDrawListener == null) {
                mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
                        setXFraction(mXFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
            }
        }
    }

    /**
     * Return the X translation as a fraction of the width, as previously set in
     * {@link #setXFraction(float)}.
     *
     * @see #setXFraction(float)
     */
    @Keep
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public float getXFraction() {
        return mXFraction;
    }
}
