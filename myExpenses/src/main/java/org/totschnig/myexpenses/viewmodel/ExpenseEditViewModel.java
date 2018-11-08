package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod;

import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ExpenseEditViewModel extends AndroidViewModel {
  private BriteContentResolver briteContentResolver;
  private Disposable disposable;

  private final MutableLiveData<List<PaymentMethod>> methods = new MutableLiveData<>();

  public ExpenseEditViewModel(@NonNull Application application) {
    super(application);
    briteContentResolver = new SqlBrite.Builder().build().wrapContentProvider(application.getContentResolver(), Schedulers.io());
  }

  public MutableLiveData<List<PaymentMethod>> getMethods() {
    return methods;
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

  public void loadMethods(boolean isIncome, AccountType type) {
    disposable = briteContentResolver.createQuery(TransactionProvider.METHODS_URI.buildUpon()
        .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
        .appendPath(isIncome ? "1" : "-1")
        .appendPath(type.name()).build(),
    null, null, null, null, false)
        .mapToList(PaymentMethod::create)
        .subscribe(ExpenseEditViewModel.this.methods::postValue);
  }
}
