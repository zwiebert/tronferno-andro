package de.bertw.tronferno

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat


class PrefMcuFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.xpref_mcu_remote, rootKey)

        MainActivity.mcuConfig_changed = true
    }
}

