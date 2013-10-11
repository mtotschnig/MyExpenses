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
  static String [][] CURRENCY_TABLE = {
      {"Afghani", "AFN"},
      {"Albania Lek", "ALL"},
      {"Algerian Dinar", "DZD"},
      {"Angola Kwanza", "AOA"},
      {"Argentine Peso", "ARS"},
      {"Armenian Dram", "AMD"},
      {"Aruban Florin", "AWG"},
      {"Australian Dollar", "AUD"},
      {"Azerbaijanian Manat", "AZN"},
      {"Bahamian Dollar", "BSD"},
      {"Bahraini Dinar", "BHD"},
      {"Bangladesh Taka", "BDT"},
      {"Barbados Dollar", "BBD"},
      {"Belarussian Ruble", "BYR"},
      {"Belize Dollar", "BZD"},
      {"Bermudian Dollar", "BMD"},
      {"Bhutan Ngultrum", "BTN"},
      {"Bolivia Boliviano", "BOB"},
      {"Bosnia and Herzegovina Convertible Mark", "BAM"},
      {"Botswana Pula", "BWP"},
      {"Brazilian Real", "BRL"},
      {"Brunei Dollar", "BND"},
      {"Bulgarian Lev", "BGN"},
      {"Burundi Franc", "BIF"},
      {"Cambodia Riel", "KHR"},
      {"Canadian Dollar", "CAD"},
      {"Cape Verde Escudo", "CVE"},
      {"Cayman Islands Dollar", "KYD"},
      {"CFA Franc BCEAO", "XOF"},
      {"CFA Franc BEAC", "XAF"},
      {"CFP Franc", "XPF"},
      {"Chilean Peso", "CLP"},
      {"China Yuan Renminbi", "CNY"},
      {"Colombian Peso", "COP"},
      {"Comoro Franc", "KMF"},
      {"Congolese Franc", "CDF"},
      {"Costa Rican Colon", "CRC"},
      {"Croatian Kuna", "HRK"},
      {"Cuban Peso", "CUP"},
      {"Cuba Peso Convertible", "CUC"},
      {"Czech Koruna", "CZK"},
      {"Danish Krone", "DKK"},
      {"Djibouti Franc", "DJF"},
      {"Dominican Peso", "DOP"},
      {"East Caribbean Dollar", "XCD"},
      {"Egyptian Pound", "EGP"},
      {"El Salvador Colon", "SVC"},
      {"Eritrea Nakfa", "ERN"},
      {"Ethiopian Birr", "ETB"},
      {"Euro", "EUR"},
      {"Falkland Islands Pound", "FKP"},
      {"Fiji Dollar", "FJD"},
      {"Gambia Dalasi", "GMD"},
      {"Georgia Lari", "GEL"},
      {"Ghana Cedi", "GHS"},
      {"Gibraltar Pound", "GIP"},
      {"Guatemala Quetzal", "GTQ"},
      {"Guinea Franc", "GNF"},
      {"Guyana Dollar", "GYD"},
      {"Haiti Gourde", "HTG"},
      {"Honduras Lempira", "HNL"},
      {"Hong Kong Dollar", "HKD"},
      {"Hungary Forint", "HUF"},
      {"Iceland Krona", "ISK"},
      {"Indian Rupee", "INR"},
      {"Indonesia Rupiah", "IDR"},
      {"Iranian Rial", "IRR"},
      {"Iraqi Dinar", "IQD"},
      {"Israeli Sheqel", "ILS"},
      {"Jamaican Dollar", "JMD"},
      {"Japan Yen", "JPY"},
      {"Jordanian Dinar", "JOD"},
      {"Kazakhstan Tenge", "KZT"},
      {"Kenyan Shilling", "KES"},
      {"Korea Won", "KRW"},
      {"Kuwaiti Dinar", "KWD"},
      {"Kyrgyzstan Som", "KGS"},
      {"Lao Kip", "LAK"},
      {"Latvian Lats", "LVL"},
      {"Lebanese Pound", "LBP"},
      {"Lesotho Loti", "LSL"},
      {"Liberian Dollar", "LRD"},
      {"Libyan Dinar", "LYD"},
      {"Lithuanian Litas", "LTL"},
      {"Macao Pataca", "MOP"},
      {"Macedonia Denar", "MKD"},
      {"Malagasy Ariary", "MGA"},
      {"Malawi Kwacha", "MWK"},
      {"Malaysian Ringgit", "MYR"},
      {"Maldives Rufiyaa", "MVR"},
      {"Mauritania Ouguiya", "MRO"},
      {"Mauritius Rupee", "MUR"},
      {"Mexican Peso", "MXN"},
      {"Moldovan Leu", "MDL"},
      {"Mongolia Tugrik", "MNT"},
      {"Moroccan Dirham", "MAD"},
      {"Mozambique Metical", "MZN"},
      {"Myanmar Kyat", "MMK"},
      {"Namibia Dollar", "NAD"},
      {"Nepalese Rupee", "NPR"},
      {"Netherlands Antillean Guilder", "ANG"},
      {"New Zealand Dollar", "NZD"},
      {"Nicaragua Cordoba Oro", "NIO"},
      {"Nigeria Naira", "NGN"},
      {"North Korean Won", "KPW"},
      {"Norwegian Krone", "NOK"},
      {"Omani Rial", "OMR"},
      {"Pakistan Rupee", "PKR"},
      {"Panama Balboa", "PAB"},
      {"Papua New Guinea Kina", "PGK"},
      {"Paraguay Guarani", "PYG"},
      {"Peru Nuevo Sol", "PEN"},
      {"Philippine Peso", "PHP"},
      {"Poland Zloty", "PLN"},
      {"Qatari Rial", "QAR"},
      {"Romanian Leu", "RON"},
      {"Russian Ruble", "RUB"},
      {"Rwanda Franc", "RWF"},
      {"Saint Helena Pound", "SHP"},
      {"Samoa Tala", "WST"},
      {"Sao Tome and Principe Dobra", "STD"},
      {"Saudi Riyal", "SAR"},
      {"Serbian Dinar", "RSD"},
      {"Seychelles Rupee", "SCR"},
      {"Sierra Leone Leone", "SLL"},
      {"Singapore Dollar", "SGD"},
      {"Solomon Islands Dollar", "SBD"},
      {"Somali Shilling", "SOS"},
      {"South Africa Rand", "ZAR"},
      {"South Sudanese Pound", "SSP"},
      {"Sri Lanka Rupee", "LKR"},
      {"Sudanese Pound", "SDG"},
      {"Surinam Dollar", "SRD"},
      {"Swaziland Lilangeni", "SZL"},
      {"Swedish Krona", "SEK"},
      {"Swiss Franc", "CHF"},
      {"Syrian Pound", "SYP"},
      {"Taiwan Dollar", "TWD"},
      {"Tajikistan Somoni", "TJS"},
      {"Tanzanian Shilling", "TZS"},
      {"Thai Baht", "THB"},
      {"Tonga Pa’anga", "TOP"},
      {"Trinidad and Tobago Dollar", "TTD"},
      {"Tunisian Dinar", "TND"},
      {"Turkish Lira", "TRY"},
      {"Turkmenistan New Manat", "TMT"},
      {"UAE Dirham", "AED"},
      {"Uganda Shilling", "UGX"},
      {"Ukraine Hryvnia", "UAH"},
      {"United Kingdom Pound Sterling", "GBP"},
      {"Uruguayo Peso", "UYU"},
      {"US Dollar", "USD"},
      {"Uzbekistan Sum", "UZS"},
      {"Vanuatu Vatu", "VUV"},
      {"Venezuela Bolivar Fuerte", "VEF"},
      {"Vietnam Dong", "VND"},
      {"Yemeni Rial", "YER"},
      {"Zambian Kwacha", "ZMK"},
      {"Zimbabwe Dollar", "ZWL"},
      {"No currency ", "XXX"},
      {"Gold", "XAU"},
      {"Palladium", "XPD"},
      {"Platinum", "XPT"},
      {"Silver", "XAG"}
  };
  public static int defaultColor = 0xff99CC00;
  public static String [] getCurrencyCodes() {
    int size = CURRENCY_TABLE.length;
    String[] codes = new String[size];
    for(int i=0; i<size; i++) 
      codes[i] = CURRENCY_TABLE[i][1];
    return codes;
  }
  public static String [] getCurrencyDescs() {
    int size = CURRENCY_TABLE.length;
    String[] descs = new String[size];
    for(int i=0; i<size; i++) 
      descs[i] = CURRENCY_TABLE[i][0];
    return descs;
  }
  static HashMap<Long,Account> accounts = new HashMap<Long,Account>();
  
  public static boolean isInstanceCached(long id) {
    return accounts.containsKey(id);
  }
  public static Account getInstanceFromDb(long id) throws DataObjectNotFoundException {
    Account account;
    account = accounts.get(id);
    if (account != null) {
      return account;
    }
    String[] projection = new String[] {KEY_LABEL,KEY_DESCRIPTION,KEY_OPENING_BALANCE,KEY_CURRENCY,KEY_TYPE,KEY_COLOR,KEY_GROUPING};
    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
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
   * @return Result object indicating success, message and output file
   * @throws IOException
   */
  public Result exportAll(File destDir, ExportFormat format, boolean notYetExportedP) throws IOException {
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
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy",Locale.US);
    OutputStreamWriter out = new OutputStreamWriter(
        new FileOutputStream(outputFile),
        settings.getString(MyApplication.PREFKEY_QIF_EXPORT_FILE_ENCODING, "UTF-8"));
    switch (format) {
    case CSV:
      int[] columns = {R.string.split_transaction,R.string.date,R.string.payee,R.string.income,R.string.expense,
          R.string.category,R.string.subcategory,R.string.comment,R.string.method,R.string.status};
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
      String payee = DbUtils.getString(c, KEY_PAYEE);
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

