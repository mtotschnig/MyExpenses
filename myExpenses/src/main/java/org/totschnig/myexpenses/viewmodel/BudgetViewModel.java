package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.viewmodel.data.Budget;

import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public class BudgetViewModel extends AndroidViewModel {
  static final int TOKEN = 0;
  private final MutableLiveData<List<Budget>> currentBudget = new MutableLiveData<>();
  private final InsertHandler asyncInsertHandler;
  private BriteContentResolver briteContentResolver;
  private Disposable budgetDisposable;
  public BudgetViewModel(@NonNull Application application) {
    super(application);
    final ContentResolver contentResolver = application.getContentResolver();
    asyncInsertHandler = new InsertHandler(contentResolver);
    briteContentResolver = new SqlBrite.Builder().build().wrapContentProvider(contentResolver, Schedulers.io());
  }

  public MutableLiveData<List<Budget>> getData() {
    return currentBudget;
  }

  public void loadBudgets(final long accountId, @NonNull final String currencyStr, Function<Cursor, Budget> budgetCreatorFunction) {
    String selection = (accountId != 0 ? KEY_ACCOUNTID : KEY_CURRENCY) + " = ?";
    String[] selectionArgs = new String[] {accountId != 0 ? String.valueOf(accountId) : currencyStr};
    final String[] projection = {KEY_ROWID, KEY_TYPE, KEY_AMOUNT};
    budgetDisposable = briteContentResolver.createQuery(TransactionProvider.BUDGETS_URI,
        projection, selection, selectionArgs, null, true)
        .mapToList(budgetCreatorFunction)
        .subscribe(currentBudget::postValue);
  }

  public void createBudget(Budget budget) {
    asyncInsertHandler.startInsert(TOKEN, null, TransactionProvider.BUDGETS_URI,
        budget.toContentValues());
  }

  @Override
  protected void onCleared() {
    budgetDisposable.dispose();
  }

  public void updateBudget(long id, Money amount) {
    ContentValues contentValues = new ContentValues(1);
    contentValues.put(KEY_AMOUNT, amount.getAmountMinor());
    asyncInsertHandler.startUpdate(TOKEN, null,
        ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, id),
        contentValues, null, null);
  }

  private static class InsertHandler extends AsyncQueryHandler {

    InsertHandler(ContentResolver cr) {
      super(cr);
    }
  }
}
