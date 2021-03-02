package org.totschnig.myexpenses.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import org.totschnig.myexpenses.R

interface Listener {
    fun onExpansionStateChanged(expanded: Boolean)
}

private const val ROTATION_EXPANDED = 0F
private const val ROTATION_COLLAPSED = 180F

class ExpansionHandle @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    fun rotate(initiallyExpanded: Boolean, listener: Listener?) {
        val animator: Animator = ObjectAnimator.ofFloat(this, ROTATION, if (initiallyExpanded) ROTATION_COLLAPSED else ROTATION_EXPANDED)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                updateContentDescription()
                listener?.run { onExpansionStateChanged(!initiallyExpanded) }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.start()
    }

    fun setExpanded(expanded: Boolean) {
        rotation = if (expanded) ROTATION_EXPANDED else ROTATION_COLLAPSED
        updateContentDescription()
    }

    private fun updateContentDescription() {
        contentDescription = resources.getString(if (rotation == ROTATION_EXPANDED) R.string.content_description_collapse else R.string.content_description_expand)
    }
}