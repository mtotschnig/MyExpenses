package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class ShareUtils {
  public static void share(Context ctx, ArrayList<Uri> fileUris, String target, String mimeType) {
    if ("".equals(target)) {
      handleGeneric(ctx, fileUris, mimeType);
    } else {
      URI uri = validateUri(target);
      if (uri == null) {
        Toast.makeText(ctx, ctx.getString(R.string.ftp_uri_malformed, target),
            Toast.LENGTH_LONG).show();
      } else {
        String scheme = uri.getScheme();
        // if we get a String that does not include a scheme,
        // we interpret it as a mail address
        if (scheme == null) {
          scheme = "mailto";
        }
        switch (scheme) {
          case "ftp":
            handleFtp(ctx, fileUris, target, mimeType);
            break;
          case "mailto":
            handleMailto(ctx, fileUris, mimeType, uri);
            break;
          default:
            complain(ctx, ctx.getString(R.string.share_scheme_not_supported, scheme));
            break;
        }
      }
    }
  }

  private static void handleGeneric(Context ctx, ArrayList<Uri> fileUris, String mimeType) {
    Intent intent = buildIntent(fileUris, mimeType, null);
    if (Utils.isIntentAvailable(ctx, intent)) {
      // we launch the chooser in order to make action more explicit
      ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_sending)));
    } else {
      complain(ctx, "No app for sharing found");
    }
  }

  private static void handleMailto(Context ctx, ArrayList<Uri> fileUris, String mimeType, @NonNull URI uri) {
    Intent intent = buildIntent(fileUris, mimeType, uri.getSchemeSpecificPart());
    if (Utils.isIntentAvailable(ctx, intent)) {
      ctx.startActivity(intent);
    } else {
      complain(ctx, ctx.getString(R.string.no_app_handling_email_available));
    }
  }

  private static Intent buildIntent(ArrayList<Uri> fileUris, String mimeType, @Nullable String emailAddress) {
    Intent intent;
    if (fileUris.size() > 1) {
      intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
      ArrayList<Uri> uriArrayList = Stream.of(fileUris)
          .map(AppDirHelper::ensureContentUri)
          .collect(Collectors.toCollection(ArrayList::new));
      intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriArrayList);
    } else {
      intent = new Intent(Intent.ACTION_SEND);
      intent.putExtra(Intent.EXTRA_STREAM, AppDirHelper.ensureContentUri(fileUris.get(0)));
    }
    intent.setType(mimeType);
    if (emailAddress != null) {
      intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
    }
    intent.putExtra(Intent.EXTRA_SUBJECT, R.string.export_expenses);
    return intent;
  }

  private static void handleFtp(Context ctx, ArrayList<Uri> fileUris, String target, String mimeType) {
    Intent intent;
    if (fileUris.size() > 1) {
      Toast.makeText(ctx,
          "sending multiple file through ftp is not supported",
          Toast.LENGTH_LONG).show();
    } else {
      intent = new Intent(Intent.ACTION_SENDTO);
      intent.putExtra(Intent.EXTRA_STREAM, AppDirHelper.ensureContentUri(fileUris.get(0)));
      intent.setDataAndType(Uri.parse(target), mimeType);
      if (Utils.isIntentAvailable(ctx, intent)) {
        ctx.startActivity(intent);
      } else {
        complain(ctx, ctx.getString(R.string.no_app_handling_ftp_available));
      }
    }
  }

  private static void complain(Context ctx, String string) {
    Toast.makeText(ctx, string, Toast.LENGTH_LONG).show();
  }

  public static URI validateUri(@NonNull String target) {
    boolean targetParsable;
    URI uri = null;
    if (!target.equals("")) {
      try {
        uri = new URI(target);
        String scheme = uri.getScheme();
        // strangely for mailto URIs getHost returns null,
        // so we make sure that mailto URIs handled as valid
        targetParsable = scheme != null
            && (scheme.equals("mailto") || uri.getHost() != null);
      } catch (URISyntaxException e1) {
        targetParsable = false;
      }
      if (!targetParsable) {
        return null;
      }
      return uri;
    }
    return null;
  }
}
