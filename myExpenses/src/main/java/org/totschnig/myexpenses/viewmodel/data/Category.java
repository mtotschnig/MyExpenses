package org.totschnig.myexpenses.viewmodel.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Category {
  public final long id;
  public final Long parentId;
  public final String label;
  public final Long budget;
  public final Long sum;
  public final Boolean hasMappedBudgets;
  private final List<Category> children = new ArrayList<>();
  public final int color;
  public final String icon;

  public Category(long id, Long parentId, String label, Long sum,
                  Boolean hasMappedBudgets, int color, Long budget, String icon) {
    this.id = id;
    this.parentId = parentId;
    this.label = label;
    this.sum = sum;
    this.hasMappedBudgets = hasMappedBudgets;
    this.color = color;
    this.budget = budget;
    this.icon = icon;
  }

  public void addChild(Category child) {
    if (child.parentId != id) {
      throw new IllegalStateException("Cannot accept child with wrong parent");
    }
    children.add(child);
  }

  public boolean hasChildren() {
    return !children.isEmpty();
  }

  public int getChildCount() {
    return children.size();
  }

  public Category getChildAt(int index) {
    return children.get(index);
  }

  public List<Category> getChildren() {
    return Collections.unmodifiableList(children);
  }
}
