package org.totschnig.myexpenses.dialog;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.annimon.stream.Exceptional;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.WebDavBackendProvider;
import org.totschnig.myexpenses.sync.webdav.CertificateHelper;
import org.totschnig.myexpenses.sync.webdav.InvalidCertificateException;
import org.totschnig.myexpenses.sync.webdav.NotCompliantWebDavException;
import org.totschnig.myexpenses.sync.webdav.UntrustedCertificateException;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.task.TestLoginTask;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.form.AbstractFormFieldValidator;
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator;
import org.totschnig.myexpenses.util.form.FormValidator;

import java.io.FileNotFoundException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import okhttp3.HttpUrl;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.ASYNC_TAG;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.PROGRESS_TAG;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_ALLOW_UNVERIFIED;

public class SetupWebdavDialogFragment extends BaseDialogFragment {

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
    AlertDialog.Builder builder = initBuilderWithView(R.layout.setup_webdav);
    ((TextView) dialogView.findViewById(R.id.description_webdav_url)).setText(
        Utils.getTextWithAppName(getContext(), R.string.description_webdav_url));
    mEdtUrl = dialogView.findViewById(R.id.edt_url);
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
    mEdtUserName = dialogView.findViewById(R.id.edt_user_name);
    mEdtPassword = dialogView.findViewById(R.id.edt_password);
    certificateContainer = dialogView.findViewById(R.id.certificate_container);
    mTxtTrustCertificate = dialogView.findViewById(R.id.txt_trust_certificate);
    mChkTrustCertificate = dialogView.findViewById(R.id.chk_trust_certificate);

    certificateContainer.setVisibility(View.GONE);
    mTxtTrustCertificate.setVisibility(View.GONE);
    mChkTrustCertificate.setVisibility(View.GONE);
    AlertDialog alertDialog = builder.setTitle("WebDAV")
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null)
        .create();
    alertDialog.setOnShowListener(dialog -> {

      Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(this::onOkClick);
    });
    return alertDialog;
  }

  private void onOkClick(View view) {
    FormValidator validator = new FormValidator();
    validator.add(new FormFieldNotEmptyValidator(mEdtUrl));
    validator.add(new UrlValidator(mEdtUrl));
    validator.add(new FormFieldNotEmptyValidator(mEdtUserName));
    validator.add(new FormFieldNotEmptyValidator(mEdtPassword));

    if (validator.validate()) {
      Bundle args = new Bundle();
      args.putString(TestLoginTask.KEY_URL, mEdtUrl.getText().toString().trim());
      args.putString(TestLoginTask.KEY_USERNAME, mEdtUserName.getText().toString().trim());
      args.putString(TestLoginTask.KEY_PASSWORD, mEdtPassword.getText().toString().trim());
      args.putSerializable(TestLoginTask.KEY_CERTIFICATE, mChkTrustCertificate.isChecked() ? mTrustCertificate : null);
      args.putBoolean(KEY_ALLOW_UNVERIFIED, prefHandler.getBoolean(PrefKey.WEBDAV_ALLOW_UNVERIFIED_HOST, false));
      getParentFragmentManager()
          .beginTransaction()
          .add(TaskExecutionFragment.newInstanceWithBundle(args, TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN), ASYNC_TAG)
          .add(ProgressDialogFragment.newInstance("WebDAV", null, 0, false),
              PROGRESS_TAG).commit();
      view.setEnabled(false);
    }
  }

  public void onTestLoginResult(Exceptional<Void> result) {
    if (result.isPresent()) {
      finish(prepareData());
    } else {
      Throwable exception = result.getException();
      if (exception instanceof UntrustedCertificateException) {
        certificateContainer.setVisibility(View.VISIBLE);
        mTrustCertificate = ((UntrustedCertificateException) exception).getCertificate();
        mTxtTrustCertificate.setText(CertificateHelper.getShortDescription(mTrustCertificate, getActivity()));
        mTxtTrustCertificate.setVisibility(View.VISIBLE);
        mChkTrustCertificate.setVisibility(View.VISIBLE);
      } else if (exception instanceof InvalidCertificateException) {
        mChkTrustCertificate.setError(getString(R.string.validate_error_webdav_invalid_certificate));
      } else if (exception instanceof NotCompliantWebDavException) {
        if (((NotCompliantWebDavException) exception).isFallbackToClass1()) {
          Bundle data = prepareData();
          data.putBoolean(WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1, true);
          finish(data);
          return;
        } else {
          mEdtUrl.setError(getString(R.string.validate_error_webdav_not_compliant));
        }
      } else if (exception instanceof FileNotFoundException) {
        mEdtUrl.setError(getString(R.string.validate_error_webdav_404));
      } else {
        //noinspection ThrowableResultOfMethodCallIgnored
        mEdtUrl.setError(Utils.getCause(exception).getMessage());
      }
      ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }
  }

  private Bundle prepareData() {
    Bundle data = new Bundle();
    data.putString(AccountManager.KEY_ACCOUNT_NAME, mEdtUserName.getText().toString());
    data.putString(AccountManager.KEY_PASSWORD, mEdtPassword.getText().toString());
    data.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, mEdtUrl.getText().toString());
    if (mTrustCertificate != null && mChkTrustCertificate.isChecked()) {
      try {
        data.putString(WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE, CertificateHelper.toString(mTrustCertificate));
      } catch (CertificateEncodingException e) {
        CrashHandler.report(e);
      }
    }
    return data;
  }

  private void finish(Bundle data) {
    ((SyncBackendSetupActivity) getActivity()).onFinishWebDavSetup(data);
    dismiss();
  }

  private static class UrlValidator extends AbstractFormFieldValidator {
    UrlValidator(EditText mEdtUrl) {
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
