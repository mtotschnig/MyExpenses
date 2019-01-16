package org.totschnig.myexpenses.di;

import android.support.annotation.VisibleForTesting;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.AmountFilterDialog;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;
import org.totschnig.myexpenses.dialog.ExportDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.export.pdf.PdfPrinter;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.fragment.CurrencyList;
import org.totschnig.myexpenses.fragment.HistoryChart;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.SettingsFragment;
import org.totschnig.myexpenses.fragment.SplitPartList;
import org.totschnig.myexpenses.fragment.StaleImagesList;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.AutoBackupService;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;
import org.totschnig.myexpenses.task.LicenceApiTask;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, UiModule.class, UtilsModule.class, NetworkModule.class})
public interface AppComponent {
  void inject(MyApplication application);

  void inject(ExpenseEdit expenseEdit);

  void inject(MyExpenses myExpenses);

  void inject(ProtectedFragmentActivity protectedFragmentActivity);

  void inject(TransactionDetailFragment transactionDetailFragment);

  void inject(StaleImagesList staleImagesList);

  void inject(PdfPrinter pdfPrinter);

  void inject(TemplatesList templatesList);

  void inject(TransactionList transactionList);

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

  CrashHandler crashHandler();

  Tracker tracker();

  PrefHandler prefHandler();

  @VisibleForTesting
  LicenceHandler licenceHandler();

  @Named("userCountry") String userCountry();

  CurrencyContext currencyContext();

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
}
