package com.example.space_war_ar_demo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Менеджер прогресса уровней
 * Управляет разблокировкой уровней в игре
 */
class LevelProgressManager(private val context: Context) {

    companion object {
        private const val TAG = "LevelProgressManager"
        private const val PREFS_NAME = "level_progress"
        private const val KEY_SOLAR_SYSTEM_PROGRESS = "solar_system_progress"
        private const val KEY_ALPHA_CENTAURI_PROGRESS = "alpha_centauri_progress"

        // Порядок уровней в Солнечной системе
        val SOLAR_SYSTEM_LEVELS = listOf(
            "planetPluto",      // 1. Плутон
            "planetNeptune",    // 2. Нептун

        )


    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Получить текущий прогресс Солнечной системы
     */
    fun getSolarSystemProgress(): Int {
        return prefs.getInt(KEY_SOLAR_SYSTEM_PROGRESS, 0)
    }

    /**
     * Получить текущий прогресс системы Альфа Центавра
     */
    fun getAlphaCentauriProgress(): Int {
        return prefs.getInt(KEY_ALPHA_CENTAURI_PROGRESS, 0)
    }

    /**
     * Установить прогресс Солнечной системы
     */
    fun setSolarSystemProgress(level: Int) {
        val currentProgress = getSolarSystemProgress()
        if (level > currentProgress) {
            prefs.edit().putInt(KEY_SOLAR_SYSTEM_PROGRESS, level).apply()
            Log.d(TAG, "Solar system progress updated: $currentProgress -> $level")
        }
    }

    /**
     * Установить прогресс системы Альфа Центавра
     */
    fun setAlphaCentauriProgress(level: Int) {
        val currentProgress = getAlphaCentauriProgress()
        if (level > currentProgress) {
            prefs.edit().putInt(KEY_ALPHA_CENTAURI_PROGRESS, level).apply()
            Log.d(TAG, "Alpha Centauri progress updated: $currentProgress -> $level")
        }
    }

    /**
     * Проверить, разблокирован ли уровень в Солнечной системе
     */
    fun isSolarSystemLevelUnlocked(levelIndex: Int): Boolean {


         val progress = getSolarSystemProgress()
         val isUnlocked = levelIndex <= progress
         Log.d(
             TAG,
             "isSolarSystemLevelUnlocked: levelIndex=$levelIndex, progress=$progress, unlocked=$isUnlocked"
         )
         return isUnlocked
    }

    /**
     * Проверить, разблокирована ли система Альфа Центавра
     */
    fun isAlphaCentauriSystemUnlocked(): Boolean {
        return getSolarSystemProgress() >= SOLAR_SYSTEM_LEVELS.size
    }

    /**
     * Проверить, разблокирован ли уровень в системе Альфа Центавра
     */
    fun isAlphaCentauriLevelUnlocked(levelIndex: Int): Boolean {

        return true
         //Система Альфа Центавра разблокируется только после прохождения всей Солнечной системы
         if (!isAlphaCentauriSystemUnlocked()) {
             return false
         }
         return levelIndex <= getAlphaCentauriProgress()
    }



    /**
     * Получить индекс уровня по ID для Солнечной системы
     */
    fun getSolarSystemLevelIndex(levelId: Int): Int {
        return when (levelId) {
            101 -> 0  // Плутон
            102 -> 1  // Нептун
            103 -> 2  // Уран
            104 -> 3  // Сатурн
            105 -> 4  // Юпитер
            106 -> 5  // Марс
            107 -> 6  // Земля
            108 -> 7  // Венера
            109 -> 8  // Меркурий
            110 -> 9  // Солнце
            else -> 0
        }
    }



    /**
     * Отметить уровень как пройденный
     */
    fun markLevelCompleted(levelId: Int) {
        Log.d(TAG, "Marking level $levelId as completed")

        // Проверяем, к какой системе относится уровень
        if (levelId in 101..110) {
            // Солнечная система
            val levelIndex = getSolarSystemLevelIndex(levelId)
            setSolarSystemProgress(levelIndex + 1) // +1 потому что прогресс - это количество пройденных уровней
            Log.d(TAG, "Solar system level $levelId (index $levelIndex) marked as completed")
        }
    }

    /**
     * Сбросить весь прогресс (для тестирования)
     */
    fun resetAllProgress() {
        prefs.edit()
            .putInt(KEY_SOLAR_SYSTEM_PROGRESS, 0)
            .putInt(KEY_ALPHA_CENTAURI_PROGRESS, 0)
            .apply()
        Log.d(TAG, "All progress reset")
    }

    /**
     * Установить начальный прогресс (только Плутон разблокирован)
     */
    fun setInitialProgress() {
        prefs.edit()
            .putInt(KEY_SOLAR_SYSTEM_PROGRESS, 0)
            .putInt(KEY_ALPHA_CENTAURI_PROGRESS, 0)
            .apply()
        Log.d(TAG, "Initial progress set: only Pluto unlocked")
    }
}
