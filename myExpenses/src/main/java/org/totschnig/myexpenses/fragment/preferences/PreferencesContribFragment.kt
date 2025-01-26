package org.totschnig.myexpenses.fragment.preferences

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.util.licence.LicenceHandler
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
        savedInstanceState: Bundle?,
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
                                ContribInfoDialogActivity.getIntentFor(requireActivity(), it, true)
                        )
                    },
                    validateLicence = {
                        preferenceActivity.validateLicence()
                    },
                    removeLicence = {
                        ConfirmationDialogFragment.newInstance(Bundle().apply {
                            putInt(
                                ConfirmationDialogFragment.KEY_TITLE,
                                R.string.information
                            )
                            putString(
                                ConfirmationDialogFragment.KEY_MESSAGE,
                                getString(R.string.licence_removal_information, 5)
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                                R.string.remove
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                                R.id.REMOVE_LICENCE_COMMAND
                            )
                        })
                            .show(parentFragmentManager, "REMOVE_LICENCE")
                    },
                    manageSubscription = {
                        preferenceActivity.startActivity(
                            Intent(Intent.ACTION_VIEW).apply { data = it },
                            R.string.error_accessing_market)
                    }
                )
            }
        }
    }
}