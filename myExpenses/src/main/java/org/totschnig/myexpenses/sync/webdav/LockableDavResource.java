package org.totschnig.myexpenses.sync.webdav;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.IOException;

import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.GetETag;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LockableDavResource extends at.bitfire.dav4android.DavResource {
  public LockableDavResource(@NonNull OkHttpClient httpClient, @NonNull HttpUrl location) {
    super(httpClient, location);
  }

  public void put(@NonNull RequestBody body, String ifHeader) throws IOException, HttpException {
    Response response = null;
    Request.Builder builder = new Request.Builder()
        .header("If", ifHeader)
        .put(body)
        .url(location);

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
}
