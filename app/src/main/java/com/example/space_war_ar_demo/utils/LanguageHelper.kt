package com.example.space_war_ar_demo.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.example.space_war_ar_demo.SettingsManager
import java.util.Locale

/**
 * Утилита для управления языком приложения
 * Обеспечивает переключение языка и применение локали к контексту
 */
object LanguageHelper {

    /**
     * Получает локаль на основе сохраненного языка
     */
    fun getLocale(context: Context): Locale {
        val language = SettingsManager.getLanguage(context)
        return when (language) {
            "Русский", "ru", "Russian" -> Locale("ru", "RU")
            "Английский", "English", "en" -> Locale("en", "US")
            "Китайский", "Chinese", "zh" -> Locale("zh", "CN")
            else -> Locale.getDefault()
        }
    }

    /**
     * Применяет локаль к контексту и возвращает обновленный контекст
     */
    fun attachBaseContext(context: Context): Context {
        val locale = getLocale(context)
        return updateContext(context, locale)
    }

    /**
     * Обновляет контекст с новой локалью
     */
    private fun updateContext(context: Context, locale: Locale): Context {
        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }

    /**
     * Обновляет ресурсы с новой локалью
     */
    fun updateResources(context: Context, locale: Locale): Resources {
        val configuration = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration).resources
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            return context.resources
        }
    }

    /**
     * Применяет язык к Activity (вызывать в onCreate перед setContentView)
     */
    fun applyLanguage(context: Context) {
        val locale = getLocale(context)
        updateResources(context, locale)
    }
}

















