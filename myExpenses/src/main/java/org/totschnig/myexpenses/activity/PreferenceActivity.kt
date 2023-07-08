package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.databinding.SettingsBinding
import org.totschnig.myexpenses.fragment.TwoPanePreference

class PreferenceActivity: ProtectedFragmentActivity() {
    lateinit var binding: SettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        supportFragmentManager
            .beginTransaction()
            .replace(binding.fragmentContainer.id, TwoPanePreference())
            .commit()
    }
}