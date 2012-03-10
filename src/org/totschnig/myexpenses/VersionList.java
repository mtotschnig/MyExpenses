/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

package org.totschnig.myexpenses;

import java.util.ArrayList;
import java.util.HashMap;

public class VersionList {
  
  public static ArrayList<HashMap<String, String>> get () {
    final String[][] versions = {
        {
          "1.4.7.1",
          "March 10 2012",
          "Fixed layout of expense list in landscape mode. Bug fix for backup." 
        },
        {
          "1.4.7",
          "March 9 2012",
          "Improved usability of currency input. Reduced apk size, since tutorial is no longer included, but hosted on Github." 
        },
        {
          "1.4.6",
          "March 4 2012",
          "Backup and restore of database and preferences. User interface translated to Italian."
        },
        {
          "1.4.5.2", 
          "February 27 2012",
          "Tutorial translated to German and French. Fixed problems with orientation change during categories import and help and changelog dialog. Support for Grisbi file format 0.5.0."
        },
        {
          "1.4.5.1",
          "February 17 2012",
          "Problem with export (FTP, email) fixed."
        },
        {
         "1.4.5",
         "February 13 2012",
         "Support for sending QIF file via email. Reduced memory footprint."
        },
        {
          "1.4.4",
          "February 8 2012",
          "FTP upload done in background. Default account created in user's language and currency."
        },
        {
          "1.4.3",
          "February 3 2012",
          "Improved parsing of number input in German locale. Upload of QIF file to FTP server."
        },
        {
          "1.4.1",
          "January 31 2012",
          "Input of transaction amounts now respects user's locale. Bug fix: Opening balance could not be set."
        },
        {
          "1.4.0",
          "January 27 2012",
          "Currencies now handled with java.util.Currency. Support for transfers between accounts."
        },
        {
          "1.3.2",
          "January 17 2012",
          "Improved help and included tutorial."
        },
        {
          "1.3.0",
          "January 13 2012",
          "Cleaned up layout."
        },
        {
          "v1.2.8",
          "January 8 2012",
          "Fixed bug in export."
        },
        {
          "1.2.7",
          "January 7 2012",
          "Added payee field. Ignore empty fields in QIF export."
        },
        {
          "1.2.6",
          "November 6 2011",
          "Better management of categories: Default category files from Grisbi are now included in the app, categories can be deleted and renamed."
        },
        {
          "1.2.0",
          "October 7 2011",
          "Support for multiple accounts"
        },
        {
          "1.1.3",
          "June 5 2011",
          "Use workaround class for bug in SimpleCursorTreeAdapter (NullPointerException when adding the first subcategory) from http://code.google.com/p/android/issues/detail?id=9170; better feedback when category import goes wrong."
        },
        {
          "1.1.2",
          "May 18 2011",
          "Sort categories by usages; Allow assignment of main category; Allow manual creation of categories; Improved handling of categories import."
        },
        {
          "1.0",
          "May 4 2011",
          "Initial version"
        }
    };
    ArrayList<HashMap<String, String>> versionList = new ArrayList<HashMap<String, String>>();
    for (String[] version : versions) {
      HashMap<String, String> map = new HashMap<String, String>();
      map.put("version", version[0]);
      map.put("date", version[1]);
      map.put("changes", version[2]);
      versionList.add(map);
    }
    return versionList;
  }
}
