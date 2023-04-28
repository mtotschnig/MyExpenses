package org.totschnig.ocr

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.BaseDialogFragment
import org.totschnig.myexpenses.feature.OcrHost
import org.totschnig.ocr.databinding.ScanPreviewBinding
import javax.inject.Inject

class ScanPreviewFragment : BaseDialogFragment() {
    private lateinit var binding: ScanPreviewBinding
    @Inject
    lateinit var picasso: Picasso
    private lateinit var viewModel: ScanPreviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerOcrComponent.builder().appComponent((requireActivity().application as MyApplication).appComponent).build().inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ScanPreviewBinding.inflate(LayoutInflater.from(requireContext()))
        viewModel = ViewModelProvider(this)[ScanPreviewViewModel::class.java]
        viewModel.getResult().observe(this) { result ->
            (activity as? OcrHost)?.processOcrResult(result, scanUri)
            dismiss()
        }
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(requireActivity())
                .setView(binding.root)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.button_scan, null)
        return builder.create().apply {
            setOnShowListener {
                loadImage()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)?.let {
                        it.isEnabled = false
                    }
                    showSnackBar(getString(R.string.ocr_recognition_info, viewModel.getOcrInfo(requireContext())), Snackbar.LENGTH_INDEFINITE, null)
                    activity?.let { viewModel.runTextRecognition(scanUri, it) }
                }
            }
        }
    }

    private fun loadImage() {
        scanUri.let {
            picasso.invalidate(it)
            val requestCreator = picasso.load(it)
            with(binding.imageView) {
                if (width > 0 || height > 0) {
                    requestCreator.resize(width, height).onlyScaleDown()
                }
                requestCreator.into(this)
            }
        }
    }

    fun handleData(intent: Intent?) {
        intent?.let { viewModel.handleData(it) } ?: run { dismissSnackBar() }
    }

    private val scanUri: Uri
        get() = requireArguments().getParcelable(KEY_FILE)!!

    companion object {
        const val KEY_FILE = "file"
        fun with(uri: Uri) = ScanPreviewFragment().apply {
            isCancelable = false
            arguments = Bundle().apply {
                putParcelable(KEY_FILE, uri)
            }
        }
    }
}