package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.AmountFilterDialog;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;
import org.totschnig.myexpenses.dialog.ExportDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectFromTableDialogFragment;
import org.totschnig.myexpenses.export.pdf.PdfPrinter;
import org.totschnig.myexpenses.fragment.BaseTransactionList;
import org.totschnig.myexpenses.fragment.BudgetList;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.fragment.CurrencyList;
import org.totschnig.myexpenses.fragment.HistoryChart;
import org.totschnig.myexpenses.fragment.OnBoardingPrivacyFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.SettingsFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.fragment.StaleImagesList;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.ExchangeRateRepository;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.AutoBackupService;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;
import org.totschnig.myexpenses.task.LicenceApiTask;
import org.totschnig.myexpenses.ui.DiscoveryHelper;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel;
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.ExchangeRateViewModel;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel;
import org.totschnig.myexpenses.viewmodel.TransactionViewModel;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplatetRemoteViewsFactory;

import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.VisibleForTesting;
import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, UiModule.class, UtilsModule.class, NetworkModule.class, LicenceModule.class, DbModule.class, CoroutineModule.class})
public interface AppComponent {
  @Singleton
  DiscoveryHelper discoveryHelper();

  String USER_COUNTRY = "userCountry";

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder applicationContext(MyApplication applicationContext);

    Builder licenceModule(LicenceModule licenceModule);

    Builder coroutineModule(CoroutineModule coroutineModule);

    AppComponent build();
  }

  void inject(MyApplication application);

  void inject(ExpenseEdit expenseEdit);

  void inject(MyExpenses myExpenses);

  void inject(ProtectedFragmentActivity protectedFragmentActivity);

  void inject(TransactionDetailFragment transactionDetailFragment);

  void inject(StaleImagesList staleImagesList);

  void inject(PdfPrinter pdfPrinter);

  void inject(TemplatesList templatesList);

  void inject(BaseTransactionList transactionList);

  void inject(SplitPartList splitPartList);

  void inject(TransactionListDialogFragment transactionListDialogFragment);

  void inject(CategoryList categoryList);

  void inject(AdHandler adHandler);

  void inject(LicenceApiTask licenceApiTask);

  void inject(SettingsFragment settingsFragment);

  void inject(ContribDialogFragment contribDialogFragment);

  void inject(WebDavClient webDavClient);

  void inject(RoadmapViewModel roadmapViewModel);

  void inject(HistoryChart historyChart);

  void inject(TransactionViewModel transactionEditViewModel);

  CrashHandler crashHandler();

  Tracker tracker();

  PrefHandler prefHandler();

  @VisibleForTesting
  LicenceHandler licenceHandler();

  @Named(USER_COUNTRY)
  String userCountry();

  CurrencyContext currencyContext();

  ExchangeRateRepository exchangeRateRepository();

  void inject(DonateDialogFragment donateDialogFragment);

  void inject(AutoBackupService autoBackupService);

  void inject(SyncBackendList syncBackendList);

  void inject(AmountFilterDialog amountFilterDialog);

  void inject(CurrencyList currencyList);

  void inject(EditCurrencyDialog editCurrencyDialog);

  void inject(TransactionProvider transactionProvider);

  void inject(OnboardingDataFragment onboardingDataFragment);

  void inject(EditCurrencyViewModel editCurrencyViewModel);

  void inject(ExportDialogFragment exportDialogFragment);

  void inject(PlanExecutor planExecutor);

  void inject(BudgetViewModel budgetViewModel);

  void inject(BudgetEditViewModel budgetEditViewModel);

  void inject(ContentResolvingAndroidViewModel myExpensesViewModel);

  void inject(SelectFromTableDialogFragment selectFromTableDialogFragment);

  void inject(BudgetList budgetList);

  void inject(SetupWebdavDialogFragment setupWebdavDialogFragment);

  void inject(OnBoardingPrivacyFragment onBoardingPrivacyFragment);

  void inject(RemindRateDialogFragment remindRateDialogFragment);

  void inject(TemplatetRemoteViewsFactory templatetRemoteViewsFactory);

  void inject(AbstractWidget abstractWidget);

  void inject(TransactionDetailViewModel transactionDetailViewModel);

  void inject(ExchangeRateViewModel exchangeRateViewModel);
}
