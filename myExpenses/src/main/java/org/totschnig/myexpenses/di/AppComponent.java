package org.totschnig.myexpenses.di;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.BaseActivity;
import org.totschnig.myexpenses.activity.BaseMyExpenses;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.db2.Repository;
import org.totschnig.myexpenses.dialog.AmountFilterDialog;
import org.totschnig.myexpenses.dialog.BaseDialogFragment;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectFromTableDialogFragment;
import org.totschnig.myexpenses.export.pdf.PdfPrinter;
import org.totschnig.myexpenses.feature.OcrFeature;
import org.totschnig.myexpenses.fragment.BaseSettingsFragment;
import org.totschnig.myexpenses.fragment.BaseTransactionList;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.fragment.BudgetList;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.fragment.CsvImportDataFragment;
import org.totschnig.myexpenses.fragment.CsvImportParseFragment;
import org.totschnig.myexpenses.fragment.CurrencyList;
import org.totschnig.myexpenses.fragment.DistributionFragment;
import org.totschnig.myexpenses.fragment.HistoryChart;
import org.totschnig.myexpenses.fragment.OnBoardingPrivacyFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;
import org.totschnig.myexpenses.fragment.PlannerFragment;
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
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.ads.BaseAdHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.ExchangeRateViewModel;
import org.totschnig.myexpenses.viewmodel.FeatureViewModel;
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel;
import org.totschnig.myexpenses.viewmodel.OcrViewModel;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel;
import org.totschnig.myexpenses.viewmodel.TransactionViewModel;
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplatetRemoteViewsFactory;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.Nullable;
import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, UiModule.class, NetworkModule.class, LicenceModule.class,
    DataModule.class, CoroutineModule.class, ViewModelModule.class, FeatureModule.class,
    CrashHandlerModule.class})
public interface AppComponent {
  String USER_COUNTRY = "userCountry";
  String DATABASE_NAME = "databaseName";

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder systemLocale(Locale locale);

    @BindsInstance
    Builder applicationContext(MyApplication applicationContext);

    Builder licenceModule(LicenceModule licenceModule);

    Builder coroutineModule(CoroutineModule coroutineModule);

    Builder viewModelModule(ViewModelModule viewModelModule);

    Builder dataModule(DataModule dataModule);

    Builder featureModule(FeatureModule featureModule);

    Builder crashHandlerModule(CrashHandlerModule crashHandlerModule);

    Builder uiModule(UiModule uiModule);

    AppComponent build();
  }

  CrashHandler crashHandler();

  Tracker tracker();

  PrefHandler prefHandler();

  LicenceHandler licenceHandler();

  @Named(USER_COUNTRY)
  String userCountry();

  CurrencyContext currencyContext();

  CurrencyFormatter currencyFormatter();

  ExchangeRateRepository exchangeRateRepository();

  UserLocaleProvider userLocaleProvider();

  Picasso picasso();

  Context context();

  Repository repository();

  JsonDeserializer<LocalDate> localDateJsonDeserializer();

  Gson gson();

  @Nullable
  OcrFeature ocrFeature();

  void inject(MyApplication application);

  void inject(ExpenseEdit expenseEdit);

  void inject(BaseMyExpenses myExpenses);

  void inject(ProtectedFragmentActivity protectedFragmentActivity);

  void inject(TransactionDetailFragment transactionDetailFragment);

  void inject(StaleImagesList staleImagesList);

  void inject(PdfPrinter pdfPrinter);

  void inject(TemplatesList templatesList);

  void inject(BaseTransactionList transactionList);

  void inject(SplitPartList splitPartList);

  void inject(TransactionListDialogFragment transactionListDialogFragment);

  void inject(CategoryList categoryList);

  void inject(BudgetFragment budgetFragment);

  void inject(DistributionFragment distributionFragment);

  void inject(BaseAdHandler adHandler);

  void inject(LicenceApiTask licenceApiTask);

  void inject(SettingsFragment settingsFragment);

  void inject(ContribDialogFragment contribDialogFragment);

  void inject(WebDavClient webDavClient);

  void inject(RoadmapViewModel roadmapViewModel);

  void inject(HistoryChart historyChart);

  void inject(TransactionViewModel transactionEditViewModel);

  void inject(DonateDialogFragment donateDialogFragment);

  void inject(AutoBackupService autoBackupService);

  void inject(SyncBackendList syncBackendList);

  void inject(AmountFilterDialog amountFilterDialog);

  void inject(CurrencyList currencyList);

  void inject(EditCurrencyDialog editCurrencyDialog);

  void inject(TransactionProvider transactionProvider);

  void inject(OnboardingDataFragment onboardingDataFragment);

  void inject(EditCurrencyViewModel editCurrencyViewModel);

  void inject(PlanExecutor planExecutor);

  void inject(BudgetViewModel budgetViewModel);

  void inject(BudgetEditViewModel budgetEditViewModel);

  void inject(ContentResolvingAndroidViewModel contentResolvingAndroidViewModel);

  void inject(CurrencyViewModel contentResolvingAndroidViewModel);

  void inject(MyExpensesViewModel myExpensesViewModel);

  void inject(UpgradeHandlerViewModel upgradeHandlerViewModel);

  void inject(SelectFromTableDialogFragment selectFromTableDialogFragment);

  void inject(BudgetList budgetList);

  void inject(OnBoardingPrivacyFragment onBoardingPrivacyFragment);

  void inject(RemindRateDialogFragment remindRateDialogFragment);

  void inject(TemplatetRemoteViewsFactory templatetRemoteViewsFactory);

  void inject(AbstractWidget abstractWidget);

  void inject(TransactionDetailViewModel transactionDetailViewModel);

  void inject(ExchangeRateViewModel exchangeRateViewModel);

  void inject(OnboardingUiFragment onboardingUiFragment);

  void inject(@NotNull PlannerFragment.PlanInstanceViewHolder planInstanceViewHolder);

  void inject(BaseSettingsFragment baseSettingsFragment);

  void inject(@NotNull ExtendProLicenceDialogFragment extendProLicenceDialogFragment);

  void inject(VersionDialogFragment versionDialogFragment);

  void inject(@NotNull BaseActivity baseActivity);

  void inject(@NotNull OcrViewModel ocrViewModel);

  void inject(BaseDialogFragment confirmationDialogFragment);

  void inject(CsvImportParseFragment csvImportParseFragment);

  void inject(@NotNull FeatureViewModel featureViewModel);

  void inject(@NotNull CsvImportDataFragment csvImportDataFragment);
}
