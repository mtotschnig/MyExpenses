---
scope: The My Expenses Android app's accounts, transactions, transfers, splits, reconciliation, archives, export, templates and plans, budgets, categories, QIF and CSV import and home currency, at b8090dc102fee34cef4e0aa427e978bd5b95e388.
method: Static inspection of source, resources and supporting documentation; nothing was run.
covered:
- description: Main screen, account and transaction activities, transaction form and its delegates.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/, myExpenses/src/main/java/org/totschnig/myexpenses/delegate/, myExpenses/src/main/java/org/totschnig/myexpenses/compose/accounts/, myExpenses/src/main/java/org/totschnig/myexpenses/compose/transactions/, myExpenses/src/main/java/org/totschnig/myexpenses/compose/main/]
- description: View models for accounts, transactions, templates, plans, budgets, categories, export and import.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/]
- description: Repository functions for accounts, transactions, templates, plans, budgets and categories.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/db2/]
- description: Database schema, triggers, content provider, archive and transfer operations.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/provider/]
- description: Dialogs and fragments for balancing, archiving, export, templates, planner and CSV import.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/dialog/, myExpenses/src/main/java/org/totschnig/myexpenses/fragment/]
- description: Plan execution worker and notification handler.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/service/PlanExecutor.kt, myExpenses/src/main/java/org/totschnig/myexpenses/service/PlanNotificationClickHandler.kt]
- description: Licence features, trials and limits.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt, myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/]
- description: Core value types and import/export formats.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/model/, myExpenses/src/main/java/org/totschnig/myexpenses/export/, myExpenses/src/main/java/org/totschnig/myexpenses/io/]
- description: Strings, menus and preference screens naming the product's words.
  paths: [myExpenses/src/main/res/values/, myExpenses/src/main/res/menu/, myExpenses/src/main/res/xml/]
- description: Product description and README.
  paths: [README.md, metadata/en-US/]
exclusions:
- description: Build, Gradle and CI configuration.
  paths: [build.gradle, settings.gradle, gradle/, .github/, gradlew, gradlew.bat]
- description: The developer's licence-sale invoicing and key e-mail scripts.
  paths: [business/]
- description: Project website, store listings and translations of them.
  paths: [docs/, metadata/, doc/]
- description: Test sources and shared test helpers.
  paths: [tests/, shared-test/, myExpenses/src/test/, myExpenses/src/androidTest/]
- description: Patched third-party libraries, licence-check copy and build helpers.
  paths: [jaxb-patch/, jackson/, PlayLicensingOrig/, sqlcrypt/, webui_css_sources/]
unmapped:
- description: Synchronization backends and the sync adapter.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/sync/, webdav/, drive/, dropbox/, onedrive/]
- description: Backup and restore.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/BackupRestoreActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BackupViewModel.kt, myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/RestoreViewModel.kt, myExpenses/src/main/java/org/totschnig/myexpenses/service/AutoBackupWorker.kt]
- description: Bank download over FinTS.
  paths: [fints/]
- description: Receipt scanning.
  paths: [ocr/, tesseract/, mlkit/, mlkit_deva/, mlkit_han/, mlkit_jpan/, mlkit_kore/, mlkit_latn/]
- description: Local web interface for entering transactions from a browser.
  paths: [webui/]
- description: Insert contract for third-party apps.
  paths: [transactionscontract/]
- description: Debts, portfolios, trades and price history.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/DebtActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/DebtEdit.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/DebtOverview.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/PriceHistory.kt, myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryPrices.kt, myExpenses/src/main/java/org/totschnig/myexpenses/service/DailyExchangeRateDownloadService.kt]
- description: Distribution and history charts, balance sheet and PDF printing.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/DistributionActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/HistoryActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/BalanceSheet.kt, myExpenses/src/main/java/org/totschnig/myexpenses/export/pdf/, myExpenses/src/main/java/org/totschnig/myexpenses/fragment/HistoryChart.kt]
- description: Parties, payment methods, tags, account types, account flags and currencies management.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageParties.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageMethods.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/MethodEdit.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageTags.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageAccountTypes.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageAccountFlags.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageCurrencies.kt]
- description: Attachments and stale image cleanup.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryAttachments.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ManageStaleImages.kt]
- description: Category import and export, default category setup and Grisbi import.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/GrisbiImport.java, doc/categories-sample-src/]
- description: Budget synchronization and budget import/export.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetSyncHelper.kt, myExpenses/src/main/java/org/totschnig/myexpenses/model2/BudgetExport.kt]
- description: Widgets, shortcuts and template shortcut selection.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/widget/, myExpenses/src/main/java/org/totschnig/myexpenses/activity/TemplateShortcutSelect.kt]
- description: App protection, licence purchase and validation, roadmap voting and onboarding UI choices.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/activity/IapActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/ContribInfoDialogActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/activity/RoadmapVoteActivity.kt, myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/LicenceValidationViewModel.kt, setupwizard/, preferences/]
- description: 'Remaining settings: display, auto-fill, export defaults and planner calendar.'
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/fragment/preferences/, myExpenses/src/main/java/org/totschnig/myexpenses/preference/]
- description: Linking a transaction to a debt, and value dates and equivalent amounts per transaction in detail.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/delegate/MainDelegate.kt]
limitations:
- description: Plan occurrences come from the Android calendar provider, whose recurrence expansion is outside the repository.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/provider/CalendarProviderProxy.java, myExpenses/src/main/java/org/totschnig/myexpenses/provider/PlannerUtils.kt]
- description: Store billing that grants licences in the Play distribution is in flavor code not covered here.
  paths: [myExpenses/src/main/java/org/totschnig/myexpenses/util/licence/BillingManager.kt]
---

# Coverage
