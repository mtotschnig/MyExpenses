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

import android.test.InstrumentationTestCase;
import android.util.SparseArray;

import junit.framework.Assert;

import org.totschnig.myexpenses.debug.test.R;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.util.ArrayList;

import androidx.core.util.Pair;

public class GrisbiImportTest extends InstrumentationTestCase {
  private Result<Pair<CategoryTree, ArrayList<String>>> analyze(int id) {
    return analyzeSAX(id);
  }
  private Result<Pair<CategoryTree, ArrayList<String>>> analyzeSAX(int id) {
    return Utils.analyzeGrisbiFileWithSAX(
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
    Result<Pair<CategoryTree, ArrayList<String>>> result = analyze(R.raw.grisbi);
    Assert.assertEquals(true, result.isSuccess());
    CategoryTree catTree = result.getExtra().first;
    SparseArray<CategoryTree> main = catTree.children();
    Assert.assertEquals(22, main.size());
    Assert.assertEquals(10, main.get(1).children().size());
    ArrayList<String> partiesList = result.getExtra().second;
    Assert.assertEquals("Peter Schnock",partiesList.get(0));
  }
  public void testGrisbi5() {
    Result<Pair<CategoryTree, ArrayList<String>>> result = analyze(R.raw.grisbi_050);
    Assert.assertEquals(true, result.isSuccess());
    CategoryTree catTree = result.getExtra().first;
    SparseArray<CategoryTree> main = catTree.children();
    Assert.assertEquals(22, main.size());
    Assert.assertEquals(9, main.get(1).children().size());
    ArrayList<String> partiesList = result.getExtra().second;
    Assert.assertEquals("Peter Schnock",partiesList.get(0));
  }
  public void testGrisbiParseError() {
    Result<Pair<CategoryTree, ArrayList<String>>> result = analyze(R.raw.grisbi_error);
    Assert.assertEquals(false, result.isSuccess());
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_parse_exception, result.getMessage());
  }
  public void testGrisbi7() {
    Result<Pair<CategoryTree, ArrayList<String>>> result = analyze(R.raw.grisbi_070);
    Assert.assertEquals(false, result.isSuccess());
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_grisbi_version_not_supported, result.getMessage());
    Assert.assertTrue(result.print(getInstrumentation().getTargetContext()).contains("0.7.0"));
  }
  public void testGrisbi4() {
    Result<Pair<CategoryTree, ArrayList<String>>> result = analyze(R.raw.grisbi_040);
    Assert.assertEquals(false, result.isSuccess());
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_grisbi_version_not_supported, result.getMessage());
    Assert.assertTrue(result.print(getInstrumentation().getTargetContext()).contains("0.4.0"));
  }
  public void testGrisbiEmpty() {
    Result result = analyze(R.raw.grisbi_empty);
    Assert.assertEquals(false, result.isSuccess());
    Assert.assertEquals(org.totschnig.myexpenses.R.string.parse_error_no_data_found, result.getMessage());
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
