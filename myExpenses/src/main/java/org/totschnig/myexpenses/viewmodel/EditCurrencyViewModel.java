package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import javax.inject.Inject;

public class EditCurrencyViewModel extends AndroidViewModel {
  @Inject
  protected CurrencyContext currencyContext;
  private final InsertHandler asyncInsertHandler;

  public EditCurrencyViewModel(@NonNull Application application) {
    super(application);
    final ContentResolver contentResolver = application.getContentResolver();
    asyncInsertHandler = new InsertHandler(contentResolver);
    ((MyApplication) application).getAppComponent().inject(this);
  }

  public void saveSymbol(Currency currency, String symbol) {
    if (currencyContext.storeCustomSymbol(currency.code(), symbol)) {
      CurrencyFormatter.instance().invalidate(currency.code());
    }
  }

  public void saveFractionDigits(Currency currency, int numberFractionDigits, boolean withUpdate) {
    if (withUpdate) {
      asyncInsertHandler.startUpdate(0, null,
          TransactionProvider.CURRENCIES_URI.buildUpon()
              .appendPath(TransactionProvider.URI_SEGMENT_CHANGE_FRACTION_DIGITS)
              .appendPath(currency.code())
              .appendPath(String.valueOf(numberFractionDigits))
              .build(), null, null, null);
    } else {
      currencyContext.storeCustomFractionDigits(currency.code(), numberFractionDigits);
      CurrencyFormatter.instance().invalidate(currency.code());
    }
  }

  private static class InsertHandler extends AsyncQueryHandler {

    InsertHandler(ContentResolver cr) {
      super(cr);
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {

    }
  }
}
