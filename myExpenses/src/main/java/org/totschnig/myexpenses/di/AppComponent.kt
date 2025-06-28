package org.totschnig.myexpenses.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.BaseMyExpenses
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.EditActivity
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.delegate.CategoryDelegate
import org.totschnig.myexpenses.delegate.SplitDelegate
import org.totschnig.myexpenses.delegate.TransferDelegate
import org.totschnig.myexpenses.dialog.ArchiveDialogFragment
import org.totschnig.myexpenses.dialog.BaseDialogFragment
import org.totschnig.myexpenses.dialog.ContribDialogFragment
import org.totschnig.myexpenses.dialog.DebtDetailsDialogFragment
import org.totschnig.myexpenses.dialog.DonateDialogFragment
import org.totschnig.myexpenses.dialog.EditCurrencyDialog
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment
import org.totschnig.myexpenses.dialog.TransactionDetailFragment
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment
import org.totschnig.myexpenses.dialog.VersionDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectFromTableDialogFragment
import org.totschnig.myexpenses.export.pdf.PdfPrinter
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.fragment.CsvImportDataFragment
import org.totschnig.myexpenses.fragment.CsvImportParseFragment
import org.totschnig.myexpenses.fragment.CurrencyList
import org.totschnig.myexpenses.fragment.HistoryChart
import org.totschnig.myexpenses.fragment.OnBoardingPrivacyFragment
import org.totschnig.myexpenses.fragment.OnboardingDataFragment
import org.totschnig.myexpenses.fragment.OnboardingUiFragment
import org.totschnig.myexpenses.fragment.PartiesList
import org.totschnig.myexpenses.fragment.PlannerFragment.PlanInstanceViewHolder
import org.totschnig.myexpenses.fragment.StaleImagesList
import org.totschnig.myexpenses.fragment.SyncBackendList
import org.totschnig.myexpenses.fragment.TemplatesList
import org.totschnig.myexpenses.fragment.preferences.BasePreferenceFragment
import org.totschnig.myexpenses.fragment.preferences.PreferencesContribFragment
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.service.DailyExchangeRateDownloadService
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.service.PlanNotificationClickHandler
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.task.GrisbiImportTask
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.ads.BaseAdHandler
import org.totschnig.myexpenses.util.config.Configurator
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel
import org.totschnig.myexpenses.viewmodel.BaseViewModel
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel
import org.totschnig.myexpenses.viewmodel.ExportViewModel
import org.totschnig.myexpenses.viewmodel.FeatureViewModel
import org.totschnig.myexpenses.viewmodel.LicenceValidationViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.OcrViewModel
import org.totschnig.myexpenses.viewmodel.PlannerViewModel
import org.totschnig.myexpenses.viewmodel.PriceHistoryViewModel
import org.totschnig.myexpenses.viewmodel.PrintLayoutConfigurationViewModel
import org.totschnig.myexpenses.viewmodel.RestoreViewModel
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.TemplatesListViewModel
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel
import org.totschnig.myexpenses.widget.AbstractListWidget
import org.totschnig.myexpenses.widget.AccountRemoteViewsFactory
import org.totschnig.myexpenses.widget.BudgetWidget
import org.totschnig.myexpenses.widget.TemplateRemoteViewsFactory
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [AppModule::class, UiModule::class, NetworkModule::class, LicenceModule::class, DataModule::class, CoroutineModule::class, ViewModelModule::class, FeatureModule::class, CrashHandlerModule::class, SyncModule::class, ConfigurationModule::class]
)
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationContext(applicationContext: MyApplication): Builder

        fun licenceModule(licenceModule: LicenceModule): Builder

        fun coroutineModule(coroutineModule: CoroutineModule): Builder

        fun viewModelModule(viewModelModule: ViewModelModule): Builder

        fun dataModule(dataModule: DataModule): Builder

        fun featureModule(featureModule: FeatureModule): Builder

        fun crashHandlerModule(crashHandlerModule: CrashHandlerModule): Builder

        fun uiModule(uiModule: UiModule): Builder

        fun networkModule(networkModule: NetworkModule): Builder

        fun appmodule(appModule: AppModule): Builder

        fun configurationModule(configurationModule: ConfigurationModule): Builder

        fun build(): AppComponent
    }

    fun crashHandler(): CrashHandler

    fun tracker(): Tracker

    fun prefHandler(): PrefHandler

    fun licenceHandler(): LicenceHandler

    @Named(USER_COUNTRY)
    fun userCountry(): String

    fun currencyContext(): CurrencyContext

    fun currencyFormatter(): ICurrencyFormatter

    fun exchangeRateService(): ExchangeRateService

    fun picasso(): Picasso

    fun myApplication(): MyApplication

    fun repository(): Repository

    fun gson(): Gson

    fun ocrFeature(): OcrFeature?

    fun bankingFeature(): BankingFeature?

    fun okHttpClientBuilder(): OkHttpClient.Builder

    fun featureManager(): FeatureManager

    fun plannerUtils(): PlannerUtils

    fun coroutineDispatcher(): CoroutineDispatcher

    fun preferencesDataStore(): DataStore<Preferences>

    fun configurator(): Configurator

    fun inject(application: MyApplication)

    fun inject(expenseEdit: ExpenseEdit)

    fun inject(myExpenses: BaseMyExpenses)

    fun inject(preferenceActivity: PreferenceActivity)

    fun inject(protectedFragmentActivity: ProtectedFragmentActivity)

    fun inject(transactionDetailFragment: TransactionDetailFragment)

    fun inject(staleImagesList: StaleImagesList)

    fun inject(pdfPrinter: PdfPrinter)

    fun inject(templatesList: TemplatesList)

    fun inject(transactionListComposeDialogFragment: TransactionListComposeDialogFragment)

    fun inject(adHandler: BaseAdHandler)

    fun inject(contribDialogFragment: ContribDialogFragment)

    fun inject(roadmapViewModel: RoadmapViewModel)

    fun inject(historyChart: HistoryChart)

    fun inject(donateDialogFragment: DonateDialogFragment)

    fun inject(syncBackendList: SyncBackendList)

    fun inject(currencyList: CurrencyList)

    fun inject(editCurrencyDialog: EditCurrencyDialog)

    fun inject(transactionProvider: BaseTransactionProvider)

    fun inject(onboardingDataFragment: OnboardingDataFragment)

    fun inject(editCurrencyViewModel: EditCurrencyViewModel)

    fun inject(budgetViewModel: BudgetViewModel)

    fun inject(baseViewModel: BaseViewModel)

    fun inject(contentResolvingAndroidViewModel: ContentResolvingAndroidViewModel)

    fun inject(plannerViewModel: PlannerViewModel)

    fun inject(transactionEditViewModel: TransactionEditViewModel)

    fun inject(contentResolvingAndroidViewModel: CurrencyViewModel)

    fun inject(settingsViewModel: SettingsViewModel)

    fun inject(upgradeHandlerViewModel: UpgradeHandlerViewModel)

    fun inject(selectFromTableDialogFragment: SelectFromTableDialogFragment)

    fun inject(onBoardingPrivacyFragment: OnBoardingPrivacyFragment)

    fun inject(remindRateDialogFragment: RemindRateDialogFragment)

    fun inject(templateRemoteViewsFactory: TemplateRemoteViewsFactory)

    fun inject(accountRemoteViewsFactory: AccountRemoteViewsFactory)

    fun inject(abstractListWidget: AbstractListWidget)

    fun inject(budgetWidget: BudgetWidget)

    fun inject(onboardingUiFragment: OnboardingUiFragment)

    fun inject(planInstanceViewHolder: PlanInstanceViewHolder)

    fun inject(basePreferenceFragment: BasePreferenceFragment)

    fun inject(calendarListPreferenceDialogFragmentCompat: CalendarListPreferenceDialogFragmentCompat)

    fun inject(extendProLicenceDialogFragment: ExtendProLicenceDialogFragment)

    fun inject(versionDialogFragment: VersionDialogFragment)

    fun inject(baseActivity: BaseActivity)

    fun inject(ocrViewModel: OcrViewModel)

    fun inject(baseDialogFragment: BaseDialogFragment)

    fun inject(csvImportParseFragment: CsvImportParseFragment)

    fun inject(featureViewModel: FeatureViewModel)

    fun inject(csvImportDataFragment: CsvImportDataFragment)

    fun inject(debtViewModel: DebtViewModel)

    fun inject(debtDetailsDialogFragment: DebtDetailsDialogFragment)

    fun inject(transactionDelegate: CategoryDelegate)

    fun inject(transactionDelegate: SplitDelegate)

    fun inject(transactionDelegate: TransferDelegate)

    fun inject(exportViewModel: ExportViewModel)

    fun inject(partiesList: PartiesList)

    fun inject(distributionViewModel: DistributionViewModel)

    fun inject(distributionViewModel: BudgetViewModel2)

    fun inject(grisbiImportTask: GrisbiImportTask)

    fun inject(licenceValidationViewModel: LicenceValidationViewModel)

    fun inject(baseFunctionalityViewModel: BaseFunctionalityViewModel)

    fun inject(syncAdapter: SyncAdapter)

    fun inject(myExpensesViewModel: MyExpensesViewModel)

    fun inject(restoreViewModel: RestoreViewModel)

    fun inject(planExecutor: PlanExecutor)

    fun inject(csvImportActivity: CsvImportActivity)

    fun inject(preferencesContribFragment: PreferencesContribFragment)

    fun inject(archiveDialogFragment: ArchiveDialogFragment)

    fun inject(budgetListViewModel: BudgetListViewModel)

    fun inject(dailyExchangeRateService: DailyExchangeRateDownloadService)

    fun inject(printLayoutConfigurationViewModel: PrintLayoutConfigurationViewModel)

    fun inject(templatesListViewModel: TemplatesListViewModel)

    fun inject(priceHistoryViewModel: PriceHistoryViewModel)

    fun inject(editActivity: EditActivity)

    fun inject(planNotificationClickHandler: PlanNotificationClickHandler)

    companion object {
        const val USER_COUNTRY: String = "userCountry"
        const val DATABASE_NAME: String = "databaseName"
        const val UI_SETTINGS_DATASTORE_NAME: String = "uiSettings"
    }
}
