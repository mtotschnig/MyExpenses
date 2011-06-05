/*
 * http://code.google.com/p/android/issues/detail?id=9170
 * Based on android.widget.SimpleCursorTreeAdapter
 * Copyright (C) 2006 The Android Open Source Project
 * Modifications Copyright (C)2010 OzDroid Pty Ltd
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

package com.ozdroid.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorTreeAdapter.ViewBinder;

/**
 * An easy adapter to map columns from a cursor to TextViews or ImageViews
 * defined in an XML file. You can specify which columns you want, which views
 * you want to display the columns, and the XML file that defines the appearance
 * of these views. Separate XML files for child and groups are possible.
 *
 * Binding occurs in two phases. First, if a
 * {@link android.widget.SimpleCursorTreeAdapter.ViewBinder} is available,
 * {@link ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)}
 * is invoked. If the returned value is true, binding has occurred. If the
 * returned value is false and the view to bind is a TextView,
 * {@link #setViewText(TextView, String)} is invoked. If the returned value
 * is false and the view to bind is an ImageView,
 * {@link #setViewImage(ImageView, String)} is invoked. If no appropriate
 * binding can be found, an {@link IllegalStateException} is thrown.
 * @author Geoff Bruckner
 */
public abstract class SimpleCursorTreeAdapter2 extends SimpleCursorTreeAdapter {
    

	/** The  of columns that contain data to display for a child. */
    private String[] mChildFromNames;
	
    /** The indices of columns that contain data to display for a child. */
    private int[] mChildFrom;
   
    /**
     * The View IDs that will display a child's data fetched from the
     * corresponding column.
     */
    private int[] mChildTo;
    
 
    /**
     * Constructor.
     * 
     * @param context The context where the {@link ExpandableListView}
     *            associated with this {@link SimpleCursorTreeAdapter} is
     *            running
     * @param cursor The database cursor
     * @param collapsedGroupLayout The resource identifier of a layout file that
     *            defines the views for a collapsed group. The layout file
     *            should include at least those named views defined in groupTo.
     * @param expandedGroupLayout The resource identifier of a layout file that
     *            defines the views for an expanded group. The layout file
     *            should include at least those named views defined in groupTo.
     * @param groupFrom A list of column names that will be used to display the
     *            data for a group.
     * @param groupTo The group views (from the group layouts) that should
     *            display column in the "from" parameter. These should all be
     *            TextViews or ImageViews. The first N views in this list are
     *            given the values of the first N columns in the from parameter.
     * @param childLayout The resource identifier of a layout file that defines
     *            the views for a child (except the last). The layout file
     *            should include at least those named views defined in childTo.
     * @param lastChildLayout The resource identifier of a layout file that
     *            defines the views for the last child within a group. The
     *            layout file should include at least those named views defined
     *            in childTo.
     * @param childFrom A list of column names that will be used to display the
     *            data for a child.
     * @param childTo The child views (from the child layouts) that should
     *            display column in the "from" parameter. These should all be
     *            TextViews or ImageViews. The first N views in this list are
     *            given the values of the first N columns in the from parameter.
     */
    public SimpleCursorTreeAdapter2(Context context, Cursor cursor, int collapsedGroupLayout,
            int expandedGroupLayout, String[] groupFrom, int[] groupTo, int childLayout,
            int lastChildLayout, String[] childFrom, int[] childTo) {
        super( context, cursor, collapsedGroupLayout,
                expandedGroupLayout, groupFrom, groupTo, childLayout,
                lastChildLayout, childFrom, childTo);
        mChildFromNames = childFrom;
        mChildTo = childTo;
    }

    /**
     * Constructor.
     * 
     * @param context The context where the {@link ExpandableListView}
     *            associated with this {@link SimpleCursorTreeAdapter} is
     *            running
     * @param cursor The database cursor
     * @param collapsedGroupLayout The resource identifier of a layout file that
     *            defines the views for a collapsed group. The layout file
     *            should include at least those named views defined in groupTo.
     * @param expandedGroupLayout The resource identifier of a layout file that
     *            defines the views for an expanded group. The layout file
     *            should include at least those named views defined in groupTo.
     * @param groupFrom A list of column names that will be used to display the
     *            data for a group.
     * @param groupTo The group views (from the group layouts) that should
     *            display column in the "from" parameter. These should all be
     *            TextViews or ImageViews. The first N views in this list are
     *            given the values of the first N columns in the from parameter.
     * @param childLayout The resource identifier of a layout file that defines
     *            the views for a child. The layout file
     *            should include at least those named views defined in childTo.
     * @param childFrom A list of column names that will be used to display the
     *            data for a child.
     * @param childTo The child views (from the child layouts) that should
     *            display column in the "from" parameter. These should all be
     *            TextViews or ImageViews. The first N views in this list are
     *            given the values of the first N columns in the from parameter.
     */
    public SimpleCursorTreeAdapter2(Context context, Cursor cursor, int collapsedGroupLayout,
            int expandedGroupLayout, String[] groupFrom, int[] groupTo,
            int childLayout, String[] childFrom, int[] childTo) {
        super ( context, cursor, collapsedGroupLayout,
                expandedGroupLayout, groupFrom, groupTo,
                childLayout, childFrom, childTo);
        mChildFromNames = childFrom;
        mChildTo = childTo;
    }

    /**
     * Constructor.
     * 
     * @param context The context where the {@link ExpandableListView}
     *            associated with this {@link SimpleCursorTreeAdapter} is
     *            running
     * @param cursor The database cursor
     * @param groupLayout The resource identifier of a layout file that defines
     *            the views for a group. The layout file should include at least
     *            those named views defined in groupTo.
     * @param groupFrom A list of column names that will be used to display the
     *            data for a group.
     * @param groupTo The group views (from the group layouts) that should
     *            display column in the "from" parameter. These should all be
     *            TextViews or ImageViews. The first N views in this list are
     *            given the values of the first N columns in the from parameter.
     * @param childLayout The resource identifier of a layout file that defines
     *            the views for a child. The layout file should include at least
     *            those named views defined in childTo.
     * @param childFrom A list of column names that will be used to display the
     *            data for a child.
     * @param childTo The child views (from the child layouts) that should
     *            display column in the "from" parameter. These should all be
     *            TextViews or ImageViews. The first N views in this list are
     *            given the values of the first N columns in the from parameter.
     */
    public SimpleCursorTreeAdapter2(Context context, Cursor cursor, int groupLayout,
            String[] groupFrom, int[] groupTo, int childLayout, String[] childFrom,
            int[] childTo) {
        super(context, cursor, groupLayout,
                groupFrom, groupTo, childLayout, childFrom,
                 childTo);
        mChildFromNames = childFrom;
        mChildTo = childTo;
    }

    
    private void initFromColumns(Cursor cursor, String[] fromColumnNames, int[] fromColumns) {
        for (int i = fromColumnNames.length - 1; i >= 0; i--) {
            fromColumns[i] = cursor.getColumnIndexOrThrow(fromColumnNames[i]);
        }
    }
    

    private void initChildrenFromColumns(String[] childFromNames, Cursor childCursor) {
        mChildFrom = new int[childFromNames.length];
        initFromColumns(childCursor, childFromNames, mChildFrom);
    }


    private void bindView(View view, Context context, Cursor cursor, int[] from, int[] to) {
        final ViewBinder binder = getViewBinder();
        
        for (int i = 0; i < to.length; i++) {
            View v = view.findViewById(to[i]);
            if (v != null) {
                boolean bound = false;
                if (binder != null) {
                    bound = binder.setViewValue(v, cursor, from[i]);
                }
                
                if (!bound) {
                    String text = cursor.getString(from[i]);
                    if (text == null) {
                        text = "";
                    }
                    if (v instanceof TextView) {
                        setViewText((TextView) v, text);
                    } else if (v instanceof ImageView) {
                        setViewImage((ImageView) v, text);
                    } else {
                        throw new IllegalStateException("SimpleCursorTreeAdapter can bind values" +
                                " only to TextView and ImageView!");
                    }
                }
            }
        }
    }
    
    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
    	if(mChildFrom == null){
    		initChildrenFromColumns(mChildFromNames, cursor);
    	}
        bindView(view, context, cursor, mChildFrom, mChildTo);
    }


}
