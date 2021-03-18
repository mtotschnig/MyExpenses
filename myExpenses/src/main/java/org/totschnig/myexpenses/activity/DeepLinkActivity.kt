package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.util.Utils

class DeepLinkActivity : ProtectedFragmentActivity() {
    private var isPdt = true //PayPalDataTransfer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW == intent.action) {
                val data = intent.data
                if (data == null) {
                    showWebSite()
                } else if (data.lastPathSegment == "callback.html") {
                    showMessage("My Expenses implements a new licence validation mechanism. Please request a new key.")
                } else if ("verify" == data.fragment) { //callback2.html
                    val isSandbox = data.getBooleanQueryParameter("sandbox", false)
                    if (isSandbox == BuildConfig.DEBUG) { //prevent a sandbox call from hitting production app, and vice versa
                        val existingKey = prefHandler.getString(PrefKey.NEW_LICENCE, "")
                        val existingEmail = prefHandler.getString(PrefKey.LICENCE_EMAIL, "")
                        val key = data.getQueryParameter("key")
                        val email = data.getQueryParameter("email")
                        isPdt = data.getBooleanQueryParameter("isPdt", true)
                        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(email)) {
                            showMessageWithPayPalInfo("Missing parameter key and/or email")
                        } else if (existingKey == "" || existingKey == key && existingEmail == email ||
                                !licenceHandler.isContribEnabled) {
                            prefHandler.putString(PrefKey.NEW_LICENCE, key)
                            prefHandler.putString(PrefKey.LICENCE_EMAIL, email)
                            startTaskExecution(TaskExecutionFragment.TASK_VALIDATE_LICENCE, arrayOf<String>(), null, R.string.progress_validating_licence)
                        } else {
                            showMessageWithPayPalInfo(String.format(
                                    "There is already a licence active on this device, key: %s", existingKey))
                        }
                    } else {
                        showMessageWithPayPalInfo(String.format("%s app was called from %s environment",
                                if (BuildConfig.DEBUG) "Debug" else "Production",
                                if (isSandbox) "Sandbox" else "Live"))
                    }
                } else {
                    showWebSite()
                }
            }
        }
    }

    override fun onMessageDialogDismissOrCancel() {
        finish()
    }

    private fun showMessageWithPayPalInfo(message: CharSequence) {
        showMessage(if (isPdt) "${getString(R.string.paypal_callback_info)} $message" else message)
    }

    private fun showWebSite() {
        dispatchCommand(R.id.WEB_COMMAND, null)
        finish()
    }

    override fun onPostExecute(taskId: Int, o: Any?) {
        super.onPostExecute(taskId, o)
        if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE) {
            if (o is Result<*>) {
                showMessageWithPayPalInfo(o.print(this))
            }
        }
    }
}