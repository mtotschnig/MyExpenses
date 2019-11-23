package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

public class ShareUtils {
  public static Result share(Context ctx, List<Uri> uriList, String target, String mimeType) {
    if ("".equals(target)) {
      return handleGeneric(ctx, uriList, mimeType);
    } else {
      URI uri = parseUri(target);
      if (uri == null) {
        return complain(ctx.getString(R.string.ftp_uri_malformed, target));
      } else {
        String scheme = uri.getScheme();
        switch (scheme) {
          case "ftp":
            return handleFtp(ctx, uriList, target, mimeType);
          case "mailto":
            return handleMailto(ctx, uriList, mimeType, uri);
          default:
            return complain(ctx.getString(R.string.share_scheme_not_supported, scheme));
        }
      }
    }
  }

  private static Result handleGeneric(Context ctx, List<Uri> fileUris, String mimeType) {
    Intent intent = buildIntent(fileUris, mimeType, null);
    if (Utils.isIntentAvailable(ctx, intent)) {
      // we launch the chooser in order to make action more explicit
      ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_sending)));
    } else {
      return complain("No app for sharing found");
    }
    return Result.SUCCESS;
  }

  private static Result handleMailto(Context ctx, List<Uri> fileUris, String mimeType, @NonNull URI uri) {
    Intent intent = buildIntent(fileUris, mimeType, uri.getSchemeSpecificPart());
    if (Utils.isIntentAvailable(ctx, intent)) {
      ctx.startActivity(intent);
    } else {
      return complain(ctx.getString(R.string.no_app_handling_email_available));
    }
    return Result.SUCCESS;
  }

  @VisibleForTesting
  public static Intent buildIntent(List<Uri> fileUris, String mimeType, @Nullable String emailAddress) {
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

  private static Result handleFtp(Context ctx, List<Uri> fileUris, String target, String mimeType) {
    Intent intent;
    if (fileUris.size() > 1) {
      return complain("sending multiple file through ftp is not supported");
    } else {
      intent = new Intent(Intent.ACTION_SENDTO);
      final Uri contentUri = AppDirHelper.ensureContentUri(fileUris.get(0));
      intent.putExtra(Intent.EXTRA_STREAM, contentUri);
      intent.setDataAndType(Uri.parse(target), mimeType);
      ctx.grantUriPermission("org.totschnig.sendwithftp", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      if (Utils.isIntentAvailable(ctx, intent)) {
        ctx.startActivity(intent);
      } else {
        return complain(ctx.getString(R.string.no_app_handling_ftp_available));
      }
    }
    return Result.SUCCESS;
  }

  private static Result complain(String string) {
    return Result.ofFailure(string);
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
