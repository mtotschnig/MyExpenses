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

package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;
import android.view.InflateException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * An XML inflater that creates items by reading the tag as a class name, and constructs said class
 * by invoking the 2-argument constructor {@code Constructor(Context, AttributeSet)} via reflection.
 *
 * <p>Optionally a "default package" can be specified so that for unqualified tag names (i.e. names
 * that do not contain "."), the default package will be prefixed onto the tag.
 *
 * @param <T> The class where all instances (including child elements) belong to. If parent and
 *     child elements belong to different class hierarchies, it's OK to set this to {@link Object}.
 */
public abstract class ReflectionInflater<T> extends SimpleInflater<T> {

    /* static section */

    private static final Class<?>[] CONSTRUCTOR_SIGNATURE =
            new Class<?>[] {Context.class, AttributeSet.class};

    private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap<>();

    /* non-static section */

    // Array used to contain the constructor arguments (Context, AttributeSet), to avoid allocating
    // a new array for creation of every item.
    private final Object[] mTempConstructorArgs = new Object[2];

    @Nullable
    private String mDefaultPackage;

    @NonNull
    private final Context mContext;

    /**
     * Create a new inflater instance associated with a particular Context.
     *
     * @param context The context used to resolve resource IDs. This context is also passed to the
     *     constructor of the items created as the first argument.
     */
    protected ReflectionInflater(@NonNull Context context) {
        super(context.getResources());
        mContext = context;
    }

    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Instantiate the class by name. This attempts to instantiate class of the given {@code name}
     * found in this inflater's ClassLoader.
     *
     * @param tagName The full name of the class to be instantiated.
     * @param attrs The XML attributes supplied for this instance.
     *
     * @return The newly instantiated item.
     */
    @NonNull
    public final T createItem(String tagName, String prefix, AttributeSet attrs) {
        String qualifiedName = tagName;
        if (prefix != null && qualifiedName.indexOf('.') == -1) {
            qualifiedName = prefix.concat(qualifiedName);
        }
        @SuppressWarnings("unchecked") // qualifiedName should correspond to a subclass of T
        Constructor<? extends T> constructor =
                (Constructor<? extends T>) sConstructorMap.get(qualifiedName);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real, and try to add it
                @SuppressWarnings("unchecked") // qualifiedName should correspond to a subclass of T
                Class<? extends T> clazz =
                        (Class<? extends T>) mContext.getClassLoader().loadClass(qualifiedName);
                constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
                constructor.setAccessible(true);
                sConstructorMap.put(tagName, constructor);
            }

            mTempConstructorArgs[0] = mContext;
            mTempConstructorArgs[1] = attrs;
            final T item = constructor.newInstance(mTempConstructorArgs);
            mTempConstructorArgs[0] = null;
            mTempConstructorArgs[1] = null;
            return item;
        } catch (Exception e) {
            throw new InflateException(attrs.getPositionDescription()
                    + ": Error inflating class " + qualifiedName, e);
        }
    }

    @Override
    protected T onCreateItem(String tagName, AttributeSet attrs) {
        return createItem(tagName, mDefaultPackage, attrs);
    }

    /**
     * Sets the default package that will be searched for classes to construct for tag names that
     * have no explicit package.
     *
     * @param defaultPackage The default package. This will be prepended to the tag name, so it
     *     should end with a period.
     */
    public void setDefaultPackage(@Nullable String defaultPackage) {
        mDefaultPackage = defaultPackage;
    }

    /**
     * Returns the default package, or null if it is not set.
     *
     * @see #setDefaultPackage(String)
     * @return The default package.
     */
    @Nullable
    public String getDefaultPackage() {
        return mDefaultPackage;
    }
}
