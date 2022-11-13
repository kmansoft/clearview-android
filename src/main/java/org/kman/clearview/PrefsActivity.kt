@file:Suppress("DEPRECATION")

package org.kman.clearview

import android.os.Bundle
import android.preference.PreferenceActivity

class PrefsActivity : PreferenceActivity() {
    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.prefs)
    }
}