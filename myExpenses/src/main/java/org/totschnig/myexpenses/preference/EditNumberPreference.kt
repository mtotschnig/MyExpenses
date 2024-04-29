package org.totschnig.myexpenses.preference

import android.content.Context
import android.content.res.TypedArray
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import org.totschnig.myexpenses.R

class EditNumberPreference(context: Context, attrs: AttributeSet) :
    EditTextPreference(context, attrs) {


    private var value: Int = 0
    private val min: Int
    private val max: Int

    init {
        setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.EditNumberPreference, 0, 0)) {
            min = getInt(R.styleable.EditNumberPreference_min, 0)
            max = getInt(R.styleable.EditNumberPreference_android_max, Int.MAX_VALUE)
            require(max > min)
            recycle()
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int) = a.getInt(index, 0)

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedInt(defaultValue as? Int ?: 0)
    }

    override fun setText(text: String?) {
        try {
            text?.toInt()
        } catch (e: NumberFormatException) {
            null
        }
            ?.coerceAtLeast(min)
            ?.coerceAtMost(max)
            ?.let {
                value = it
                persistInt(it)
                notifyChanged()
            }
    }

    private val _title: CharSequence?
        get() {
            val hasMin = min > 0
            val hasMax = max < Integer.MAX_VALUE
            return when {
                hasMin && hasMax -> context.getString(
                    R.string.between_and,
                    min.toString(),
                    max.toString()
                )

                hasMin -> ">= $min"
                hasMax -> "<= $max"
                else -> null
            }?.let {
                TextUtils.concat(super.getTitle(), " (", it, ")")
            } ?: super.getTitle()
        }

    override fun getDialogTitle() = _title
    override fun getTitle() = _title

    override fun getText() = value.toString()
}