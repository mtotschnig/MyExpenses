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
import java.util.HashMap;
import java.util.Locale;

import org.totschnig.myexpenses.DataObjectNotFoundException;
import org.totschnig.myexpenses.ExpensesDbAdapter;
import org.totschnig.myexpenses.Money;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.Utils;
import org.totschnig.myexpenses.R.string;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.content.Context;
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
public class Account {

  public long id = 0;

  public String label;

  public Money openingBalance;

  public Currency currency;

  public String description;

  public int color;

  private static ExpensesDbAdapter mDbHelper  = MyApplication.db();

  public static final String[] PROJECTION = new String[] {KEY_ROWID,"label","description","opening_balance","currency","color",
    "(SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id and amount>0 and transfer_peer = 0) as sum_income",
    "(SELECT coalesce(abs(sum(amount)),0) FROM transactions WHERE account_id = accounts._id and amount<0 and transfer_peer = 0) as sum_expenses",
    "(SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id and transfer_peer != 0) as sum_transfer",
    "opening_balance + (SELECT coalesce(sum(amount),0) FROM transactions WHERE account_id = accounts._id) as current_balance"};
  public static final Uri CONTENT_URI = TransactionProvider.ACCOUNTS_URI;
  
  public enum Type {
    CASH,BANK,CCARD,ASSET,LIABILITY;
    public String getDisplayName(Context ctx) {
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
    return mDbHelper.deleteAccount(id);
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
    String[] projection = new String[] {"label","description","opening_balance","currency","type","color"};
    Cursor c = MyApplication.cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null || c.getCount() == 0) {
      throw new DataObjectNotFoundException();
    }
    c.moveToFirst();
    this.label = c.getString(c.getColumnIndexOrThrow("label"));
    this.description = c.getString(c.getColumnIndexOrThrow("description"));
    String strCurrency = c.getString(c.getColumnIndexOrThrow("currency"));
    try {
      this.currency = Currency.getInstance(strCurrency);
    } catch (IllegalArgumentException e) {
      Log.e("MyExpenses",strCurrency + " is not defined in ISO 4217");
      this.currency = Currency.getInstance(Locale.getDefault());
    }    
    this.openingBalance = new Money(this.currency,
        c.getLong(c.getColumnIndexOrThrow("opening_balance")));
    try {
      this.type = Type.valueOf(c.getString(c.getColumnIndexOrThrow("type")));
    } catch (IllegalArgumentException ex) { 
      this.type = Type.CASH;
    }
    try {
        this.color = c.getInt(c.getColumnIndexOrThrow("color"));
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
    Cursor c = MyApplication.cr().query(TransactionProvider.TRANSACTIONS_URI,
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
    mDbHelper.updateAccountOpeningBalance(id,currentBalance);
    deleteAllTransactions();
  }
  /**
   * For transfers the peer transaction will survive, but we transform it to a normal transaction
   * with a note about the deletion of the peer_transaction
   */
  public void deleteAllTransactions() {
    String[] selectArgs = new String[] { String.valueOf(id) };
    ContentValues args = new ContentValues();
    args.put(KEY_COMMENT, MyApplication.getInstance().getString(R.string.peer_transaction_deleted,label));
    args.put(KEY_CATID,0);
    args.put(KEY_TRANSFER_PEER,0);
    MyApplication.cr().update(TransactionProvider.TRANSACTIONS_URI, args,
        KEY_CATID + " = ? and " + KEY_TRANSFER_PEER + " != 0", selectArgs);
    MyApplication.cr().delete(TransactionProvider.TRANSACTIONS_URI, KEY_ACCOUNTID + " = ?", selectArgs);
  }
  public void exportAllDo(File output) throws IOException {
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy",Locale.US);
    OutputStreamWriter out = new OutputStreamWriter(
        new FileOutputStream(output),
        MyApplication.getInstance().getSettings().getString(MyApplication.PREFKEY_QIF_EXPORT_FILE_ENCODING, "UTF-8"));
    String header = "!Type:" + type.getQifName() + "\n";
    out.write(header);
    Cursor c = MyApplication.cr().query(TransactionProvider.TRANSACTIONS_URI, null,
        "account_id = ?", new String[] { String.valueOf(id) }, null);
    c.moveToFirst();
    while( c.getPosition() < c.getCount() ) {
      String comment = c.getString(
          c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_COMMENT));
      comment = (comment == null || comment.length() == 0) ? "" : "\nM" + comment;
      String full_label = "";
      String label_main =  c.getString(
          c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_LABEL_MAIN));

      if (label_main != null && label_main.length() > 0) {
        long transfer_peer = c.getLong(
            c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_TRANSFER_PEER));
        if (transfer_peer != 0) {
          full_label = "[" + label_main + "]";
        } else {
          full_label = label_main;
          String label_sub =  c.getString(
              c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_LABEL_SUB));
          if (label_sub != null && label_sub.length() > 0) {
            full_label += ":" + label_sub;
          }
        }
        full_label = "\nL" + full_label;
      }

      String payee = c.getString(
          c.getColumnIndexOrThrow("payee"));
      payee = (payee == null || payee.length() == 0) ? "" : "\nP" + payee;
      String dateStr = formatter.format(Utils.fromSQL(c.getString(
          c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_DATE))));
      long amount = c.getLong(
          c.getColumnIndexOrThrow(ExpensesDbAdapter.KEY_AMOUNT));
      String amountStr = new Money(currency,amount)
          .getAmountMajor().toPlainString();
      String row = "D"+ dateStr +
          "\nT" + amountStr +
          comment +
          full_label +
          payee +  
           "\n^\n";
      out.write(row);
      c.moveToNext();
    }
    out.close();
    c.close();
  }
  /**
   * writes all transactions to a QIF file
   * @throws IOException
   */
  public File exportAll(Context ctx) throws IOException {
    SimpleDateFormat now = new SimpleDateFormat("ddMM-HHmm",Locale.US);
    Log.i("MyExpenses","now starting export");
    File appDir = Utils.requireAppDir();
    if (appDir == null)
      throw new IOException();
    File outputFile = new File(appDir,
        label.replaceAll("\\W","") + "-" +
        now.format(new Date()) + ".qif");
    if (outputFile.exists()) {
      Toast.makeText(ctx,String.format(ctx.getString(R.string.export_expenses_outputfile_exists), outputFile.getAbsolutePath() ), Toast.LENGTH_LONG).show();
      return null;
    }
    exportAllDo(outputFile);
    Toast.makeText(ctx,String.format(ctx.getString(R.string.export_expenses_sdcard_success), outputFile.getAbsolutePath() ), Toast.LENGTH_LONG).show();
    return outputFile;
  }
  
  /**
   * Saves the account, creating it new if necessary
   * @return the id of the account. Upon creation it is returned from the database
   */
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("opening_balance",openingBalance.getAmountMinor());
    initialValues.put("description",description);
    initialValues.put("currency",currency.getCurrencyCode());
    initialValues.put("type",type.name());
    initialValues.put("color",color);
    
    if (id == 0) {
      uri = MyApplication.cr().insert(CONTENT_URI, initialValues);
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      MyApplication.cr().update(uri,initialValues,null,null);
    }
    if (!accounts.containsKey(id))
      accounts.put(id, this);
    return uri;
  }
  public long getSize() {
    return mDbHelper.getTransactionCountPerAccount(id);
  }
}

