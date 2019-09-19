package org.totschnig.myexpenses.viewmodel;

import android.app.Application;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ExpenseEditViewModel extends AndroidViewModel {
  @Inject
  BriteContentResolver briteContentResolver;
  private Disposable disposable;

  private final MutableLiveData<List<PaymentMethod>> methods = new MutableLiveData<>();

  public ExpenseEditViewModel(@NonNull Application application) {
    super(application);
    ((MyApplication) application).getAppComponent().inject(this);  }

  public LiveData<List<PaymentMethod>> getMethods() {
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
