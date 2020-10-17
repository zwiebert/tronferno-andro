package de.bertw.tronferno

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat


class PrefMembersFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.xpref_members, rootKey)
    }
}

