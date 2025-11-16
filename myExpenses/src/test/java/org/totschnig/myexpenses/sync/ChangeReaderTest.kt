package org.totschnig.myexpenses.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.sync.json.Utils


class ChangeReaderTest {


    @Test
    fun shouldParseIntoListOfChanges() {
        val uuid = "825ec542-a434-4954-b59e-e47b71138b35"
        val timestamp: Long = 1475560175
        val date: Long = 1475559751
        val amount: Long = -12300
        val crStatus = "UNRECONCILED"
        val expected = TransactionChange(
            type = TransactionChange.Type.created,
            uuid = uuid,
            timeStamp = timestamp,
            date = date,
            amount = amount,
            crStatus = crStatus
        )

        val json = String.format(
            java.util.Locale.US,
            "[{\"type\":\"created\",\"uuid\":\"%s\",\"timeStamp\":%d,\"date\":%d,\"amount\":%d,\"crStatus\":\"%s\"}]",
            uuid, timestamp, date, amount, crStatus
        )
        val result = Utils.getChanges(json)
        assertThat(result).hasSize(1)
        assertThat(result).containsExactly(expected)
    }
}
