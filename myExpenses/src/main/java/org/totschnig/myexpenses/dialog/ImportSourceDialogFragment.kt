package org.totschnig.myexpenses.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.IMPORT_FILENAME_REQUEST_CODE
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.util.ImportFileResultHandler
import org.totschnig.myexpenses.util.ImportFileResultHandler.FileNameHostFragment
import org.totschnig.myexpenses.util.ImportFileResultHandler.handleFilenameRequestResult
import org.totschnig.myexpenses.util.PermissionHelper.canReadUri
import org.totschnig.myexpenses.viewmodel.ImportSourceViewModel
import androidx.core.net.toUri
import okhttp3.HttpUrl.Companion.toHttpUrl

abstract class ImportSourceDialogFragment : BaseDialogFragment(),
    DialogInterface.OnClickListener, FileNameHostFragment {
    private val viewModel: ImportSourceViewModel by viewModels()
    private var mFilename: EditText? = null

    override var uri: Uri? = null

    override val filenameEditText: EditText
        get() = mFilename!!

    abstract val layoutId: Int
    abstract val layoutTitle: String?
    open val withSelectFromAppFolder = false

    override fun checkTypeParts(mimeType: String, extension: String): Boolean {
        return ImportFileResultHandler.checkTypePartsDefault(mimeType)
    }

    override fun onCancel(dialog: DialogInterface) {
        if (activity == null) {
            return
        }
        //TODO: we should not depend on activity implementing MessageDialogListener
        (activity as MessageDialogListener?)!!.onMessageDialogDismissOrCancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(injector) {
            inject(viewModel)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilderWithLayoutResource(layoutId)
        setupDialogView(dialogView)
        return builder.setTitle(layoutTitle)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mFilename = null
    }

    protected open fun setupDialogView(view: View) {
        mFilename = view.findViewById(R.id.Filename)
        view.findViewById<View>(R.id.btn_browse).setOnClickListener {
            DialogUtils.openBrowse(uri, this)
        }
        val listButton = view.findViewById<View>(R.id.btn_list)
        if (withSelectFromAppFolder) {
            listButton.isVisible = true
            listButton.setOnClickListener {
                viewModel.appData.value?.let {
                    val popup = PopupMenu(requireContext(), listButton)
                    val popupMenu = popup.menu
                    popup.setOnMenuItemClickListener { item ->
                        uri = it[item.itemId].uri
                        handleFilenameRequestResult(this, uri)
                        setButtonState()
                        true
                    }
                    it.forEachIndexed { i, item ->
                        popupMenu.add(Menu.NONE, i, Menu.NONE, item.name)
                    }
                    popup.show()
                }
            }
            viewModel.appData.observe(this) {
                if (it.isEmpty()) {
                    listButton.isVisible = false
                }
            }
            viewModel.loadAppData(::checkTypeParts)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == IMPORT_FILENAME_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                uri = intent.data
                try {
                    handleFilenameRequestResult(this, uri)
                } catch (throwable: Throwable) {
                    uri = null
                    showSnackBar(throwable.message!!, Snackbar.LENGTH_LONG, null)
                }
            }
        }
    }

    override fun onClick(dialog: DialogInterface, id: Int) {
        if (id == AlertDialog.BUTTON_NEGATIVE) {
            onCancel(dialog)
        }
    }

    override fun onResume() {
        super.onResume()
        ImportFileResultHandler.handleFileNameHostOnResume(this, prefHandler)
        setButtonState()
    }

    //we cannot persist document Uris because we use ACTION_GET_CONTENT instead of ACTION_OPEN_DOCUMENT
    protected fun maybePersistUri() {
        ImportFileResultHandler.maybePersistUri(this, prefHandler)
    }

    protected open val isReady: Boolean
        get() = uri?.let { canReadUri(it, requireContext()) } == true

    protected fun setButtonState() {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isReady
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        uri?.let {
            outState.putString(prefKey, it.toString())
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.getString(prefKey)?.toUri()?.let {
            val displayName = requireActivity().contentResolver.getDisplayName(it)
            uri = it
            mFilename!!.setText(displayName)
        }
    }
}