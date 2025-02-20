package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.compose.material3.Text
import org.totschnig.myexpenses.databinding.ActivityComposeBinding

class HistoricPrices : ProtectedFragmentActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.composeView.setContent {
            Text("TODO")
        }
    }
}