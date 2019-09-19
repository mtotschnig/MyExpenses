package org.totschnig.myexpenses.viewmodel;

import android.app.Application;

import com.squareup.sqlbrite3.BriteContentResolver;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.disposables.Disposable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN;

public class MyExpensesViewModel extends AndroidViewModel {
  @Inject
  BriteContentResolver briteContentResolver;
  private Disposable disposable;

  public LiveData<Boolean> getHasHiddenAccounts() {
    return hasHiddenAccounts;
  }

  private final MutableLiveData<Boolean> hasHiddenAccounts = new MutableLiveData<>();

  public MyExpensesViewModel(@NonNull Application application) {
    super(application);
    ((MyApplication) application).getAppComponent().inject(this);
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

  public void loadHiddenAccountCount() {
    disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_URI,
        new String[] {"count(*)"}, KEY_HIDDEN + " = 1", null, null, false )
        .mapToOne(cursor -> cursor.getInt(0) > 0)
        .subscribe(MyExpensesViewModel.this.hasHiddenAccounts::postValue);
  }
}
