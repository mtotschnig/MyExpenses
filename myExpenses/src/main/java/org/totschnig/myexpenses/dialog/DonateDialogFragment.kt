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

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.Package
import javax.inject.Inject

class DonateDialogFragment : BaseDialogFragment() {
    @Inject
    lateinit var licenceHandler: LicenceHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val aPackage = requirePackage()
        val listener = DonationUriVisitor()
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(requireContext())
        val items = getPaymentOptions(aPackage).map { getString(it) }.toTypedArray()
        return builder
            .setTitle(licenceHandler.getButtonLabel(aPackage))
            .setItems(items, listener)
            .create()
    }

    private fun requirePackage(): Package = requireArguments().getParcelable(KEY_PACKAGE)!!

    private inner class DonationUriVisitor : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val ctx: Activity? = activity
            val aPackage: Package = requirePackage()
            (ctx as ContribInfoDialogActivity?)!!.startPayment(
                getPaymentOptions(aPackage)[which],
                aPackage
            )
        }
    }

    private fun getPaymentOptions(aPackage: Package) =
        licenceHandler.getPaymentOptions(aPackage, injector.userCountry())

    override fun onCancel(dialog: DialogInterface) {
        (activity as? ContribInfoDialogActivity)?.finish()
    }

    companion object {
        private const val KEY_PACKAGE = "package"
        fun newInstance(aPackage: Package) = DonateDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_PACKAGE, aPackage)
            }
        }
    }
}