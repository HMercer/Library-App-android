package com.sensorpic.demo.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.sensorpic.demo.DEFAULT_ML_CONFIDENCE_THRESHOLD
import com.sensorpic.demo.R

object Settings {

    private lateinit var KEY_CONFIDENCE_THRESHOLD: String
    private var context: Context? = null
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private const val defaultThreshold = (DEFAULT_ML_CONFIDENCE_THRESHOLD * 100).toInt()

    fun setContext(context: Context) {
        this.context = context
        KEY_CONFIDENCE_THRESHOLD = context.getString(R.string.pref_confidence_threshold)
        if (prefs.getInt(KEY_CONFIDENCE_THRESHOLD, -1) == -1) setThresholdToDefault()
    }

    /// Algorithm confidence threshold

    fun getThreshold() : Float {
        return prefs.getInt(KEY_CONFIDENCE_THRESHOLD, defaultThreshold) / 100f
    }

    /// Private methods

    private fun setThresholdToDefault() {
        prefs.edit().putInt(KEY_CONFIDENCE_THRESHOLD, defaultThreshold).apply()
    }

}
