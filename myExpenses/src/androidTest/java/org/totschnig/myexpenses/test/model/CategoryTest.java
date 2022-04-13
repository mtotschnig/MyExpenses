package org.totschnig.myexpenses.test.model;

import android.content.ContentUris;

import org.totschnig.myexpenses.model.Category;

public class CategoryTest extends ModelTest {

  public void testShouldAllowArbitrarilyDeepCategoryTree() {
    Category parent = new Category(0L, "Main", null);
    long parentId = ContentUris.parseId(parent.save());
    assertTrue(parentId > 0);
    Category sub = new Category(0L, "Sub", parentId);
    long subId = ContentUris.parseId(sub.save());
    assertTrue(subId > 0);
    Category subsub = new Category(0L, "Sub", subId);
    long subSubId = ContentUris.parseId(subsub.save());
    assertTrue(subSubId > 0);
  }

  public void testShouldStripWhiteSpace() {
    String labelUnderTest = " Main ";
    String labelUnderTestStripped = "Main";
    assertEquals(-1, Category.find(labelUnderTest, null));
    assertEquals(-1, Category.find(labelUnderTestStripped, null));
    assertNotNull(new Category(0L, labelUnderTest, null).save());
    long testedId = Category.find(labelUnderTest, null);
    long testedStrippedId = Category.find(labelUnderTestStripped, null);
    assertTrue(testedId != -1);
    assertTrue(testedStrippedId != -1);
    assertEquals(testedId, testedStrippedId);
  }
}
