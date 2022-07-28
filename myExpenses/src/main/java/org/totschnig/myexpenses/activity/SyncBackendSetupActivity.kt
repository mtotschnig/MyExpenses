package org.totschnig.myexpenses.activity

import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.SubMenu
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.EditTextDialog
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.SyncViewModel
import org.totschnig.myexpenses.viewmodel.SyncViewModel.Companion.KEY_RETURN_BACKUPS
import java.io.File

abstract class SyncBackendSetupActivity : RestoreActivity(), EditTextDialogListener,
    OnDialogResultListener {

    private lateinit var backendProviders: List<BackendService>
    protected val viewModel: SyncViewModel by viewModels()
    private var isResumed = false

    @JvmField
    @State
    var selectedFactoryId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backendProviders = BackendService.allAvailable(this)
        (applicationContext as MyApplication).appComponent.inject(viewModel)
    }

    //LocalFileBackend
    override fun onFinishEditDialog(args: Bundle) {
        val filePath = args.getString(EditTextDialog.KEY_RESULT)!!
        val baseFolder = File(filePath)
        if (!baseFolder.isDirectory) {
            showSnackBar("No directory $filePath", Snackbar.LENGTH_SHORT)
        } else {
            val accountName =
                getBackendServiceByIdOrThrow(R.id.SYNC_BACKEND_LOCAL).buildAccountName(
                    filePath
                )
            val bundle = Bundle(1)
            bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, filePath)
            createAccount(accountName, null, null, bundle)
        }
    }

    //WebDav
    fun onFinishWebDavSetup(
        passWord: String,
        url: String,
        bundle: Bundle
    ) {
        createAccount(
            getBackendServiceByIdOrThrow(R.id.SYNC_BACKEND_WEBDAV).buildAccountName(
                url
            ), passWord, null, bundle
        )
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        if (selectedFactoryId != 0) {
            startSetupDo()
        }
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    fun startSetup(itemId: Int) {
        selectedFactoryId = itemId
        if (isResumed) {
            startSetupDo()
        }
    }

    private fun startSetupDo() {
        val backendService = getBackendServiceById(selectedFactoryId)
        val feature = backendService?.feature
        if (feature == null || featureManager.isFeatureInstalled(feature, this)) {
            backendService?.instantiate()?.startSetup(this)
            selectedFactoryId = 0
        } else {
            featureManager.requestFeature(feature, this)
        }
    }

    override fun onFeatureAvailable(feature: Feature) {
        featureManager.initActivity(this)
        if (selectedFactoryId != 0 && getBackendServiceByIdOrThrow(selectedFactoryId).feature == feature) {
            startSetupDo()
        }
    }

    //Google Drive & Dropbox
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == SYNC_BACKEND_SETUP_REQUEST && resultCode == RESULT_OK && intent != null) {
            val accountName = getBackendServiceByIdOrThrow(
                intent.getIntExtra(
                    KEY_SYNC_PROVIDER_ID, 0
                )
            )
                .buildAccountName(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!)
            createAccount(
                accountName,
                null,
                intent.getStringExtra(AccountManager.KEY_AUTHTOKEN),
                intent.getBundleExtra(AccountManager.KEY_USERDATA)
            )
        }
    }

    private fun createAccount(
        accountName: String?,
        password: String?,
        authToken: String?,
        bundle: Bundle?
    ) {
        val args = Bundle()
        args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName)
        args.putString(AccountManager.KEY_PASSWORD, password)
        args.putString(AccountManager.KEY_AUTHTOKEN, authToken)
        args.putParcelable(AccountManager.KEY_USERDATA, bundle)
        args.putBoolean(
            KEY_RETURN_BACKUPS,
            createAccountTaskShouldReturnBackups()
        )
        SimpleFormDialog.build().msg(R.string.passphrase_for_synchronization)
            .fields(
                Input.password(GenericAccountService.KEY_PASSWORD_ENCRYPTION).required()
                    .hint(R.string.input_label_passphrase)
            )
            .extra(args)
            .neut(R.string.button_label_no_encryption)
            .show(this, DIALOG_TAG_PASSWORD)
    }

    private fun createAccountDo(args: Bundle) {
        viewModel.createSyncAccount(args).observe(this) { result ->
            result.onSuccess {
                recordUsage(ContribFeature.SYNCHRONIZATION)
                if ("xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
                    showMessage("On some Xiaomi devices, synchronization does not work without AutoStart permission. Visit <a href=\"https://github.com/mtotschnig/MyExpenses/wiki/FAQ:-Synchronization#q2\">MyExpenses FAQ</a> for more information.")
                }
                onReceiveSyncAccountData(it)
            }.onFailure { throwable ->
                showSnackBar("Unable to set up account: " + throwable.message)
            }
        }
    }

    abstract fun onReceiveSyncAccountData(data: SyncViewModel.SyncAccountData)

    fun checkForDuplicateUuids(data: List<AccountMetaData>) =
        data.map(AccountMetaData::uuid).distinct().count() < data.count()

    fun fetchAccountData(accountName: String) {
        showSnackBar(R.string.progress_dialog_fetching_data_from_sync_backend, Snackbar.LENGTH_INDEFINITE)
        viewModel.fetchAccountData(accountName).observe(this) { result ->
            dismissSnackBar()
            result.onSuccess {
                onReceiveSyncAccountData(it)
            }.onFailure {
                showSnackBar(it.safeMessage)
            }
        }
    }

    protected open fun createAccountTaskShouldReturnBackups(): Boolean {
        return false
    }

    override fun onCancelEditDialog() {}

    fun addSyncProviderMenuEntries(subMenu: SubMenu) {
        for (factory in backendProviders) {
            subMenu.add(Menu.NONE, factory.id, Menu.NONE, factory.label)
        }
    }

    fun getBackendServiceById(id: Int): BackendService? {
        return try {
            getBackendServiceByIdOrThrow(id)
        } catch (e: IllegalStateException) {
            null
        }
    }

    @Throws(IllegalStateException::class)
    fun getBackendServiceByIdOrThrow(id: Int): BackendService {
        for (backendService in backendProviders) {
            if (backendService.id == id) {
                return backendService
            }
        }
        throw IllegalStateException()
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (DIALOG_TAG_PASSWORD == dialogTag) {
            if (which != OnDialogResultListener.BUTTON_POSITIVE || "" == extras.getString(
                    GenericAccountService.KEY_PASSWORD_ENCRYPTION
                )
            ) {
                extras.remove(GenericAccountService.KEY_PASSWORD_ENCRYPTION)
            }
            createAccountDo(extras)
        }
        return false
    }

    companion object {
        private const val DIALOG_TAG_PASSWORD = "password"
        const val REQUEST_CODE_RESOLUTION = 1
        const val KEY_SYNC_PROVIDER_ID = "syncProviderId"
    }
}