package org.totschnig.myexpenses.dialog;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.WebDavBackendProvider;
import org.totschnig.myexpenses.sync.webdav.CertificateHelper;
import org.totschnig.myexpenses.sync.webdav.HttpException;
import org.totschnig.myexpenses.sync.webdav.InvalidCertificateException;
import org.totschnig.myexpenses.sync.webdav.UntrustedCertificateException;
import org.totschnig.myexpenses.sync.webdav.WebDavClient;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.form.AbstractFormFieldValidator;
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator;
import org.totschnig.myexpenses.util.form.FormValidator;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import okhttp3.HttpUrl;

public class SetupWebdavDialogFragment extends CommitSafeDialogFragment {
  enum TestLoginStatus {
    OK,
    UNTRUSTED_CERTIFICATE,
    INVALID_CERTIFICATE,
    FAILED
  }

  private EditText mEdtUrl;
  private EditText mEdtUserName;
  private EditText mEdtPassword;
  private ViewGroup certificateContainer;
  private TextView mTxtTrustCertificate;
  private CheckBox mChkTrustCertificate;
  private X509Certificate mTrustCertificate;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.setup_webdav, null);
    mEdtUrl = (EditText) view.findViewById(R.id.edt_url);
    mEdtUrl.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        certificateContainer.setVisibility(View.GONE);
        mChkTrustCertificate.setChecked(false);
      }
    });
    mEdtUserName = (EditText) view.findViewById(R.id.edt_user_name);
    mEdtPassword = (EditText) view.findViewById(R.id.edt_password);
    certificateContainer = (ViewGroup) view.findViewById(R.id.certificate_container);
    mTxtTrustCertificate = (TextView) view.findViewById(R.id.txt_trust_certificate);
    mChkTrustCertificate = (CheckBox) view.findViewById(R.id.chk_trust_certificate);

    certificateContainer.setVisibility(View.GONE);
    mTxtTrustCertificate.setVisibility(View.GONE);
    mChkTrustCertificate.setVisibility(View.GONE);
    AlertDialog alertDialog = new AlertDialog.Builder(ctx)
        .setTitle("WebDAV")
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null)
        .create();
    alertDialog.setOnShowListener(dialog -> {

      Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(this::onOkClick);
    });
    return alertDialog;
  }

  public void onOkClick(View view) {
    FormValidator validator = new FormValidator();
    validator.add(new FormFieldNotEmptyValidator(mEdtUrl));
    validator.add(new UrlValidator(mEdtUrl));
    validator.add(new FormFieldNotEmptyValidator(mEdtUserName));
    validator.add(new FormFieldNotEmptyValidator(mEdtPassword));

    if (validator.validate()) {
      new AsyncTask<Void, Void, TestLoginStatus>() {
        private String url;
        private String userName;
        private String password;
        private X509Certificate trustedCertificate;
        private HttpException exception;

        @Override
        protected void onPreExecute() {
          url = mEdtUrl.getText().toString().trim();
          userName = mEdtUserName.getText().toString().trim();
          password = mEdtPassword.getText().toString().trim();
          trustedCertificate = mChkTrustCertificate.isChecked() ? mTrustCertificate : null;
          exception = null;

          view.setEnabled(false);
        }

        @Override
        protected TestLoginStatus doInBackground(Void... params) {
          try {
            WebDavClient client = new WebDavClient(url, userName, password, trustedCertificate);
            client.testLogin();
            return TestLoginStatus.OK;
          } catch (UntrustedCertificateException e) {
            mTrustCertificate = e.getCertificate();
            return TestLoginStatus.UNTRUSTED_CERTIFICATE;
          } catch (InvalidCertificateException e) {
            return TestLoginStatus.INVALID_CERTIFICATE;
          } catch (HttpException e) {
            exception = e;
            return TestLoginStatus.FAILED;
          }
        }

        @Override
        protected void onPostExecute(TestLoginStatus status) {
          ManageSyncBackends activity = (ManageSyncBackends) getActivity();
          if (activity == null) {
            return;
          }
          if (status == TestLoginStatus.OK) {
            Bundle data = new Bundle();
            data.putString(AccountManager.KEY_ACCOUNT_NAME, mEdtUserName.getText().toString());
            data.putString(AccountManager.KEY_PASSWORD, mEdtPassword.getText().toString());
            data.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, mEdtUrl.getText().toString());
            if (mTrustCertificate != null && mChkTrustCertificate.isChecked()) {
              try {
                data.putString(WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE, CertificateHelper.toString(mTrustCertificate));
              } catch (CertificateEncodingException e) {
                AcraHelper.report(e);
              }
            }

            activity.onFinishWebDavSetup(data);
            dismiss();
          } else if (status == TestLoginStatus.UNTRUSTED_CERTIFICATE) {
            certificateContainer.setVisibility(View.VISIBLE);
            mTxtTrustCertificate.setText(CertificateHelper.getShortDescription(mTrustCertificate, getActivity()));
            mTxtTrustCertificate.setVisibility(View.VISIBLE);
            mChkTrustCertificate.setVisibility(View.VISIBLE);
          } else if (status == TestLoginStatus.INVALID_CERTIFICATE) {
            mChkTrustCertificate.setError(getString(R.string.validate_error_webdav_invalid_certificate));
          } else {
            mEdtUrl.setError(exception.getMessage());
          }

          view.setEnabled(true);
        }
      }.execute();
    }
  }

  private static class UrlValidator extends AbstractFormFieldValidator {
    public UrlValidator(EditText mEdtUrl) {
      super(mEdtUrl);
    }

    @Override
    protected int getMessage() {
      return R.string.url_not_valid;
    }

    @Override
    protected boolean isValid() {
      return HttpUrl.parse(fields[0].getText().toString().trim()) != null;
    }
  }
}
