package com.example.data.local

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log

/**
 * Gerenciador local de preferências de configuração do LC Player.
 * Persiste preferências de orientação, formato de hora, idioma e player do usuário,
 * e aplica dinamicamente a orientação na Activity.
 */
object AppSettingsManager {
    private const val TAG = "LCPlayer_Orientation"
    private const val PREFS_NAME = "lc_player_settings"

    private const val KEY_ORIENTATION = "pref_orientation"
    private const val KEY_TIME_FORMAT = "pref_time_format"
    private const val KEY_LANGUAGE = "pref_language"
    private const val KEY_PLAYER_ENGINE = "pref_player_engine"
    private const val KEY_LIVE_STREAM_FORMAT = "pref_live_stream_format"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getOrientation(context: Context): String {
        return getPrefs(context).getString(KEY_ORIENTATION, "AUTO") ?: "AUTO"
    }

    fun setOrientation(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_ORIENTATION, value).apply()
    }

    /**
     * Aplica a orientação desejada à Activity em tempo de execução e registra o diagnóstico no Logcat.
     */
    fun applyOrientation(activity: Activity, orientationValue: String) {
        val targetOrientation = when (orientationValue) {
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        Log.i(TAG, "DISPLAY_ORIENTATION_REQUEST=$orientationValue (requestedOrientation=$targetOrientation)")

        try {
            activity.requestedOrientation = targetOrientation
            val observedConfig = activity.resources.configuration.orientation
            val observedName = when (observedConfig) {
                Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
                Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
                else -> "UNDEFINED"
            }
            Log.i(TAG, "DISPLAY_ORIENTATION_OBSERVED=$observedName ($observedConfig)")
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao aplicar orientação $orientationValue no dispositivo: ${e.message}")
        }
    }

    fun getTimeFormat(context: Context): String {
        return getPrefs(context).getString(KEY_TIME_FORMAT, "24H") ?: "24H"
    }

    fun setTimeFormat(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_TIME_FORMAT, value).apply()
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "PT_BR") ?: "PT_BR"
    }

    fun setLanguage(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, value).apply()
        applyLanguage(context, value)
    }

    fun applyLanguage(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            "EN" -> java.util.Locale.forLanguageTag("en")
            "ES" -> java.util.Locale.forLanguageTag("es")
            else -> java.util.Locale.forLanguageTag("pt-BR")
        }

        java.util.Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun getPlayerEngine(context: Context): String {
        return getPrefs(context).getString(KEY_PLAYER_ENGINE, "EXOPLAYER") ?: "EXOPLAYER"
    }

    fun setPlayerEngine(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PLAYER_ENGINE, value).apply()
    }

    fun getLiveStreamFormat(context: Context): String {
        return getPrefs(context).getString(KEY_LIVE_STREAM_FORMAT, "HLS") ?: "HLS"
    }

    fun setLiveStreamFormat(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_LIVE_STREAM_FORMAT, value).apply()
    }

    fun getLiveStreamExtension(context: Context): String {
        return if (getLiveStreamFormat(context) == "TS") "ts" else "m3u8"
    }

    private const val KEY_PARENTAL_PIN = "pref_parental_pin"
    private const val KEY_PARENTAL_ENABLED = "pref_parental_enabled"
    private const val KEY_AUTO_NEXT_EPISODE = "pref_auto_next_episode"

    // PIN Padrão de fábrica inicial é "0000"
    fun getParentalPin(context: Context): String {
        return getPrefs(context).getString(KEY_PARENTAL_PIN, "0000") ?: "0000"
    }

    fun setParentalPin(context: Context, newPin: String) {
        getPrefs(context).edit().putString(KEY_PARENTAL_PIN, newPin).apply()
    }

    fun isParentalControlEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PARENTAL_ENABLED, true)
    }

    fun setParentalControlEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PARENTAL_ENABLED, enabled).apply()
    }

    fun isAutoNextEpisodeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_NEXT_EPISODE, true)
    }

    fun setAutoNextEpisodeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_NEXT_EPISODE, enabled).apply()
    }
}
