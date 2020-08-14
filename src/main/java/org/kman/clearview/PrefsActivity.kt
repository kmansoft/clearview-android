@file:Suppress("DEPRECATION")

package org.kman.clearview

import android.os.Bundle
import android.preference.PreferenceActivity

@Suppress("DEPRECATION")
class PrefsActivity : PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.prefs)
    }
}