package de.bertw.tronferno

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat


class PrefGeneralFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.xpref_general, rootKey)
    }
}

