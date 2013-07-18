package org.totschnig.myexpenses.provider;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

public class TransactionDatabase extends SQLiteOpenHelper {
  public static final int DATABASE_VERSION = 29;
  public static final String DATABASE_NAME = "data";

  private static final String TAG = "TransactionDatabase";
  /**
   * SQL statement for expenses TABLE
   * both transactions and transfers are stored in this table
   * for transfers there are two rows (one per account) which
   * are linked by transfer_peer
   * for normal transactions transfer_peer is set to NULL
   * for transfers cat_id stores the account
   */
  private static final String DATABASE_CREATE =
    "CREATE TABLE " + TABLE_TRANSACTIONS  +  "( "
    + KEY_ROWID            + " integer primary key autoincrement, "
    + KEY_COMMENT          + " text, "
    + KEY_DATE             + " DATETIME not null, "
    + KEY_AMOUNT           + " integer not null, "
    + KEY_CATID            + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
    + KEY_ACCOUNTID        + " integer not null references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + "),"
    + KEY_PAYEE            + " text, "
    + KEY_TRANSFER_PEER    + " integer references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + "), "
    + KEY_TRANSFER_ACCOUNT + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + "),"
    + KEY_METHODID         + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "),"
    + KEY_PARENTID         + " integer references " + TABLE_TRANSACTIONS + "(" + KEY_ROWID + "), "
    + KEY_STATUS           + " integer default 0);";

  /**
   * SQL statement for accounts TABLE
   */
  private static final String ACCOUNTS_CREATE =
    "CREATE TABLE " + TABLE_ACCOUNTS + " ("
        + KEY_ROWID           + " integer primary key autoincrement, "
        + KEY_LABEL           + " text not null, "
        + KEY_OPENING_BALANCE + " integer, "
        + KEY_DESCRIPTION     + " text, "
        + KEY_CURRENCY        + " text not null, "
        + KEY_TYPE            + " text not null check (" + KEY_TYPE + " in (" + Account.Type.JOIN + ")) default '" + Account.Type.CASH.name() + "', "
        + KEY_COLOR           + " integer default -3355444);";


  /**
   * SQL statement for categories TABLE
   * Table definition reflects format of Grisbis categories
   * Main categories have parent_id 0
   * usages counts how often the cat is selected
   */
  private static final String CATEGORIES_CREATE =
    "CREATE TABLE " + TABLE_CATEGORIES + " ("
      + KEY_ROWID    + " integer primary key autoincrement, "
      + KEY_LABEL    + " text not null, "
      + KEY_PARENTID + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
      + KEY_USAGES   + " integer default 0, unique (" + KEY_LABEL + "," + KEY_PARENTID + "));";

  private static final String PAYMENT_METHODS_CREATE =
    "CREATE TABLE " + TABLE_METHODS + " ("
        + KEY_ROWID + " integer primary key autoincrement, " 
        + KEY_LABEL + " text not null, " 
        + KEY_TYPE  + " integer " + 
          "check (" + KEY_TYPE + " in (" 
            + PaymentMethod.EXPENSE + ","
            + PaymentMethod.NEUTRAL + ","
            + PaymentMethod.INCOME +")) default 0);";

  private static final String ACCOUNTTYE_METHOD_CREATE =
      "CREATE TABLE " + TABLE_ACCOUNTTYES_METHODS + " ("
          + KEY_TYPE + " text not null check (" + KEY_TYPE + " in (" + Account.Type.JOIN + ")), "
          + KEY_METHODID + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "), "
          + "primary key (" + KEY_TYPE + "," + KEY_METHODID + "));";

  //in templates, transfer_peer does not point to another instance
  //but is a boolean indicating if the template is for a transfer
  private static final String TEMPLATE_CREATE =
      "CREATE TABLE " + TABLE_TEMPLATES + " ( "
      + KEY_ROWID            + " integer primary key autoincrement, "
      + KEY_COMMENT          + " text not null, "
      + KEY_AMOUNT           + " integer not null, "
      + KEY_CATID            + " integer references " + TABLE_CATEGORIES + "(" + KEY_ROWID + "), "
      + KEY_ACCOUNTID        + " integer not null references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + "),"
      + KEY_PAYEE            + " text, "
      + KEY_TRANSFER_PEER    + " boolean default false, "
      + KEY_TRANSFER_ACCOUNT + " integer references " + TABLE_ACCOUNTS + "(" + KEY_ROWID + "),"
      + KEY_METHODID         + " integer references " + TABLE_METHODS + "(" + KEY_ROWID + "), "
      + KEY_TITLE            + " text not null, "
      + KEY_USAGES           + " integer default 0, "
      + "unique(" + KEY_ACCOUNTID + "," + KEY_TITLE + "));";
  
  /**
   * we store a simple row for each time a feature has been accessed,
   * thus speeding up recording and counting 
   */
  private static final String FEATURE_USED_CREATE =
      "CREATE TABLE " + TABLE_FEATURE_USED + " (feature text not null);";

  /**
   * stores payees and payers
   * this table is only used for populating the autocompleting text field,
   * hence there is no need for using foreign keys from transactions
   */
  private static final String PAYEE_CREATE =
    "CREATE TABLE " + TABLE_PAYEES
      + " (_id integer primary key autoincrement, name text unique not null);";
  public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);

  TransactionDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }
  @Override
  public void onOpen(SQLiteDatabase db) {
      super.onOpen(db);
      if (!db.isReadOnly()) {
          // Enable foreign key constraints
          db.execSQL("PRAGMA foreign_keys=ON;");
      }
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(DATABASE_CREATE);
    db.execSQL(CATEGORIES_CREATE);
    db.execSQL(ACCOUNTS_CREATE);
    db.execSQL(PAYEE_CREATE);
    db.execSQL(PAYMENT_METHODS_CREATE);
    db.execSQL(ACCOUNTTYE_METHOD_CREATE);
    insertDefaultPaymentMethods(db);
    db.execSQL(TEMPLATE_CREATE);
    db.execSQL(FEATURE_USED_CREATE);
  }

  /**
   * @param db
   * insert the predefined payment methods in the database, all of them are valid only for bank accounts
   */
  private void insertDefaultPaymentMethods(SQLiteDatabase db) {
    ContentValues initialValues;
    long _id;
    for (PaymentMethod.PreDefined pm: PaymentMethod.PreDefined.values()) {
      initialValues = new ContentValues();
      initialValues.put(KEY_LABEL, pm.name());
      initialValues.put(KEY_TYPE,pm.paymentType);
      _id = db.insert(TABLE_METHODS, null, initialValues);
      initialValues = new ContentValues();
      initialValues.put(KEY_METHODID, _id);
      initialValues.put(KEY_TYPE,"BANK");
      db.insert(TABLE_ACCOUNTTYES_METHODS, null, initialValues);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
        + newVersion + ".");
    if (oldVersion < 17) {
      db.execSQL("drop table accounts");
      db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, " +
          "opening_balance integer, description text, currency text not null);");
      //db.execSQL("ALTER TABLE expenses add column account_id integer");
    }
    if (oldVersion < 18) {
      db.execSQL("CREATE TABLE payee (_id integer primary key autoincrement, name text unique not null);");
      db.execSQL("ALTER TABLE expenses add column payee text");
    }
    if (oldVersion < 19) {
      db.execSQL("ALTER TABLE expenses add column transfer_peer text");
    }
    if (oldVersion < 20) {
      db.execSQL("CREATE TABLE transactions ( _id integer primary key autoincrement, comment text not null, "
          + "date DATETIME not null, amount integer not null, cat_id integer, account_id integer, "
          + "payee  text, transfer_peer integer default null);");
      db.execSQL("INSERT INTO transactions (comment,date,amount,cat_id,account_id,payee,transfer_peer)" +
          " SELECT comment,date,CAST(ROUND(amount*100) AS INTEGER),cat_id,account_id,payee,transfer_peer FROM expenses");
      db.execSQL("DROP TABLE expenses");
      db.execSQL("ALTER TABLE accounts RENAME to accounts_old");
      db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, " +
          "opening_balance integer, description text, currency text not null);");
      db.execSQL("INSERT INTO accounts (label,opening_balance,description,currency)" +
          " SELECT label,CAST(ROUND(opening_balance*100) AS INTEGER),description,currency FROM accounts_old");
      db.execSQL("DROP TABLE accounts_old");
    }
    if (oldVersion < 21) {
      db.execSQL("CREATE TABLE paymentmethods (_id integer primary key autoincrement, label text not null, type integer default 0);");
      db.execSQL("CREATE TABLE accounttype_paymentmethod (type text, method_id integer, primary key (type,method_id));");
      ContentValues initialValues;
      long _id;
      for (PaymentMethod.PreDefined pm: PaymentMethod.PreDefined.values()) {
        initialValues = new ContentValues();
        initialValues.put("label", pm.name());
        initialValues.put("type",pm.paymentType);
        _id = db.insert("paymentmethods", null, initialValues);
        initialValues = new ContentValues();
        initialValues.put("method_id", _id);
        initialValues.put("type","BANK");
        db.insert("accounttype_paymentmethod", null, initialValues);
      }
      db.execSQL("ALTER TABLE transactions add column payment_method_id integer");
      db.execSQL("ALTER TABLE accounts add column type text default 'CASH'");
    }
    if (oldVersion < 22) {
      db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text not null, "
        + "amount integer not null, cat_id integer, account_id integer, payee text, transfer_peer integer default null, "
        + "payment_method_id integer, title text not null);");
    }
    if (oldVersion < 23) {
      db.execSQL("ALTER TABLE templates RENAME to templates_old");
      db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text not null, "
          + "amount integer not null, cat_id integer, account_id integer, payee text, transfer_peer integer default null, "
          + "payment_method_id integer, title text not null, unique(account_id, title));");
      try {
        db.execSQL("INSERT INTO templates(comment,amount,cat_id,account_id,payee,transfer_peer,payment_method_id,title)" +
            " SELECT comment,amount,cat_id,account_id,payee,transfer_peer,payment_method_id,title FROM templates_old");
      } catch (SQLiteConstraintException e) {
        Log.e(TAG,e.getLocalizedMessage());
        //theoretically we could have entered duplicate titles for one account
        //we silently give up in that case (since this concerns only a narrowly distributed alpha version)
      }
      db.execSQL("DROP TABLE templates_old");
    }
    if (oldVersion < 24) {
      db.execSQL("ALTER TABLE templates add column usages integer default 0");
    }
    if (oldVersion < 25) {
      //for transactions that were not transfers, transfer_peer was set to null in transactions, but to 0 in templates
      db.execSQL("update transactions set transfer_peer=0 WHERE transfer_peer is null;");
    }
    if (oldVersion < 26) {
      db.execSQL("alter table accounts add column color integer default -6697984");
    }
    if (oldVersion < 27) {
      db.execSQL("CREATE TABLE feature_used (feature text not null);");
    }
    if (oldVersion < 28) {
      db.execSQL("ALTER TABLE transactions RENAME to transactions_old");
      db.execSQL("CREATE TABLE transactions(_id integer primary key autoincrement, comment text, date DATETIME not null, amount integer not null, " +
          "cat_id integer references categories(_id), account_id integer not null references accounts(_id),payee text, " +
          "transfer_peer integer references transactions(_id), transfer_account integer references accounts(_id), " +
          "method_id integer references paymentmethods(_id));");
      db.execSQL("INSERT INTO transactions (_id,comment,date,amount,cat_id,account_id,payee,transfer_peer,transfer_account,method_id) " +
          "SELECT _id,comment,date,amount, "+
          "CASE WHEN transfer_peer THEN null ELSE CASE WHEN cat_id THEN cat_id ELSE null END END, " +
          "account_id,payee, " +
          "CASE WHEN transfer_peer THEN transfer_peer ELSE null END, " +
          "CASE WHEN transfer_peer THEN cat_id ELSE null END, " +
          "CASE WHEN payment_method_id THEN payment_method_id ELSE null END " +
          "FROM transactions_old");
      db.execSQL("ALTER TABLE accounts RENAME to accounts_old");
      db.execSQL("CREATE TABLE accounts (_id integer primary key autoincrement, label text not null, opening_balance integer, description text, " +
          "currency text not null, type text not null check (type in ('CASH','BANK','CCARD','ASSET','LIABILITY')) default 'CASH', color integer default -3355444);");
      db.execSQL("INSERT INTO accounts (_id,label,opening_balance,description,currency,type,color) " +
          "SELECT _id,label,opening_balance,description,currency,type,color FROM accounts_old");
      //previously templates where not deleted if referred to accounts were deleted
      db.execSQL("DELETE FROM templates where account_id not in (SELECT _id FROM accounts) or (cat_id != 0 and transfer_peer = 1 and cat_id not in (SELECT _id from accounts))");
      db.execSQL("ALTER TABLE templates RENAME to templates_old");
      db.execSQL("CREATE TABLE templates ( _id integer primary key autoincrement, comment text not null, amount integer not null, " +
          "cat_id integer references categories(_id), account_id integer not null references accounts(_id),payee text, " +
          "transfer_peer boolean default false, transfer_account integer references accounts(_id),method_id integer references paymentmethods(_id), " +
          "title text not null, usages integer default 0, unique(account_id,title));");
      db.execSQL("INSERT INTO templates (_id,comment,amount,cat_id,account_id,payee,transfer_peer,transfer_account,method_id,title,usages) " +
          "SELECT _id,comment,amount," +
          "CASE WHEN transfer_peer THEN null ELSE CASE WHEN cat_id THEN cat_id ELSE null END END, " +
          "account_id,payee, " +
          "CASE WHEN transfer_peer THEN 1 ELSE 0 END, " +
          "CASE WHEN transfer_peer THEN cat_id ELSE null END, " +
          "CASE WHEN payment_method_id THEN payment_method_id ELSE null END, " +
          "title,usages FROM templates_old");
      db.execSQL("ALTER TABLE categories RENAME to categories_old");
      db.execSQL("CREATE TABLE categories (_id integer primary key autoincrement, label text not null, parent_id integer references categories(_id), " +
          "usages integer default 0, unique (label,parent_id));");
      db.execSQL("INSERT INTO categories (_id,label,parent_id,usages) " +
          "SELECT _id,label,CASE WHEN parent_id THEN parent_id ELSE null END,usages FROM categories_old");
      db.execSQL("ALTER TABLE paymentmethods RENAME to paymentmethods_old");
      db.execSQL("CREATE TABLE paymentmethods (_id integer primary key autoincrement, label text not null, type integer check (type in (-1,0,1)) default 0);");
      db.execSQL("INSERT INTO paymentmethods (_id,label,type) SELECT _id,label,type FROM paymentmethods_old");
      db.execSQL("ALTER TABLE accounttype_paymentmethod RENAME to accounttype_paymentmethod_old");
      db.execSQL("CREATE TABLE accounttype_paymentmethod (type text not null check (type in ('CASH','BANK','CCARD','ASSET','LIABILITY')), method_id integer references paymentmethods (_id), primary key (type,method_id));");
      db.execSQL("INSERT INTO accounttype_paymentmethod (type,method_id) SELECT type,method_id FROM accounttype_paymentmethod_old");
      db.execSQL("DROP TABLE transactions_old");
      db.execSQL("DROP TABLE accounts_old");
      db.execSQL("DROP TABLE templates_old");
      db.execSQL("DROP TABLE categories_old");
      db.execSQL("DROP TABLE paymentmethods_old");
      db.execSQL("DROP TABLE accounttype_paymentmethod_old");
      //Changes to handle
      //1) Transfer account no longer stored as cat_id but in transfer_account (in transactions and templates)
      //2) parent_id for categories uses foreign key on itself, hence root categories have null instead of 0 as parent_id
      //3) catId etc now need to be null instead of 0
      //4) transactions payment_method_id renamed to method_id
    }
    if (oldVersion < 29) {
      db.execSQL("ALTER TABLE transactions add column status integer default 0");
    }
  }
}
