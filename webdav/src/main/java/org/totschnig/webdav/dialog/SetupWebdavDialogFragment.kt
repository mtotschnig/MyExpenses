package org.totschnig.webdav.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity
import org.totschnig.myexpenses.dialog.DialogViewBinding
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.form.AbstractFormFieldValidator
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator
import org.totschnig.myexpenses.util.form.FormValidator
import org.totschnig.webdav.databinding.SetupWebdavBinding
import org.totschnig.webdav.sync.WebDavBackendProvider
import org.totschnig.webdav.sync.client.CertificateHelper.encode
import org.totschnig.webdav.sync.client.CertificateHelper.getShortDescription
import org.totschnig.webdav.sync.client.InvalidCertificateException
import org.totschnig.webdav.sync.client.NotCompliantWebDavException
import org.totschnig.webdav.sync.client.UntrustedCertificateException
import org.totschnig.webdav.viewmodel.WebdavSetupViewModel
import java.io.FileNotFoundException
import java.security.cert.CertificateEncodingException
import java.security.cert.X509Certificate

class SetupWebdavDialogFragment : DialogViewBinding<SetupWebdavBinding>() {
    private val viewModel: WebdavSetupViewModel by viewModels()

    private var mTrustCertificate: X509Certificate? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            SetupWebdavBinding.inflate(it)
        }
        binding.descriptionWebdavUrl.text =
            Utils.getTextWithAppName(
                context, R.string.description_webdav_url
            )
        binding.edtUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.certificateContainer.isVisible = false
                binding.chkTrustCertificate.isChecked = false
            }
        })
        val alertDialog = builder.setTitle("WebDAV")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        alertDialog.setOnShowListener { dialog: DialogInterface ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener { view: View -> onOkClick(view) }
        }
        return alertDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.result.observe(this) { result ->
            result.onSuccess {
                finish(false)
            }.onFailure { throwable ->
                when (throwable) {
                    is UntrustedCertificateException -> {
                        binding.certificateContainer.isVisible = true
                        mTrustCertificate = throwable.certificate.also {
                            binding.txtTrustCertificate.text = getShortDescription(it, activity)
                        }
                    }
                    is InvalidCertificateException -> {
                        binding.chkTrustCertificate.error =
                            getString(R.string.validate_error_webdav_invalid_certificate)
                    }
                    is NotCompliantWebDavException -> {
                        if (throwable.isFallbackToClass1) {
                            finish(true)
                            return@observe
                        } else {
                            binding.edtUrl.error =
                                getString(R.string.validate_error_webdav_not_compliant)
                        }
                    }
                    is FileNotFoundException -> {
                        binding.edtUrl.error = getString(R.string.validate_error_webdav_404)
                    }
                    else -> {
                        binding.edtUrl.error = Utils.getCause(throwable).message
                    }
                }
                (dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }

    private val TextInputEditText.trimmedValue
        get() = text.toString().trim()

    private fun onOkClick(view: View) {
        val validator = FormValidator()
        validator.add(FormFieldNotEmptyValidator(binding.edtUrl))
        validator.add(UrlValidator(binding.edtUrl))
        validator.add(FormFieldNotEmptyValidator(binding.edtUserName))
        validator.add(FormFieldNotEmptyValidator(binding.edtPassword))
        if (validator.validate()) {
            viewModel.testLogin(
                url = binding.edtUrl.trimmedValue,
                userName = binding.edtUserName.trimmedValue,
                passWord = binding.edtPassword.trimmedValue,
                certificate = if (binding.chkTrustCertificate.isChecked) mTrustCertificate else null,
                allowUnverifiedHost = prefHandler.getBoolean(
                    PrefKey.WEBDAV_ALLOW_UNVERIFIED_HOST,
                    false
                )
            )
            //TODO show progress indicator
            view.isEnabled = false
        }
    }

    private fun finish(fallBackToClass1: Boolean) {

        (requireActivity() as SyncBackendSetupActivity).onFinishWebDavSetup(
            passWord = binding.edtPassword.trimmedValue,
            url = binding.edtUrl.trimmedValue,
            bundle = Bundle().apply {
                mTrustCertificate?.takeIf { binding.chkTrustCertificate.isChecked }?.let {
                    try {
                        putString(WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE, it.encode())
                    } catch (e: CertificateEncodingException) {
                        CrashHandler.report(e)
                    }
                }
                putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, binding.edtUrl.trimmedValue)
                putString(
                    GenericAccountService.KEY_SYNC_PROVIDER_USERNAME,
                    binding.edtUserName.trimmedValue
                )
                if (fallBackToClass1) {
                    putString(WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1, "1")
                }
                if (prefHandler.getBoolean(PrefKey.WEBDAV_ALLOW_UNVERIFIED_HOST, false)) {
                    putString(WebDavBackendProvider.KEY_ALLOW_UNVERIFIED, "true")
                }
            }
        )
        dismiss()
    }

    private class UrlValidator(mEdtUrl: EditText?) :
        AbstractFormFieldValidator(mEdtUrl) {
        override fun getMessage(): Int {
            return R.string.url_not_valid
        }

        override fun isValid(): Boolean =
            fields[0].text.toString().trim { it <= ' ' }.toHttpUrlOrNull() != null
    }
}