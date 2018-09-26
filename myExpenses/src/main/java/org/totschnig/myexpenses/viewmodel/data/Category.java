package org.totschnig.myexpenses.viewmodel.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Category {
  public final long id;
  public final Long parentId;
  public final String label;
  public final Long sum;
  public final Boolean hasMappedTemplates;
  public final Boolean hasMappedTransactions;
  private final List<Category> children = new ArrayList<>();
  public final int color;

  public Category(long id, Long parentId, String label, Long sum, Boolean hasMappedTemplates,
                  Boolean hasMappedTransactions, int color) {
    this.id = id;
    this.parentId = parentId;
    this.label = label;
    this.sum = sum;
    this.hasMappedTemplates = hasMappedTemplates;
    this.hasMappedTransactions = hasMappedTransactions;
    this.color = color;
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
