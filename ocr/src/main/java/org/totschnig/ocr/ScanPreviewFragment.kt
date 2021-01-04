package org.totschnig.ocr

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.ScanPreviewBinding
import org.totschnig.myexpenses.dialog.BaseDialogFragment
import org.totschnig.myexpenses.feature.OcrHost
import java.io.File
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
        viewModel = ViewModelProvider(this).get(ScanPreviewViewModel::class.java)
        viewModel.getResult().observe(this) { result ->
            (activity as? OcrHost)?.processOcrResult(result)
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
                    showSnackbar(getString(R.string.ocr_recognition_info, viewModel.getOcrInfo(requireContext())), Snackbar.LENGTH_INDEFINITE, null)
                    activity?.let { viewModel.runTextRecognition(scanFile, it) }
                }
            }
        }
    }

    private fun loadImage() {
        scanFile.let {
            picasso.invalidate(it)
            picasso.load(it).into(binding.imageView)
        }
    }

    fun handleData(intent: Intent?) {
        intent?.let { viewModel.handleData(it) } ?: run { dismissSnackbar() }
    }

    private val scanFile: File
        get() = requireArguments().let {
            (it.getSerializable(KEY_FILE) as File)
        }

    companion object {
        const val KEY_FILE = "file"
        fun with(file: File) = ScanPreviewFragment().apply {
            isCancelable = false
            arguments = Bundle().apply {
                putSerializable(KEY_FILE, file)
            }
        }
    }
}