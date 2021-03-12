package org.totschnig.myexpenses.activity

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.CalculatorBinding
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

//TODO move to DialogFragment in order to have material styled ok and cancel buttons
/*
Originally based on Financisto's Calculator
 */
class CalculatorInput : ProtectedFragmentActivity(), View.OnClickListener {
    private lateinit var binding: CalculatorBinding
    private var tvOp: TextView? = null

    private var stack = Stack<String>()
    private var result: String? = "0"
    private var isRestart = true
    private var isInEquals = false
    private var lastOp = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        arrayOf(binding.b0, binding.b1, binding.b2, binding.b3, binding.b4, binding.b5,
                binding.b6, binding.b7, binding.b8, binding.b9).forEachIndexed { index, button ->
            button.setOnClickListener(this)
            button.text = Utils.toLocalizedString(index)
        }
        arrayOf(binding.bAdd, binding.bSubtract, binding.bDivide, binding.bMultiply,
                binding.bPercent, binding.bPlusMinus, binding.bDot, binding.bResult, binding.bClear, binding.bDelete).forEach {
                    it.setOnClickListener(this)
        }
        binding.bDot.text = Utils.getDefaultDecimalSeparator().toString()
        binding.resultPane.root.setOnLongClickListener {
            ContextCompat.getSystemService(this, ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.text?.let { pasteText ->
                try {
                    val df = NumberFormat.getInstance() as DecimalFormat
                    df.isParseBigDecimal = true
                    val parsedNumber = df.parseObject(pasteText.toString().replace("[^\\d,.٫-]".toRegex(), "")) as BigDecimal
                    val popup = PopupMenu(this@CalculatorInput, binding.resultPane.root)
                    popup.setOnMenuItemClickListener {
                        setDisplay(parsedNumber.toPlainString())
                        true
                    }
                    popup.inflate(R.menu.paste)
                    popup.show()
                    true
                } catch (e: ParseException) {
                    false
                }
            } ?: false
        }
        setDisplay("0")
        tvOp = findViewById<View>(R.id.op) as TextView
        var b = findViewById<View>(R.id.bOK) as Button
        b.setOnClickListener {
            if (!isInEquals) {
                doEqualsChar()
            }
            close()
        }
        b = findViewById<View>(R.id.bCancel) as Button
        b.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        val intent = intent
        if (intent != null) {
            val amount = intent.getSerializableExtra(DatabaseConstants.KEY_AMOUNT) as BigDecimal?
            if (amount != null) {
                setDisplay(amount.toPlainString())
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        menu.add(0, v.id, 0, "Paste")
    }

    override fun onClick(v: View) {
        onButtonClick(v.id)
    }

    private fun setDisplay(s: String?) {
        if (!s.isNullOrEmpty()) {
            result = s.replace(",".toRegex(), ".")
            binding.resultPane.result.text = localize(result!!)
        }
    }

    private fun localize(`in`: String): String {
        val out = StringBuilder()
        for (c in `in`.toCharArray()) {
            when {
                Character.isDigit(c) -> {
                    out.append(Utils.toLocalizedString(Character.getNumericValue(c)))
                }
                c == '.' -> {
                    out.append(Utils.getDefaultDecimalSeparator())
                }
                else -> {
                    out.append(c)
                }
            }
        }
        return out.toString()
    }

    private fun onButtonClick(id: Int) {
        when (id) {
            R.id.bClear -> {
                resetAll()
            }
            R.id.bDelete -> {
                doBackspace()
            }
            else -> {
                doButton(id)
            }
        }
    }

    private fun resetAll() {
        setDisplay("0")
        tvOp!!.text = ""
        lastOp = 0
        isRestart = true
        stack.clear()
    }

    private fun doBackspace() {
        val s = result
        if ("0" == s || isRestart) {
            return
        }
        var newDisplay = if (s!!.length > 1) s.substring(0, s.length - 1) else "0"
        if ("-" == newDisplay) {
            newDisplay = "0"
        }
        setDisplay(newDisplay)
    }

    private fun doButton(id: Int) {
        if (id == R.id.b0) {
            addChar('0')
        } else if (id == R.id.b1) {
            addChar('1')
        } else if (id == R.id.b2) {
            addChar('2')
        } else if (id == R.id.b3) {
            addChar('3')
        } else if (id == R.id.b4) {
            addChar('4')
        } else if (id == R.id.b5) {
            addChar('5')
        } else if (id == R.id.b6) {
            addChar('6')
        } else if (id == R.id.b7) {
            addChar('7')
        } else if (id == R.id.b8) {
            addChar('8')
        } else if (id == R.id.b9) {
            addChar('9')
        } else if (id == R.id.bDot) {
            addChar('.')
        } else if (id == R.id.bAdd || id == R.id.bSubtract || id == R.id.bMultiply || id == R.id.bDivide) {
            doOpChar(id)
        } else if (id == R.id.bPercent) {
            doPercentChar()
        } else if (id == R.id.bResult) {
            doEqualsChar()
        } else if (id == R.id.bPlusMinus) {
            setDisplay(BigDecimal(result).negate().toPlainString())
        }
    }

    private fun addChar(c: Char) {
        var s = result
        if (c == '.' && s!!.indexOf('.') != -1 && !isRestart) {
            return
        }
        if (isRestart) {
            setDisplay(if (c == '.') "0." else c.toString())
            isRestart = false
        } else {
            if ("0" == s && c != '.') {
                s = c.toString()
            } else {
                s += c
            }
            setDisplay(s)
        }
    }

    private fun doOpChar(op: Int) {
        if (isInEquals) {
            stack.clear()
            isInEquals = false
        }
        stack.push(result)
        doLastOp()
        lastOp = op
        tvOp!!.text = lastOpLabel
    }

    private val lastOpLabel: String
        get() {
            return when (lastOp) {
                R.id.bAdd -> {
                    getString(R.string.calculator_operator_plus)
                }
                R.id.bSubtract -> {
                    getString(R.string.calculator_operator_minus)
                }
                R.id.bMultiply -> {
                    getString(R.string.calculator_operator_multiply)
                }
                R.id.bDivide -> {
                    getString(R.string.calculator_operator_divide)
                }
                else -> ""
            }
        }

    private fun doLastOp() {
        isRestart = true
        if (lastOp == 0 || stack.size == 1) {
            return
        }
        val valTwo = stack.pop()
        val valOne = stack.pop()
        if (lastOp == R.id.bAdd) {
            stack.push(BigDecimal(valOne).add(BigDecimal(valTwo)).toPlainString())
        } else if (lastOp == R.id.bSubtract) {
            stack.push(BigDecimal(valOne).subtract(BigDecimal(valTwo)).toPlainString())
        } else if (lastOp == R.id.bMultiply) {
            stack.push(BigDecimal(valOne).multiply(BigDecimal(valTwo)).toPlainString())
        } else if (lastOp == R.id.bDivide) {
            val d2 = BigDecimal(valTwo)
            if (d2.compareTo(NULL_VALUE) == 0) {
                stack.push("0.0")
            } else {
                stack.push(BigDecimal(valOne).divide(d2, MathContext.DECIMAL64).toPlainString())
            }
        }
        setDisplay(stack.peek())
        if (isInEquals) {
            stack.push(valTwo)
        }
    }

    private fun doPercentChar() {
        if (stack.isEmpty()) return
        setDisplay(BigDecimal(result).divide(HUNDRED).multiply(BigDecimal(stack.peek()))
                .toPlainString())
        tvOp!!.text = ""
    }

    private fun doEqualsChar() {
        if (lastOp == 0) {
            return
        }
        if (!isInEquals) {
            isInEquals = true
            stack.push(result)
        }
        doLastOp()
        tvOp!!.text = ""
    }

    private fun close() {
        val data = Intent()
        data.putExtra(DatabaseConstants.KEY_AMOUNT, result)
        data.putExtra(EXTRA_KEY_INPUT_ID, intent.getIntExtra(EXTRA_KEY_INPUT_ID, 0))
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("result", result)
        outState.putInt("lastOp", lastOp)
        outState.putBoolean("isInEquals", isInEquals)
        outState.putSerializable("stack", stack.toTypedArray())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        result = savedInstanceState.getString("result")
        lastOp = savedInstanceState.getInt("lastOp")
        isInEquals = savedInstanceState.getBoolean("isInEquals")
        stack = Stack()
        stack.addAll(savedInstanceState.getSerializable("stack") as Array<String>)
        if (lastOp != 0 && !isInEquals) tvOp!!.text = lastOpLabel
        setDisplay(result)
    }

    override fun getSnackbarContainerId(): Int {
        return R.id.Calculator
    }

    companion object {
        const val EXTRA_KEY_INPUT_ID = "input_id"
        private val HUNDRED = BigDecimal(100)
        private val NULL_VALUE = BigDecimal(0)
    }
}