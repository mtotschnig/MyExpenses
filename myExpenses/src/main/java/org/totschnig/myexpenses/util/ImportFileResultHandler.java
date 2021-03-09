package org.totschnig.myexpenses.util;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.io.FileUtils;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class ImportFileResultHandler {

  private ImportFileResultHandler() {
  }

  public static Uri handleFilenameRequestResult(FileNameHostFragment hostFragment, Intent data) throws Throwable {
    Uri uri = data.getData();
    String errorMsg;
    Context context = hostFragment.getContext();
    EditText fileNameEditText = hostFragment.getFilenameEditText();
    if (uri != null) {
      Timber.d(uri.toString());
      fileNameEditText.setError(null);
      String displayName = DialogUtils.getDisplayName(uri);
      fileNameEditText.setText(displayName);
      if (PermissionHelper.canReadUri(uri, context)) {
        if (displayName == null) {
          //SecurityException raised during getDisplayName
          errorMsg = "Error while retrieving document";
          handleError(errorMsg, context, fileNameEditText);
        } else {
          String type = context.getContentResolver().getType(uri);
          if (type != null) {
            String[] typeParts = type.split("/");
            if (typeParts.length == 0 ||
                !hostFragment.checkTypeParts(typeParts, FileUtils.getExtension(displayName))) {
              errorMsg = context.getString(R.string.import_source_select_error, hostFragment.getTypeName());
              handleError(errorMsg, context, fileNameEditText);
            }
          }
        }
      } else {
        ((ProtectedFragmentActivity) context).requestStoragePermission();
      }

/*      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && uri != null) {
        final int takeFlags = data.getFlags()
            & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
          //this probably will not succeed as long as we stick to ACTION_GET_CONTENT
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException e) {
          //Utils.reportToAcra(e);
        }
      }*/
    }
    return uri;
  }

  private static void handleError(String errorMsg, Context context, EditText fileNameEditText) throws Throwable {
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
      if (!restoredUriString.equals("")) {
        Uri restoredUri = Uri.parse(restoredUriString);
        if (!FileUtils.isDocumentUri(hostFragment.getContext(), restoredUri)) {
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
