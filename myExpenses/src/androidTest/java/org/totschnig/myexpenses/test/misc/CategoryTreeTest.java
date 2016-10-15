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

package org.totschnig.myexpenses.test.misc;

import android.util.SparseArray;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.totschnig.myexpenses.util.CategoryTree;

public class CategoryTreeTest extends TestCase {
  public void testCategoryTree() {
    CategoryTree catTree = new CategoryTree("root");
    Assert.assertEquals(true,catTree.add("Main1", 1, 0));
    Assert.assertEquals(true,catTree.add("Main2", 2, 0));
    Assert.assertEquals(true,catTree.add("Sub1", 1, 1));
    Assert.assertEquals(true,catTree.add("Sub2", 1, 2));
    //adding to a parent that does not exist returns false
    Assert.assertEquals(false,catTree.add("Sub3", 1, 3));
    Assert.assertEquals(4, catTree.getTotal());
    SparseArray<CategoryTree> mainCats = catTree.children();
    CategoryTree main1 = mainCats.get(1);
    Assert.assertEquals("Main1",main1.getLabel());
    Assert.assertEquals("Sub1",main1.children().get(1).getLabel());
    CategoryTree main2 = mainCats.get(2);
    Assert.assertEquals("Main2",main2.getLabel());
    Assert.assertEquals("Sub2",main2.children().get(1).getLabel());
    try {
      main1.add("Sub3", 3, 0);
      Assert.fail("Directly adding to a CategoryTree that is not root should have thrown UnsupportedOperationException");
    } catch(UnsupportedOperationException e) {

    }
  }
}
