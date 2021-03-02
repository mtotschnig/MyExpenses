package org.totschnig.myexpenses.ui

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.google.android.material.button.MaterialButton
import icepick.Icepick
import org.totschnig.myexpenses.preference.PrefHandler

abstract class ButtonWithDialog @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : MaterialButton(context, attrs, defStyleAttr) {
    private fun showDialog() {
        host.hideKeyBoardAndShowDialog(id)
    }

    override fun onSaveInstanceState(): Parcelable {
        return Icepick.saveInstanceState(this, super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(Icepick.restoreInstanceState(this, state))
        update()
    }

    protected abstract fun update()

    protected val host: Host
        get() {
            var context = context
            while (context is ContextWrapper) {
                if (context is Host) {
                    return context
                }
                context = context.baseContext
            }
            throw IllegalStateException("Host context does not implement interface")
        }

    interface Host {
        fun hideKeyBoardAndShowDialog(id: Int)
        fun onValueSet(view: View)
    }

    abstract fun onCreateDialog(prefHandler: PrefHandler): Dialog?

    open fun onClick() {
        showDialog()
    }

    abstract fun onPrepareDialog(dialog: Dialog)

    init {
        setOnClickListener { onClick() }
    }
}