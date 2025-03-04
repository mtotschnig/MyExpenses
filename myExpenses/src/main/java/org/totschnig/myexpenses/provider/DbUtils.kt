/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.annotation.VisibleForTesting
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.service.AutoBackupWorker
import org.totschnig.myexpenses.service.DailyExchangeRateDownloadService
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import java.io.File
import java.util.Locale

object DbUtils {
    fun restore(app: MyApplication, backupFile: File, encrypt: Boolean): Boolean {
        val result = try {
            AutoBackupWorker.cancel(app)
            PlanExecutor.cancel(app)
            DailyExchangeRateDownloadService.cancel(app)
            if (backupFile.exists()) {
                val client =
                    app.contentResolver.acquireContentProviderClient(TransactionProvider.AUTHORITY)!!
                val provider = client.localContentProvider as TransactionProvider
                provider.restore(backupFile, encrypt)
                client.release()
                true
            } else false
        } catch (e: Exception) {
            report(e)
            false
        }
        val prefHandler = app.appComponent.prefHandler()
        PlanExecutor.enqueueSelf(app, prefHandler, true)
        AutoBackupWorker.enqueueOrCancel(app, prefHandler)
        DailyExchangeRateDownloadService.enqueueOrCancel(app, prefHandler)
        return result
    }

    @JvmStatic
    fun weekStartFromGroupSqlExpression(year: Int, week: Int): String {
        return String.format(
            Locale.US,
            DatabaseConstants.getCountFromWeekStartZero() + " AS " + DatabaseConstants.KEY_WEEK_START,
            year,
            week * 7
        )
    }

    fun maximumWeekExpression(year: Int): String {
        return String.format(Locale.US, DatabaseConstants.getWeekMax(), year)
    }

    fun getSchemaDetails(contentResolver: ContentResolver): Map<String, String> = getTableDetails(
        contentResolver.query(TransactionProvider.DEBUG_SCHEMA_URI, null, null, null, null)
    )

    private fun getTableDetails(c: Cursor?) = buildMap {
        c?.use { cursor ->
            cursor.asSequence.forEach {
                put(it.getString(0), it.getString(1))
            }
        }
    }

    @VisibleForTesting
    fun fqcn(table: String?, column: String?): String {
        return String.format(Locale.ROOT, "%s.%s", table, column)
    }

    fun storeSetting(contentResolver: ContentResolver, key: String?, value: String?): Uri? {
        val values = ContentValues(2)
        values.put(DatabaseConstants.KEY_KEY, key)
        values.put(DatabaseConstants.KEY_VALUE, value)
        return contentResolver.insert(TransactionProvider.SETTINGS_URI, values)
    }

    fun loadSetting(contentResolver: ContentResolver, key: String) =
        contentResolver.query(
            TransactionProvider.SETTINGS_URI, arrayOf(DatabaseConstants.KEY_VALUE),
            DatabaseConstants.KEY_KEY + " = ?", arrayOf(key), null
        )?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    fun aggregateFunction(prefHandler: PrefHandler) = if (prefHandler.getBoolean(
            PrefKey.DB_SAFE_MODE,
            false
        )
    ) "total" else "sum"

    // @formatter:off
    fun typeWithFallBack(prefHandler: PrefHandler) =
        """coalesce(${DatabaseConstants.KEY_TYPE}, CASE ${DatabaseConstants.KEY_CATID} WHEN ${DatabaseConstants.SPLIT_CATID} THEN $FLAG_NEUTRAL ELSE ${if (prefHandler.getBoolean(PrefKey.UNMAPPED_TRANSACTION_AS_TRANSFER, false)) FLAG_TRANSFER else FLAG_NEUTRAL} END)"""
    // @formatter:on
}
