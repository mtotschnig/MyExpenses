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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.databinding.VersiondialogBinding
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.viewmodel.data.VersionInfo
import javax.inject.Inject

class VersionDialogFragment : DialogViewBinding<VersiondialogBinding>(), DialogInterface.OnClickListener {

    @Inject
    lateinit var licenceHandler: LicenceHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val from = requireArguments().getInt(KEY_FROM)
        val res = resources
        val versions = res.getStringArray(R.array.versions)
            .map { version: String -> version.split(";") }
            .takeWhile { parts -> parts[0].toInt() > from }
            .map { parts -> VersionInfo(parts[0].toInt(), parts[1]) }
        val builder = initBuilderWithBinding {
            VersiondialogBinding.inflate(it)
        }
        if (versions.isNotEmpty()) {
            binding.list.adapter = object : ArrayAdapter<VersionInfo>(
                requireActivity(),
                R.layout.version_row, R.id.versionInfoName, versions
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val row = super.getView(position, convertView, parent) as ViewGroup
                    val version = versions[position]
                    val heading = row.findViewById<TextView>(R.id.versionInfoName)
                    heading.text = version.name
                    (row.findViewById<View>(R.id.versionInfoChanges) as TextView).text =
                        version.getChanges(context)?.joinToString(separator = "\n") { "\u25b6 $it" }
                    val imageButton = row.findViewById<ImageView>(R.id.versionInfo)
                    val useMastodon = version.code >= 493
                    imageButton.setImageResource(if (useMastodon) R.drawable.ic_mastodon else R.drawable.ic_facebook)
                    imageButton.contentDescription = if (useMastodon) "Mastodon" else "Facebook"
                    configureMoreInfo(
                        imageButton,
                        version,
                        "version_more_info_",
                        if (useMastodon) "https://mastodon.social/@myexpenses/" else
                            "https://www.facebook.com/MyExpenses/posts/"
                    )
                    configureMoreInfo(
                        row.findViewById(R.id.versionInfoGithub),
                        version,
                        "project_board_",
                        "https://github.com/mtotschnig/MyExpenses/projects/"
                    )
                    return row
                }
            }
            if (requireArguments().getBoolean(KEY_WITH_IMPORTANT_UPGRADE_INFO)) {
                binding.ImportantUpgradeInfoHeading.visibility = View.VISIBLE
                with(binding.ImportantUpgradeInfoBody) {
                    visibility = View.VISIBLE
                    setText(R.string.upgrade_information_cloud_sync_storage_format)
                }
                /*      TextView importantUpgradeInfoLearnMore = view.findViewById(R.id.ImportantUpgradeInfoLearnMore);
      makeVisibleAndClickable(importantUpgradeInfoLearnMore, R.string.roadmap_particpate, new ClickableSpan() {
        @Override
        public void onClick(View widget) {
         getActivity().startActivity(new Intent(getContext(), RoadmapVoteActivity.class));
        }
      });*/
            }
            builder.setTitle(R.string.help_heading_whats_new)
        } else {
            with(binding.ImportantUpgradeInfoBody) {
                visibility = View.VISIBLE
                //noinspection SetTextI18n
                text = "${DistributionHelper.versionName} ($from -> ${DistributionHelper.versionNumber})"
            }
            builder.setTitle(R.string.new_version)
        }
        builder.setIcon(R.mipmap.ic_myexpenses)
            .setNegativeButton(android.R.string.ok, this)
        if (!licenceHandler.isContribEnabled) builder.setPositiveButton(R.string.menu_contrib, this)
        return builder.create()
    }

    private fun configureMoreInfo(
        imageButton: View,
        version: VersionInfo,
        resPrefix: String,
        baseUri: String
    ) {
        val resId = resources.getIdentifier(
            resPrefix + version.nameCondensed,
            "string",
            requireContext().packageName
        )
        if (resId == 0) {
            imageButton.visibility = View.GONE
        } else {
            imageButton.visibility = View.VISIBLE
            imageButton.setOnClickListener { showMoreInfo(baseUri + getString(resId)) }
        }
    }

    private fun showMoreInfo(uri: String?) {
        (requireActivity() as BaseActivity).startActionView(uri!!)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) (activity as? MessageDialogListener)?.dispatchCommand(
            R.id.CONTRIB_INFO_COMMAND,
            null
        )
    }

    companion object {
        private const val KEY_FROM = "from"
        private const val KEY_WITH_IMPORTANT_UPGRADE_INFO = "withImportantUpgradeInfo"
        fun newInstance(from: Int, withImportantUpgradeInfo: Boolean) =
            VersionDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_FROM, from)
                    putBoolean(KEY_WITH_IMPORTANT_UPGRADE_INFO, withImportantUpgradeInfo)
                }
                isCancelable = false
            }
    }
}