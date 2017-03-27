package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.fragment.StaleImagesList;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, UiModule.class})
public interface AppComponent {
  void inject(MyApplication application);
  void inject(ExpenseEdit expenseEdit);
  void inject(MyExpenses myExpenses);
  void inject(TransactionDetailFragment transactionDetailFragment);
  void inject(StaleImagesList staleImagesList);
}
