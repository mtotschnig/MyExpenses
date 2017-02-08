package org.totschnig.myexpenses.test.misc;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.export.CategoryInfo;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class CategoryInfoTest {

  private Map<String, Long> categoryToId;

  @Before
  public void setup() {
    categoryToId = new HashMap<>();
  }

  @Test
  public void shouldInsertNormalWithStrip() {
    insert("abc", true);
    assertNotNull(categoryToId.get("abc"));
  }

  @Test
  public void shouldInsertNormalWithoutStrip() {
    insert("abc", false);
    assertNotNull(categoryToId.get("abc"));
  }

  @Test
  public void shouldInsertTwoLevels() {
    insert("abc:def", false);
    assertNotNull(categoryToId.get("abc:def"));
    assertNotNull(categoryToId.get("abc"));
  }

  @Test
  public void shouldReduceToTwoLevels() {
    insert("abc:def:ghi", false);
    assertNull(categoryToId.get("abc:def:ghi"));
    assertNotNull(categoryToId.get("abc:def"));
    assertNotNull(categoryToId.get("abc"));
  }

  @Test
  public void shouldInsertSpecialWithStrip() {
    insert("abc/", true);
    assertNotNull(categoryToId.get("abc"));
    assertNull(categoryToId.get("abc/"));
  }

  @Test
  public void shouldInsertSpecialWithoutStrip() {
    insert("abc/", false);
    assertNull(categoryToId.get("abc"));
    assertNotNull(categoryToId.get("abc/"));
  }

  private void insert(String label, boolean stripQifCategoryClass) {
    new CategoryInfo(label).insert(categoryToId, stripQifCategoryClass);
  }
}
