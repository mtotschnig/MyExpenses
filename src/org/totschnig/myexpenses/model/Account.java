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
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
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
import android.widget.Toast;
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

  public static final String[] PROJECTION = new String[] {KEY_ROWID,KEY_LABEL,KEY_DESCRIPTION,KEY_OPENING_BALANCE,KEY_CURRENCY,KEY_COLOR,
    "(SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id and amount>0 and transfer_peer is null) as sum_income",
    "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount<0 and transfer_peer is null) as sum_expenses",
    "(SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id and transfer_peer is not null) as sum_transfer",
    "opening_balance + (SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id) as current_balance"};
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
      String result ="";
      Iterator<Type> iterator = EnumSet.allOf(Type.class).iterator();
      while (iterator.hasNext()) {
        result += "'" + iterator.next().name() + "'";
        if (iterator.hasNext())
          result += ",";
      }
      JOIN = result;
    }
  }
  public Type type;
  
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
  static int defaultColor = 0xff99CC00;
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
  
  public static Account getInstanceFromDb(long id) throws DataObjectNotFoundException {
    Account account;
    account = accounts.get(id);
    if (account != null) {
      return account;
    }
    account = new Account(id);
    accounts.put(id, account);
    return account;
  }
  /**
   * empty the cache
   */
  public static void clear() {
    accounts.clear();
  }
  public static boolean delete(long id) {
    Account account;
    try {
      account = getInstanceFromDb(id);
    } catch (DataObjectNotFoundException e) {
      return false;
    }
    account.deleteAllTransactions();
    accounts.remove(id);
    return cr().delete(TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null) > 0;
  }

  /**
   * returns an empty Account instance
   */
  public Account() {
    this("",(long)0,"");
  }
  public Account(String label, long openingBalance, String description) {
    try {
      this.currency = Currency.getInstance(Locale.getDefault());
    } catch (IllegalArgumentException e) {
      this.currency = Currency.getInstance("EUR");
    }
    this.label = label;
    this.openingBalance = new Money(currency,openingBalance);
    this.description = description;
    this.type = Type.CASH;
    this.color = defaultColor;
  }
  
  /**
   * retrieves an Account instance from the database
   * @param mDbHelper
   * @param id
   * @throws DataObjectNotFoundException if no account exists with the given id
   */
  private Account(long id) throws DataObjectNotFoundException {
    this.id = id;
    String[] projection = new String[] {KEY_LABEL,KEY_DESCRIPTION,KEY_OPENING_BALANCE,KEY_CURRENCY,KEY_TYPE,KEY_COLOR};
    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null || c.getCount() == 0) {
      throw new DataObjectNotFoundException();
    }
    c.moveToFirst();
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
        this.color = c.getInt(c.getColumnIndexOrThrow(KEY_COLOR));
      } catch (IllegalArgumentException ex) {
        this.color = defaultColor;
      }
    c.close();
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
    cr().delete(TransactionProvider.TEMPLATES_URI, KEY_ACCOUNTID + " = ?", selectArgs);
    cr().delete(TransactionProvider.TEMPLATES_URI, KEY_TRANSFER_ACCOUNT + " = ?", selectArgs);
  }
  /**
   * writes transactions to export file
   * @param destDir destination directory
   * @param format 
   * @return Result object indicating success, message and output file
   * @throws IOException
   */
  public Result exportAll(File destDir, ExportFormat format) throws IOException {
    SimpleDateFormat now = new SimpleDateFormat("ddMM-HHmm",Locale.US);
    MyApplication ctx = MyApplication.getInstance();
    SharedPreferences settings = ctx.getSettings();
    Log.i("MyExpenses","now starting export");
    File outputFile = new File(destDir,
        label.replaceAll("\\W","") + "-" +
        now.format(new Date()) + "." + format.name().toLowerCase(Locale.US));
    if (outputFile.exists()) {
      return new Result(false,R.string.export_expenses_outputfile_exists,outputFile);
    }
    StringBuilder sb;
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy",Locale.US);
    OutputStreamWriter out = new OutputStreamWriter(
        new FileOutputStream(outputFile),
        settings.getString(MyApplication.PREFKEY_QIF_EXPORT_FILE_ENCODING, "UTF-8"));
    sb = new StringBuilder();
    switch (format) {
    case CSV:
      int[] columns = {R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
      for (int column: columns) {
        sb.append("\"");
        sb.append(ctx.getString(column));
        sb.append("\";");
      }
      break;
    //QIF
    default:
      sb.append("!Type:");
      sb.append(type.getQifName());
    }
    sb.append("\n");
    //Write header
    out.write(sb.toString());
    Cursor c = cr().query(TransactionProvider.TRANSACTIONS_URI, null,
        "account_id = ?", new String[] { String.valueOf(id) }, KEY_ROWID);
    c.moveToFirst();
    while( c.getPosition() < c.getCount() ) {
      Long transfer_peer = DbUtils.getLongOrNull(c, KEY_TRANSFER_PEER);
      String comment = DbUtils.getString(c, KEY_COMMENT);
      String full_label = "",label_sub = "" ;
      String label_main =  DbUtils.getString(c, KEY_LABEL_MAIN);
      if (label_main.length() > 0) {
        if (transfer_peer != null) {
          full_label = "[" + label_main + "]"; 
        } else {
          label_sub =  DbUtils.getString(c, KEY_LABEL_SUB);
          if (label_sub.length() > 0) {
            full_label = label_main + ":" + label_sub;
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
      Log.i("DEBUG","amount: " + String.valueOf(amount) + " ; amountAbsStr: " + amountAbsStr);
      sb = new StringBuilder();
      switch (format) {
      case CSV:
        //{R.string.date,R.string.payee,R.string.income,R.string.expense,R.string.category,R.string.subcategory,R.string.comment,R.string.method};
        sb.append("\"");
        sb.append(dateStr);
        sb.append("\";\"");
        sb.append(payee);
        sb.append("\";");
        sb.append(amount>0 ? amountAbsStr : "0");
        sb.append(";");
        sb.append(amount<0 ? amountAbsStr : "0");
        sb.append(";\"");
        sb.append(transfer_peer == null ? label_main : ctx.getString(R.string.transfer));
        sb.append("\";\"");
        sb.append(transfer_peer == null ? label_sub : full_label);
        sb.append("\";\"");
        sb.append(comment);
        sb.append("\";\"");
        Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
        sb.append(methodId == null ? "" : PaymentMethod.getInstanceFromDb(methodId).getDisplayLabel());
        sb.append("\";");
        break;
      default:
        sb.append( "D" );
        sb.append( dateStr );
        sb.append( "\nT" );
        if (amount<0)
          sb.append( "-");
        sb.append( amountAbsStr );
        if ((comment.length() > 0))
          sb.append( "\nM" );
        sb.append( comment );
        if ((full_label.length() > 0))
          sb.append( "\nL" );
        sb.append( full_label );
        if ((payee.length() > 0))
          sb.append( "\nP" );
        sb.append( payee );
        sb.append( "\n^" );
      }
      sb.append("\n");
      out.write(sb.toString());
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
  public long getSize() {
    return Transaction.countPerAccount(id);
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
  public static int countPerCurrency(Currency currency) {
    return count("currency = ?",new String[] {currency.getCurrencyCode()});
  }
  public static Long firstId() {
    Cursor cursor = cr().query(CONTENT_URI,new String[] {"min(_id)"},null,null,null);
    cursor.moveToFirst();
    Long result;
    if (cursor.isNull(0))
      result = null;
    else
      result = cursor.getLong(0);
    cursor.close();
    return result;
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

