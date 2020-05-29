package com.sensorpic.demo.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.sensorpic.demo.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}
