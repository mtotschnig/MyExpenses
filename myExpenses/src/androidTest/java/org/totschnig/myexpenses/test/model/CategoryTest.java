package org.totschnig.myexpenses.test.model;

import android.content.ContentUris;

import org.totschnig.myexpenses.model.Category;

/**
 * Created by privat on 06.09.15.
 */
public class CategoryTest extends ModelTest {

    public void testShouldNotAllowMoreThanTwoLevels() {
        Category parent = new Category(0L,"Main",null);
        long parentId = ContentUris.parseId(parent.save());
        assertTrue(parentId>0);
        Category sub = new Category(0L,"Sub",parentId);
        long subId = ContentUris.parseId(sub.save());
        assertTrue(subId>0);
        Category subsub = new Category(0L,"Sub",subId);
        assertNull(subsub.save());
    }
}
