/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

import timber.log.Timber
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 9/26/11 7:50 PM
 */
class QifBufferedReader(private val r: BufferedReader): Closeable {
    @Throws(IOException::class)
    fun readLine(): String? {
        while (true) {
            var line = r.readLine() ?: return null
            line = line.trim()
            if (line.isNotEmpty()) {
                return line
            }
        }
    }

    @Throws(IOException::class)
    fun peekLine(): String? {
        r.mark(2048)
        val peek = readLine()
        try {
            r.reset()
        } catch (e: IOException) {
            Timber.w("Reset failed after reading line of length ${peek?.length}")
            throw e
        }
        return peek
    }

    @Throws(IOException::class)
    override fun close() {
        r.close()
    }
}