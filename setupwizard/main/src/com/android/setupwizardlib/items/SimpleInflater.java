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

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.InflateException;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * A simple XML inflater, which takes care of moving the parser to the correct position. Subclasses
 * need to implement {@link #onCreateItem(String, AttributeSet)} to create an object representation
 * and {@link #onAddChildItem(Object, Object)} to attach a child tag to the parent tag.
 *
 * @param <T> The class where all instances (including child elements) belong to. If parent and
 *     child elements belong to different class hierarchies, it's OK to set this to {@link Object}.
 */
public abstract class SimpleInflater<T> {

    private static final String TAG = "SimpleInflater";
    private static final boolean DEBUG = false;

    protected final Resources mResources;

    /**
     * Create a new inflater instance associated with a particular Resources bundle.
     *
     * @param resources The Resources class used to resolve given resource IDs.
     */
    protected SimpleInflater(@NonNull Resources resources) {
        mResources = resources;
    }

    public Resources getResources() {
        return mResources;
    }

    /**
     * Inflate a new hierarchy from the specified XML resource. Throws InflaterException if there is
     * an error.
     *
     * @param resId ID for an XML resource to load (e.g. <code>R.xml.my_xml</code>)
     * @return The root of the inflated hierarchy.
     */
    public T inflate(int resId) {
        XmlResourceParser parser = getResources().getXml(resId);
        try {
            return inflate(parser);
        } finally {
            parser.close();
        }
    }

    /**
     * Inflate a new hierarchy from the specified XML node. Throws InflaterException if there is an
     * error.
     * <p>
     * <em><strong>Important</strong></em>&nbsp;&nbsp;&nbsp;For performance
     * reasons, inflation relies heavily on pre-processing of XML files
     * that is done at build time. Therefore, it is not currently possible to
     * use inflater with an XmlPullParser over a plain XML file at runtime.
     *
     * @param parser XML dom node containing the description of the hierarchy.
     * @return The root of the inflated hierarchy.
     */
    public T inflate(XmlPullParser parser) {
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        T createdItem;

        try {
            // Look for the root node.
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // continue
            }

            if (type != XmlPullParser.START_TAG) {
                throw new InflateException(parser.getPositionDescription()
                        + ": No start tag found!");
            }

            createdItem = createItemFromTag(parser.getName(), attrs);

            rInflate(parser, createdItem, attrs);
        } catch (XmlPullParserException e) {
            throw new InflateException(e.getMessage(), e);
        } catch (IOException e) {
            throw new InflateException(parser.getPositionDescription() + ": " + e.getMessage(), e);
        }

        return createdItem;
    }

    /**
     * This routine is responsible for creating the correct subclass of item
     * given the xml element name.
     *
     * @param tagName The XML tag name for the item to be created.
     * @param attrs An AttributeSet of attributes to apply to the item.
     * @return The item created.
     */
    protected abstract T onCreateItem(String tagName, AttributeSet attrs);

    private T createItemFromTag(String name, AttributeSet attrs) {
        try {
            T item = onCreateItem(name, attrs);
            if (DEBUG) Log.v(TAG, item + " created for <" + name + ">");
            return item;
        } catch (InflateException e) {
            throw e;
        } catch (Exception e) {
            throw new InflateException(attrs.getPositionDescription()
                    + ": Error inflating class " + name, e);
        }
    }

    /**
     * Recursive method used to descend down the xml hierarchy and instantiate
     * items, instantiate their children, and then call onFinishInflate().
     */
    private void rInflate(XmlPullParser parser, T parent, final AttributeSet attrs)
            throws XmlPullParserException, IOException {
        final int depth = parser.getDepth();

        int type;
        while (((type = parser.next()) != XmlPullParser.END_TAG
                || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (onInterceptCreateItem(parser, parent, attrs)) {
                continue;
            }

            String name = parser.getName();
            T item = createItemFromTag(name, attrs);

            onAddChildItem(parent, item);

            rInflate(parser, item, attrs);
        }
    }

    /**
     * Whether item creation should be intercepted to perform custom handling on the parser rather
     * than creating an object from it. This is used in rare cases where a tag doesn't correspond
     * to creation of an object.
     *
     * The parser will be pointing to the start of a tag, you must stop parsing and return when you
     * reach the end of this element. That is, this method is responsible for parsing the element
     * at the given position together with all of its child tags.
     *
     * Note that parsing of the root tag cannot be intercepted.
     *
     * @param parser XML dom node containing the description of the hierarchy.
     * @param parent The item that should be the parent of whatever you create.
     * @param attrs An AttributeSet of attributes to apply to the item.
     * @return True to continue parsing without calling {@link #onCreateItem(String, AttributeSet)},
     *     or false if this inflater should proceed to create an item.
     */
    protected boolean onInterceptCreateItem(XmlPullParser parser, T parent, AttributeSet attrs)
            throws XmlPullParserException {
        return false;
    }

    protected abstract void onAddChildItem(T parent, T child);
}
