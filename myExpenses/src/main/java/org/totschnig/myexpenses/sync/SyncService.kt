/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.myexpenses.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.totschnig.myexpenses.injector
import timber.log.Timber

/** Service to handle sync requests.
 *
 *
 * This service is invoked in response to Intents with action android.content.SyncAdapter, and
 * returns a Binder connection to SyncAdapter.
 *
 *
 * For performance, only one sync adapter will be initialized within this application's context.
 *
 *
 * Note: The SyncService itself is not notified when a new sync occurs. It's role is to
 * manage the lifecycle of our [SyncAdapter] and provide a handle to said SyncAdapter to the
 * OS on request.
 */
class SyncService : Service() {
    /**
     * Thread-safe constructor, creates static [SyncAdapter] instance.
     */
    override fun onCreate() {
        super.onCreate()
        Timber.i("Service created")
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = SyncAdapter(applicationContext, true).also {
                    injector.inject(it)
                }
            }
        }
    }

    /**
     * Logging-only destructor.
     */
    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Service destroyed")
    }

    /**
     * Return Binder handle for IPC communication with [SyncAdapter].
     *
     *
     * New sync requests will be sent directly to the SyncAdapter using this channel.
     *
     * @param intent Calling intent
     * @return Binder handle for [SyncAdapter]
     */
    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter!!.syncAdapterBinder
    }

    companion object {
        private val sSyncAdapterLock = Any()
        private var sSyncAdapter: SyncAdapter? = null
    }
}