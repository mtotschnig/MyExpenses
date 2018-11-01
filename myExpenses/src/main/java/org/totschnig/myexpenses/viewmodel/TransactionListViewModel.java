package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.ContentUris;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.TransactionProvider;

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
    if (disposable != null && !disposable.isDisposed()) {
      disposable.dispose();
    }
  }

  public void loadAccount(long accountId) {
    final Uri base = accountId > 0 ? TransactionProvider.ACCOUNTS_URI : TransactionProvider.ACCOUNTS_AGGREGATE_URI;
    disposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
        Account.PROJECTION_EXTENDED, null, null, null, true)
        .mapToOne(Account::fromCursor)
        .subscribe(TransactionListViewModel.this.account::postValue);
  }
}
