package org.totschnig.myexpenses.viewmodel;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.ICurrencyFormatter;

import javax.inject.Inject;

public class EditCurrencyViewModel extends CurrencyViewModel {

  private static final int TOKEN_UPDATE_FRACTION_DIGITS = 1;
  private static final int TOKEN_UPDATE_LABEL = 2;
  private static final int TOKEN_INSERT_CURRENCY = 3;
  private static final int TOKEN_DELETE_CURRENCY = 4;

  @Inject
  protected ICurrencyFormatter currencyFormatter;

  private final DatabaseHandler asyncDatabaseHandler;
  private int updateOperationsCount = 0;
  private Integer updatedAccountsCount = null;

  private final MutableLiveData<Integer> updateComplete = new MutableLiveData<>();

  private final MutableLiveData<Boolean> insertComplete = new MutableLiveData<>();

  private final MutableLiveData<Boolean> deleteComplete = new MutableLiveData<>();

  public LiveData<Integer> getUpdateComplete() {
    return updateComplete;
  }

  public LiveData<Boolean> getInsertComplete() {
    return insertComplete;
  }

  public LiveData<Boolean> getDeleteComplete() {
    return deleteComplete;
  }

  public EditCurrencyViewModel(@NonNull Application application) {
    super(application);
    final ContentResolver contentResolver = application.getContentResolver();
    asyncDatabaseHandler = new DatabaseHandler(contentResolver);
  }

  public void save(String currency, String symbol, int fractionDigits, String label, boolean withUpdate) {
    DatabaseHandler.UpdateListener updateListener = (token, resultCount) -> {
      updateOperationsCount--;
      if (token == TOKEN_UPDATE_FRACTION_DIGITS) {
        updatedAccountsCount = resultCount;
      }
      if (updateOperationsCount == 0) {
        updateComplete.postValue(updatedAccountsCount);
      }
    };
    currencyFormatter.invalidate(getApplication().getContentResolver(), currency);
    currencyContext.storeCustomSymbol(currency, symbol);
    if (withUpdate) {
      updateOperationsCount++;
      asyncDatabaseHandler.startUpdate(TOKEN_UPDATE_FRACTION_DIGITS, updateListener,
          TransactionProvider.CURRENCIES_URI.buildUpon()
              .appendPath(TransactionProvider.URI_SEGMENT_CHANGE_FRACTION_DIGITS)
              .appendPath(currency)
              .appendPath(String.valueOf(fractionDigits))
              .build(), null, null, null);
    } else {
      currencyContext.storeCustomFractionDigits(currency, fractionDigits);
    }
    if (label != null) {
      updateOperationsCount++;
      ContentValues contentValues = new ContentValues(1);
      contentValues.put(KEY_LABEL, label);
      asyncDatabaseHandler.startUpdate(TOKEN_UPDATE_LABEL, updateListener, buildItemUri(currency),
          contentValues, null, null);
    }
    if (updateOperationsCount == 0) {
      updateComplete.postValue(null);
    }
  }


  public void newCurrency(String code, String symbol, int fractionDigits, String label) {
    ContentValues contentValues = new ContentValues(2);
    contentValues.put(KEY_LABEL, label);
    contentValues.put(KEY_CODE, code);
    asyncDatabaseHandler.startInsert(TOKEN_INSERT_CURRENCY, (DatabaseHandler.InsertListener) (token, uri) -> {
      boolean success = uri != null;
      if (success) {
        currencyContext.storeCustomSymbol(code, symbol);
        currencyContext.storeCustomFractionDigits(code, fractionDigits);
      }
      insertComplete.postValue(success);
    }, TransactionProvider.CURRENCIES_URI, contentValues);
  }


  public void deleteCurrency(String currency) {
    asyncDatabaseHandler.startDelete(TOKEN_DELETE_CURRENCY, (DatabaseHandler.DeleteListener) (token, result) -> deleteComplete.postValue(result == 1), buildItemUri(currency), null, null);
  }

  protected Uri buildItemUri(String currency) {
    return TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(currency).build();
  }

}
