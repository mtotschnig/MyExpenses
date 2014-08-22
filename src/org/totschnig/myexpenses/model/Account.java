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

package org.totschnig.myexpenses.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.Result;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * Account represents an account stored in the database.
 * Accounts have label, opening balance, description and currency
 * 
 * @author Michael Totschnig
 *
 */
public class Account extends Model {
  
  private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18,
      Font.BOLD);
  private static Font headerFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
      Font.BOLD, BaseColor.BLUE);
  private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16,
      Font.BOLD);
  private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12,
      Font.BOLD);
  private static Font italicFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
      Font.ITALIC);
  private static Font underlineFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
      Font.UNDERLINE);
  private static Font incomeFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
      Font.NORMAL, BaseColor.GREEN);
  private static Font expenseFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
      Font.NORMAL, BaseColor.RED);


  public String label;

  public Money openingBalance;

  public Currency currency;

  public String description;

  public int color;

  public static String[] PROJECTION_BASE, PROJECTION_EXTENDED, PROJECTION_FULL;
  private static String CURRENT_BALANCE_EXPR = KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART
      + " AND date(" + KEY_DATE + ",'unixepoch') <= date('now') )";

  static {
    PROJECTION_BASE = new String[] {
    KEY_ROWID,
    KEY_LABEL,
    KEY_DESCRIPTION,
    KEY_OPENING_BALANCE,
    KEY_CURRENCY,
    KEY_COLOR,
    KEY_GROUPING,
    KEY_TYPE,
    "(SELECT count(*) FROM " + TABLE_ACCOUNTS + " t WHERE "
        + KEY_CURRENCY + " = " + TABLE_ACCOUNTS + "." + KEY_CURRENCY + ") > 1 "
        +      "AS " + KEY_TRANSFER_ENABLED,
    HAS_EXPORTED
  };
  int baseLength = PROJECTION_BASE.length;
  PROJECTION_EXTENDED = new String[baseLength+1];
  System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
  PROJECTION_EXTENDED[baseLength] = CURRENT_BALANCE_EXPR + " AS " + KEY_CURRENT_BALANCE;
  PROJECTION_FULL = new String[baseLength+11];
  System.arraycopy(PROJECTION_EXTENDED, 0, PROJECTION_FULL, 0, baseLength+1);
  PROJECTION_FULL[baseLength+1] = "(" + SELECT_AMOUNT_SUM +
      " AND " + WHERE_INCOME   + ") AS " + KEY_SUM_INCOME;
  PROJECTION_FULL[baseLength+2] = "(" + SELECT_AMOUNT_SUM +
      " AND " + WHERE_EXPENSE  + ") AS " + KEY_SUM_EXPENSES;
  PROJECTION_FULL[baseLength+3] = "(" + SELECT_AMOUNT_SUM +
      " AND " + WHERE_TRANSFER + ") AS " + KEY_SUM_TRANSFERS;
  PROJECTION_FULL[baseLength+4] =
      KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
      " ) AS " + KEY_TOTAL;
  PROJECTION_FULL[baseLength+5] =
      KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
      " AND " + KEY_CR_STATUS + " IN " +
          "('" + CrStatus.RECONCILED.name() + "','" + CrStatus.CLEARED.name() + "')" +
      " ) AS " + KEY_CLEARED_TOTAL;
  PROJECTION_FULL[baseLength+6] =
      KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
      " AND " + KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'  ) AS " + KEY_RECONCILED_TOTAL;
  PROJECTION_FULL[baseLength+7] = KEY_USAGES;
  PROJECTION_FULL[baseLength+8] = "0 AS " + KEY_IS_AGGREGATE;//this is needed in the union with the aggregates to sort real accounts first
  PROJECTION_FULL[baseLength+9] = HAS_FUTURE;
  PROJECTION_FULL[baseLength+10] = HAS_CLEARED;
  }
  public static final Uri CONTENT_URI = TransactionProvider.ACCOUNTS_URI;

  public enum ExportFormat {
    QIF,CSV
  }
  
  public enum Type {
    CASH,BANK,CCARD,ASSET,LIABILITY;
    public static final String JOIN;
    public String toString() {
      Context ctx = MyApplication.getInstance();
      switch (this) {
      case CASH: return ctx.getString(R.string.account_type_cash);
      case BANK: return ctx.getString(R.string.account_type_bank);
      case CCARD: return ctx.getString(R.string.account_type_ccard);
      case ASSET: return ctx.getString(R.string.account_type_asset);
      case LIABILITY: return ctx.getString(R.string.account_type_liability);
      }
      return "";
    }
    public String toQifName() {
      switch (this) {
      case CASH: return "Cash";
      case BANK: return "Bank";
      case CCARD: return "CCard";
      case ASSET: return "Oth A";
      case LIABILITY: return "Oth L";
      }
      return "";
    }
    public static Type fromQifName (String qifName) {
      if (qifName.equals("Oth L")) {
        return LIABILITY;
      } else if (qifName.equals("Oth A")) {
        return ASSET;
      } else if (qifName.equals("CCard")) {
        return CCARD;
      } else  if (qifName.equals("Cash")) {
        return CASH;
      } else {
        return BANK;
      }
    }
    static {
      JOIN = Utils.joinEnum(Type.class);
    }
  }
  public Type type;

  public enum Grouping {
    NONE,DAY,WEEK,MONTH,YEAR;

    /**
     * @param groupYear the year of the group to display
     * @param groupSecond the number of the group in the second dimension (day, week or month)
     * @param c a cursor where we can find information about the current date
     * @return a human readable String representing the group as header or activity title
     */
    public String getDisplayTitle(Context ctx, int groupYear, int groupSecond,Cursor c) {
      int this_year_of_week_start = c.getInt(c.getColumnIndex(KEY_THIS_YEAR_OF_WEEK_START));
      int this_week = c.getInt(c.getColumnIndex(KEY_THIS_WEEK));
      int this_day = c.getInt(c.getColumnIndex(KEY_THIS_DAY));
      int this_year = c.getInt(c.getColumnIndex(KEY_THIS_YEAR));
      Calendar cal;
      switch (this) {
      case NONE:
        return ctx.getString(R.string.menu_aggregates);
      case DAY:
        if (groupYear == this_year) {
          if (groupSecond == this_day)
            return ctx.getString(R.string.grouping_today);
          else if (groupSecond == this_day -1)
            return ctx.getString(R.string.grouping_yesterday);
        }
        cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, groupYear);
        cal.set(Calendar.DAY_OF_YEAR, groupSecond);
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(cal.getTime());
      case WEEK:
        DateFormat dateformat = Utils.localizedYearlessDateFormat();
        String weekRange = " (" + Utils.convDateTime(c.getString(c.getColumnIndex(KEY_WEEK_START)),dateformat)
            + " - " + Utils.convDateTime(c.getString(c.getColumnIndex(KEY_WEEK_END)),dateformat)  + " )";
        String yearPrefix;
        if (groupYear == this_year_of_week_start) {
          if (groupSecond == this_week)
            return ctx.getString(R.string.grouping_this_week) + weekRange;
          else if (groupSecond == this_week -1)
            return ctx.getString(R.string.grouping_last_week) + weekRange;
          yearPrefix = "";
        } else
          yearPrefix = groupYear + ", ";
        return yearPrefix + ctx.getString(R.string.grouping_week) + " " + groupSecond + weekRange;
      case MONTH:
        cal = Calendar.getInstance();
        cal.set(groupYear,groupSecond-1,1);
        return new SimpleDateFormat("MMMM y").format(cal.getTime());
      case YEAR:
        return String.valueOf(groupYear);
      default:
        return null;
      }
    }

    public static final String JOIN;
    static {
        JOIN = Utils.joinEnum(Grouping.class);
      }
  }
  public Grouping grouping;

  /**
   * @see <a href="http://www.currency-iso.org/dl_iso_table_a1.xml">http://www.currency-iso.org/dl_iso_table_a1.xml</a>
   */
  public enum CurrencyEnum {
    AFN("Afghani"),
    ALL("Albania Lek"),
    DZD("Algerian Dinar"),
    AOA("Angola Kwanza"),
    ARS("Argentine Peso"),
    AMD("Armenian Dram"),
    AWG("Aruban Florin"),
    AUD("Australian Dollar"),
    AZN("Azerbaijanian Manat"),
    BSD("Bahamian Dollar"),
    BHD("Bahraini Dinar"),
    BDT("Bangladesh Taka"),
    BBD("Barbados Dollar"),
    BYR("Belarussian Ruble"),
    BZD("Belize Dollar"),
    BMD("Bermudian Dollar"),
    BTN("Bhutan Ngultrum"),
    BOB("Bolivia Boliviano"),
    BAM("Bosnia and Herzegovina Convertible Mark"),
    BWP("Botswana Pula"),
    BRL("Brazilian Real"),
    BND("Brunei Dollar"),
    BGN("Bulgarian Lev"),
    BIF("Burundi Franc"),
    KHR("Cambodia Riel"),
    CAD("Canadian Dollar"),
    CVE("Cape Verde Escudo"),
    KYD("Cayman Islands Dollar"),
    XOF("CFA Franc BCEAO"),
    XAF("CFA Franc BEAC"),
    XPF("CFP Franc"),
    CLP("Chilean Peso"),
    CNY("China Yuan Renminbi"),
    COP("Colombian Peso"),
    KMF("Comoro Franc"),
    CDF("Congolese Franc"),
    CRC("Costa Rican Colon"),
    HRK("Croatian Kuna"),
    CUP("Cuban Peso"),
    CUC("Cuba Peso Convertible"),
    CZK("Czech Koruna"),
    DKK("Danish Krone"),
    DJF("Djibouti Franc"),
    DOP("Dominican Peso"),
    XCD("East Caribbean Dollar"),
    EGP("Egyptian Pound"),
    SVC("El Salvador Colon"),
    ERN("Eritrea Nakfa"),
    ETB("Ethiopian Birr"),
    EUR("Euro"),
    FKP("Falkland Islands Pound"),
    FJD("Fiji Dollar"),
    GMD("Gambia Dalasi"),
    GEL("Georgia Lari"),
    GHS("Ghana Cedi"),
    GIP("Gibraltar Pound"),
    GTQ("Guatemala Quetzal"),
    GNF("Guinea Franc"),
    GYD("Guyana Dollar"),
    HTG("Haiti Gourde"),
    HNL("Honduras Lempira"),
    HKD("Hong Kong Dollar"),
    HUF("Hungary Forint"),
    ISK("Iceland Krona"),
    INR("Indian Rupee"),
    IDR("Indonesia Rupiah"),
    IRR("Iranian Rial"),
    IQD("Iraqi Dinar"),
    ILS("Israeli Sheqel"),
    JMD("Jamaican Dollar"),
    JPY("Japan Yen"),
    JOD("Jordanian Dinar"),
    KZT("Kazakhstan Tenge"),
    KES("Kenyan Shilling"),
    KRW("Korea Won"),
    KWD("Kuwaiti Dinar"),
    KGS("Kyrgyzstan Som"),
    LAK("Lao Kip"),
    LVL("Latvian Lats"),
    LBP("Lebanese Pound"),
    LSL("Lesotho Loti"),
    LRD("Liberian Dollar"),
    LYD("Libyan Dinar"),
    LTL("Lithuanian Litas"),
    MOP("Macao Pataca"),
    MKD("Macedonia Denar"),
    MGA("Malagasy Ariary"),
    MWK("Malawi Kwacha"),
    MYR("Malaysian Ringgit"),
    MVR("Maldives Rufiyaa"),
    MRO("Mauritania Ouguiya"),
    MUR("Mauritius Rupee"),
    MXN("Mexican Peso"),
    MDL("Moldovan Leu"),
    MNT("Mongolia Tugrik"),
    MAD("Moroccan Dirham"),
    MZN("Mozambique Metical"),
    MMK("Myanmar Kyat"),
    NAD("Namibia Dollar"),
    NPR("Nepalese Rupee"),
    ANG("Netherlands Antillean Guilder"),
    NZD("New Zealand Dollar"),
    NIO("Nicaragua Cordoba Oro"),
    NGN("Nigeria Naira"),
    KPW("North Korean Won"),
    NOK("Norwegian Krone"),
    OMR("Omani Rial"),
    PKR("Pakistan Rupee"),
    PAB("Panama Balboa"),
    PGK("Papua New Guinea Kina"),
    PYG("Paraguay Guarani"),
    PEN("Peru Nuevo Sol"),
    PHP("Philippine Peso"),
    PLN("Poland Zloty"),
    QAR("Qatari Rial"),
    RON("Romanian Leu"),
    RUB("Russian Ruble"),
    RWF("Rwanda Franc"),
    SHP("Saint Helena Pound"),
    WST("Samoa Tala"),
    STD("Sao Tome and Principe Dobra"),
    SAR("Saudi Riyal"),
    RSD("Serbian Dinar"),
    SCR("Seychelles Rupee"),
    SLL("Sierra Leone Leone"),
    SGD("Singapore Dollar"),
    SBD("Solomon Islands Dollar"),
    SOS("Somali Shilling"),
    ZAR("South Africa Rand"),
    SSP("South Sudanese Pound"),
    LKR("Sri Lanka Rupee"),
    SDG("Sudanese Pound"),
    SRD("Surinam Dollar"),
    SZL("Swaziland Lilangeni"),
    SEK("Swedish Krona"),
    CHF("Swiss Franc"),
    SYP("Syrian Pound"),
    TWD("Taiwan Dollar"),
    TJS("Tajikistan Somoni"),
    TZS("Tanzanian Shilling"),
    THB("Thai Baht"),
    TOP("Tonga Pa’anga"),
    TTD("Trinidad and Tobago Dollar"),
    TND("Tunisian Dinar"),
    TRY("Turkish Lira"),
    TMT("Turkmenistan New Manat"),
    AED("UAE Dirham"),
    UGX("Uganda Shilling"),
    UAH("Ukraine Hryvnia"),
    GBP("United Kingdom Pound Sterling"),
    UYU("Uruguayo Peso"),
    USD("US Dollar"),
    UZS("Uzbekistan Sum"),
    VUV("Vanuatu Vatu"),
    VEF("Venezuela Bolivar Fuerte"),
    VND("Vietnam Dong"),
    YER("Yemeni Rial"),
    ZMW("Zambian Kwacha"),
    ZWL("Zimbabwe Dollar"),
    XXX("No currency "),
    XAU("Gold"),
    XPD("Palladium"),
    XPT("Platinum"),
    XAG("Silver");
    private String description;
    CurrencyEnum(String description) {
      this.description = description;
    }
    public String toString() {
      return description;
    }
  }

  public static int defaultColor = 0xff99CC00;

  static HashMap<Long,Account> accounts = new HashMap<Long,Account>();
  
  public static boolean isInstanceCached(long id) {
    return accounts.containsKey(id);
  }
  public static void reportNull(long id) {
    //This can happen if user deletes account, and changes
    //device orientation before the accounts cursor in MyExpenses is switched
    /*org.acra.ACRA.getErrorReporter().handleSilentException(
        new Exception("Error instantiating account "+id));*/
  }
  /**
   * @param id
   * @return Account object, if id == 0, the first entry in the accounts cache will be returned or
   * if it is empty the account with the lowest id will be fetched from db,
   * if id < 0 we forward to AggregateAccount
   * return null if no account with id exists in db
   */
  public static Account getInstanceFromDb(long id) {
    if (id < 0)
      return AggregateAccount.getInstanceFromDb(id);
    Account account;
    String selection = KEY_ROWID + " = ";
    if (id == 0) {
      if (accounts.size() > 0) {
        for (long _id: accounts.keySet()) {
          return accounts.get(_id);
        }
      }
      selection += "(SELECT min(" + KEY_ROWID + ") FROM accounts)";
    }
    else {
      account = accounts.get(id);
      if (account != null) {
        return account;
      }
      selection += id;
    }
    Cursor c = cr().query(
        CONTENT_URI, null,selection,null, null);
    if (c == null) {
      //reportNull(id);
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      //reportNull(id);
      return null;
    }
    c.moveToFirst();
    account = new Account(c);
    c.close();
    return account;
  }
  /**
   * empty the cache
   */
  public static void clear() {
    accounts.clear();
  }
  public static void delete(long id) {
    Account account = getInstanceFromDb(id);
    if (account == null) {
      return;
    }
    account.deleteAllTransactions(false);
    account.deleteAllTemplates();
    accounts.remove(id);
    cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null);
  }

  /**
   * returns an empty Account instance
   */
  public Account() {
    this("",(long)0,"");
  }
  public static Currency getLocaleCurrency() {
    try {
      return Currency.getInstance(Locale.getDefault());
    } catch (IllegalArgumentException e) {
      return Currency.getInstance("EUR");
    }
  }
  /**
   * @param label
   * @param openingBalance
   * @param description
   * @return Account with currency from locale, of type CASH and with defaultColor
   */
  public Account(String label, long openingBalance, String description) {
    this(label,getLocaleCurrency(),openingBalance,description,Type.CASH,defaultColor);
  }
  public Account(String label, Currency currency, long openingBalance, String description,
      Type type, int color) {
    this.label = label;
    this.currency = currency;
    this.openingBalance = new Money(currency,openingBalance);
    this.description = description;
    this.type = type;
    this.grouping = Grouping.NONE;
    this.color = color;
  }
  
  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public Account(Cursor c) {
    extract(c);
    accounts.put(getId(), this);
  }
  /**
   * extract information from Cursor and populate fields
   * @param c
   */
  protected void extract(Cursor c) {
    this.setId(c.getLong(c.getColumnIndexOrThrow(KEY_ROWID)));
    Log.d("DEBUG","extracting account from cursor with id "+ getId());
    this.label = c.getString(c.getColumnIndexOrThrow(KEY_LABEL));
    this.description = c.getString(c.getColumnIndexOrThrow(KEY_DESCRIPTION));
    this.currency = Utils.getSaveInstance(c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY)));
    this.openingBalance = new Money(this.currency,
        c.getLong(c.getColumnIndexOrThrow(KEY_OPENING_BALANCE)));
    try {
      this.type = Type.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE)));
    } catch (IllegalArgumentException ex) { 
      this.type = Type.CASH;
    }
    try {
      this.grouping = Grouping.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_GROUPING)));
    } catch (IllegalArgumentException ex) { 
      this.grouping = Grouping.NONE;
    }
    try {
      //TODO ???
      this.color = c.getInt(c.getColumnIndexOrThrow(KEY_COLOR));
    } catch (IllegalArgumentException ex) {
      this.color = defaultColor;
    }
  }

   public void setCurrency(String currency) throws IllegalArgumentException {
     this.currency = Currency.getInstance(currency);
     openingBalance.setCurrency(this.currency);
   }
  
  /**
   * @return the sum of opening balance and all transactions for the account
   */
  public Money getTotalBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() + getTransactionSum(null)
    );
  }
  /**
   * @return the sum of opening balance and all transactions for the account
   */
  public Money getClearedBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() +
          getTransactionSum(
              KEY_CR_STATUS + " IN " +
                  "('" + CrStatus.RECONCILED.name() + "','" + CrStatus.CLEARED.name() + "')")
    );
  }
  /**
   * @return the sum of opening balance and all transactions for the account
   */
  public Money getReconciledBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() +
          getTransactionSum(
              KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'")
    );
  }
  /**
   * @return sum of all transcations
   */
  public long getTransactionSum(String condition) {
    String selection = KEY_ACCOUNTID + " = ? AND " + WHERE_NOT_SPLIT_PART;
    if (condition != null) {
      selection += " AND " + condition;
    }
    Cursor c = cr().query(TransactionProvider.TRANSACTIONS_URI,
        new String[] {"sum(" + KEY_AMOUNT + ")"},
        selection,
        new String[] { String.valueOf(getId()) },
        null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result;
  }
  /**
   * deletes all expenses and set the new opening balance to the current balance
   * @param reconciled if true only reconciled expenses will be deleted
   */
  public void reset(boolean reconciled) {
    long currentBalance = (reconciled ? getReconciledBalance() : getTotalBalance())
        .getAmountMinor();
    openingBalance.setAmountMinor(currentBalance);
    ContentValues args = new ContentValues();
    args.put(KEY_OPENING_BALANCE,currentBalance);
    cr().update(CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build(), args,
        null, null);
    deleteAllTransactions(reconciled);
  }
  public void markAsExported() {
    ContentValues args = new ContentValues();
    args.put(KEY_STATUS, STATUS_EXPORTED);
    cr().update(TransactionProvider.TRANSACTIONS_URI, args,
        KEY_ACCOUNTID + " = ? and " + KEY_PARENTID + " is null",
        new String[] { String.valueOf(getId()) });
  }

  /**
   * @param accountId
   * @return true if the account with id accountId has transactions marked as exported
   * if accountId is null returns true if any account has transactions marked as exported
   */
  public static boolean getHasExported(Long accountId) {
    String selection = null;
    String[] selectionArgs = null;
    if (accountId != null) {
      if (accountId < 0L) {
        //aggregate account
        AggregateAccount aa = AggregateAccount.getInstanceFromDb(accountId);
        selection = KEY_ACCOUNTID +  " IN " +
            "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
        selectionArgs = new String[]{aa.currency.getCurrencyCode()};
      } else {
        selection = KEY_ACCOUNTID + " = ?";
        selectionArgs  = new String[] { String.valueOf(accountId) };
      }
    }
    Cursor c = cr().query(TransactionProvider.TRANSACTIONS_URI,
        new String[] {"max(" + KEY_STATUS + ")"}, selection, selectionArgs, null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result == 1;
  }
  
  public static boolean getTransferEnabledGlobal() {
    Cursor cursor = cr().query(
        TransactionProvider.AGGREGATES_COUNT_URI,
        null,null, null, null);
    boolean result = cursor.getCount() > 0;
    cursor.close();
    return result;
  }

  /**
   * For transfers the peer transaction will survive, but we transform it to a normal transaction
   * with a note about the deletion of the peer_transaction
   * @param reconciled 
   */
  public void deleteAllTransactions(boolean reconciled) {
    String rowSelect = "SELECT " + KEY_ROWID + " from " + TABLE_TRANSACTIONS + " WHERE " + KEY_ACCOUNTID + " = ?";
    if (reconciled) {
      rowSelect += " AND " + KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'";
    }
    String[] selectArgs = new String[] { String.valueOf(getId()) };
    ContentValues args = new ContentValues();
    args.put(KEY_COMMENT, MyApplication.getInstance().getString(R.string.peer_transaction_deleted,label));
    args.putNull(KEY_TRANSFER_ACCOUNT);
    args.putNull(KEY_TRANSFER_PEER);
    cr().update(TransactionProvider.TRANSACTIONS_URI, args,
        KEY_TRANSFER_PEER + " IN (" + rowSelect + ")",
        selectArgs);
    cr().delete(
        TransactionProvider.PLAN_INSTANCE_STATUS_URI,
        KEY_TRANSACTIONID + " IN (" + rowSelect + ")",
        selectArgs);
    cr().delete(
        TransactionProvider.TRANSACTIONS_URI,
        KEY_ROWID + " IN (" + rowSelect + ")",
        selectArgs);
  }
  public void deleteAllTemplates() {
    String[] selectArgs = new String[] { String.valueOf(getId()) };
    cr().delete(
        TransactionProvider.PLAN_INSTANCE_STATUS_URI,
        KEY_TEMPLATEID + " IN (SELECT " + KEY_ROWID + " from " + TABLE_TEMPLATES + " WHERE " + KEY_ACCOUNTID + " = ?)",
        selectArgs);
    cr().delete(
        TransactionProvider.PLAN_INSTANCE_STATUS_URI,
        KEY_TEMPLATEID + " IN (SELECT " + KEY_ROWID + " from " + TABLE_TEMPLATES + " WHERE " + KEY_TRANSFER_ACCOUNT + " = ?)",
        selectArgs);
    cr().delete(TransactionProvider.TEMPLATES_URI, KEY_ACCOUNTID + " = ?", selectArgs);
    cr().delete(TransactionProvider.TEMPLATES_URI, KEY_TRANSFER_ACCOUNT + " = ?", selectArgs);
  }

  /**
   * calls {@link #exportAll(File, ExportFormat, boolean, String)} with date format "dd/MM/yyyy"
   * @param destDir
   * @param format
   * @param notYetExportedP
   * @return Result object
   * @throws IOException
   */
  public Result exportAll(File destDir, ExportFormat format, boolean notYetExportedP) throws IOException {
    return exportAll(destDir, format, notYetExportedP, "dd/MM/yyyy",'.');
  }
  /**
   * writes transactions to export file
   * @param destDir destination directory
   * @param format QIF or CSV
   * @param notYetExportedP if true only transactions not marked as exported will be handled
   * @param dateFormat format parseable by SimpleDateFormat class
   * @param decimalSeparator 
   * @return Result object indicating success, message and output file
   * @throws IOException
   */
  public Result exportAll(File destDir, ExportFormat format, boolean notYetExportedP, String dateFormat, char decimalSeparator)
      throws IOException {
    SimpleDateFormat now = new SimpleDateFormat("yyyMMdd-HHmmss",Locale.US);
    MyApplication ctx = MyApplication.getInstance();
    DecimalFormat nfFormat =  Utils.getDecimalFormat(currency, decimalSeparator);
    Log.i("MyExpenses","now starting export");
    //first we check if there are any exportable transactions
    String selection = KEY_ACCOUNTID + " = " + getId() + " AND " + KEY_PARENTID + " is null";
    if (notYetExportedP)
      selection += " AND " + KEY_STATUS + " = 0";
    Cursor c = cr().query(TransactionProvider.TRANSACTIONS_URI, null,selection, null, KEY_DATE);
    if (c.getCount() == 0)
      return new Result(false,R.string.no_exportable_expenses);
    //then we check if the filename we construct already exists
    File outputFile = new File(destDir,
        label.replaceAll("\\W","") + "-" +
        now.format(new Date()) + "." + format.name().toLowerCase(Locale.US));
    if (outputFile.exists()) {
      return new Result(false,R.string.export_expenses_outputfile_exists,outputFile);
    }
    c.moveToFirst();
    Utils.StringBuilderWrapper sb = new Utils.StringBuilderWrapper();
    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat,Locale.US);
    OutputStreamWriter out = new OutputStreamWriter(
        new FileOutputStream(outputFile),
        MyApplication.PrefKey.QIF_EXPORT_FILE_ENCODING.getString("UTF-8"));
    switch (format) {
    case CSV:
      int[] columns = {R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,
          R.string.category,R.string.subcategory,R.string.comment,R.string.method,R.string.status,R.string.reference_number};
      for (int column: columns) {
        sb.append("\"")
          .appendQ(ctx.getString(column))
          .append("\";");
      }
      break;
    //QIF
    default:
      sb.append("!Account\nN")
        .append(label)
        .append("\nT")
        .append(type.toQifName())
        .append("\n^\n!Type:")
        .append(type.toQifName());
    }
    //Write header
    out.write(sb.toString());
    while( c.getPosition() < c.getCount() ) {
      String comment = DbUtils.getString(c, KEY_COMMENT);
      String full_label="",label_sub = "",label_main;
      CrStatus status;
      Long catId =  DbUtils.getLongOrNull(c,KEY_CATID);
      Cursor splits = null, readCat;
      if (SPLIT_CATID.equals(catId)) {
        //split transactions take their full_label from the first split part
        splits = cr().query(TransactionProvider.TRANSACTIONS_URI,null,
            KEY_PARENTID + " = "+c.getLong(c.getColumnIndex(KEY_ROWID)), null, null);
        if (splits.moveToFirst()) {
          readCat = splits;
        } else {
          readCat = c;
        }
      } else {
        readCat = c;
      }
      Long transfer_peer = DbUtils.getLongOrNull(readCat, KEY_TRANSFER_PEER);
      label_main =  DbUtils.getString(readCat, KEY_LABEL_MAIN);
      if (label_main.length() > 0) {
        if (transfer_peer != null) {
          full_label = "[" + label_main + "]";
          label_main = ctx.getString(R.string.transfer);
          label_sub = full_label;
        } else {
          full_label = label_main;
          label_sub =  DbUtils.getString(readCat, KEY_LABEL_SUB);
          if (label_sub.length() > 0)
            full_label += ":" + label_sub;
        }
      }
      String payee = DbUtils.getString(c, KEY_PAYEE_NAME);
      String dateStr = formatter.format(new Date(c.getLong(
          c.getColumnIndexOrThrow(KEY_DATE))*1000));
      long amount = c.getLong(
          c.getColumnIndexOrThrow(KEY_AMOUNT));
      BigDecimal bdAmount = new Money(currency,amount).getAmountMajor();
      String amountQIF = nfFormat.format(bdAmount);
      String amountAbsCSV = nfFormat.format(bdAmount.abs());
      try {
        status = CrStatus.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_CR_STATUS)));
      } catch (IllegalArgumentException ex) {
        status = CrStatus.UNRECONCILED;
      }
      String referenceNumber = DbUtils.getString(c, KEY_REFERENCE_NUMBER);
      sb.clear();
      switch (format) {
      case CSV:
        //{R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
        Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
        sb.append("\n\"\";\"")
          .append(dateStr)
          .append("\";\"")
          .appendQ(payee)
          .append("\";")
          .append(amount>0 ? amountAbsCSV : "0")
          .append(";")
          .append(amount<0 ? amountAbsCSV : "0")
          .append(";\"")
          .appendQ(label_main)
          .append("\";\"")
          .appendQ(label_sub)
          .append("\";\"")
          .appendQ(comment)
          .append("\";\"")
          .appendQ(methodId == null ? "" : PaymentMethod.getInstanceFromDb(methodId).getDisplayLabel())
          .append("\";\"")
          .append(status.symbol)
          .append("\";\"")
          .append(referenceNumber)
          .append("\";");
        break;
      default:
        sb.append( "\nD" )
          .append( dateStr )
          .append( "\nT" )
          .append( amountQIF );
        if (comment.length() > 0) {
          sb.append( "\nM" )
          .append( comment );
        }
        if (full_label.length() > 0) {
          sb.append( "\nL" )
            .append( full_label );
        }
        if (payee.length() > 0) {
          sb.append( "\nP" )
            .append( payee );
        }
        if (!status.equals(CrStatus.UNRECONCILED))
          sb.append( "\nC")
            .append( status.symbol );
        if (referenceNumber.length() > 0) {
              sb.append( "\nN" )
              .append( referenceNumber );
          }
      }
      out.write(sb.toString());
      if (SPLIT_CATID.equals(catId)) {
        while( splits.getPosition() < splits.getCount() ) {
          transfer_peer = DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER);
          comment = DbUtils.getString(splits, KEY_COMMENT);
          label_main =  DbUtils.getString(splits, KEY_LABEL_MAIN);
          if (label_main.length() > 0) {
            if (transfer_peer != null) {
              full_label = "[" + label_main + "]";
              label_main = ctx.getString(R.string.transfer);
              label_sub = full_label;
            } else {
              full_label = label_main;
              label_sub =  DbUtils.getString(splits, KEY_LABEL_SUB);
              if (label_sub.length() > 0)
                full_label += ":" + label_sub;
            }
          } else {
            label_main = full_label = ctx.getString(R.string.no_category_assigned);
            label_sub = "";
          }
          amount = splits.getLong(
              splits.getColumnIndexOrThrow(KEY_AMOUNT));
          bdAmount = new Money(currency,amount).getAmountMajor();
          amountQIF = nfFormat.format(bdAmount);
          amountAbsCSV = nfFormat.format(bdAmount.abs());
          sb.clear();
          switch (format) {
          case CSV:
            //{R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
            Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
            sb.append("\n\"B\";\"")
              .append(dateStr)
              .append("\";\"")
              .appendQ(payee)
              .append("\";")
              .append(amount>0 ? amountAbsCSV : "0")
              .append(";")
              .append(amount<0 ? amountAbsCSV : "0")
              .append(";\"")
              .appendQ(label_main)
              .append("\";\"")
              .appendQ(label_sub)
              .append("\";\"")
              .appendQ(comment)
              .append("\";\"")
              .appendQ(methodId == null ? "" : PaymentMethod.getInstanceFromDb(methodId).getDisplayLabel())
              .append("\";");
            break;
          //QIF  
          default:
            sb.append( "\nS" )
              .append( full_label );
            if ((comment.length() > 0)) {
              sb.append( "\nE" )
              .append( comment );
            }
            sb.append( "\n$" )
              .append( amountQIF );
          }
          out.write(sb.toString());
          splits.moveToNext();
        }
        splits.close();
      }
      if (format.equals(ExportFormat.QIF)) {
        out.write( "\n^" );
      }
      c.moveToNext();
    }
    out.close();
    c.close();
    return new Result(true,R.string.export_expenses_sdcard_success,outputFile);
  }
  
  /**
   * Saves the account, creating it new if necessary
   * @return the id of the account. Upon creation it is returned from the database
   */
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_LABEL, label);
    initialValues.put(KEY_OPENING_BALANCE,openingBalance.getAmountMinor());
    initialValues.put(KEY_DESCRIPTION,description);
    initialValues.put(KEY_CURRENCY,currency.getCurrencyCode());
    initialValues.put(KEY_TYPE,type.name());
    initialValues.put(KEY_GROUPING, grouping.name());
    initialValues.put(KEY_COLOR,color);
    
    if (getId() == 0) {
      uri = cr().insert(CONTENT_URI, initialValues);
      if (uri==null) {
        return null;
      }
      setId(ContentUris.parseId(uri));
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      cr().update(uri,initialValues,null,null);
    }
    if (!accounts.containsKey(getId()))
      accounts.put(getId(), this);
    return uri;
  }
  public static int count(String selection,String[] selectionArgs) {
    Cursor cursor = cr().query(CONTENT_URI,new String[] {"count(*)"},
        selection, selectionArgs, null);
    if (cursor.getCount() == 0) {
      cursor.close();
      return 0;
    } else {
      cursor.moveToFirst();
      int result = cursor.getInt(0);
      cursor.close();
      return result;
    }
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Account other = (Account) obj;
    if (color != other.color)
      return false;
    if (currency == null) {
      if (other.currency != null)
        return false;
    } else if (!currency.equals(other.currency))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (getId() != other.getId())
      return false;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    if (openingBalance == null) {
      if (other.openingBalance != null)
        return false;
    } else if (!openingBalance.equals(other.openingBalance))
      return false;
    if (type != other.type)
      return false;
    return true;
  }
  /**
   * mark cleared transactions as reconciled
   * @param resetP if true immediately delete reconciled transactions
   * and reset opening balance 
   */
  public void balance(boolean resetP) {
    ContentValues args = new ContentValues();
    args.put(KEY_CR_STATUS, CrStatus.RECONCILED.name());
    cr().update(TransactionProvider.TRANSACTIONS_URI, args,
        KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " is null AND " +
            KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'",
        new String[] { String.valueOf(getId()) });
    if (resetP) {
      reset(true);
    }
  }

  public Result print(File destDir, WhereFilter filter) throws IOException, DocumentException {
    Log.d("MyExpenses","now starting print");
    String selection;
    String[] selectionArgs;
    if (getId() < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[] {currency.getCurrencyCode()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[] { String.valueOf(getId()) };
    }
    if (!filter.isEmpty()) {
      selection += " AND " + filter.getSelection();
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs());
    }
    Uri uri = TransactionProvider.TRANSACTIONS_URI.buildUpon().appendQueryParameter("extended", "1").build();
    Cursor transactionCursor;
    SimpleDateFormat now = new SimpleDateFormat("yyyMMdd-HHmmss",Locale.US);
    File outputFile = new File(destDir,
        label.replaceAll("\\W","") + "-" +
        now.format(new Date()) + ".pdf");
    Document document = new Document();
    transactionCursor = cr().query(uri, null,selection + " AND " + KEY_PARENTID + " is null", selectionArgs, null);
    //first we check if there are any exportable transactions
    //String selection = KEY_ACCOUNTID + " = " + getId() + " AND " + KEY_PARENTID + " is null";
    if (transactionCursor.getCount() == 0) {
      transactionCursor.close();
      return new Result(false,R.string.no_exportable_expenses);
    }
    //then we check if the filename we construct already exists
    if (outputFile.exists()) {
      transactionCursor.close();
      return new Result(false,R.string.export_expenses_outputfile_exists,outputFile);
    }
    PdfWriter.getInstance(document, new FileOutputStream(outputFile));
    document.open();
    addMetaData(document);
    addHeader(document);
    addTransactionList(document,transactionCursor);
    transactionCursor.close();
    document.close();
    return new Result(true,R.string.export_expenses_sdcard_success,outputFile);
  }

  private void addMetaData(Document document) {
    document.addTitle(label);
    document.addSubject("Generated by MyExpenses.mobi");
  }

  private void addHeader(Document document)
      throws DocumentException {
    String selection, column;
    String[] selectionArgs;
    if (getId() < 0) {
      column = "sum("+CURRENT_BALANCE_EXPR+")";
      selection = KEY_ROWID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[] {currency.getCurrencyCode()};
    } else {
      column = CURRENT_BALANCE_EXPR;
      selection = KEY_ROWID + " = ?";
      selectionArgs = new String[] { String.valueOf(getId()) };
    }
    Cursor account = cr().query(
        CONTENT_URI,
        new String[] {column},
        selection,
        selectionArgs,
        null);
    account.moveToFirst();
    long currentBalance = account.getLong(0);
    account.close();
    Paragraph preface = new Paragraph();

    preface.add(new Paragraph(label, catFont));

    preface.add(new Paragraph(
        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date()),
        smallBold));
    preface.add(new Paragraph(
        MyApplication.getInstance().getString(R.string.current_balance) + " : " +
            Utils.formatCurrency(new Money(currency, currentBalance)),
        smallBold));
    
    addEmptyLine(preface, 1);
    document.add(preface);
  }

  private void addTransactionList(Document document, Cursor transactionCursor)
      throws DocumentException {
    Builder builder = TransactionProvider.TRANSACTIONS_URI.buildUpon();
    builder.appendPath("groups")
      .appendPath(grouping.name());
    if (getId() < 0) {
      builder.appendQueryParameter(KEY_CURRENCY, currency.getCurrencyCode());
    } else {
      builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(getId()));
    }
    Cursor groupCursor = cr().query(builder.build(), null, null, null, null);

    MyApplication ctx = MyApplication.getInstance();

    int columnIndexGroupSumIncome = groupCursor.getColumnIndex(KEY_SUM_INCOME);
    int columnIndexGroupSumExpense = groupCursor.getColumnIndex(KEY_SUM_EXPENSES);
    int columnIndexGroupSumTransfer = groupCursor.getColumnIndex(KEY_SUM_TRANSFERS);
    int columIndexGroupSumInterim = groupCursor.getColumnIndex(KEY_INTERIM_BALANCE);
    int columnIndexRowId = transactionCursor.getColumnIndex(KEY_ROWID);
    int columnIndexYear = transactionCursor.getColumnIndex(KEY_YEAR);
    int columnIndexYearOfWeekStart = transactionCursor.getColumnIndex(KEY_YEAR_OF_WEEK_START);
    int columnIndexMonth = transactionCursor.getColumnIndex(KEY_MONTH);
    int columnIndexWeek = transactionCursor.getColumnIndex(KEY_WEEK);
    int columnIndexDay  = transactionCursor.getColumnIndex(KEY_DAY);
    int columnIndexAmount = transactionCursor.getColumnIndex(KEY_AMOUNT);
    int columnIndexLabelSub = transactionCursor.getColumnIndex(KEY_LABEL_SUB);
    int columnIndexLabelMain = transactionCursor.getColumnIndex(KEY_LABEL_MAIN);
    int columnIndexComment = transactionCursor.getColumnIndex(KEY_COMMENT);
    int columnIndexReferenceNumber= transactionCursor.getColumnIndex(KEY_REFERENCE_NUMBER);
    int columnIndexPayee = transactionCursor.getColumnIndex(KEY_PAYEE_NAME);
    int columnIndexTransferPeer = transactionCursor.getColumnIndex(KEY_TRANSFER_PEER);
    int columnIndexDate = transactionCursor.getColumnIndex(KEY_DATE);
    DateFormat itemDateFormat;
    switch (grouping) {
    case DAY:
      itemDateFormat = android.text.format.DateFormat.getTimeFormat(ctx);
      break;
    case MONTH:
      itemDateFormat = new SimpleDateFormat("dd");
      break;
    case WEEK:
      itemDateFormat = new SimpleDateFormat("EEE");
      break;
    default:
      itemDateFormat = Utils.localizedYearlessDateFormat();
    }
    PdfPTable table = null;

    int prevHeaderId = 0,currentHeaderId;

    transactionCursor.moveToFirst();
    groupCursor.moveToFirst();

    while( transactionCursor.getPosition() < transactionCursor.getCount() ) {
      int year = transactionCursor.getInt(grouping.equals(Grouping.WEEK)?columnIndexYearOfWeekStart:columnIndexYear);
      int month = transactionCursor.getInt(columnIndexMonth);
      int week = transactionCursor.getInt(columnIndexWeek);
      int day = transactionCursor.getInt(columnIndexDay);
      int second=-1;

      switch(grouping) {
      case DAY:
        currentHeaderId = year*1000+day;
        break;
      case WEEK:
        currentHeaderId = year*1000+week;
        break;
      case MONTH:
        currentHeaderId = year*1000+month;
        break;
      case YEAR:
        currentHeaderId = year*1000;
        break;
      default:
        currentHeaderId = 1;
      }
      if (currentHeaderId != prevHeaderId) {
        if (table !=null) {
          document.add(table);
        }
        switch (grouping) {
        case DAY:
          second = transactionCursor.getInt(columnIndexDay);
            break;
        case MONTH:
          second = transactionCursor.getInt(columnIndexMonth);
            break;
        case WEEK:
          second = transactionCursor.getInt(columnIndexWeek);
            break;
        }
        table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        PdfPCell cell = new PdfPCell(
            new Phrase(grouping.getDisplayTitle(ctx,year,second,transactionCursor),headerFont));
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
        cell = new PdfPCell(
            new Phrase("= " + Utils.convAmount(
                DbUtils.getLongOr0L(groupCursor, columIndexGroupSumInterim),
                currency),
            headerFont));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
        document.add(table);
        table = new PdfPTable(3);
        table.setWidthPercentage(100f);
        cell = new PdfPCell(
            new Phrase("- " + Utils.convAmount(
            DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumExpense),
            currency)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = new PdfPCell(
            new Phrase("+ " + Utils.convAmount(
            DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumIncome),
            currency)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = new PdfPCell(
                new Phrase("<-> " + Utils.convAmount(
            DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumTransfer),
            currency)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        table.setSpacingAfter(2f);
        document.add(table);
        LineSeparator sep = new LineSeparator();
        document.add(sep);
        table = new PdfPTable(3);
        table.setWidths(new int[] {1,5,1});
        table.setSpacingBefore(2f);
        table.setSpacingAfter(2f);
        table.setWidthPercentage(95f);
        prevHeaderId = currentHeaderId;
        groupCursor.moveToNext();
      }
      long amount = transactionCursor.getLong(columnIndexAmount);
      Paragraph catPara = new Paragraph();
      String catText = transactionCursor.getString(columnIndexLabelMain);
      if (DbUtils.getLongOrNull(transactionCursor,columnIndexTransferPeer) != null) {
        catText = ((amount < 0) ? "=> " : "<= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(transactionCursor,KEY_CATID);
        if (SPLIT_CATID.equals(catId)) {
          Cursor splits = cr().query(TransactionProvider.TRANSACTIONS_URI,null,
              KEY_PARENTID + " = "+transactionCursor.getLong(columnIndexRowId), null, null);
          splits.moveToFirst();
          catText = "";
          while( splits.getPosition() < splits.getCount() ) {
            String splitText = DbUtils.getString(splits, KEY_LABEL_MAIN);
            if (splitText.length() > 0) {
              if (DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER) != null) {
                splitText = "[" + splitText + "]";
              } else {
                String label_sub =  DbUtils.getString(splits, KEY_LABEL_SUB);
                if (label_sub.length() > 0)
                  splitText += TransactionList.CATEGORY_SEPARATOR + label_sub;
              }
            } else {
              splitText = ctx.getString(R.string.no_category_assigned);
            }
            splitText += " " + Utils.convAmount(splits.getLong(
                splits.getColumnIndexOrThrow(KEY_AMOUNT)),currency);
            String splitComment = DbUtils.getString(splits, KEY_COMMENT);
            if (splitComment != null && splitComment.length() > 0) {
              splitText += " (" + splitComment + ")";
            }
            catText += splitText;
            if (splits.getPosition()!=splits.getCount()-1) {
              catText += "; ";
            }
            splits.moveToNext();
          }
          splits.close();
          }
        else if (catId == null) {
          catText = ctx.getString(R.string.no_category_assigned);
        } else {
          String label_sub = transactionCursor.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + TransactionList.CATEGORY_SEPARATOR + label_sub;
          }
        }
      }
      String referenceNumber= transactionCursor.getString(columnIndexReferenceNumber);
      if (referenceNumber != null && referenceNumber.length() > 0)
        catText = "(" + referenceNumber + ") " + catText;
      catPara.add(new Phrase(catText));
      String comment = transactionCursor.getString(columnIndexComment);
      if (comment != null && comment.length() > 0) {
        catPara.add(new Phrase(TransactionList.COMMENT_SEPARATOR));
        catPara.add(new Phrase(comment,italicFont));
      }
      String payee = transactionCursor.getString(columnIndexPayee);
      if (payee != null && payee.length() > 0) {
        catPara.add(new Phrase(TransactionList.COMMENT_SEPARATOR));
        catPara.add(new Phrase(payee,underlineFont));
      }
      PdfPCell cell = new PdfPCell(
          new Phrase(Utils.convDateTime(transactionCursor.getString(columnIndexDate),itemDateFormat)));
      cell.setBorder(Rectangle.NO_BORDER);
      table.addCell(cell);
      cell = new PdfPCell(catPara);
      cell.setBorder(Rectangle.NO_BORDER);
      table.addCell(cell);
      cell = new PdfPCell(
              new Phrase(Utils.convAmount(amount,currency),
                  amount<0 ? expenseFont : incomeFont));
      cell.setBorder(Rectangle.NO_BORDER);
      table.addCell(cell);
      transactionCursor.moveToNext();
    }
    // now add all this to the document
    document.add(table);
    groupCursor.close();
  }

  private void addEmptyLine(Paragraph paragraph, int number) {
    for (int i = 0; i < number; i++) {
      paragraph.add(new Paragraph(" "));
    }
  }
}
