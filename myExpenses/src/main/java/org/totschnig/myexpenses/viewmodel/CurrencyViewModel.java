package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.text.Collator;
import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CurrencyViewModel extends AndroidViewModel {
  private BriteContentResolver briteContentResolver;
  private Disposable disposable;

  private final MutableLiveData<List<Currency>> currencies = new MutableLiveData<>();

  public CurrencyViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<Currency>> getCurrencies() {
    return currencies;
  }

  @Override
  protected void onCleared() {
    dispose();
  }

  private void dispose() {
    if (disposable != null && !disposable.isDisposed()) {
      disposable.dispose();
    }
  }

  public void loadCurrencies() {
    ensureSqlBrite();
    final Collator collator = Collator.getInstance();
    disposable = briteContentResolver.createQuery(TransactionProvider.CURRENCIES_URI,
        null, null, null, null, true)
        .mapToList(Currency::create)
        .subscribe(currencies -> {
          Collections.sort(currencies, (lhs, rhs) -> {
            int classCompare = Utils.compare(lhs.sortClass(), rhs.sortClass());
            return classCompare == 0 ?
                collator.compare(lhs.toString(), rhs.toString()) : classCompare;
          });
          this.currencies.postValue(currencies);
        });
  }

  private void ensureSqlBrite() {
    briteContentResolver = new SqlBrite.Builder().build().wrapContentProvider(getApplication().getContentResolver(), Schedulers.io());
  }

  public Currency getDefault() {
    return Currency.create(Utils.getHomeCurrency().code());
  }

}
