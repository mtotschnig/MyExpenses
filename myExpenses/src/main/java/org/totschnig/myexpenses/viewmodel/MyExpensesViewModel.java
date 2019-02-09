package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.provider.TransactionProvider;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN;

public class MyExpensesViewModel extends AndroidViewModel {
  private BriteContentResolver briteContentResolver;
  private Disposable disposable;

  public LiveData<Boolean> getHasHiddenAccounts() {
    return hasHiddenAccounts;
  }

  private final MutableLiveData<Boolean> hasHiddenAccounts = new MutableLiveData<>();

  public MyExpensesViewModel(@NonNull Application application) {
    super(application);
    briteContentResolver = new SqlBrite.Builder().build().wrapContentProvider(application.getContentResolver(), Schedulers.io());
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
