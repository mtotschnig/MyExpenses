package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import javax.inject.Inject;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;

public class EditCurrencyViewModel extends AndroidViewModel {
  interface Listener {
    void onUpdateComplete(int token, int result);
  }

  private static final int TOKEN_UPDATE_FRACTION_DIGITS = 1;
  private static final int TOKEN_UPDATE_LABEL = 2;
  @Inject
  protected CurrencyContext currencyContext;
  private final UpdateHandler asyncUpdateHandler;
  private int updateOperationsCount = 0;
  private Integer updatedAccountsCount = null;

  private MutableLiveData<Integer> updateComplete = new MutableLiveData<>();

  public LiveData<Integer> getUpdateComplete() {
    return updateComplete;
  }

  public EditCurrencyViewModel(@NonNull Application application) {
    super(application);
    final ContentResolver contentResolver = application.getContentResolver();
    asyncUpdateHandler = new UpdateHandler(contentResolver, (token, result) -> {
      updateOperationsCount--;
      if (token == TOKEN_UPDATE_FRACTION_DIGITS) {
        updatedAccountsCount = result;
      }
      if (updateOperationsCount == 0) {
        updateComplete.postValue(updatedAccountsCount);
      }
    });

    ((MyApplication) application).getAppComponent().inject(this);
  }

  public void save(String currency, String symbol, int fractionDigits, String label, boolean withUpdate) {
    CurrencyFormatter.instance().invalidate(currency);
    currencyContext.storeCustomSymbol(currency, symbol);
    if (withUpdate) {
      updateOperationsCount++;
      asyncUpdateHandler.startUpdate(TOKEN_UPDATE_FRACTION_DIGITS, null,
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
      asyncUpdateHandler.startUpdate(TOKEN_UPDATE_LABEL, null,
          TransactionProvider.CURRENCIES_URI, contentValues, KEY_CODE + " = ?", new String[]{currency});
    }
    if (updateOperationsCount == 0) {
      updateComplete.postValue(null);
    }
  }

  static class UpdateHandler extends AsyncQueryHandler {

    private final Listener listener;

    UpdateHandler(ContentResolver cr, Listener listener) {
      super(cr);
      this.listener = listener;
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
     listener.onUpdateComplete(token, result);
    }
  }
}
