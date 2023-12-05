package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.LicenceStatus
import org.totschnig.myexpenses.util.licence.Package
import javax.inject.Inject

@Keep
class PreferencesContribFragment : Fragment() {

    val preferenceActivity get() = requireActivity() as PreferenceActivity

    @Inject
    lateinit var licenceHandler: LicenceHandler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                licenceHandler.ManageLicence(
                    contribBuyDo = {
                        startActivity(
                            if (it == null)
                                ContribInfoDialogActivity.getIntentFor(requireActivity(), null)
                            else
                                ContribInfoDialogActivity.getIntentFor(requireActivity(), it, false)
                        )
                    },
                    validateLicence = {
                        preferenceActivity.validateLicence()
                    },
                    removeLicence = {
                        ConfirmationDialogFragment.newInstance(Bundle().apply {
                            putInt(
                                ConfirmationDialogFragment.KEY_TITLE,
                                R.string.dialog_title_information
                            )
                            putString(
                                ConfirmationDialogFragment.KEY_MESSAGE,
                                getString(R.string.licence_removal_information, 5)
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                                R.string.menu_remove
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                                R.id.REMOVE_LICENCE_COMMAND
                            )
                        })
                            .show(parentFragmentManager, "REMOVE_LICENCE")
                    }
                )
            }
        }
    }
}