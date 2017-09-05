package org.totschnig.myexpenses.sync.webdav;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.dav4android.property.ResourceType;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LockableDavResource extends at.bitfire.dav4android.DavResource {
  LockableDavResource(@NonNull OkHttpClient httpClient, @NonNull HttpUrl location) {
    super(httpClient, location);
  }

  public static boolean isCollection(DavResource davResource) {
    ResourceType type = (ResourceType)davResource.properties.get(ResourceType.NAME);
    return type != null && type.types.contains(ResourceType.COLLECTION);
  }

  /**
   *
   * @param body content to be written to resource
   * @param ifHeader DAV compliant If header
   * @throws IOException
   * @throws HttpException
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
   * a HEAD request to it,
   * @throws HttpException if status is < 200 or > 299
   */
  public void head() throws HttpException, IOException {
    Request request = new Request.Builder()
        .url(location)
        .head()
        .build();
      Response response = httpClient.newCall(request).execute();
      checkStatus(response, true);
  }
}
