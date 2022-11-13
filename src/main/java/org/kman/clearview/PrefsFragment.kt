package org.kman.clearview

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.children

class PrefsFragment
    : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)

        removeIconSpace(preferenceScreen)
    }

    private fun removeIconSpace(pref: Preference) {
        pref.isIconSpaceReserved = false

        if (pref is PreferenceGroup) {
            for (child in pref.children) {
                removeIconSpace(child)
            }
        }
    }
}