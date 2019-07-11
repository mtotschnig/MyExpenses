package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.content.ContentUris;
import android.net.Uri;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.TransactionProvider;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TransactionListViewModel extends AndroidViewModel {

  private BriteContentResolver briteContentResolver;
  private Disposable disposable;

  private final MutableLiveData<Account> account = new MutableLiveData<>();

  public TransactionListViewModel(@NonNull Application application) {
    super(application);
    briteContentResolver = new SqlBrite.Builder().build().wrapContentProvider(application.getContentResolver(), Schedulers.io());
  }

  public MutableLiveData<Account> getAccount() {
    return account;
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

  public void loadAccount(long accountId) {
    dispose();
    final Uri base = accountId > 0 ? TransactionProvider.ACCOUNTS_URI : TransactionProvider.ACCOUNTS_AGGREGATE_URI;
    disposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
        Account.PROJECTION_EXTENDED, null, null, null, true)
        .mapToOne(Account::fromCursor)
        .subscribe(TransactionListViewModel.this.account::postValue);
  }
}
