package org.totschnig.myexpenses.sync

import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.util.Utils
import java.util.*

data class SequenceNumber(val shard: Int, val number: Int) {
    operator fun next(): SequenceNumber {
        return if (number >= LIMIT) SequenceNumber(
            shard + 1, 1
        ) else SequenceNumber(shard, number + 1)
    }

    override fun toString(): String {
        return String.format(Locale.ROOT, "%d_%d", shard, number)
    }

    companion object {
        private const val LIMIT = 100
        @JvmStatic
        fun max(first: SequenceNumber, second: SequenceNumber): SequenceNumber {
            return when (first.shard.compareTo(second.shard)) {
                1 -> first
                -1 -> second
                else -> if (first.number >= second.number) first else second
            }
        }

        @JvmStatic
        fun parse(serialized: String): SequenceNumber {
            val parts = serialized.split("_".toRegex()).toTypedArray()
            return if (parts.size == 2) {
                SequenceNumber(parts[0].toInt(), parts[1].toInt())
            } else {
                SequenceNumber(0, parts[0].toInt())
            }
        }
    }
}