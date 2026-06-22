package com.example.space_war_ar_demo

import android.content.Context

object SettingsManager {
    private const val PREFS = "game_settings"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_SOUND_VOLUME = "sound_volume"
    private const val KEY_ADS_ENABLED = "ads_enabled"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_LANGUAGE = "language"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getMusicVolume(context: Context): Int = prefs(context).getInt(KEY_MUSIC_VOLUME, 70)
    fun setMusicVolume(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_MUSIC_VOLUME, value.coerceIn(0, 100)).apply()
    }

    fun getSoundVolume(context: Context): Int = prefs(context).getInt(KEY_SOUND_VOLUME, 70)
    fun setSoundVolume(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SOUND_VOLUME, value.coerceIn(0, 100)).apply()
    }

    fun isAdsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ADS_ENABLED, true)
    fun setAdsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ADS_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getLanguage(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, "Русский") ?: "Русский"

    fun setLanguage(context: Context, language: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }
}



