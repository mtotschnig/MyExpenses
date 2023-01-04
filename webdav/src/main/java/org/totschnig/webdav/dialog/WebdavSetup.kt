package org.totschnig.webdav.dialog

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.EditActivity
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.KEY_SYNC_PROVIDER_URL
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.KEY_SYNC_PROVIDER_USERNAME
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory.Companion.ACTION_RECONFIGURE
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.form.AbstractFormFieldValidator
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator
import org.totschnig.myexpenses.util.form.FormValidator
import org.totschnig.myexpenses.viewmodel.SyncViewModel.Companion.KEY_ORIGINAL_ACCOUNT_NAME
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

class WebdavSetup : EditActivity() {
    private val viewModel: WebdavSetupViewModel by viewModels()

    private var mTrustCertificate: X509Certificate? = null

    private lateinit var binding: SetupWebdavBinding

    private val isReconfigure: Boolean
        get() = intent.action == ACTION_RECONFIGURE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SetupWebdavBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        if (isReconfigure) {
            binding.edtUrl.setText(intent.getStringExtra(KEY_SYNC_PROVIDER_URL))
            binding.edtUserName.setText(intent.getStringExtra(KEY_SYNC_PROVIDER_USERNAME))
            binding.edtPassword.setText(intent.getStringExtra(AccountManager.KEY_PASSWORD))
        }
        binding.edtUrl.addTextChangedListener(this)
        binding.edtUserName.addTextChangedListener(this)
        binding.edtPassword.addTextChangedListener(this)
        binding.descriptionWebdavUrl.text =
            Utils.getTextWithAppName(
                this, R.string.description_webdav_url
            )
        binding.bOK.setOnClickListener { onOkClick() }
        viewModel.result.observe(this) { result ->
            result.onSuccess {
                finish(false)
            }.onFailure { throwable ->
                when (throwable) {
                    is UntrustedCertificateException -> {
                        binding.certificateContainer.isVisible = true
                        mTrustCertificate = throwable.certificate.also {
                            binding.txtTrustCertificate.text = getShortDescription(it, this)
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
                binding.progressBar.isVisible = false
                binding.bOK.isEnabled = true
                binding.bOK.isVisible = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.edtUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.certificateContainer.isVisible = false
                binding.chkTrustCertificate.isChecked = false
            }
        })
    }

    private val TextInputEditText.trimmedValue
        get() = text.toString().trim()

    private fun onOkClick() {
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
            binding.progressBar.isVisible = true
            binding.bOK.isEnabled = false
            binding.bOK.isVisible = false
        }
    }

    private fun finish(fallBackToClass1: Boolean) {

        setResult(RESULT_OK, Intent().apply {
            if (isReconfigure) {
                putExtra(
                    KEY_ORIGINAL_ACCOUNT_NAME,
                    intent.getStringExtra(KEY_ORIGINAL_ACCOUNT_NAME)
                )
            }
            putExtra(AccountManager.KEY_PASSWORD, binding.edtPassword.trimmedValue)
            putExtra(
                AccountManager.KEY_ACCOUNT_NAME,
                BackendService.WEBDAV.buildAccountName(binding.edtUrl.trimmedValue)
            )
            putExtra(AccountManager.KEY_USERDATA, Bundle().apply {
                mTrustCertificate?.takeIf { binding.chkTrustCertificate.isChecked }?.let {
                    try {
                        putString(WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE, it.encode())
                    } catch (e: CertificateEncodingException) {
                        CrashHandler.report(e)
                    }
                }
                putString(KEY_SYNC_PROVIDER_URL, binding.edtUrl.trimmedValue)
                putString(
                    KEY_SYNC_PROVIDER_USERNAME,
                    binding.edtUserName.trimmedValue
                )
                if (fallBackToClass1) {
                    putString(WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1, "1")
                }
                if (prefHandler.getBoolean(PrefKey.WEBDAV_ALLOW_UNVERIFIED_HOST, false)) {
                    putString(WebDavBackendProvider.KEY_ALLOW_UNVERIFIED, "true")
                }
            })
        })
        finish()
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