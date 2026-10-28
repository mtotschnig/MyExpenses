package org.totschnig.myexpenses.next.feature.merchants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.designsystem.DestructiveTextButton
import org.totschnig.myexpenses.designsystem.FormDialog
import org.totschnig.myexpenses.designsystem.PayeeTile
import org.totschnig.myexpenses.next.core.common.R as NextR
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.data.merchants.Payee
import org.totschnig.myexpenses.next.data.merchants.domainOf

/**
 * Lets the user enter the web address of a payee and loads its logo from it, like KeePassXC loads
 * the icon of an entry. Closes once a logo was found, else says that there is none.
 */
@Composable
fun PayeeLogoDialog(logos: MerchantLogos, payee: Payee, onDismiss: () -> Unit) {
    val payeeId = payee.id ?: return
    val entered = logos.enteredDomains.collectAsStateWithLifecycle().value[payeeId]
    val logo = (logos.images.collectAsStateWithLifecycle().value[payeeId] as? PayeeImage.Logo)?.bitmap
    var address by rememberSaveable { mutableStateOf(entered ?: "") }
    // Not saved: after a rotation the answer goes to the dialog shown before, the logo still comes
    var isLoading by remember { mutableStateOf(false) }
    var notFound by rememberSaveable { mutableStateOf<String?>(null) }
    val domain = domainOf(address)
    val load = {
        if (domain != null && !isLoading) {
            isLoading = true
            notFound = null
            logos.enterDomain(payeeId, address) { found ->
                isLoading = false
                if (found != null) onDismiss() else notFound = domain
            }
        }
    }
    FormDialog(
        title = stringResource(NextR.string.next_payee_logo_title, payee.displayName),
        onConfirm = { load() },
        onDismiss = onDismiss,
        confirmEnabled = domain != null && !isLoading,
        confirmLabel = stringResource(NextR.string.next_payee_logo_load),
        intro = stringResource(NextR.string.next_payee_logo_intro)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PayeeTile(payee.displayName, logo) {}
            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    notFound = null
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isLoading,
                label = { Text(stringResource(NextR.string.next_payee_logo_address)) },
                placeholder = { Text("example.com") },
                isError = notFound != null || (address.isNotBlank() && domain == null),
                supportingText = when {
                    notFound != null -> stringResource(NextR.string.next_payee_logo_not_found, notFound!!)
                    address.isNotBlank() && domain == null -> stringResource(NextR.string.next_payee_logo_invalid)
                    else -> null
                }?.let { { Text(it) } },
                trailingIcon = if (isLoading) {
                    { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { load() })
            )
        }
        if (entered != null) {
            DestructiveTextButton(
                stringResource(NextR.string.next_payee_logo_remove),
                onClick = {
                    logos.removeDomain(payeeId)
                    onDismiss()
                },
                icon = Icons.Default.Delete
            )
        }
    }
}
