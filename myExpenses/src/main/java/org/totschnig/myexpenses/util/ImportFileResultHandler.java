package org.totschnig.myexpenses.util;


import android.content.Context;
import android.net.Uri;
import android.widget.EditText;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileUtils;

import androidx.annotation.Nullable;

public class ImportFileResultHandler {

  private ImportFileResultHandler() {
  }

  public static void handleFilenameRequestResult(FileNameHostFragment hostFragment, Uri uri) throws Throwable {
    String errorMsg;
    Context context = hostFragment.getContext();
    EditText fileNameEditText = hostFragment.getFilenameEditText();
    if (uri != null) {
      fileNameEditText.setError(null);
      String displayName = DialogUtils.getDisplayName(uri);
      fileNameEditText.setText(displayName);
      if (PermissionHelper.canReadUri(uri, context)) {
        if (displayName == null) {
          //SecurityException raised during getDisplayName
          errorMsg = "Error while retrieving document";
          CrashHandler.report(new Exception(errorMsg));
          handleError(errorMsg, fileNameEditText);
        } else {
          String type = context.getContentResolver().getType(uri);
          if (type != null) {
            String[] typeParts = type.split("/");
            if (typeParts.length == 0 ||
                !hostFragment.checkTypeParts(typeParts, FileUtils.getExtension(displayName))) {
              errorMsg = context.getString(R.string.import_source_select_error, hostFragment.getTypeName());
              handleError(errorMsg, fileNameEditText);
            }
          }
        }
      } else {
        handleError("Unable to read file. Please select from a different source.", fileNameEditText);
      }
    }
  }

  private static void handleError(String errorMsg, EditText fileNameEditText) throws Throwable {
    fileNameEditText.setError(errorMsg);
    throw new Throwable(errorMsg);
  }

  public static boolean checkTypePartsDefault(String[] typeParts) {
    return typeParts[0].equals("*") ||
        typeParts[0].equals("text") ||
        typeParts[0].equals("application");
  }

  public static void maybePersistUri(FileNameHostFragment hostFragment, PrefHandler prefHandler) {
    if (!FileUtils.isDocumentUri(hostFragment.getContext(), hostFragment.getUri())) {
      prefHandler.putString(hostFragment.getPrefKey(), hostFragment.getUri().toString());
    }
  }

  public static void handleFileNameHostOnResume(FileNameHostFragment hostFragment, PrefHandler prefHandler) {
    if (hostFragment.getUri() == null) {
      String restoredUriString = prefHandler.getString(hostFragment.getPrefKey(), "");
      if (!"".equals(restoredUriString)) {
        Uri restoredUri = Uri.parse(restoredUriString);
        if (PermissionHelper.canReadUri(restoredUri, hostFragment.getContext())) {
          String displayName = DialogUtils.getDisplayName(restoredUri);
          if (displayName != null) {
            hostFragment.setUri(restoredUri);
            hostFragment.getFilenameEditText().setText(displayName);
          }
        }
      }
    }
  }

  public interface FileNameHostFragment {
    String getPrefKey();

    @Nullable
    Uri getUri();

    void setUri(@Nullable Uri uri);

    EditText getFilenameEditText();

    boolean checkTypeParts(String[] typeParts, String extension);

    String getTypeName();

    Context getContext();
  }
}
