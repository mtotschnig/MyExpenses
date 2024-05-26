package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.viewmodel.LicenceValidationViewModel

class LicenceKeyDialogFragment : ComposeBaseDialogFragment2() {

    companion object {
        const val VALIDATION_SUCCESS = "validationSuccess"
    }

    private val viewModel: LicenceValidationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
    }

    @Composable
    override fun BuildContent() {
        Column(
            modifier = Modifier.padding(dialogPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge,
                text = stringResource(id = R.string.pref_enter_licence_title)
            )
            var licenceKey by rememberSaveable {
                mutableStateOf(prefHandler.requireString(PrefKey.NEW_LICENCE, ""))
            }
            var licenceEmail by rememberSaveable {
                mutableStateOf(prefHandler.requireString(PrefKey.LICENCE_EMAIL, ""))
            }
            var showProgressIndicator by rememberSaveable {
                mutableStateOf(false)
            }
            // lifecycleOwner argument should be removed once we upgrade to Compose 1.7
            val result = viewModel.result.collectAsStateWithLifecycle(
                lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            )
            LaunchedEffect(result.value) {
                if (result.value != null) {
                    showProgressIndicator = false
                }
            }
            OutlinedTextField(
                value = licenceEmail,
                onValueChange = { licenceEmail = it },
                label = {
                    Text(text = stringResource(id = eltos.simpledialogfragment.R.string.email_address))
                }
            )
            OutlinedTextField(
                value = licenceKey,
                onValueChange = { licenceKey = it },
                label = {
                    Text(text = stringResource(id = R.string.licence_key))
                }
            )
            result.value?.let {
                Text(
                    color = if (!it.first) MaterialTheme.colorScheme.error else Color.Unspecified,
                    text = it.second
                )
            }

            ButtonRow {
                val success = result.value?.first == true
                if (success) {
                    TextButton(onClick = {
                        setFragmentResult(VALIDATION_SUCCESS, Bundle())
                        dismiss()
                    }) {
                        Text(stringResource(id = R.string.menu_close))
                    }
                } else {
                    TextButton(onClick = { dismiss() }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }


                if (!success) {
                    if (showProgressIndicator) {
                        CircularProgressIndicator()
                    }
                    TextButton(
                        enabled = licenceKey.isNotEmpty() && licenceEmail.isNotEmpty(),
                        onClick = {
                            viewModel.messageShown()
                            prefHandler.putString(PrefKey.NEW_LICENCE, licenceKey.trim())
                            prefHandler.putString(PrefKey.LICENCE_EMAIL, licenceEmail.trim())
                            showProgressIndicator = true
                            viewModel.validateLicence()
                        }) {
                        Text(stringResource(id = R.string.button_validate))
                    }
                }
            }
        }
    }
}