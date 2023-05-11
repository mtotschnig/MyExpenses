package org.totschnig.myexpenses

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker.Companion.EVENT_BACKUP_PERFORMED
import org.totschnig.myexpenses.util.tracking.Tracker.Companion.EVENT_BACKUP_SKIPPED
import org.totschnig.myexpenses.util.tracking.Tracker.Companion.EVENT_RESTORE_FINISHED

class MyBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput?) {
        with(injector) {
            if (prefHandler().encryptDatabase) {
                tracker().logEvent(EVENT_BACKUP_SKIPPED, null)
            } else {
                tracker().logEvent(EVENT_BACKUP_PERFORMED, null)
                super.onFullBackup(data)
            }
        }
    }

    override fun onQuotaExceeded(backupDataBytes: Long, quotaBytes: Long) {
        CrashHandler.report(Exception("QuotaExceeded: $backupDataBytes / $quotaBytes"))
    }

    override fun onRestoreFinished() {
        injector.tracker().logEvent(EVENT_RESTORE_FINISHED, null)
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {}

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {}
}