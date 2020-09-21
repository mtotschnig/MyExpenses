package org.totschnig.ocr

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import java.io.File
import javax.inject.Inject

class ScanPreviewFragment : DialogFragment() {
    @Inject
    lateinit var ocrFeature: OcrFeature

    @Inject
    lateinit var picasso: Picasso
    lateinit var imageView: ImageView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DaggerOcrComponent.builder().appComponent(MyApplication.getInstance().appComponent).build().inject(this)
        val inflater: LayoutInflater = requireActivity().getLayoutInflater()
        val view: View = inflater.inflate(R.layout.scan_preview, null)
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Process", null)
        imageView = view.findViewById(R.id.imageView)
        return builder.create().apply {
            setOnShowListener {
                loadImage()
                view.findViewById<ImageView>(R.id.RotateRight).setOnClickListener { rotate(true) }
                view.findViewById<ImageView>(R.id.RotateLeft).setOnClickListener { rotate(false) }
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Default) {
                            try {
                                ocrFeature.runTextRecognition(scanFile, requireContext()).get(0)
                            } catch (e: Throwable) {
                                e.toString()
                            }
                        }.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
    }

    private fun rotate(right: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val exif = ExifInterface(scanFile.path)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_NORMAL -> if (right) ExifInterface.ORIENTATION_ROTATE_90 else ExifInterface.ORIENTATION_ROTATE_270
                    ExifInterface.ORIENTATION_ROTATE_90 -> if (right) ExifInterface.ORIENTATION_ROTATE_180 else ExifInterface.ORIENTATION_NORMAL
                    ExifInterface.ORIENTATION_ROTATE_180 -> if (right) ExifInterface.ORIENTATION_ROTATE_270 else ExifInterface.ORIENTATION_ROTATE_90
                    ExifInterface.ORIENTATION_ROTATE_270 -> if (right) ExifInterface.ORIENTATION_NORMAL else ExifInterface.ORIENTATION_ROTATE_180
                    else -> 0
                }.takeIf { it != 0 }?.also {
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, it.toString())
                    exif.saveAttributes()
                }
            }?.run { loadImage() }
        }
    }

    private fun loadImage() {
        scanFile.let {
            picasso.invalidate(it)
            picasso.load(it).into(imageView)
        }
    }

    val scanFile: File
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