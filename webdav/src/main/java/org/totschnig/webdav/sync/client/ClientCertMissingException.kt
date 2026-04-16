package org.totschnig.webdav.sync.client

class ClientCertMissingException : Exception(
    "The configured client certificate is no longer available on this device"
)
