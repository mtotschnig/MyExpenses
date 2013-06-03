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

package org.totschnig.myexpenses.test;

import java.util.ArrayList;
import java.util.HashMap;

import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;

import android.test.InstrumentationTestCase;

import junit.framework.Assert;

public class GrisbiImportTest extends InstrumentationTestCase {
  private Result analyze(int id) {
    return analyzeSAX(id);
  }
  private Result analyzeSAX(int id) {
    return GrisbiImport.analyzeGrisbiFileWithSAX(
        getInstrumentation().getContext().getResources().openRawResource(id)
    );
  }
  /*
  private Result analyzeDOM(int id) {
    return GrisbiImport.analyzeGrisbiFileWithDOM(
        getInstrumentation().getContext().getResources().openRawResource(id)
    );
  }
  */
  public void testGrisbi6() {
    Result result = analyze(R.raw.grisbi);
    Assert.assertEquals(true, result.success);
    CategoryTree catTree = (CategoryTree) result.extra[0];
    HashMap<Integer,CategoryTree> main = catTree.children();
    Assert.assertEquals(22, main.size());
    Assert.assertEquals(10, main.get(1).children().size());
    ArrayList<String> partiesList = (ArrayList<String>) result.extra[1];
    Assert.assertEquals("Peter Schnock",partiesList.get(0));
  }
  public void testGrisbi5() {
    Result result = analyze(R.raw.grisbi_050);
    Assert.assertEquals(true, result.success);
    CategoryTree catTree = (CategoryTree) result.extra[0];
    HashMap<Integer,CategoryTree> main = catTree.children();
    Assert.assertEquals(22, main.size());
    Assert.assertEquals(9, main.get(1).children().size());
    ArrayList<String> partiesList = (ArrayList<String>) result.extra[1];
    Assert.assertEquals("Peter Schnock",partiesList.get(0));
  }
  public void testGrisbiParseError() {
    Result result = analyze(R.raw.grisbi_error);
    Assert.assertEquals(false, result.success);
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_parse_exception, result.message);
  }
  public void testGrisbi7() {
    Result result = analyze(R.raw.grisbi_070);
    Assert.assertEquals(false, result.success);
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_grisbi_version_not_supported, result.message);
    Assert.assertEquals("0.7.0", (String) result.extra[0]);
  }
  public void testGrisbi4() {
    Result result = analyze(R.raw.grisbi_040);
    Assert.assertEquals(false, result.success);
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_grisbi_version_not_supported, result.message);
    Assert.assertEquals("0.4.0", (String) result.extra[0]);
  }
  public void testGrisbiEmpty() {
    Result result = analyze(R.raw.grisbi_empty);
    Assert.assertEquals(false, result.success);
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_no_data_found, result.message);
  }
  /*
  *these tests are commented out since the large XML files are not added to git
  *they  are  arbitrary large well-formed XML file (1,2,4,6 M large respectively)
  *Results (time in seconds running test)
  *FileSize  SAX     DOM
  *1M        0,755   4,934
  *2M        1,475   8,335
  *4M        3,019   19,347
  *6M        4,756   OutOfMemoryError
  */
  /*
 public void testGrisbiBigFile1() {
    try {
      Result result = analyze(R.raw.grisbi_big1);
      Assert.assertEquals(false, result.success);
    } catch(OutOfMemoryError e) {
      Assert.fail("File too big to be handled");
    }
  }
 public void testGrisbiBigFile2() {
   try {
     Result result = analyze(R.raw.grisbi_big2);
     Assert.assertEquals(false, result.success);
   } catch(OutOfMemoryError e) {
     Assert.fail("File too big to be handled");
   }
 }
 public void testGrisbiBigFile4() {
   try {
     Result result = analyze(R.raw.grisbi_big4);
     Assert.assertEquals(false, result.success);
   } catch(OutOfMemoryError e) {
     Assert.fail("File too big to be handled");
   }
 }
 public void testGrisbiBigFile6() {
   try { 
     Result result = analyze(R.raw.grisbi_big6);
     Assert.assertEquals(false, result.success);
   } catch(OutOfMemoryError e) {
     Assert.fail("File too big to be handled");
   }
 }
 */
}
