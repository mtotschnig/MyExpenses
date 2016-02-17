/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.util;

import android.util.SparseArray;

/**
 * simple two level category tree, used for storing categories extracted from Grisbi XML
 * guarantees that children are always added through root
 *
 */
public class CategoryTree {
  private SparseArray<CategoryTree> children;
  private String label;
  private int total;
  private boolean rootP;

  public CategoryTree(String label) {
    this(label,true);
  }
  public CategoryTree(String label, boolean rootP) {
    children = new SparseArray<>();
    this.setLabel(label);
    total = 0;
    this.rootP = rootP;
  }
  
  /**
   * @param label
   * @param id
   * @param parent
   * adds a new CategoryTree under parent with a given label and id
   * This operation is only allowed for the root tree, it is not allowed to add directly to
   * subtrees (throws {@link UnsupportedOperationException}). If parent is 0, a top level 
   * category tree is created. If there is no parent with id parent, the method returns without
   * creating a CategoryTree
   */
  public boolean add(String label, Integer id, Integer parent) {
    if (!rootP) {
      throw new UnsupportedOperationException();
    }
    if (parent == 0) {
      addChild(label,id);
    } else {
      CategoryTree parentCat = children.get(parent);
      if (parentCat == null) {
        return false;
      }
      parentCat.addChild(label, id);
    }
    total++;
    return true;
  }
  private void addChild(String label, Integer id) {
    children.put(id,new CategoryTree(label,false));
  }
  
  public SparseArray<CategoryTree> children() {
    return children;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
  public int getTotal() {
    return total;
  }
}