package org.totschnig.myexpenses.architecture

import com.lemonappdev.konsist.api.Konsist

/** Production code of the new UI: the shell :ui_next and the modules below next/ */
val nextScope by lazy {
    Konsist.scopeFromProject().slice {
        it.projectPath.startsWith("/ui_next/src/main/") ||
            (it.projectPath.startsWith("/next/") && "/src/main/" in it.projectPath)
    }
}
