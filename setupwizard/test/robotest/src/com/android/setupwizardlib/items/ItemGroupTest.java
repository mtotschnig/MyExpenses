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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.android.setupwizardlib.robolectric.SuwLibRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SuwLibRobolectricTestRunner.class)
@Config(sdk = { Config.OLDEST_SDK, Config.NEWEST_SDK })
public class ItemGroupTest {

    private static final Item CHILD_1 = new EqualsItem("Child 1");
    private static final Item CHILD_2 = new EqualsItem("Child 2");
    private static final Item CHILD_3 = new EqualsItem("Child 3");
    private static final Item CHILD_4 = new EqualsItem("Child 4");

    private ItemGroup mItemGroup;

    @Mock
    private ItemHierarchy.Observer mObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mItemGroup = new ItemGroup();
        mItemGroup.registerObserver(mObserver);
    }

    @Test
    public void testGroup() {
        mItemGroup.addChild(CHILD_1);
        mItemGroup.addChild(CHILD_2);

        assertSame("Item at position 0 should be child1", CHILD_1, mItemGroup.getItemAt(0));
        assertSame("Item at position 1 should be child2", CHILD_2, mItemGroup.getItemAt(1));
        assertEquals("Should have 2 children", 2, mItemGroup.getCount());

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(mItemGroup), eq(0), eq(1));
        inOrder.verify(mObserver).onItemRangeInserted(eq(mItemGroup), eq(1), eq(1));
    }

    @Test
    public void testRemoveChild() {
        mItemGroup.addChild(CHILD_1);
        mItemGroup.addChild(CHILD_2);
        mItemGroup.addChild(CHILD_3);

        mItemGroup.removeChild(CHILD_2);

        assertSame("Item at position 0 should be child1", CHILD_1, mItemGroup.getItemAt(0));
        assertSame("Item at position 1 should be child3", CHILD_3, mItemGroup.getItemAt(1));
        assertEquals("Should have 2 children", 2, mItemGroup.getCount());

        verify(mObserver).onItemRangeRemoved(eq(mItemGroup), eq(1), eq(1));
    }

    @Test
    public void testClear() {
        mItemGroup.addChild(CHILD_1);
        mItemGroup.addChild(CHILD_2);

        mItemGroup.clear();

        assertEquals("Should have 0 child", 0, mItemGroup.getCount());

        verify(mObserver).onItemRangeRemoved(eq(mItemGroup), eq(0), eq(2));
    }

    @Test
    public void testNestedGroup() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup = new ItemGroup();
        parentGroup.registerObserver(mObserver);

        parentGroup.addChild(CHILD_1);
        childGroup.addChild(CHILD_2);
        childGroup.addChild(CHILD_3);
        parentGroup.addChild(childGroup);
        parentGroup.addChild(CHILD_4);

        assertSame("Position 0 should be child 1", CHILD_1, parentGroup.getItemAt(0));
        assertSame("Position 1 should be child 2", CHILD_2, parentGroup.getItemAt(1));
        assertSame("Position 2 should be child 3", CHILD_3, parentGroup.getItemAt(2));
        assertSame("Position 3 should be child 4", CHILD_4, parentGroup.getItemAt(3));

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(3), eq(1));
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testNestedGroupClearNotification() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup = new ItemGroup();
        parentGroup.registerObserver(mObserver);

        parentGroup.addChild(CHILD_1);
        childGroup.addChild(CHILD_2);
        childGroup.addChild(CHILD_3);
        parentGroup.addChild(childGroup);
        parentGroup.addChild(CHILD_4);

        childGroup.clear();

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(3), eq(1));
        verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(1), eq(2));
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testNestedGroupRemoveNotification() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup = new ItemGroup();
        parentGroup.registerObserver(mObserver);

        parentGroup.addChild(CHILD_1);
        childGroup.addChild(CHILD_2);
        childGroup.addChild(CHILD_3);
        parentGroup.addChild(childGroup);
        parentGroup.addChild(CHILD_4);

        childGroup.removeChild(CHILD_3);
        childGroup.removeChild(CHILD_2);

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(3), eq(1));
        inOrder.verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(2), eq(1));
        inOrder.verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(1), eq(1));
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testNestedGroupClear() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup = new ItemGroup();
        parentGroup.registerObserver(mObserver);

        parentGroup.addChild(CHILD_1);
        childGroup.addChild(CHILD_2);
        childGroup.addChild(CHILD_3);
        parentGroup.addChild(childGroup);

        childGroup.clear();

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
        inOrder.verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(1), eq(2));
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testNestedGroupRemoveLastChild() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup1 = new ItemGroup();
        ItemGroup childGroup2 = new ItemGroup();
        parentGroup.registerObserver(mObserver);

        childGroup1.addChild(CHILD_1);
        childGroup1.addChild(CHILD_2);
        parentGroup.addChild(childGroup1);
        childGroup2.addChild(CHILD_3);
        childGroup2.addChild(CHILD_4);
        parentGroup.addChild(childGroup2);

        childGroup2.removeChild(CHILD_4);
        childGroup2.removeChild(CHILD_3);

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(0), eq(2));
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(2), eq(2));
        inOrder.verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(3), eq(1));
        inOrder.verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(2), eq(1));
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testNestedGroupClearOnlyChild() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup = new ItemGroup();
        parentGroup.registerObserver(mObserver);

        childGroup.addChild(CHILD_1);
        childGroup.addChild(CHILD_2);
        parentGroup.addChild(childGroup);

        childGroup.clear();

        final InOrder inOrder = inOrder(mObserver);
        inOrder.verify(mObserver).onItemRangeInserted(eq(parentGroup), eq(0), eq(2));
        inOrder.verify(mObserver).onItemRangeRemoved(eq(parentGroup), eq(0), eq(2));
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testNotifyChange() {
        mItemGroup.addChild(CHILD_1);
        mItemGroup.addChild(CHILD_2);

        CHILD_2.setTitle("Child 2 modified");

        verify(mObserver).onItemRangeChanged(eq(mItemGroup), eq(1), eq(1));
    }

    @Test
    public void testEmptyChildGroup() {
        ItemGroup parentGroup = new ItemGroup();
        ItemGroup childGroup = new ItemGroup();

        parentGroup.addChild(CHILD_1);
        parentGroup.addChild(childGroup);
        parentGroup.addChild(CHILD_2);

        assertSame("Position 0 should be child 1", CHILD_1, parentGroup.getItemAt(0));
        assertSame("Position 1 should be child 2", CHILD_2, parentGroup.getItemAt(1));
    }

    @Test
    public void testFindItemById() {
        CHILD_1.setId(12345);
        CHILD_2.setId(23456);

        mItemGroup.addChild(CHILD_1);
        mItemGroup.addChild(CHILD_2);

        assertSame("Find item 23456 should return child 2",
                CHILD_2, mItemGroup.findItemById(23456));
    }

    @Test
    public void testFindItemByIdNotFound() {
        CHILD_1.setId(12345);
        CHILD_2.setId(23456);

        mItemGroup.addChild(CHILD_1);
        mItemGroup.addChild(CHILD_2);

        assertNull("ID not found should return null", mItemGroup.findItemById(56789));
    }

    /**
     * This class will always return true on {@link #equals(Object)}. Used to ensure that ItemGroup
     * is using identity rather than equals(). Be sure to use assertSame rather than assertEquals
     * when comparing items of this class.
     */
    private static class EqualsItem extends Item {

        EqualsItem(String name) {
            setTitle(name);
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Item;
        }

        @Override
        public String toString() {
            return "EqualsItem{title=" + getTitle() + "}";
        }
    }
}
