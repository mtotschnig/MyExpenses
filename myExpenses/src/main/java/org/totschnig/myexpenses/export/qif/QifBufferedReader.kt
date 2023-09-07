/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

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
            line = line.trim { it <= ' ' }
            if (line.isNotEmpty()) {
                return line
            }
        }
    }

    @Throws(IOException::class)
    fun peekLine(): String? {
        r.mark(256)
        val peek = readLine()
        r.reset()
        return peek
    }

    @Throws(IOException::class)
    override fun close() {
        r.close()
    }
}