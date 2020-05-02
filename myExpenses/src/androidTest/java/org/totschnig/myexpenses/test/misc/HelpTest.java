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

import android.content.Context;
import android.content.res.Resources;

import junit.framework.Assert;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.BudgetEdit;
import org.totschnig.myexpenses.activity.Distribution;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.activity.ManageBudgets;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.activity.ManageMethods;
import org.totschnig.myexpenses.activity.ManageParties;
import org.totschnig.myexpenses.activity.ManageTags;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MethodEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;

//TODO use parameterized test

/**
 * We test if the resources needed for the help screen are defined
 * 1) For each class an info (no longer the case)
 * 2) If there are no variants a title for the class
 * 3) If there are variants a title and an info (no longer the case) for each variant
 * 4) If there are menuitems defined either for the class or the variants, we need for each menuitem
 * 4a) title
 * 4b) icon ((no longer the case)
 * 4c) help_text
 */
public class HelpTest extends android.test.InstrumentationTestCase {

  private Context context;
  private Resources resources;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    resources = context.getResources();
  }

  public void testHelpStringResExists() {
    String pack = context.getPackageName();
    int menuItemsIdentifier;
    //TODO complete with new activities
    Class<?>[] activities = new Class<?>[]{
        ManageParties.class,
        MethodEdit.class,
        ExpenseEdit.class,
        AccountEdit.class,
        ManageMethods.class,
        ManageTemplates.class,
        MyExpenses.class,
        ManageCategories.class,
        ManageCurrencies.class,
        ManageTags.class,
        Distribution.class,
        BudgetEdit.class,
        ManageBudgets.class,
        FolderBrowser.class
    };
    for (Class<?> activity : activities) {
      String className = activity.getSimpleName();
      Assert.assertTrue(org.totschnig.myexpenses.activity.ProtectedFragmentActivity.class.isAssignableFrom(activity));
      int titleIdentifier = resources.getIdentifier("help_" + className + "_title", "string", pack);
      menuItemsIdentifier = resources.getIdentifier(className + "_menuitems", "array", pack);
      if (menuItemsIdentifier != 0) {
        testMenuItems(className, null, resources.getStringArray(menuItemsIdentifier), "menu");
      }
      menuItemsIdentifier = resources.getIdentifier(className + "_cabitems", "array", pack);
      if (menuItemsIdentifier != 0) {
        testMenuItems(className, null, resources.getStringArray(menuItemsIdentifier), "cab");
      }
      menuItemsIdentifier = resources.getIdentifier(className + "_formfields", "array", pack);
      if (menuItemsIdentifier != 0) {
        testMenuItems(className, null, resources.getStringArray(menuItemsIdentifier), "form");
      }
      try {
        Class<Enum<?>> variants = (Class<Enum<?>>) Class.forName(activity.getName() + "$" + "HelpVariant");
        for (Enum<?> variant : variants.getEnumConstants()) {
          String variantName = variant.name();
          //if there is no generic title, variant specifc ones are required
          if (titleIdentifier == 0)
            Assert.assertTrue("title not defined for " + className + ", variant " + variantName + " and no generic title exists",
                resources.getIdentifier("help_" + className + "_" + variantName + "_title", "string", pack) != 0);
          //and its specific info
          //Assert.assertTrue("info not defined for "+ className+", variant "+variantName,res.getIdentifier("help_" +className + "_" + variantName + "_info", "string", pack)!=0);
          menuItemsIdentifier = resources.getIdentifier(className + "_" + variantName + "_menuitems", "array", pack);
          if (menuItemsIdentifier != 0) {
            testMenuItems(className, variantName, resources.getStringArray(menuItemsIdentifier), "menu");
          }
          menuItemsIdentifier = resources.getIdentifier(className + "_" + variantName + "_cabitems", "array", pack);
          if (menuItemsIdentifier != 0) {
            testMenuItems(className, variantName, resources.getStringArray(menuItemsIdentifier), "cab");
          }
        }
      } catch (ClassNotFoundException e) {
        //title if there are no variants
        Assert.assertTrue("title not defined for " + className, titleIdentifier != 0);
        //classes with variants can have a generic info that is displayed in all variants, but it is not required
        //Assert.assertTrue("info not defined for "+ className,res.getIdentifier("help_" +className + "_info", "string", pack)!=0);
      }
    }
  }

  public void testVersionCodes() {
    Context ctx = getInstrumentation().getTargetContext();
    Resources res = ctx.getResources();
    String[] versions = res.getStringArray(R.array.versions);
    for (String version : versions) {
      String[] parts = version.split(";");
      Assert.assertEquals(2, parts.length);
      Assert.assertNotNull("Could not get changes for version " + parts[1],
          new VersionDialogFragment.VersionInfo(Integer.parseInt(parts[0]), parts[1]).getChanges(ctx));
    }
  }

  private void testMenuItems(
      String activityName,
      String variant,
      String[] menuItems,
      String prefix) {
    context = getInstrumentation().getTargetContext();
    resources = context.getResources();
    String pack = context.getPackageName();
    String resIdString;
    for (String item : menuItems) {
      //assertTrue("icon not found for " + item,HelpDialogFragment.iconMap.containsKey(item));
      if (prefix.equals("form")) {
        for (String resIdPart : item.split("\\.")) {
          assertTrue(String.format("title not found for %s-%s-%s-%s", activityName, variant, prefix, item), resources.getIdentifier(resIdPart, "string", pack) != 0);
        }
      } else {
        resIdString = "menu_" + item;
        assertTrue(String.format("title not found for %s-%s-%s-%s", activityName, variant, prefix, item), resources.getIdentifier(resIdString, "string", pack) != 0);
      }
      if (!resolveStringOrArray(prefix + "_" + activityName + "_" + variant + "_" + item + "_help_text")) {
        if (!resolveStringOrArray(prefix + "_" + activityName + "_" + item + "_help_text")) {
          assertTrue(String.format("help text not found for %s-%s-%s-%s", activityName, variant, prefix, item), resolveStringOrArray(prefix + "_" + item + "_help_text"));
        }
      }
    }
  }

  private boolean resolveStringOrArray(String resString) {
    if (resources.getIdentifier(resString, "array", context.getPackageName()) != 0)
      return true;
    return resources.getIdentifier(resString, "string", context.getPackageName()) != 0;
  }
}
