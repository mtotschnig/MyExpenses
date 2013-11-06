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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.Result;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
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

  public long id = 0;

  public String label;

  public Money openingBalance;

  public Currency currency;

  public String description;

  public int color;

  public static final String[] PROJECTION = new String[] {KEY_ROWID,KEY_LABEL,KEY_DESCRIPTION,KEY_OPENING_BALANCE,KEY_CURRENCY,KEY_COLOR,KEY_GROUPING,KEY_TYPE,
    "(SELECT coalesce(sum(amount),0)      FROM " + VIEW_COMMITTED + "  WHERE account_id = accounts._id AND " + WHERE_INCOME   + ") AS sum_income",
    "(SELECT coalesce(abs(sum(amount)),0) FROM " + VIEW_COMMITTED + "  WHERE account_id = accounts._id AND " + WHERE_EXPENSE  + ") AS sum_expenses",
    "(SELECT coalesce(sum(amount),0)      FROM " + VIEW_COMMITTED + "  WHERE account_id = accounts._id AND " + WHERE_TRANSFER + ") AS sum_transfer",
    "opening_balance + (SELECT coalesce(sum(amount),0) FROM " + VIEW_COMMITTED + "  WHERE account_id = accounts._id and (cat_id is null OR cat_id != "
        + SPLIT_CATID + ")) as current_balance"};
  public static final Uri CONTENT_URI = TransactionProvider.ACCOUNTS_URI;

  public enum ExportFormat {
    QIF,CSV
  }
  
  public enum Type {
    CASH,BANK,CCARD,ASSET,LIABILITY;
    public static final String JOIN;
    public String getDisplayName() {
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
    public String getQifName() {
      switch (this) {
      case CASH: return "Cash";
      case BANK: return "Bank";
      case CCARD: return "CCard";
      case ASSET: return "Oth A";
      case LIABILITY: return "Oth L";
      }
      return "";
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
      int this_year_of_week_start = c.getInt(c.getColumnIndex("this_year_of_week_start"));
      int this_week = c.getInt(c.getColumnIndex("this_week"));
      int this_day = c.getInt(c.getColumnIndex("this_day"));
      switch (this) {
      case DAY:
        if (groupSecond == this_day)
          return ctx.getString(R.string.grouping_today);
        else if (groupSecond == this_day -1)
          return ctx.getString(R.string.grouping_yesterday);
        else {
          Calendar cal = Calendar.getInstance();
          cal.set(Calendar.YEAR, groupYear);
          cal.set(Calendar.DAY_OF_YEAR, groupSecond);
          return java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(cal.getTime());
        }
      case WEEK:
        String weekRange = " (" + c.getString(c.getColumnIndex("week_range")) + " )";
        if (groupSecond == this_week)
          return ctx.getString(R.string.grouping_this_week) + weekRange;
        else if (groupSecond == this_week -1)
          return ctx.getString(R.string.grouping_last_week) + weekRange;
        else {
          return (groupYear != this_year_of_week_start ? (groupYear + ", ") : "") + ctx.getString(R.string.grouping_week) + " " + groupSecond + weekRange;
        }
      case MONTH:
        Calendar cal = Calendar.getInstance();
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
    ZMK("Zambian Kwacha"),
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
  /**
   * @param id
   * @return Accouht object, if id == 0, the account with the lowest id is returned
   * @throws DataObjectNotFoundException
   */
  public static Account getInstanceFromDb(long id) throws DataObjectNotFoundException {
    Account account;
    String selection = KEY_ROWID + " = ";
    if (id == 0)
      selection += "(SELECT min(" + KEY_ROWID + ") FROM accounts)";
    else {
      account = accounts.get(id);
      if (account != null) {
        return account;
      }
      selection += id;
    }
    String[] projection = new String[] {KEY_LABEL,KEY_DESCRIPTION,KEY_OPENING_BALANCE,KEY_CURRENCY,KEY_TYPE,KEY_COLOR,KEY_GROUPING};
    Cursor c = cr().query(
        CONTENT_URI, projection,selection,null, null);
    if (c == null || c.getCount() == 0) {
      throw new DataObjectNotFoundException(id);
    }
    c.moveToFirst();
    account = new Account(id,c);
    c.close();
    return account;
  }
  /**
   * empty the cache
   */
  public static void clear() {
    accounts.clear();
  }
  public static boolean delete(long id) {
    Account account = getInstanceFromDb(id);
    account.deleteAllTransactions();
    account.deleteAllTemplates();
    accounts.remove(id);
    return cr().delete(TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null) > 0;
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
  public Account(Long id,Cursor c) {
    this.id = id;
    this.label = c.getString(c.getColumnIndexOrThrow(KEY_LABEL));
    this.description = c.getString(c.getColumnIndexOrThrow(KEY_DESCRIPTION));
    String strCurrency = c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY));
    try {
      this.currency = Currency.getInstance(strCurrency);
    } catch (IllegalArgumentException e) {
      Log.e("MyExpenses",strCurrency + " is not defined in ISO 4217");
      this.currency = Currency.getInstance(Locale.getDefault());
    }    
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
      this.color = c.getInt(c.getColumnIndexOrThrow(KEY_COLOR));
    } catch (IllegalArgumentException ex) {
      this.color = defaultColor;
    }
    accounts.put(id, this);
  }

   public void setCurrency(String currency) throws IllegalArgumentException {
     this.currency = Currency.getInstance(currency);
     openingBalance.setCurrency(this.currency);
   }
  
  /**
   * @return the sum of opening balance and all transactions for the account
   */
  public Money getCurrentBalance() { 
    return new Money(currency,
        openingBalance.getAmountMinor() + getTransactionSum()
    );
  }
  /**
   * @return sum of all transcations
   */
  public long getTransactionSum() {
    Cursor c = cr().query(TransactionProvider.TRANSACTIONS_URI,
        new String[] {"sum(" + KEY_AMOUNT + ")"}, "account_id = ?", new String[] { String.valueOf(id) }, null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result;
  }
  /**
   * deletes all expenses and set the new opening balance to the current balance
   */
  public void reset() {
    long currentBalance = getCurrentBalance().getAmountMinor();
    openingBalance.setAmountMinor(currentBalance);
    ContentValues args = new ContentValues();
    args.put(KEY_OPENING_BALANCE,currentBalance);
    cr().update(TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(id)).build(), args,
        null, null);
    deleteAllTransactions();
  }
  public void markAsExported() {
    ContentValues args = new ContentValues();
    args.put(KEY_STATUS, STATUS_EXPORTED);
    cr().update(TransactionProvider.TRANSACTIONS_URI, args, "account_id = ? and parent_id is null", new String[] { String.valueOf(id) });
  }
  
  /**
   * @param accountId
   * @return true if the account with id accountId has transactions marked as exported
   * if accountId is null returns true if any account has transactions marked as exported
   */
  public static boolean getHasExported(Long accountId) {
    String selection = accountId == null ? null : "account_id = ?";
    String[] selectionArgs  = accountId == null ? null : new String[] { String.valueOf(accountId) };
    Cursor c = cr().query(TransactionProvider.TRANSACTIONS_URI,
        new String[] {"max(" + KEY_STATUS + ")"}, selection, selectionArgs, null);
    c.moveToFirst();
    long result = c.getLong(0);
    c.close();
    return result == 1;
  }
  /**
   * For transfers the peer transaction will survive, but we transform it to a normal transaction
   * with a note about the deletion of the peer_transaction
   * also takes care of templates
   */
  public void deleteAllTransactions() {
    String[] selectArgs = new String[] { String.valueOf(id) };
    ContentValues args = new ContentValues();
    args.put(KEY_COMMENT, MyApplication.getInstance().getString(R.string.peer_transaction_deleted,label));
    args.putNull(KEY_TRANSFER_ACCOUNT);
    args.putNull(KEY_TRANSFER_PEER);
    cr().update(TransactionProvider.TRANSACTIONS_URI, args,
        KEY_TRANSFER_ACCOUNT + " = ?", selectArgs);
    cr().delete(TransactionProvider.TRANSACTIONS_URI, KEY_ACCOUNTID + " = ?", selectArgs);
  }
  public void deleteAllTemplates() {
    String[] selectArgs = new String[] { String.valueOf(id) };
    cr().delete(TransactionProvider.TEMPLATES_URI, KEY_ACCOUNTID + " = ?", selectArgs);
    cr().delete(TransactionProvider.TEMPLATES_URI, KEY_TRANSFER_ACCOUNT + " = ?", selectArgs);
  }
  /**
   * writes transactions to export file
   * @param destDir destination directory
   * @param format QIF or CSV
   * @param notYetExportedP if true only transactions not marked as exported will be handled
   * @param dateFormat format parseable by SimpleDateFormat class
   * @return Result object indicating success, message and output file
   * @throws IOException
   */
  public Result exportAll(File destDir, ExportFormat format, boolean notYetExportedP, String dateFormat) throws IOException {
    SimpleDateFormat now = new SimpleDateFormat("ddMM-HHmm",Locale.US);
    MyApplication ctx = MyApplication.getInstance();
    SharedPreferences settings = ctx.getSettings();
    Log.i("MyExpenses","now starting export");
    //first we check if there are any exportable transactions
    String selection = KEY_ACCOUNTID + " = " + id + " AND " + KEY_PARENTID + " is null";
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
        settings.getString(MyApplication.PREFKEY_QIF_EXPORT_FILE_ENCODING, "UTF-8"));
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
      sb.append("!Type:")
        .append(type.getQifName());
    }
    //Write header
    out.write(sb.toString());
    while( c.getPosition() < c.getCount() ) {
      Long transfer_peer = DbUtils.getLongOrNull(c, KEY_TRANSFER_PEER);
      String comment = DbUtils.getString(c, KEY_COMMENT);
      String full_label="",label_sub = "",label_main;
      CrStatus status;
      Long catId =  DbUtils.getLongOrNull(c,KEY_CATID);
      if (SPLIT_CATID.equals(catId)) {
        full_label = ctx.getString(R.string.split_transaction);
        label_main = full_label;
        label_sub = "";
      } else {
        label_main =  DbUtils.getString(c, KEY_LABEL_MAIN);
        if (label_main.length() > 0) {
          if (transfer_peer != null) {
            full_label = "[" + label_main + "]";
            label_main = ctx.getString(R.string.transfer);
            label_sub = full_label;
          } else {
            full_label = label_main;
            label_sub =  DbUtils.getString(c, KEY_LABEL_SUB);
            if (label_sub.length() > 0)
              full_label += ":" + label_sub;
          }
        }
      }
      String payee = DbUtils.getString(c, KEY_PAYEE_NAME);
      String dateStr = formatter.format(Utils.fromSQL(c.getString(
          c.getColumnIndexOrThrow(KEY_DATE))));
      long amount = c.getLong(
          c.getColumnIndexOrThrow(KEY_AMOUNT));
      String amountAbsStr = new Money(currency,amount)
          .getAmountMajor().abs().toPlainString();
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
          .append(amount>0 ? amountAbsStr : "0")
          .append(";")
          .append(amount<0 ? amountAbsStr : "0")
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
          .append( "\nT" );
        if (amount<0)
          sb.append( "-");
        sb.append( amountAbsStr );
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
        Cursor splits = cr().query(TransactionProvider.TRANSACTIONS_URI,null,
            KEY_PARENTID + " = "+c.getLong(c.getColumnIndex(KEY_ROWID)), null, null);
        splits.moveToFirst();
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
          amountAbsStr = new Money(currency,amount)
              .getAmountMajor().abs().toPlainString();
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
              .append(amount>0 ? amountAbsStr : "0")
              .append(";")
              .append(amount<0 ? amountAbsStr : "0")
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
            sb.append( "\n$" );
            if (amount<0)
              sb.append( "-");
            sb.append( amountAbsStr );
          }
          out.write(sb.toString());
          splits.moveToNext();
        }
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
    
    if (id == 0) {
      uri = cr().insert(CONTENT_URI, initialValues);
      id = Integer.valueOf(uri.getLastPathSegment());
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      cr().update(uri,initialValues,null,null);
    }
    if (!accounts.containsKey(id))
      accounts.put(id, this);
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
    if (id != other.id)
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
}

