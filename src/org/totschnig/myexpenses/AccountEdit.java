package org.totschnig.myexpenses;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AccountEdit extends Activity {
  private ExpensesDbAdapter mDbHelper;
  private EditText mLabelText;
  private EditText mDescriptionText;
  private EditText mOpeningBalanceText;
  private EditText mCurrencyText;
  private Long mRowId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDbHelper = new ExpensesDbAdapter(this);
    mDbHelper.open();
    setContentView(R.layout.one_account);

    mLabelText = (EditText) findViewById(R.id.Label);
    mDescriptionText = (EditText) findViewById(R.id.Description);
    mOpeningBalanceText = (EditText) findViewById(R.id.Opening_balance);
    mCurrencyText = (EditText) findViewById(R.id.Currency);

    Button confirmButton = (Button) findViewById(R.id.Confirm);
    Button cancelButton = (Button) findViewById(R.id.Cancel);

    mRowId = savedInstanceState != null ? savedInstanceState.getLong(ExpensesDbAdapter.KEY_ROWID) 
        : null;
    Bundle extras = getIntent().getExtras();
    if (mRowId == null) {
      mRowId = extras != null ? extras.getLong(ExpensesDbAdapter.KEY_ROWID) 
          : 0;
    }

    confirmButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
        saveState();
        finish();
      }
    });
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(RESULT_OK);
        finish();
      }
    });
    populateFields();
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    mDbHelper.close();
  }

  private void populateFields() {
    float opening_balance;
    if (mRowId != 0) {
      setTitle(R.string.menu_edit_account);
      Cursor note = mDbHelper.fetchAccount(mRowId);
      startManagingCursor(note);
      try {
        opening_balance = Float.valueOf(note.getString(
            note.getColumnIndexOrThrow("opening_balance")));
      } catch (NumberFormatException e) {
        opening_balance = 0;
      }
      mLabelText.setText(note.getString(
          note.getColumnIndexOrThrow("label")));
      mDescriptionText.setText(note.getString(
          note.getColumnIndexOrThrow("description")));
      mOpeningBalanceText.setText(Float.toString(opening_balance));
      mCurrencyText.setText(note.getString(
          note.getColumnIndexOrThrow("currency")));
    } else {
      setTitle(R.string.menu_insert_account);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong(ExpensesDbAdapter.KEY_ROWID, mRowId);
  }

  private void saveState() {
    String label = mLabelText.getText().toString();
    String description = mDescriptionText.getText().toString();
    String opening_balance = mOpeningBalanceText.getText().toString();
    String currency = mCurrencyText.getText().toString();

    if (mRowId == 0) {
      long id = mDbHelper.createAccount(label, opening_balance, description,currency);
      if (id > 0) {
        mRowId = id;
      }
    } else {
      mDbHelper.updateAccount(mRowId, label,opening_balance,description,currency);
    }
  }

}
