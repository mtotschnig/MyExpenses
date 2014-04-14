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

package org.totschnig.myexpenses.activity;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for editing an account
 * @author Michael Totschnig
 */
public class AccountEdit extends AmountActivity implements OnItemSelectedListener {
  private static final String OPENINTENTS_COLOR_EXTRA = "org.openintents.extra.COLOR";
  private static final String OPENINTENTS_PICK_COLOR_ACTION = "org.openintents.action.PICK_COLOR";
  private static final int PICK_COLOR_REQUEST = 11;
  private EditText mLabelText;
  private EditText mDescriptionText;
  private Spinner mCurrencySpinner;
  private Spinner mAccountTypeSpinner;
  private Spinner mColorSpinner;
  Account mAccount;
  private ArrayList<Integer> mColors;
  private boolean mColorIntentAvailable;
  private Intent mColorIntent;
  private ArrayAdapter<Integer> mColAdapter;
  
  @SuppressLint("InlinedApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.one_account);
    changeEditTextBackground((ViewGroup)findViewById(android.R.id.content));
    configAmountInput();

    mLabelText = (EditText) findViewById(R.id.Label);
    mDescriptionText = (EditText) findViewById(R.id.Description);

    Bundle extras = getIntent().getExtras();
    long rowId = extras != null ? extras.getLong(DatabaseConstants.KEY_ROWID)
          : 0;
    if (rowId != 0) {
      mAccount = Account.getInstanceFromDb(rowId);
      if (mAccount == null) {
        Toast.makeText(this,"Error instantiating account "+rowId,Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
      setTitle(R.string.menu_edit_account);
      mLabelText.setText(mAccount.label);
      mDescriptionText.setText(mAccount.description);
    } else {
      setTitle(R.string.menu_create_account);
      mAccount = new Account();
      String currency = extras != null ? extras.getString(DatabaseConstants.KEY_CURRENCY) : null;
      if (currency != null)
        mAccount.setCurrency(currency);
    }
    
    

    mCurrencySpinner = (Spinner) findViewById(R.id.Currency);
    ArrayAdapter<Account.CurrencyEnum> curAdapter = new ArrayAdapter<Account.CurrencyEnum>(
        this, android.R.layout.simple_spinner_item, android.R.id.text1,Account.CurrencyEnum.values());
    curAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mCurrencySpinner.setAdapter(curAdapter);
    
    mAccountTypeSpinner = (Spinner) findViewById(R.id.AccountType);
    ArrayAdapter<Account.Type> typAdapter = new ArrayAdapter<Account.Type>(
        this, android.R.layout.simple_spinner_item, android.R.id.text1,Account.Type.values());
    typAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mAccountTypeSpinner.setAdapter(typAdapter);
    
    mColorSpinner = (Spinner) findViewById(R.id.Color);
    mColors = new ArrayList<Integer>();
    if (Build.VERSION.SDK_INT > 13) {
      Resources r = getResources();
      mColors.add(r.getColor(android.R.color.holo_blue_bright));
      mColors.add(r.getColor(android.R.color.holo_blue_light));
      mColors.add(r.getColor(android.R.color.holo_blue_dark));
      mColors.add(r.getColor(android.R.color.holo_green_dark));
      mColors.add(r.getColor(android.R.color.holo_green_light));
      mColors.add(r.getColor(android.R.color.holo_orange_dark));
      mColors.add(r.getColor(android.R.color.holo_orange_light));
      mColors.add(r.getColor(android.R.color.holo_purple));
      mColors.add(r.getColor(android.R.color.holo_red_dark));
      mColors.add(r.getColor(android.R.color.holo_red_light));
    } else {
      mColors.add(Color.BLUE);
      mColors.add(Color.CYAN);
      mColors.add(Color.GREEN);
      mColors.add(Color.MAGENTA);
      mColors.add(Color.RED);
      mColors.add(Color.YELLOW);
      mColors.add(Color.BLACK);
      mColors.add(Color.DKGRAY);
      mColors.add(Color.GRAY);
      mColors.add(Color.LTGRAY);
      mColors.add(Color.WHITE);
    }
    if (mColors.indexOf(mAccount.color) == -1)
      mColors.add(mAccount.color);
    mColors.add(0);
    mColorIntent = new Intent(OPENINTENTS_PICK_COLOR_ACTION);
    mColorIntentAvailable = Utils.isIntentAvailable(AccountEdit.this, mColorIntent);
    mColAdapter = new ArrayAdapter<Integer>(this,
        android.R.layout.simple_spinner_item, mColors) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getView(position, convertView, parent);
        if (mColors.get(position) != 0)
          setColor(tv,mColors.get(position));
        else
          setColor(tv,mAccount.color);
        if (getResources().getConfiguration().orientation ==  android.content.res.Configuration.ORIENTATION_LANDSCAPE ) {
          tv.setTextColor(Utils.getTextColorForBackground(mAccount.color));
          tv.setText(R.string.color);
        }
        return tv;
      }
      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
        if (mColors.get(position) != 0)
          setColor(tv,mColors.get(position));
        else {
          tv.setBackgroundColor(getResources().getColor(android.R.color.black));
          tv.setTextColor(getResources().getColor(android.R.color.white));
          if (mColorIntentAvailable)
            tv.setText("OI Color Picker");
          else
            tv.setText(R.string.oi_pick_colors_info);
        }
        return tv;
      }
      public void setColor(TextView tv,int color) {
        tv.setBackgroundColor(color);
        tv.setText("");
      }
    };
    mColAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mColorSpinner.setAdapter(mColAdapter);
    populateFields();
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("type", mType);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mType = savedInstanceState.getBoolean("type");
    configureType();
  }
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_COLOR_REQUEST) {
      if (resultCode == RESULT_OK) {
        mAccount.color = data.getExtras().getInt(OPENINTENTS_COLOR_EXTRA);
        if (mColors.indexOf(mAccount.color) == -1) {
          final int lastButOne = mColors.size()-1;
          mColors.add(lastButOne,mAccount.color);
          mColorSpinner.setSelection(lastButOne,true);
          mColAdapter.notifyDataSetChanged();
        }
      }
    }
  }
  /**
   * populates the input field either from the database or with default value for currency (from Locale)
   */
  private void populateFields() {
    mTypeButton = (Button) findViewById(R.id.TaType);
    mTypeButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        mType = ! mType;
        configureType();
      }
    });

    BigDecimal amount = mAccount.openingBalance.getAmountMajor();
    if (amount.signum() == -1) {
      amount = amount.abs();
    } else {
      mType = INCOME;
      configureType();
    }
    mAmountText.setText(nfDLocal.format(amount));
    mCurrencySpinner.setSelection(Account.CurrencyEnum.valueOf(mAccount.currency.getCurrencyCode()).ordinal());
    mAccountTypeSpinner.setSelection(mAccount.type.ordinal());
    int selected = mColors.indexOf(mAccount.color);
    mColorSpinner.setSelection(selected);
    mColorSpinner.post(new Runnable() {
      public void run() {
        mColorSpinner.setOnItemSelectedListener(AccountEdit.this);
      }
    });
  }

  /**
   * validates currency (must be code from ISO 4217) and opening balance
   * (a valid float according to the format from the locale)
   * @return true upon success, false if validation fails
   */
  protected void saveState() {
    BigDecimal openingBalance = validateAmountInput(true);
    if (openingBalance == null)
       return;
    String label;
    mAccount.setCurrency(((Account.CurrencyEnum) mCurrencySpinner.getSelectedItem()).name());

    label = mLabelText.getText().toString();
    if (label.equals("")) {
      mLabelText.setError(getString(R.string.no_title_given));
      return;
    }
    mAccount.label = label;
    mAccount.description = mDescriptionText.getText().toString();
    if (mType == EXPENSE) {
      openingBalance = openingBalance.negate();
    }
    mAccount.openingBalance.setAmountMajor(openingBalance);
    mAccount.type = (Type) mAccountTypeSpinner.getSelectedItem();
    //EditActivity.saveState calls DbWriteFragment
    super.saveState();
  }
  @Override
  public Model getObject() {
    // TODO Auto-generated method stub
    return mAccount;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
      long id) {
    if (mColors.get(position) != 0)
      mAccount.color = mColors.get(position);
    else {
      if (mColorIntentAvailable) {
        mColorIntent.putExtra(OPENINTENTS_COLOR_EXTRA, mAccount.color);
        startActivityForResult(mColorIntent, PICK_COLOR_REQUEST);
      } else {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(MyApplication.MARKET_PREFIX + "org.openintents.colorpicker"));
          startActivity(intent);
        } catch(Exception e) {
            Toast.makeText(
                AccountEdit.this,
                R.string.error_accessing_market,
                Toast.LENGTH_SHORT)
              .show();
        }
      }
    }
  }
  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // TODO Auto-generated method stub
  }

  /*
   * callback of DbWriteFragment
   */
  @Override
  public void onPostExecute(Object result) {
    Intent intent=new Intent();
    intent.putExtra(DatabaseConstants.KEY_ROWID, ContentUris.parseId((Uri)result));
    setResult(RESULT_OK,intent);
    finish();
    //no need to call super after finish
  }
}
