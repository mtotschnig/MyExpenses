package org.totschnig.webdav.sync.client

class ClientCertMissingException(cause: Throwable? = null) : Exception(
    "The configured client certificate is no longer available on this device", cause
)
