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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CrStatusCriteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.LazyFontSelector.FontType;
import org.totschnig.myexpenses.util.PdfHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.Result;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * Account represents an account stored in the database.
 * Accounts have label, opening balance, description and currency
 *
 * @author Michael Totschnig
 */
public class Account extends Model {

  public static final int EXPORT_HANDLE_DELETED_DO_NOTHING = -1;
  public static final int EXPORT_HANDLE_DELETED_UPDATE_BALANCE = 0;
  public static final int EXPORT_HANDLE_DELETED_CREATE_HELPER = 1;

  public String label;

  public Money openingBalance;

  public Currency currency;

  public String description;

  public int color;

  public boolean excludeFromTotals = false;

  public static String[] PROJECTION_BASE, PROJECTION_EXTENDED, PROJECTION_FULL;
  private static String CURRENT_BALANCE_EXPR = KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART
      + " AND date(" + KEY_DATE + ",'unixepoch') <= date('now') )";

  static {
    PROJECTION_BASE = new String[]{
        KEY_ROWID,
        KEY_LABEL,
        KEY_DESCRIPTION,
        KEY_OPENING_BALANCE,
        KEY_CURRENCY,
        KEY_COLOR,
        KEY_GROUPING,
        KEY_TYPE,
        KEY_SORT_KEY,
        KEY_EXCLUDE_FROM_TOTALS,
        HAS_EXPORTED
    };
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength + 1];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = CURRENT_BALANCE_EXPR + " AS " + KEY_CURRENT_BALANCE;
    PROJECTION_FULL = new String[baseLength + 13];
    System.arraycopy(PROJECTION_EXTENDED, 0, PROJECTION_FULL, 0, baseLength + 1);
    PROJECTION_FULL[baseLength + 1] = "(" + SELECT_AMOUNT_SUM +
        " AND " + WHERE_INCOME + ") AS " + KEY_SUM_INCOME;
    PROJECTION_FULL[baseLength + 2] = "(" + SELECT_AMOUNT_SUM +
        " AND " + WHERE_EXPENSE + ") AS " + KEY_SUM_EXPENSES;
    PROJECTION_FULL[baseLength + 3] = "(" + SELECT_AMOUNT_SUM +
        " AND " + WHERE_TRANSFER + ") AS " + KEY_SUM_TRANSFERS;
    PROJECTION_FULL[baseLength + 4] =
        KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
            " ) AS " + KEY_TOTAL;
    PROJECTION_FULL[baseLength + 5] =
        KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
            " AND " + KEY_CR_STATUS + " IN " +
            "('" + CrStatus.RECONCILED.name() + "','" + CrStatus.CLEARED.name() + "')" +
            " ) AS " + KEY_CLEARED_TOTAL;
    PROJECTION_FULL[baseLength + 6] =
        KEY_OPENING_BALANCE + " + (" + SELECT_AMOUNT_SUM + " AND " + WHERE_NOT_SPLIT_PART +
            " AND " + KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'  ) AS " + KEY_RECONCILED_TOTAL;
    PROJECTION_FULL[baseLength + 7] = KEY_USAGES;
    PROJECTION_FULL[baseLength + 8] = "0 AS " + KEY_IS_AGGREGATE;//this is needed in the union with the aggregates to sort real accounts first
    PROJECTION_FULL[baseLength + 9] = HAS_FUTURE;
    PROJECTION_FULL[baseLength + 10] = HAS_CLEARED;
    PROJECTION_FULL[baseLength + 11] = Type.sqlOrderExpression();
    PROJECTION_FULL[baseLength + 12] = KEY_LAST_USED;

  }

  public static final Uri CONTENT_URI = TransactionProvider.ACCOUNTS_URI;

  public enum ExportFormat {
    QIF, CSV;

    public String getMimeType() {
      return "text/" + getExtension();
    }

    public String getExtension() {
      return name().toLowerCase(Locale.US);
    }
  }

  public enum Type {
    CASH, BANK, CCARD, ASSET, LIABILITY;
    public static final String JOIN;

    public String toString() {
      Context ctx = MyApplication.getInstance();
      switch (this) {
        case CASH:
          return ctx.getString(R.string.account_type_cash);
        case BANK:
          return ctx.getString(R.string.account_type_bank);
        case CCARD:
          return ctx.getString(R.string.account_type_ccard);
        case ASSET:
          return ctx.getString(R.string.account_type_asset);
        case LIABILITY:
          return ctx.getString(R.string.account_type_liability);
      }
      return "";
    }

    public int toStringResPlural() {
      switch (this) {
        case CASH:
          return R.string.account_type_cash_plural;
        case BANK:
          return R.string.account_type_bank_plural;
        case CCARD:
          return R.string.account_type_ccard_plural;
        case ASSET:
          return R.string.account_type_asset_plural;
        case LIABILITY:
          return R.string.account_type_liability_plural;
        default:
          return 0;
      }
    }

    public String toQifName() {
      switch (this) {
        case CASH:
          return "Cash";
        case BANK:
          return "Bank";
        case CCARD:
          return "CCard";
        case ASSET:
          return "Oth A";
        case LIABILITY:
          return "Oth L";
      }
      return "";
    }

    public static Type fromQifName(String qifName) {
      if (qifName.equals("Oth L")) {
        return LIABILITY;
      } else if (qifName.equals("Oth A")) {
        return ASSET;
      } else if (qifName.equals("CCard")) {
        return CCARD;
      } else if (qifName.equals("Cash")) {
        return CASH;
      } else {
        return BANK;
      }
    }

    public static String sqlOrderExpression() {
      String result = "CASE " + KEY_TYPE;
      for (Type type : Type.values()) {
        result += " WHEN '" + type.name() + "' THEN " + type.getSortOrder();
      }
      result += " ELSE -1 END AS " + KEY_SORT_KEY_TYPE;
      return result;
    }

    private String getSortOrder() {
      switch (this) {
        case CASH:
          return "0";
        case BANK:
          return "1";
        case CCARD:
          return "2";
        case ASSET:
          return "3";
        case LIABILITY:
          return "4";
      }
      return "-1";
    }

    static {
      JOIN = Utils.joinEnum(Type.class);
    }
  }

  public Type type;

  /**
   * grouping of accounts in account list
   */
  public enum AccountGrouping {
    NONE, TYPE, CURRENCY
  }

  /**
   * grouping of transactions
   */
  public enum Grouping {
    NONE, DAY, WEEK, MONTH, YEAR;

    /**
     * @param groupYear   the year of the group to display
     * @param groupSecond the number of the group in the second dimension (day, week or month)
     * @param c           a cursor where we can find information about the current date
     * @return a human readable String representing the group as header or activity title
     */
    public String getDisplayTitle(Context ctx, int groupYear, int groupSecond, Cursor c) {
      int this_week = c.getInt(c.getColumnIndex(KEY_THIS_WEEK));
      int this_day = c.getInt(c.getColumnIndex(KEY_THIS_DAY));
      int this_year = c.getInt(c.getColumnIndex(KEY_THIS_YEAR));
      Calendar cal;
      switch (this) {
        case NONE:
          return ctx.getString(R.string.menu_aggregates);
        case DAY:
          cal = Calendar.getInstance();
          cal.set(Calendar.YEAR, groupYear);
          cal.set(Calendar.DAY_OF_YEAR, groupSecond);
          String title = DateFormat.getDateInstance(DateFormat.FULL).format(cal.getTime());
          if (groupYear == this_year) {
            if (groupSecond == this_day)
              return ctx.getString(R.string.grouping_today) + " (" + title + ")";
            else if (groupSecond == this_day - 1)
              return ctx.getString(R.string.grouping_yesterday) + " (" + title + ")";
          }
          return title;
        case WEEK:
          int this_year_of_week_start = c.getInt(c.getColumnIndex(KEY_THIS_YEAR_OF_WEEK_START));
          DateFormat dateformat = Utils.localizedYearlessDateFormat();
          String weekRange = " (" + Utils.convDateTime(c.getString(c.getColumnIndex(KEY_WEEK_START)), dateformat)
              + " - " + Utils.convDateTime(c.getString(c.getColumnIndex(KEY_WEEK_END)), dateformat) + " )";
          String yearPrefix;
          if (groupYear == this_year_of_week_start) {
            if (groupSecond == this_week)
              return ctx.getString(R.string.grouping_this_week) + weekRange;
            else if (groupSecond == this_week - 1)
              return ctx.getString(R.string.grouping_last_week) + weekRange;
            yearPrefix = "";
          } else
            yearPrefix = groupYear + ", ";
          return yearPrefix + ctx.getString(R.string.grouping_week) + " " + groupSecond + weekRange;
        case MONTH:
          int monthStarts = Integer.parseInt(MyApplication.PrefKey.GROUP_MONTH_STARTS.getString("1"));
          cal = Calendar.getInstance();
          if (monthStarts == 1) {
            cal.set(groupYear, groupSecond - 1, 1);
            //noinspection SimpleDateFormat
            return new SimpleDateFormat("MMMM y").format(cal.getTime());
          } else {
            dateformat = android.text.format.DateFormat.getLongDateFormat(ctx);
            int beginYear = groupYear, beginMonth = groupSecond - 1;
            cal = Calendar.getInstance();
            cal.set(beginYear, beginMonth, 1);
            if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < monthStarts) {
              cal.set(beginYear, beginMonth + 1, 1);
            } else {
              cal.set(Calendar.DATE, monthStarts);
            }
            String startDate = dateformat.format(cal.getTime());
            int endYear = beginYear, endMonth = beginMonth + 1;
            if (endMonth > Calendar.DECEMBER) {
              endMonth = Calendar.JANUARY;
              endYear++;
            }
            cal = Calendar.getInstance();
            cal.set(endYear, endMonth, 1);
            if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < monthStarts - 1) {
              cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
              cal.set(Calendar.DATE, monthStarts - 1);
            }
            String endDate = dateformat.format(cal.getTime());
            String monthRange = " (" + startDate + " - " + endDate + " )";
            return monthRange;
          }
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String toString() {
      if (Utils.hasApiLevel(Build.VERSION_CODES.KITKAT)) {
        try {
          return Currency.getInstance(name()).getDisplayName();
        } catch (IllegalArgumentException e) {}
      }
      return description;
    }

    public static CurrencyEnum[] sortedValues() {
      CurrencyEnum[] result = values();
      final Collator collator = Collator.getInstance();
      if (Utils.hasApiLevel(Build.VERSION_CODES.KITKAT)) {
        Arrays.sort(result, new Comparator<CurrencyEnum>() {
          @Override
          public int compare(CurrencyEnum lhs, CurrencyEnum rhs) {
            int classCompare = Ints.compare(lhs.sortClass(), rhs.sortClass());
            return classCompare == 0 ?
                collator.compare(lhs.toString(), rhs.toString()) : classCompare;
          }
        });
      }
      return result;
    }

    private int sortClass() {
      switch (this) {
        case XXX:
          return 3;
        case XAU:
        case XPD:
        case XPT:
        case XAG:
          return 2;
        default:
          return 1;
      }
    }
  }

  public static int defaultColor = 0xff009688;

  static HashMap<Long, Account> accounts = new HashMap<>();

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
   * @param id id of account to be retrieved, if id == 0, the first entry in the accounts cache will be returned or
   *           if it is empty the account with the lowest id will be fetched from db,
   *           if id < 0 we forward to AggregateAccount
   * @return Account object or null if no account with id exists in db
   */
  public static Account getInstanceFromDb(long id) {
    if (id < 0)
      return AggregateAccount.getInstanceFromDb(id);
    Account account;
    String selection = KEY_ROWID + " = ";
    if (id == 0) {
      if (!accounts.isEmpty()) {
        for (long _id : accounts.keySet()) {
          if (_id > 0) {
            return accounts.get(_id);
          }
        }
      }
      selection += "(SELECT min(" + KEY_ROWID + ") FROM accounts)";
    } else {
      account = accounts.get(id);
      if (account != null) {
        return account;
      }
      selection += id;
    }
    Cursor c = cr().query(
        CONTENT_URI, null, selection, null, null);
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

  public static void delete(long id) throws RemoteException, OperationApplicationException {
    Account account = getInstanceFromDb(id);
    if (account == null) {
      return;
    }
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ops.add(account.updateTransferPeersForTransactionDelete(
        buildTransactionRowSelect(null),
        new String[]{String.valueOf(account.getId())}));
    ops.add(ContentProviderOperation.newDelete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build())
        .build());
    cr().applyBatch(TransactionProvider.AUTHORITY, ops);
    accounts.remove(id);
  }

  /**
   * returns an empty Account instance
   */
  public Account() {
    this("", (long) 0, "");
  }

  public static Currency getLocaleCurrency() {
    try {
      Currency c = Currency.getInstance(Locale.getDefault());
      //makeSure we know about the currency
      return Utils.getSaveInstance(c);
    } catch (IllegalArgumentException e) {
      return Currency.getInstance("EUR");
    }
  }

  /**
   * Account with currency from locale, of type CASH and with defaultColor
   *
   * @param label          the label
   * @param openingBalance the opening balance
   * @param description    the description
   */
  public Account(String label, long openingBalance, String description) {
    this(label, getLocaleCurrency(), openingBalance, description, Type.CASH, defaultColor);
  }

  public Account(String label, Currency currency, long openingBalance, String description,
                 Type type, int color) {
    this.label = label;
    this.currency = currency;
    this.openingBalance = new Money(currency, openingBalance);
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
   *
   * @param c a Cursor retrieved from {@link TransactionProvider#ACCOUNTS_URI}
   */
  protected void extract(Cursor c) {
    this.setId(c.getLong(c.getColumnIndexOrThrow(KEY_ROWID)));
    Log.d("DEBUG", "extracting account from cursor with id " + getId());
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
    this.excludeFromTotals = c.getInt(c.getColumnIndex(KEY_EXCLUDE_FROM_TOTALS)) != 0;
  }

  public void setCurrency(String currency) throws IllegalArgumentException {
    this.currency = Currency.getInstance(currency);
    openingBalance.setCurrency(this.currency);
  }

  /**
   * @return the sum of opening balance and all transactions for the account
   */
  @VisibleForTesting
  public Money getTotalBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() + getTransactionSum(null)
    );
  }

  /**
   * @return the sum of opening balance and all cleared and reconciled transactions for the account
   */
  @VisibleForTesting
  public Money getClearedBalance() {
    WhereFilter filter = WhereFilter.empty();
    filter.put(R.id.FILTER_STATUS_COMMAND,
        new CrStatusCriteria(CrStatus.RECONCILED.name(), CrStatus.CLEARED.name()));
    return new Money(currency,
        openingBalance.getAmountMinor() +
            getTransactionSum(filter));
  }

  /**
   * @return the sum of opening balance and all reconciled transactions for the account
   */
  @VisibleForTesting
  public Money getReconciledBalance() {
    return new Money(currency,
        openingBalance.getAmountMinor() +
            getTransactionSum(reconciledFilter()));
  }

  /**
   * @param filter if not null only transactions matched by current filter will be taken into account
   *               if null all transactions are taken into account
   * @return the sum of opening balance and all transactions for the account
   */
  public Money getFilteredBalance(WhereFilter filter) {
    return new Money(currency,
        openingBalance.getAmountMinor() +
            getTransactionSum(filter));
  }

  /**
   * @return sum of all transcations
   */
  public long getTransactionSum(WhereFilter filter) {
    String selection = KEY_ACCOUNTID + " = ? AND " + WHERE_NOT_SPLIT_PART;
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_COMMITTED);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor c = cr().query(Transaction.CONTENT_URI,
        new String[]{"sum(" + KEY_AMOUNT + ")"},
        selection,
        selectionArgs,
        null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result;
  }

  /**
   * deletes all expenses and updates account according to value of handleDelete
   *
   * @param filter        if not null only expenses matched by filter will be deleted
   * @param handleDelete  if equals {@link #EXPORT_HANDLE_DELETED_UPDATE_BALANCE} opening balance will
   *                      be adjusted to account for the deleted expenses,
   *                      if equals {@link #EXPORT_HANDLE_DELETED_CREATE_HELPER} a helper transaction
   * @param helperComment
   */
  public void reset(WhereFilter filter, int handleDelete, String helperComment) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ContentProviderOperation handleDeleteOperation = null;
    if (handleDelete == EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
      long currentBalance = getFilteredBalance(filter).getAmountMinor();
      openingBalance.setAmountMinor(currentBalance);
      handleDeleteOperation = ContentProviderOperation.newUpdate(
          CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build())
          .withValue(KEY_OPENING_BALANCE, currentBalance)
          .build();
    } else if (handleDelete == EXPORT_HANDLE_DELETED_CREATE_HELPER) {
      Transaction helper = new Transaction(this, getTransactionSum(filter));
      helper.comment = helperComment;
      helper.status = STATUS_HELPER;
      handleDeleteOperation = ContentProviderOperation.newInsert(Transaction.CONTENT_URI)
          .withValues(helper.buildInitialValues()).build();
    }
    String rowSelect = buildTransactionRowSelect(filter);
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (filter != null && !filter.isEmpty()) {
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    ops.add(updateTransferPeersForTransactionDelete(rowSelect, selectionArgs));
    ops.add(ContentProviderOperation.newDelete(
        Transaction.CONTENT_URI)
        .withSelection(
            KEY_ROWID + " IN (" + rowSelect + ")",
            selectionArgs)
        .build());
    //needs to be last, otherwise helper transaction would be deleted
    if (handleDeleteOperation != null) ops.add(handleDeleteOperation);
    try {
      cr().applyBatch(TransactionProvider.AUTHORITY, ops);
    } catch (Exception e) {
      Utils.reportToAcra(e);
      e.printStackTrace();
    }
  }

  public void markAsExported(WhereFilter filter) {
    String selection = KEY_ACCOUNTID + " = ? and " + KEY_PARENTID + " is null";
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.TABLE_TRANSACTIONS);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    ContentValues args = new ContentValues();
    args.put(KEY_STATUS, STATUS_EXPORTED);
    cr().update(Transaction.CONTENT_URI, args,
        selection,
        selectionArgs);
  }

  /**
   * @param accountId id of account or null
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
        selection = KEY_ACCOUNTID + " IN " +
            "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
        if (aa == null) {
          return false;
        }
        selectionArgs = new String[]{aa.currency.getCurrencyCode()};
      } else {
        selection = KEY_ACCOUNTID + " = ?";
        selectionArgs = new String[]{String.valueOf(accountId)};
      }
    }
    Cursor c = cr().query(Transaction.CONTENT_URI,
        new String[]{"max(" + KEY_STATUS + ")"}, selection, selectionArgs, null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result == 1;
  }

  public static boolean getTransferEnabledGlobal() {
    Cursor cursor = cr().query(
        TransactionProvider.AGGREGATES_COUNT_URI,
        null, null, null, null);
    boolean result = cursor.getCount() > 0;
    cursor.close();
    return result;
  }

  private static String buildTransactionRowSelect(WhereFilter filter) {
    String rowSelect = "SELECT " + KEY_ROWID + " from " + TABLE_TRANSACTIONS + " WHERE " + KEY_ACCOUNTID + " = ?";
    if (filter != null && !filter.isEmpty()) {
      rowSelect += " AND " + filter.getSelectionForParents(DatabaseConstants.TABLE_TRANSACTIONS);
    }
    return rowSelect;
  }

  private ContentProviderOperation updateTransferPeersForTransactionDelete(
      String rowSelect, String[] selectionArgs) {
    ContentValues args = new ContentValues();
    args.put(KEY_COMMENT, MyApplication.getInstance().getString(R.string.peer_transaction_deleted, label));
    args.putNull(KEY_TRANSFER_ACCOUNT);
    args.putNull(KEY_TRANSFER_PEER);
    return ContentProviderOperation.newUpdate(Transaction.CONTENT_URI)
        .withValues(args)
        .withSelection(
            KEY_TRANSFER_PEER + " IN (" + rowSelect + ")",
            selectionArgs)
        .build();
  }

  /**
   * writes transactions to export file
   *
   * @param destDir          destination directory
   * @param format           QIF or CSV
   * @param notYetExportedP  if true only transactions not marked as exported will be handled
   * @param dateFormat       format parseable by SimpleDateFormat class
   * @param decimalSeparator , or .
   * @param filter           only transactions matched by filter will be considered
   * @return Result object indicating success, message, extra if not null contains uri
   * @throws IOException
   */
  public Result exportWithFilter(
      DocumentFile destDir,
      String fileName,
      ExportFormat format,
      boolean notYetExportedP,
      String dateFormat,
      char decimalSeparator,
      String encoding,
      WhereFilter filter)
      throws IOException {
    MyApplication ctx = MyApplication.getInstance();
    DecimalFormat nfFormat = Utils.getDecimalFormat(currency, decimalSeparator);
    Log.i("MyExpenses", "now starting export");
    //first we check if there are any exportable transactions
    String selection = KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " is null";
    String[] selectionArgs = new String[]{String.valueOf(getId())};
    if (notYetExportedP)
      selection += " AND " + KEY_STATUS + " = " + STATUS_NONE;
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor c = cr().query(
        Transaction.EXTENDED_URI,
        null, selection, selectionArgs, KEY_DATE);
    if (c.getCount() == 0) {
      c.close();
      return new Result(false, R.string.no_exportable_expenses);
    }
    //then we check if the destDir is writable
    DocumentFile outputFile = Utils.newFile(
        destDir,
        fileName,
        format.getMimeType(), true);
    if (outputFile == null) {
      c.close();
      return new Result(
          false,
          R.string.io_error_unable_to_create_file,
          fileName,
          FileUtils.getPath(MyApplication.getInstance(), destDir.getUri()));
    }
    c.moveToFirst();
    Utils.StringBuilderWrapper sb = new Utils.StringBuilderWrapper();
    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.US);
    OutputStreamWriter out = new OutputStreamWriter(
        cr().openOutputStream(outputFile.getUri()),
        encoding);
    switch (format) {
      case CSV:
        int[] columns = {R.string.split_transaction, R.string.date, R.string.payee, R.string.income, R.string.expense,
            R.string.category, R.string.subcategory, R.string.comment, R.string.method, R.string.status, R.string.reference_number};
        for (int column : columns) {
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
    while (c.getPosition() < c.getCount()) {
      String comment = DbUtils.getString(c, KEY_COMMENT);
      String full_label = "", label_sub = "", label_main;
      CrStatus status;
      Long catId = DbUtils.getLongOrNull(c, KEY_CATID);
      Cursor splits = null, readCat;
      if (SPLIT_CATID.equals(catId)) {
        //split transactions take their full_label from the first split part
        splits = cr().query(Transaction.CONTENT_URI, null,
            KEY_PARENTID + " = " + c.getLong(c.getColumnIndex(KEY_ROWID)), null, null);
        if (splits != null && splits.moveToFirst()) {
          readCat = splits;
        } else {
          readCat = c;
        }
      } else {
        readCat = c;
      }
      Long transfer_peer = DbUtils.getLongOrNull(readCat, KEY_TRANSFER_PEER);
      label_main = DbUtils.getString(readCat, KEY_LABEL_MAIN);
      if (label_main.length() > 0) {
        if (transfer_peer != null) {
          full_label = "[" + label_main + "]";
          label_main = ctx.getString(R.string.transfer);
          label_sub = full_label;
        } else {
          full_label = label_main;
          label_sub = DbUtils.getString(readCat, KEY_LABEL_SUB);
          if (label_sub.length() > 0)
            full_label += ":" + label_sub;
        }
      }
      String payee = DbUtils.getString(c, KEY_PAYEE_NAME);
      String dateStr = formatter.format(new Date(c.getLong(
          c.getColumnIndexOrThrow(KEY_DATE)) * 1000));
      long amount = c.getLong(
          c.getColumnIndexOrThrow(KEY_AMOUNT));
      BigDecimal bdAmount = new Money(currency, amount).getAmountMajor();
      String amountQIF = nfFormat.format(bdAmount);
      String amountAbsCSV = nfFormat.format(bdAmount.abs());
      try {
        status = CrStatus.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_CR_STATUS)));
      } catch (IllegalArgumentException ex) {
        status = CrStatus.UNRECONCILED;
      }
      String referenceNumber = DbUtils.getString(c, KEY_REFERENCE_NUMBER);
      String splitIndicator = SPLIT_CATID.equals(catId) ? SplitTransaction.CSV_INDICATOR : "";
      sb.clear();
      switch (format) {
        case CSV:
          //{R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method,R.string.status,R.string.reference_number};
          Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
          PaymentMethod method = methodId == null ? null : PaymentMethod.getInstanceFromDb(methodId);
          sb.append("\n\"")
              .append(splitIndicator)
              .append("\";\"")
              .append(dateStr)
              .append("\";\"")
              .appendQ(payee)
              .append("\";")
              .append(amount > 0 ? amountAbsCSV : "0")
              .append(";")
              .append(amount < 0 ? amountAbsCSV : "0")
              .append(";\"")
              .appendQ(label_main)
              .append("\";\"")
              .appendQ(label_sub)
              .append("\";\"")
              .appendQ(comment)
              .append("\";\"")
              .appendQ(method == null ? "" : method.getLabel())
              .append("\";\"")
              .append(status.symbol)
              .append("\";\"")
              .append(referenceNumber)
              .append("\"");
          break;
        default:
          sb.append("\nD")
              .append(dateStr)
              .append("\nT")
              .append(amountQIF);
          if (comment.length() > 0) {
            sb.append("\nM")
                .append(comment);
          }
          if (full_label.length() > 0) {
            sb.append("\nL")
                .append(full_label);
          }
          if (payee.length() > 0) {
            sb.append("\nP")
                .append(payee);
          }
          if (!status.equals(CrStatus.UNRECONCILED))
            sb.append("\nC")
                .append(status.symbol);
          if (referenceNumber.length() > 0) {
            sb.append("\nN")
                .append(referenceNumber);
          }
      }
      out.write(sb.toString());
      if (SPLIT_CATID.equals(catId) && splits != null) {
        while (splits.getPosition() < splits.getCount()) {
          transfer_peer = DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER);
          comment = DbUtils.getString(splits, KEY_COMMENT);
          label_main = DbUtils.getString(splits, KEY_LABEL_MAIN);
          if (label_main.length() > 0) {
            if (transfer_peer != null) {
              full_label = "[" + label_main + "]";
              label_main = ctx.getString(R.string.transfer);
              label_sub = full_label;
            } else {
              full_label = label_main;
              label_sub = DbUtils.getString(splits, KEY_LABEL_SUB);
              if (label_sub.length() > 0)
                full_label += ":" + label_sub;
            }
          } else {
            label_main = full_label = Category.NO_CATEGORY_ASSIGNED_LABEL;
            label_sub = "";
          }
          amount = splits.getLong(
              splits.getColumnIndexOrThrow(KEY_AMOUNT));
          bdAmount = new Money(currency, amount).getAmountMajor();
          amountQIF = nfFormat.format(bdAmount);
          amountAbsCSV = nfFormat.format(bdAmount.abs());
          sb.clear();
          switch (format) {
            case CSV:
              //{R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
              Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
              PaymentMethod method = methodId == null ? null : PaymentMethod.getInstanceFromDb(methodId);
              sb.append("\n\"")
                  .append(SplitTransaction.CSV_PART_INDICATOR)
                  .append("\";\"")
                  .append(dateStr)
                  .append("\";\"")
                  .appendQ(payee)
                  .append("\";")
                  .append(amount > 0 ? amountAbsCSV : "0")
                  .append(";")
                  .append(amount < 0 ? amountAbsCSV : "0")
                  .append(";\"")
                  .appendQ(label_main)
                  .append("\";\"")
                  .appendQ(label_sub)
                  .append("\";\"")
                  .appendQ(comment)
                  .append("\";\"")
                  .appendQ(method == null ? "" : method.getLabel())
                  .append("\";\"\";\"\"");
              break;
            //QIF
            default:
              sb.append("\nS")
                  .append(full_label);
              if ((comment.length() > 0)) {
                sb.append("\nE")
                    .append(comment);
              }
              sb.append("\n$")
                  .append(amountQIF);
          }
          out.write(sb.toString());
          splits.moveToNext();
        }
        splits.close();
      }
      if (format.equals(ExportFormat.QIF)) {
        out.write("\n^");
      }
      c.moveToNext();
    }
    out.close();
    c.close();
    return new Result(true, R.string.export_sdcard_success, outputFile.getUri());
  }

  /**
   * Saves the account, creating it new if necessary
   *
   * @return the id of the account. Upon creation it is returned from the database
   */
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_LABEL, label);
    initialValues.put(KEY_OPENING_BALANCE, openingBalance.getAmountMinor());
    initialValues.put(KEY_DESCRIPTION, description);
    initialValues.put(KEY_CURRENCY, currency.getCurrencyCode());
    initialValues.put(KEY_TYPE, type.name());
    initialValues.put(KEY_GROUPING, grouping.name());
    initialValues.put(KEY_COLOR, color);

    if (getId() == 0) {
      uri = cr().insert(CONTENT_URI, initialValues);
      if (uri == null) {
        return null;
      }
      setId(ContentUris.parseId(uri));
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      cr().update(uri, initialValues, null, null);
    }
    if (!accounts.containsKey(getId()))
      accounts.put(getId(), this);
    return uri;
  }

  public static int count(String selection, String[] selectionArgs) {
    Cursor cursor = cr().query(CONTENT_URI, new String[]{"count(*)"},
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
    if (!getId().equals(other.getId()))
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
   *
   * @param resetP if true immediately delete reconciled transactions
   *               and reset opening balance
   */
  public void balance(boolean resetP) {
    ContentValues args = new ContentValues();
    args.put(KEY_CR_STATUS, CrStatus.RECONCILED.name());
    cr().update(Transaction.CONTENT_URI, args,
        KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " is null AND " +
            KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'",
        new String[]{String.valueOf(getId())});
    if (resetP) {
      reset(reconciledFilter(), EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null);
    }
  }

  private WhereFilter reconciledFilter() {
    WhereFilter filter = WhereFilter.empty();
    filter.put(R.id.FILTER_STATUS_COMMAND,
        new CrStatusCriteria(CrStatus.RECONCILED.name()));
    return filter;
  }

  public Result print(DocumentFile destDir, WhereFilter filter) throws IOException, DocumentException {
    long start = System.currentTimeMillis();
    Log.d("MyExpenses", "Print start " + start);
    PdfHelper helper = new PdfHelper();
    Log.d("MyExpenses", "Helper created " + (System.currentTimeMillis() - start));
    String selection;
    String[] selectionArgs;
    if (getId() < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[]{currency.getCurrencyCode()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[]{String.valueOf(getId())};
    }
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor transactionCursor;
    String fileName = label.replaceAll("\\W", "");
    DocumentFile outputFile = Utils.timeStampedFile(
        destDir,
        fileName,
        "application/pdf", false);
    Document document = new Document();
    transactionCursor = cr().query(Transaction.EXTENDED_URI, null, selection + " AND " + KEY_PARENTID + " is null", selectionArgs, KEY_DATE + " ASC");
    //first we check if there are any exportable transactions
    //String selection = KEY_ACCOUNTID + " = " + getId() + " AND " + KEY_PARENTID + " is null";
    if (transactionCursor.getCount() == 0) {
      transactionCursor.close();
      return new Result(false, R.string.no_exportable_expenses);
    }
    //then we check if the filename we construct already exists
    if (outputFile == null) {
      transactionCursor.close();
      return new Result(
          false,
          R.string.io_error_unable_to_create_file,
          fileName,
          FileUtils.getPath(MyApplication.getInstance(), destDir.getUri()));
    }
    PdfWriter.getInstance(document, cr().openOutputStream(outputFile.getUri()));
    Log.d("MyExpenses", "All setup " + (System.currentTimeMillis() - start));
    document.open();
    Log.d("MyExpenses", "Document open " + (System.currentTimeMillis() - start));
    addMetaData(document);
    Log.d("MyExpenses", "Metadata " + (System.currentTimeMillis() - start));
    addHeader(document, helper);
    Log.d("MyExpenses", "Header " + (System.currentTimeMillis() - start));
    addTransactionList(document, transactionCursor, helper, filter);
    Log.d("MyExpenses", "List " + (System.currentTimeMillis() - start));
    transactionCursor.close();
    document.close();
    return new Result(true, R.string.export_sdcard_success, outputFile.getUri());
  }

  private void addMetaData(Document document) {
    document.addTitle(label);
    document.addSubject("Generated by MyExpenses.mobi");
  }

  private void addHeader(Document document, PdfHelper helper)
      throws DocumentException, IOException {
    String selection, column;
    String[] selectionArgs;
    if (getId() < 0) {
      column = "sum(" + CURRENT_BALANCE_EXPR + ")";
      selection = KEY_ROWID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[]{currency.getCurrencyCode()};
    } else {
      column = CURRENT_BALANCE_EXPR;
      selection = KEY_ROWID + " = ?";
      selectionArgs = new String[]{String.valueOf(getId())};
    }
    Cursor account = cr().query(
        CONTENT_URI,
        new String[]{column},
        selection,
        selectionArgs,
        null);
    account.moveToFirst();
    long currentBalance = account.getLong(0);
    account.close();
    PdfPTable preface = new PdfPTable(1);

    preface.addCell(helper.printToCell(label, FontType.TITLE));

    preface.addCell(helper.printToCell(
        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date()), FontType.BOLD));
    preface.addCell(helper.printToCell(
        MyApplication.getInstance().getString(R.string.current_balance) + " : " +
            Utils.formatCurrency(new Money(currency, currentBalance)), FontType.BOLD));

    document.add(preface);
    Paragraph empty = new Paragraph();
    addEmptyLine(empty, 1);
    document.add(empty);
  }

  private void addTransactionList(Document document, Cursor transactionCursor, PdfHelper helper, WhereFilter filter)
      throws DocumentException, IOException {
    String selection;
    String[] selectionArgs;
    if (!filter.isEmpty()) {
      selection = filter.getSelectionForParts(DatabaseConstants.VIEW_EXTENDED);//GROUP query uses extended view
      selectionArgs = filter.getSelectionArgs(true);
    } else {
      selection = null;
      selectionArgs = null;
    }
    Builder builder = Transaction.CONTENT_URI.buildUpon();
    builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
        .appendPath(grouping.name());
    if (getId() < 0) {
      builder.appendQueryParameter(KEY_CURRENCY, currency.getCurrencyCode());
    } else {
      builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(getId()));
    }
    Cursor groupCursor = cr().query(builder.build(), null, selection, selectionArgs,
        KEY_YEAR + " ASC," + KEY_SECOND_GROUP + " ASC");

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
    int columnIndexDay = transactionCursor.getColumnIndex(KEY_DAY);
    int columnIndexAmount = transactionCursor.getColumnIndex(KEY_AMOUNT);
    int columnIndexLabelSub = transactionCursor.getColumnIndex(KEY_LABEL_SUB);
    int columnIndexLabelMain = transactionCursor.getColumnIndex(KEY_LABEL_MAIN);
    int columnIndexComment = transactionCursor.getColumnIndex(KEY_COMMENT);
    int columnIndexReferenceNumber = transactionCursor.getColumnIndex(KEY_REFERENCE_NUMBER);
    int columnIndexPayee = transactionCursor.getColumnIndex(KEY_PAYEE_NAME);
    int columnIndexTransferPeer = transactionCursor.getColumnIndex(KEY_TRANSFER_PEER);
    int columnIndexDate = transactionCursor.getColumnIndex(KEY_DATE);
    DateFormat itemDateFormat;
    switch (grouping) {
      case DAY:
        itemDateFormat = android.text.format.DateFormat.getTimeFormat(ctx);
        break;
      case MONTH:
        //noinspection SimpleDateFormat
        itemDateFormat = new SimpleDateFormat("dd");
        break;
      case WEEK:
        //noinspection SimpleDateFormat
        itemDateFormat = new SimpleDateFormat("EEE");
        break;
      default:
        itemDateFormat = Utils.localizedYearlessDateFormat();
    }
    PdfPTable table = null;

    int prevHeaderId = 0, currentHeaderId;

    transactionCursor.moveToFirst();
    groupCursor.moveToFirst();

    while (transactionCursor.getPosition() < transactionCursor.getCount()) {
      int year = transactionCursor.getInt(grouping.equals(Grouping.WEEK) ? columnIndexYearOfWeekStart : columnIndexYear);
      int month = transactionCursor.getInt(columnIndexMonth);
      int week = transactionCursor.getInt(columnIndexWeek);
      int day = transactionCursor.getInt(columnIndexDay);
      int second = -1;

      switch (grouping) {
        case DAY:
          currentHeaderId = year * 1000 + day;
          break;
        case WEEK:
          currentHeaderId = year * 1000 + week;
          break;
        case MONTH:
          currentHeaderId = year * 1000 + month;
          break;
        case YEAR:
          currentHeaderId = year * 1000;
          break;
        default:
          currentHeaderId = 1;
      }
      if (currentHeaderId != prevHeaderId) {
        if (table != null) {
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
        table = helper.newTable(2);
        table.setWidthPercentage(100f);
        PdfPCell cell = helper.printToCell(grouping.getDisplayTitle(ctx, year, second, transactionCursor), FontType.HEADER);
        table.addCell(cell);
        Long sumExpense = DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumExpense);
        Long sumIncome = DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumIncome);
        Long sumTransfer = DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumTransfer);
        Long delta = sumIncome - sumExpense + sumTransfer;
        Long interimBalance = DbUtils.getLongOr0L(groupCursor, columIndexGroupSumInterim);
        Long previousBalance = interimBalance - delta;
        cell = helper.printToCell(String.format("%s %s %s = %s",
            Utils.convAmount(previousBalance, currency),
            Long.signum(delta) > -1 ? "+" : "-",
            Utils.convAmount(Math.abs(delta), currency),
            Utils.convAmount(interimBalance, currency)), FontType.HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
        document.add(table);
        table = helper.newTable(3);
        table.setWidthPercentage(100f);
        cell = helper.printToCell("+ " + Utils.convAmount(sumIncome,
            currency), FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = helper.printToCell("- " + Utils.convAmount(sumExpense,
            currency), FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = helper.printToCell("<-> " + Utils.convAmount(
            DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumTransfer),
            currency), FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        table.setSpacingAfter(2f);
        document.add(table);
        LineSeparator sep = new LineSeparator();
        document.add(sep);
        table = helper.newTable(4);
        table.setWidths(table.getRunDirection() == PdfWriter.RUN_DIRECTION_RTL ?
            new int[]{2, 3, 5, 1} : new int[]{1, 5, 3, 2});
        table.setSpacingBefore(2f);
        table.setSpacingAfter(2f);
        table.setWidthPercentage(100f);
        prevHeaderId = currentHeaderId;
        groupCursor.moveToNext();
      }
      long amount = transactionCursor.getLong(columnIndexAmount);
      String catText = transactionCursor.getString(columnIndexLabelMain);

      PdfPCell cell = helper.printToCell(Utils.convDateTime(transactionCursor.getString(columnIndexDate), itemDateFormat), FontType.NORMAL);
      table.addCell(cell);
      if (DbUtils.getLongOrNull(transactionCursor, columnIndexTransferPeer) != null) {
        catText = ((amount < 0) ? "=> " : "<= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(transactionCursor, KEY_CATID);
        if (SPLIT_CATID.equals(catId)) {
          Cursor splits = cr().query(Transaction.CONTENT_URI, null,
              KEY_PARENTID + " = " + transactionCursor.getLong(columnIndexRowId), null, null);
          splits.moveToFirst();
          catText = "";
          while (splits.getPosition() < splits.getCount()) {
            String splitText = DbUtils.getString(splits, KEY_LABEL_MAIN);
            if (splitText.length() > 0) {
              if (DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER) != null) {
                splitText = "[" + splitText + "]";
              } else {
                String label_sub = DbUtils.getString(splits, KEY_LABEL_SUB);
                if (label_sub.length() > 0)
                  splitText += TransactionList.CATEGORY_SEPARATOR + label_sub;
              }
            } else {
              splitText = Category.NO_CATEGORY_ASSIGNED_LABEL;
            }
            splitText += " " + Utils.convAmount(splits.getLong(
                splits.getColumnIndexOrThrow(KEY_AMOUNT)), currency);
            String splitComment = DbUtils.getString(splits, KEY_COMMENT);
            if (splitComment != null && splitComment.length() > 0) {
              splitText += " (" + splitComment + ")";
            }
            catText += splitText;
            if (splits.getPosition() != splits.getCount() - 1) {
              catText += "; ";
            }
            splits.moveToNext();
          }
          splits.close();
        } else if (catId == null) {
          catText = Category.NO_CATEGORY_ASSIGNED_LABEL;
        } else {
          String label_sub = transactionCursor.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + TransactionList.CATEGORY_SEPARATOR + label_sub;
          }
        }
      }
      String referenceNumber = transactionCursor.getString(columnIndexReferenceNumber);
      if (referenceNumber != null && referenceNumber.length() > 0)
        catText = "(" + referenceNumber + ") " + catText;
      cell = helper.printToCell(catText, FontType.NORMAL);
      String payee = transactionCursor.getString(columnIndexPayee);
      if (payee == null || payee.length() == 0) {
        cell.setColspan(2);
      }
      table.addCell(cell);
      if (payee != null && payee.length() > 0) {
        table.addCell(helper.printToCell(payee, FontType.UNDERLINE));
      }
      FontType t = amount < 0 ? FontType.EXPENSE : FontType.INCOME;
      cell = helper.printToCell(Utils.convAmount(amount, currency), t);
      cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
      table.addCell(cell);
      String comment = transactionCursor.getString(columnIndexComment);
      if (comment != null && comment.length() > 0) {
        cell = helper.printToCell(comment, FontType.ITALIC);
        cell.setColspan(2);
        table.addCell(helper.emptyCell());
        table.addCell(cell);
        table.addCell(helper.emptyCell());
      }
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

  public void persistGrouping(Grouping value) {
    grouping = value;
    save();
  }

  /**
   * Looks for an account with a label. WARNING: If several accounts have the same label, this
   * method fill return the first account retrieved in the cursor, order is undefined
   *
   * @param label label of the account we want to retrieve
   * @return id or -1 if not found
   */
  public static long findAny(String label) {
    String selection = KEY_LABEL + " = ?";
    String[] selectionArgs = new String[]{label};

    Cursor mCursor = cr().query(CONTENT_URI,
        new String[]{KEY_ROWID}, selection, selectionArgs, null);
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return -1;
    } else {
      mCursor.moveToFirst();
      long result = mCursor.getLong(0);
      mCursor.close();
      return result;
    }
  }

  /**
   * return an Account or AggregateAccount that matches the one found in the cursor at the row it is
   * positioned at. Either the one found in the cache is returned or it is extracted from the cursor
   *
   * @param cursor
   * @return
   */
  public static Account fromCacheOrFromCursor(Cursor cursor) {
    long accountId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID));
    if (!Account.isInstanceCached(accountId)) {
      //calling the constructors, puts the objects into the cache from where the fragment can
      //retrieve it, without needing to create a new cursor
      if (accountId < 0) {
        return new AggregateAccount(cursor);
      } else {
        return new Account(cursor);
      }
    }
    return accounts.get(accountId);
  }
}
