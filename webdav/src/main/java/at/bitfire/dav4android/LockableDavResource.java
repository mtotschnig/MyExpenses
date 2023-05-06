package at.bitfire.dav4android;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.totschnig.myexpenses.sync.SyncAdapter;

import java.io.IOException;

import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.dav4android.property.ResourceType;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class LockableDavResource extends DavResource {
  public LockableDavResource(@NonNull OkHttpClient httpClient, @NonNull HttpUrl location) {
    super(httpClient, location);
  }

  public static boolean isCollection(DavResource davResource) {
    ResourceType type = (ResourceType) davResource.properties.get(ResourceType.NAME);
    Timber.tag(SyncAdapter.TAG).i("isCollection - properties: %s; type: %s", davResource.properties, type);
    return type != null && type.types.contains(ResourceType.COLLECTION);
  }

  public boolean isCollection() {
    return isCollection(this);
  }

  /**
   * @param body     content to be written to resource
   * @param ifHeader DAV compliant If header
   */
  public void put(@NonNull RequestBody body, @Nullable String ifHeader) throws IOException, HttpException {
    Response response;
    Request.Builder builder = new Request.Builder()
        .put(body)
        .url(location);

    if (ifHeader != null) {
      builder.header("If", ifHeader);
    }

    response = httpClient.newCall(builder.build()).execute();

    checkStatus(response, true);
    if (response.code() == 207) {
      /* Apache mod_dav returns 207 if update fails due to collection being locked
       * we need to verify if 207 can also be returned in some cases of success */
      throw new HttpException(response);
    }

    String eTag = response.header("ETag");
    if (TextUtils.isEmpty(eTag))
      properties.remove(GetETag.NAME);
    else
      properties.put(GetETag.NAME, new GetETag(eTag));
  }

  /**
   * Tries to establish if the Dav resource represented by this object exists on the server by sending
   * a HEAD request to it. A resource is supposed to exist unless the server explicitly returns 404
   */
  private boolean head() throws IOException {
    Request request = new Request.Builder()
        .url(location)
        .head()
        .build();
    Response response = httpClient.newCall(request).execute();
    return response.code() != 404;
  }

  /**
   * calls {@link #head()} without throwing exception
   *
   * @return true if head request succeeds
   */
  public boolean exists() {
    try {
      return head();
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Tests first if collection exists. As a workaround for
   * Webservers where testing for existence with HEAD request does not work, as a fallback we check
   * if MKCOL request returned 405 which would indicate that folder already existed
   *
   * @param ifHeader DAV compliant If header
   */
  public void mkColWithLock(String ifHeader) throws IOException {
    if (!exists()) {
      try {
        Response response = null;
        for (int attempt = 0; attempt < MAX_REDIRECTS; attempt++) {
          final Request.Builder builder = new Request.Builder()
              .method("MKCOL", null)
              .url(location);
          if (ifHeader != null) {
            builder.header("If", ifHeader);
          }
          response = httpClient.newCall(builder.build()).execute();
          if (response.isRedirect()) {
            processRedirection(response);
          } else {
            break;
          }
        }
        checkStatus(response, true);
      } catch (HttpException e) {
        if (e.status != 405) {
          throw new IOException(e);
        }
      }
    }
  }
}
