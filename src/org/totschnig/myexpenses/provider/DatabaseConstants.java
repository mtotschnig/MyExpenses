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

package org.totschnig.myexpenses.provider;

import java.util.GregorianCalendar;

public class DatabaseConstants {
  public static String YEAR_OF_WEEK_START, WEEK, THIS_YEAR_OF_WEEK_START, THIS_WEEK, WEEK_START, WEEK_END;
  static {
    buildLocalized();
  }

  public static void buildLocalized() {
    int weekStartsOn = new GregorianCalendar().getFirstDayOfWeek();
    int nextWeekEnd;
    if(weekStartsOn==1) {
      nextWeekEnd = 6;
    } else  {
      nextWeekEnd = weekStartsOn -2;
    }
    YEAR_OF_WEEK_START  = "CAST(strftime('%Y',date,'unixepoch','localtime','weekday " + nextWeekEnd + "', '-6 day') AS integer)";
    WEEK_START = "strftime('%s',date,'unixepoch','localtime', 'weekday " + nextWeekEnd + "', '-6 day')";
    THIS_YEAR_OF_WEEK_START  = "CAST(strftime('%Y','now','localtime','weekday " + nextWeekEnd + "', '-6 day') AS integer)";
    WEEK_END = "strftime('%s',date,'unixepoch','localtime', 'weekday " + nextWeekEnd + "')";
    WEEK  = "CAST(strftime('%W',date,'unixepoch','localtime','weekday " + nextWeekEnd
        + "') AS integer)";
    THIS_WEEK  = "CAST(strftime('%W','now','localtime','weekday " + nextWeekEnd + "') AS integer)";
  }
  //if we do not cast the result to integer, we would need to do the conversion in Java
  public static final String YEAR  = "CAST(strftime('%Y',date,'unixepoch','localtime') AS integer)";
  public static final String THIS_DAY   = "CAST(strftime('%j','now','localtime') AS integer)";
  public static final String MONTH = "CAST(strftime('%m',date,'unixepoch','localtime') AS integer)";
  public static final String DAY   = "CAST(strftime('%j',date,'unixepoch','localtime') AS integer)";
  public static final String THIS_YEAR  = "CAST(strftime('%Y','now','localtime') AS integer)";
  public static final String THIS_MONTH = "CAST(strftime('%m','now','localtime') AS integer)";
  public static final String KEY_DATE = "date";
  public static final String KEY_AMOUNT = "amount";
  public static final String KEY_COMMENT = "comment";
  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATID = "cat_id";
  public static final String KEY_ACCOUNTID = "account_id";
  public static final String KEY_PAYEEID = "payee_id";
  public static final String KEY_TRANSFER_PEER = "transfer_peer";
  public static final String KEY_METHODID = "method_id";
  public static final String KEY_TITLE = "title";
  public static final String KEY_LABEL_MAIN = "label_main";
  public static final String KEY_LABEL_SUB = "label_sub";
  public static final String KEY_LABEL = "label";
  public static final String KEY_COLOR = "color";
  public static final String KEY_TYPE = "type";
  public static final String KEY_CURRENCY = "currency";
  public static final String KEY_DESCRIPTION = "description";
  public static final String KEY_OPENING_BALANCE = "opening_balance";
  public static final String KEY_USAGES = "usages";
  public static final String KEY_PARENTID = "parent_id";
  public static final String KEY_TRANSFER_ACCOUNT = "transfer_account";
  public static final String KEY_STATUS = "status";
  public static final String KEY_PAYEE_NAME = "name";
  public static final String KEY_TRANSACTIONID = "transaction_id";
  public static final String KEY_GROUPING = "grouping";
  public static final String KEY_CR_STATUS = "cr_status";
  public static final String KEY_REFERENCE_NUMBER = "number";
  public static final String KEY_IS_NUMBERED = "is_numbered";
  public static final String KEY_PLANID = "plan_id";
  public static final String KEY_PLAN_EXECUTION = "plan_execution";
  public static final String KEY_TEMPLATEID = "template_id";
  public static final String KEY_INSTANCEID = "instance_id";
  public static final String KEY_CODE = "code";
  /**
   * transaction that already has been exported
   */
  public static final int STATUS_EXPORTED = 1;
  /**
   * split transaction (and its parts) that are currently edited
   */
  public static final int STATUS_UNCOMMITTED = 2;

  public static final String TABLE_TRANSACTIONS = "transactions";
  public static final String TABLE_ACCOUNTS = "accounts";
  public static final String TABLE_CATEGORIES = "categories";
  public static final String TABLE_METHODS = "paymentmethods";
  public static final String TABLE_ACCOUNTTYES_METHODS = "accounttype_paymentmethod";
  public static final String TABLE_TEMPLATES = "templates";
  public static final String TABLE_PAYEES = "payee";
  public static final String TABLE_CURRENCIES = "currency";
  public static final String TABLE_FEATURE_USED = "feature_used";
  public static final String VIEW_COMMITTED = "transactions_committed";
  public static final String VIEW_UNCOMMITTED = "transactions_uncommitted";
  public static final String VIEW_ALL = "transactions_all";
  public static final String VIEW_TEMPLATES = "templates_all";
  public static final String VIEW_EXTENDED = "transactions_extended";
  public static final String VIEW_TEMPLATES_EXTENDED = "templates_extended";
  public static final String TABLE_PLAN_INSTANCE_STATUS = "planinstance_transaction";

  /**
   * an SQL CASE expression for transactions
   * that gives either the category for normal transactions
   * or the account for transfers
   */
  public static final String LABEL_MAIN =
    "CASE WHEN " +
        KEY_TRANSFER_PEER +
    " THEN " +
    "  (SELECT label FROM " + TABLE_ACCOUNTS + " WHERE _id = " + KEY_TRANSFER_ACCOUNT + ") " +
    "WHEN " +
    "  cat_id " +
    "THEN " +
    "  CASE WHEN " +
    "    (SELECT parent_id FROM " + TABLE_CATEGORIES + " WHERE _id = cat_id) " +
    "  THEN " +
    "    (SELECT label FROM " + TABLE_CATEGORIES
        + " WHERE _id = (SELECT parent_id FROM " + TABLE_CATEGORIES
            + " WHERE _id = cat_id)) " +
    "  ELSE " +
    "    (SELECT label FROM " + TABLE_CATEGORIES + " WHERE _id = cat_id) " +
    "  END " +
    "END AS " + KEY_LABEL_MAIN;
 public static final String LABEL_SUB =
    "CASE WHEN " +
    "  " + KEY_TRANSFER_PEER + " is null AND cat_id AND (SELECT " + KEY_PARENTID + " FROM " + TABLE_CATEGORIES
        + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
    "THEN " +
    "  (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES  + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
    "END AS " + KEY_LABEL_SUB;
  /**
   * if transaction is linked to a subcategory
   * only the label from the subcategory is returned
   */
  public static final String SHORT_LABEL = 
    "CASE WHEN " +
    "  " + KEY_TRANSFER_PEER + " " +
    "THEN " +
    "  (SELECT " + KEY_LABEL + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ") " +
    "ELSE " +
    "  (SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
    "END AS  " + KEY_LABEL;
  public static final String TRANSFER_PEER_PARENT =
      "(SELECT " + KEY_PARENTID
          + " FROM " + TABLE_TRANSACTIONS + " peer WHERE peer." + KEY_ROWID
          + " = " + VIEW_EXTENDED + "." + KEY_TRANSFER_PEER + ")";

  public static final Long SPLIT_CATID = 0L;
  
  public static final String WHERE_NOT_SPLIT =
      "(" + KEY_CATID + " IS null OR " + KEY_CATID + " != " + SPLIT_CATID + ")";
  public static final String WHERE_TRANSACTION =
      WHERE_NOT_SPLIT + " AND transfer_peer is null";
  public static final String WHERE_INCOME = "amount>0 AND " + WHERE_TRANSACTION;
  public static final String WHERE_EXPENSE = "amount<0 AND " + WHERE_TRANSACTION;
  public static final String WHERE_TRANSFER =
      WHERE_NOT_SPLIT+ " AND transfer_peer is not null";
  public static final String INCOME_SUM = 
    "sum(CASE WHEN " + WHERE_INCOME + " THEN amount ELSE 0 END) AS sum_income";
  public static final String EXPENSE_SUM = 
      "abs(sum(CASE WHEN " + WHERE_EXPENSE + " THEN amount ELSE 0 END)) AS sum_expense";
  public static final String TRANSFER_SUM = 
      "sum(CASE WHEN " + WHERE_TRANSFER + " THEN amount ELSE 0 END) AS sum_transfer";
  
  //exclude split_catid
  public static final String MAPPED_CATEGORIES =
      "count(CASE WHEN  " + KEY_CATID + ">0 THEN 1 ELSE null END) as mapped_categories";
}
