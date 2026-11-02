package org.totschnig.myexpenses.next.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Workspaces

/**
 * Icons that tell what kind of label is shown, the same in the details and in the list, so that
 * categories, groups and markers are not mistaken for each other
 */
object LabelIcons {
    /** One per transaction, part of a tree */
    val category = Icons.Outlined.Folder

    /** Trips and projects, any number per transaction */
    val group = Icons.Outlined.Workspaces

    /** One-off, reimbursed, business */
    val marker = Icons.Outlined.Check
}
