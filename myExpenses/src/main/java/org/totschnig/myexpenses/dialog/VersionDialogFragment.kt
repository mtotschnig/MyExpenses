/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel
import org.totschnig.myexpenses.viewmodel.data.VersionInfo
import javax.inject.Inject

class VersionDialogFragment : ComposeBaseDialogFragment(), DialogInterface.OnClickListener {

    @Inject
    lateinit var licenceHandler: LicenceHandler

    private lateinit var versions: List<VersionInfo>

    private val upgradeHandlerViewModel: UpgradeHandlerViewModel by activityViewModels()

    private val from: Int
        get() = requireArguments().getInt(KEY_FROM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
        versions = resources.getStringArray(R.array.versions)
            .map { version: String -> version.split(";") }
            .takeWhile { parts -> parts[0].toInt() > from }
            .map { parts -> VersionInfo(parts[0].toInt(), parts[1]) }
    }

    @Composable
    override fun BuildContent() {
        val context = LocalContext.current
        val upgradeInfo = upgradeHandlerViewModel.upgradeInfo.collectAsState()
        val upgradeIsPending = upgradeInfo.value is UpgradeHandlerViewModel.UpgradePending
        if (versions.isEmpty() || upgradeIsPending) {
            Column(Modifier.padding(start = 24.dp, end = 8.dp)) {
                if (upgradeIsPending) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "${DistributionHelper.versionName} ($from -> ${DistributionHelper.versionNumber})"
                )
            }

        } else {
            LazyColumn(Modifier.padding(start = 24.dp, end = 8.dp)) {
                items(versions.size) { position ->
                    val version = versions[position]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
                    ) {
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Column(Modifier.weight(1f).fillMaxHeight().padding(start=8.dp, top = 4.dp, bottom = 4.dp)) {
                                Text(
                                    version.name,
                                    modifier = Modifier.padding(bottom = 0.dp),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                version.getChanges(context)?.let { changes ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(changes.joinToString(separator = "\n") { "\u25b6 $it" })
                                    }

                                }
                            }
                            Column (modifier = Modifier.align(Alignment.CenterVertically)){
                                VersionInfoButton(
                                    uri = version.githubUrl(context),
                                    drawableRes = R.drawable.ic_github,
                                    contentDescription = "Github"
                                )
                                VersionInfoButton(
                                    uri = version.mastodonUrl(context),
                                    drawableRes = R.drawable.ic_mastodon,
                                    contentDescription = "Mastodon"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun VersionInfoButton(
        uri: String?,
        drawableRes: Int,
        contentDescription: String
    ) {
        uri?.let {
            IconButton(
                onClick = {
                    showMoreInfo(uri)
                }
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = contentDescription
                )
            }
        }
    }

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            setTitle(
                if (versions.isEmpty()) R.string.new_version else R.string.help_heading_whats_new
            )
            setIcon(R.mipmap.ic_myexpenses)
            setNegativeButton(android.R.string.ok, null)
            if (!licenceHandler.isContribEnabled) setPositiveButton(
                R.string.menu_contrib,
                this@VersionDialogFragment
            )
        }

    private fun showMoreInfo(uri: String) {
        (requireActivity() as BaseActivity).startActionView(uri)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) (activity as? MessageDialogListener)?.dispatchCommand(
            R.id.CONTRIB_INFO_COMMAND,
            null
        )
    }

    companion object {
        private const val KEY_FROM = "from"
        fun newInstance(from: Int) =
            VersionDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_FROM, from)
                }
                isCancelable = false
            }
    }
}