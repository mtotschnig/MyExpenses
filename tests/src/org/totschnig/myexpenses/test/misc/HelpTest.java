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

import java.util.ArrayList;
import java.util.Arrays;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.*;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import junit.framework.Assert;

/**
 * We test if the resources needed for the help screen are defined
 * 1) For each class an info
 * 2) If there are no variants a title for the class
 * 3) If there are variants a title and an info for each variant
 * 4) If there are menuitems defined either for the class or the variants, we need for each menuitem
 * 4a) title
 * 4b) icon
 * 4c) help_text
 *
 */
public class HelpTest extends android.test.InstrumentationTestCase {
  public void testHelpStringResExists() {
    Context ctx =  getInstrumentation().getTargetContext();
    Resources res = ctx.getResources();
    String pack = ctx.getPackageName();
    int menuItemsIdentifier;
    ArrayList<String> menuItems= new ArrayList<String>();
    Class<?>[] activities = new Class<?>[] {
        ManageParties.class,
        MethodEdit.class,
        ExpenseEdit.class,
        AccountEdit.class,
        ManageMethods.class,
        ManageTemplates.class,
        MyExpenses.class,
        ManageCategories.class,
        };
    for (Class<?> activity: activities) {
      String className = activity.getSimpleName();
      Assert.assertTrue(org.totschnig.myexpenses.activity.ProtectedFragmentActivity.class.isAssignableFrom(activity));
      int titleIdentifier = res.getIdentifier("help_" +className + "_title", "string", pack);
      menuItemsIdentifier = res.getIdentifier(className+"_menuitems", "array", pack);
      if (menuItemsIdentifier != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(menuItemsIdentifier)));
      try {
        Class<Enum<?>> variants = (Class<Enum<?>>) Class.forName(activity.getName()+"$"+"HelpVariant");
        for (Enum<?> variant: variants.getEnumConstants()) {
          String variantName = variant.name();
          //if there is no generic title, variant specifc ones are required
          if (titleIdentifier == 0)
            Assert.assertTrue("title not defined for "+ className+", variant "+variantName+ " and no generic title exists",res.getIdentifier("help_" +className + "_" + variantName + "_title", "string", pack)!=0);
          //and its specific info
          Assert.assertTrue("info not defined for "+ className+", variant "+variantName,res.getIdentifier("help_" +className + "_" + variantName + "_info", "string", pack)!=0);
          menuItemsIdentifier = res.getIdentifier(className + "_" + variantName +"_menuitems", "array", pack);
          if (menuItemsIdentifier != 0)
            menuItems.addAll(Arrays.asList(res.getStringArray(menuItemsIdentifier)));
        }
      } catch (ClassNotFoundException e) {
        //title if there are no variants
        Assert.assertTrue("title not defined for "+ className,titleIdentifier!=0);
        //classes with variants can have a generic info that is displayed in all variants, but it is not required
        Assert.assertTrue("info not defined for "+ className,res.getIdentifier("help_" +className + "_info", "string", pack)!=0);

      }
    }
    for (String item : menuItems) {
      Assert.assertTrue("icon not defined for "+ item,res.getIdentifier(item+"_icon", "drawable", pack)!=0);
      Assert.assertTrue("title not defined for "+ item,res.getIdentifier("menu_"+item,"string",pack)!=0);
      Assert.assertTrue("help text not defined for "+ item,res.getIdentifier("menu_"+item+"_help_text","string",pack)!=0);
    }
  }
  public void testVersionCodes() {
    Context ctx =  getInstrumentation().getTargetContext();
    Resources res = ctx.getResources();
    int[] versionCodes = res.getIntArray(R.array.version_codes);
    for (int i=0;i<versionCodes.length;i++) {
      int code = versionCodes[i];
      Assert.assertTrue("missing change log entry for version " + code,
          res.getIdentifier("whats_new_"+code, "array", ctx.getPackageName()) != 0);
    }
  }
}
