package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ShareUtils {
  public static void share(Context ctx, List<Uri> uriList, String target, String mimeType) {
    if ("".equals(target)) {
      handleGeneric(ctx, uriList, mimeType);
    } else {
      URI uri = parseUri(target);
      if (uri == null) {
        Toast.makeText(ctx, ctx.getString(R.string.ftp_uri_malformed, target),
            Toast.LENGTH_LONG).show();
      } else {
        String scheme = uri.getScheme();
        switch (scheme) {
          case "ftp":
            handleFtp(ctx, uriList, target, mimeType);
            break;
          case "mailto":
            handleMailto(ctx, uriList, mimeType, uri);
            break;
          default:
            complain(ctx, ctx.getString(R.string.share_scheme_not_supported, scheme));
            break;
        }
      }
    }
  }

  private static void handleGeneric(Context ctx, List<Uri> fileUris, String mimeType) {
    Intent intent = buildIntent(fileUris, mimeType, null);
    if (Utils.isIntentAvailable(ctx, intent)) {
      // we launch the chooser in order to make action more explicit
      ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_sending)));
    } else {
      complain(ctx, "No app for sharing found");
    }
  }

  private static void handleMailto(Context ctx, List<Uri> fileUris, String mimeType, @NonNull URI uri) {
    Intent intent = buildIntent(fileUris, mimeType, uri.getSchemeSpecificPart());
    if (Utils.isIntentAvailable(ctx, intent)) {
      ctx.startActivity(intent);
    } else {
      complain(ctx, ctx.getString(R.string.no_app_handling_email_available));
    }
  }

  @VisibleForTesting
  static Intent buildIntent(List<Uri> fileUris, String mimeType, @Nullable String emailAddress) {
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

  private static void handleFtp(Context ctx, List<Uri> fileUris, String target, String mimeType) {
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

  @Nullable
  public static URI parseUri(@NonNull String target) {
    if (!"".equals(target)) {
      try {
        URI uri = new URI(target);
        String scheme = uri.getScheme();
        // strangely for mailto URIs getHost returns null,
        // so we make sure that mailto URIs handled as valid
        if ((scheme != null && ("mailto".equals(scheme) || uri.getHost() != null))) {
          return uri;
        }
      } catch (URISyntaxException ignored) {
      }
    }
    return null;
  }
}
