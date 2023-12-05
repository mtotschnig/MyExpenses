package org.totschnig.myexpenses.di;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.BaseActivity;
import org.totschnig.myexpenses.activity.BaseMyExpenses;
import org.totschnig.myexpenses.activity.CsvImportActivity;
import org.totschnig.myexpenses.activity.DebtOverview;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.db2.Repository;
import org.totschnig.myexpenses.delegate.CategoryDelegate;
import org.totschnig.myexpenses.delegate.SplitDelegate;
import org.totschnig.myexpenses.delegate.TransferDelegate;
import org.totschnig.myexpenses.dialog.AmountFilterDialog;
import org.totschnig.myexpenses.dialog.BaseDialogFragment;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DebtDetailsDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectFromTableDialogFragment;
import org.totschnig.myexpenses.export.pdf.PdfPrinter;
import org.totschnig.myexpenses.feature.BankingFeature;
import org.totschnig.myexpenses.feature.FeatureManager;
import org.totschnig.myexpenses.feature.OcrFeature;
import org.totschnig.myexpenses.fragment.BudgetList;
import org.totschnig.myexpenses.fragment.CsvImportDataFragment;
import org.totschnig.myexpenses.fragment.CsvImportParseFragment;
import org.totschnig.myexpenses.fragment.CurrencyList;
import org.totschnig.myexpenses.fragment.HistoryChart;
import org.totschnig.myexpenses.fragment.OnBoardingPrivacyFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;
import org.totschnig.myexpenses.fragment.PartiesList;
import org.totschnig.myexpenses.fragment.PlannerFragment;
import org.totschnig.myexpenses.fragment.StaleImagesList;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.fragment.TemplatesList;
import org.totschnig.myexpenses.fragment.preferences.BasePreferenceFragment;
import org.totschnig.myexpenses.fragment.preferences.PreferencesContribFragment;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.BaseTransactionProvider;
import org.totschnig.myexpenses.provider.PlannerUtils;
import org.totschnig.myexpenses.retrofit.ExchangeRateService;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.service.SyncNotificationDismissHandler;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.task.GrisbiImportTask;
import org.totschnig.myexpenses.util.ICurrencyFormatter;
import org.totschnig.myexpenses.util.ads.BaseAdHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.locale.HomeCurrencyProvider;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.viewmodel.BaseViewModel;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2;
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.DebtViewModel;
import org.totschnig.myexpenses.viewmodel.DistributionViewModel;
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.ExchangeRateViewModel;
import org.totschnig.myexpenses.viewmodel.ExportViewModel;
import org.totschnig.myexpenses.viewmodel.FeatureViewModel;
import org.totschnig.myexpenses.viewmodel.LicenceValidationViewModel;
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel;
import org.totschnig.myexpenses.viewmodel.OcrViewModel;
import org.totschnig.myexpenses.viewmodel.PlannerViewModel;
import org.totschnig.myexpenses.viewmodel.RestoreViewModel;
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel;
import org.totschnig.myexpenses.viewmodel.SettingsViewModel;
import org.totschnig.myexpenses.viewmodel.ShareViewModel;
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel;
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel;
import org.totschnig.myexpenses.widget.AbstractListWidget;
import org.totschnig.myexpenses.widget.AccountRemoteViewsFactory;
import org.totschnig.myexpenses.widget.BudgetWidget;
import org.totschnig.myexpenses.widget.TemplateRemoteViewsFactory;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import okhttp3.OkHttpClient;

@Singleton
@Component(modules = {AppModule.class, UiModule.class, NetworkModule.class, LicenceModule.class,
    DataModule.class, CoroutineModule.class, ViewModelModule.class, FeatureModule.class,
    CrashHandlerModule.class, SyncModule.class})
public interface AppComponent {
  String USER_COUNTRY = "userCountry";
  String DATABASE_NAME = "databaseName";

  @Component.Builder
  interface Builder {

    @BindsInstance
    Builder applicationContext(MyApplication applicationContext);

    Builder licenceModule(LicenceModule licenceModule);

    Builder coroutineModule(CoroutineModule coroutineModule);

    Builder viewModelModule(ViewModelModule viewModelModule);

    Builder dataModule(DataModule dataModule);

    Builder featureModule(FeatureModule featureModule);

    Builder crashHandlerModule(CrashHandlerModule crashHandlerModule);

    Builder uiModule(UiModule uiModule);

    Builder networkModule(NetworkModule networkModule);

    Builder appmodule(AppModule appModule);

    AppComponent build();
  }

  CrashHandler crashHandler();

  Tracker tracker();

  PrefHandler prefHandler();

  LicenceHandler licenceHandler();

  @Named(USER_COUNTRY)
  String userCountry();

  CurrencyContext currencyContext();

  ICurrencyFormatter currencyFormatter();

  ExchangeRateService exchangeRateService();

  HomeCurrencyProvider homeCurrencyProvider();

  Picasso picasso();

  MyApplication myApplication();

  Repository repository();

  Gson gson();

  @Nullable
  OcrFeature ocrFeature();

  @Nullable
  BankingFeature bankingFeature();

  OkHttpClient.Builder okHttpClientBuilder();

  FeatureManager featureManager();

  PlannerUtils plannerUtils();

  void inject(MyApplication application);

  void inject(ExpenseEdit expenseEdit);

  void inject(BaseMyExpenses myExpenses);

  void inject(DebtOverview debtOverview);

  void inject(ProtectedFragmentActivity protectedFragmentActivity);

  void inject(TransactionDetailFragment transactionDetailFragment);

  void inject(StaleImagesList staleImagesList);

  void inject(PdfPrinter pdfPrinter);

  void inject(TemplatesList templatesList);

  void inject(TransactionListComposeDialogFragment transactionListComposeDialogFragment);

  void inject(BaseAdHandler adHandler);

  void inject(ContribDialogFragment contribDialogFragment);

  void inject(RoadmapViewModel roadmapViewModel);

  void inject(HistoryChart historyChart);

  void inject(DonateDialogFragment donateDialogFragment);

  void inject(SyncNotificationDismissHandler syncNotificationDismissHandler);

  void inject(SyncBackendList syncBackendList);

  void inject(AmountFilterDialog amountFilterDialog);

  void inject(CurrencyList currencyList);

  void inject(EditCurrencyDialog editCurrencyDialog);

  void inject(BaseTransactionProvider transactionProvider);

  void inject(OnboardingDataFragment onboardingDataFragment);

  void inject(EditCurrencyViewModel editCurrencyViewModel);

  void inject(BudgetViewModel budgetViewModel);

  void inject(BaseViewModel baseViewModel);

  void inject(ContentResolvingAndroidViewModel contentResolvingAndroidViewModel);

  void inject(PlannerViewModel plannerViewModel);

  void inject(TransactionEditViewModel transactionEditViewModel);

  void inject(CurrencyViewModel contentResolvingAndroidViewModel);

  void inject(SettingsViewModel settingsViewModel);

  void inject(UpgradeHandlerViewModel upgradeHandlerViewModel);

  void inject(SelectFromTableDialogFragment selectFromTableDialogFragment);

  void inject(BudgetList budgetList);

  void inject(OnBoardingPrivacyFragment onBoardingPrivacyFragment);

  void inject(RemindRateDialogFragment remindRateDialogFragment);

  void inject(TemplateRemoteViewsFactory templateRemoteViewsFactory);

  void inject(AccountRemoteViewsFactory accountRemoteViewsFactory);

  void inject(AbstractListWidget abstractListWidget);

  void inject(BudgetWidget budgetWidget);

  void inject(ExchangeRateViewModel exchangeRateViewModel);

  void inject(OnboardingUiFragment onboardingUiFragment);

  void inject(PlannerFragment.PlanInstanceViewHolder planInstanceViewHolder);

  void inject(BasePreferenceFragment basePreferenceFragment);

  void inject(CalendarListPreferenceDialogFragmentCompat calendarListPreferenceDialogFragmentCompat);

  void inject(ExtendProLicenceDialogFragment extendProLicenceDialogFragment);

  void inject(VersionDialogFragment versionDialogFragment);

  void inject(BaseActivity baseActivity);

  void inject(OcrViewModel ocrViewModel);

  void inject(BaseDialogFragment baseDialogFragment);

  void inject(CsvImportParseFragment csvImportParseFragment);

  void inject(FeatureViewModel featureViewModel);

  void inject(CsvImportDataFragment csvImportDataFragment);

  void inject(DebtViewModel debtViewModel);

  void inject(DebtDetailsDialogFragment debtDetailsDialogFragment);

  void inject(CategoryDelegate transactionDelegate);

  void inject(SplitDelegate transactionDelegate);

  void inject(TransferDelegate transactionDelegate);

  void inject(ExportViewModel exportViewModel);

  void inject(PartiesList partiesList);

  void inject(DistributionViewModel distributionViewModel);

  void inject(BudgetViewModel2 distributionViewModel);

  void inject(GrisbiImportTask grisbiImportTask);

  void inject(LicenceValidationViewModel licenceValidationViewModel);

  void inject(ShareViewModel shareViewModel);

  void inject(SyncAdapter syncAdapter);

  void inject(MyExpensesViewModel myExpensesViewModel);

  void inject(RestoreViewModel restoreViewModel);

  void inject(PlanExecutor planExecutor);

  void inject(CsvImportActivity csvImportActivity);

  void inject(PreferencesContribFragment preferencesContribFragment);

}
