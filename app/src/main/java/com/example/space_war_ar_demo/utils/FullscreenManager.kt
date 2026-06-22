package com.example.space_war_ar_demo.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * Менеджер для управления полноэкранным режимом
 * Обеспечивает скрытие системных UI элементов и настройку полноэкранного режима
 */
class FullscreenManager {

    companion object {
        private const val TAG = "FullscreenManager"

        /**
         * Настройка полноэкранного режима ДО установки контента
         * @param activity Активность для настройки
         */
        fun setupFullscreenMode(activity: Activity) {
            try {
                Log.d(TAG, "setupFullscreenMode: configuring fullscreen mode")

                // Убираем заголовок окна
                //activity.supportActionBar?.hide()

                // Настройка флагов окна для полноэкранного режима
                activity.window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
                )

                // Убираем отступы для системных панелей
                //activity.window.setDecorFitsSystemWindows(false)

                // Настройка для устройств с вырезами экрана
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    activity.window.attributes.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }

                Log.d(TAG, "setupFullscreenMode: fullscreen mode configured successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error setting up fullscreen mode", e)
            }
        }

        /**
         * Скрывает системные UI элементы для полноэкранного режима
         * Убирает все рамки, черные полоски и системные элементы
         * @param activity Активность для настройки
         */
        fun hideSystemUI(activity: Activity) {
            try {
                Log.d(TAG, "hideSystemUI: hiding system UI for fullscreen mode")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+)
                    activity.window.setDecorFitsSystemWindows(false)
                    activity.window.insetsController?.let { controller ->
                        // Скрываем все системные панели
                        controller.hide(
                            WindowInsets.Type.statusBars() or
                                    WindowInsets.Type.navigationBars() or
                                    WindowInsets.Type.systemBars() or
                                    WindowInsets.Type.displayCutout()
                        )
                        // Настройка поведения системных панелей
                        controller.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }

                    // Дополнительные настройки для полного экрана
                    activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                    activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Android 9+ (API 28+)
                    activity.window.attributes.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_LOW_PROFILE
                            )

                    // Прозрачные системные панели
                    activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                    activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT

                } else {
                    // Android 8 и ниже
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_LOW_PROFILE
                            )

                    // Прозрачные системные панели
                    activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                    activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                }

                // Дополнительные настройки для устранения черных полосок
                activity.window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )

                // Убираем отступы для контента
                activity.window.decorView.fitsSystemWindows = false

                // Настройка для устройств с жестовой навигацией (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                        // Игнорируем системные отступы для полного экрана
                        view.setPadding(0, 0, 0, 0)
                        insets
                    }
                }

                Log.d(TAG, "hideSystemUI: system UI hidden successfully for fullscreen mode")

            } catch (e: Exception) {
                Log.e(TAG, "Error hiding system UI", e)
            }
        }

        /**
         * Применяет полноэкранный режим с дополнительной проверкой
         * Используется в onWindowFocusChanged для обеспечения постоянного полноэкранного режима
         * @param activity Активность для настройки
         */
        fun ensureFullscreenMode(activity: Activity) {
            try {
                Log.d(TAG, "ensureFullscreenMode: ensuring fullscreen mode")

                // Повторно применяем полноэкранный режим
                setupFullscreenMode(activity)
                hideSystemUI(activity)

                // Дополнительная проверка через небольшую задержку
                activity.window.decorView.postDelayed({
                    if (activity.hasWindowFocus()) {
                        hideSystemUI(activity)
                    }
                }, 100)

                Log.d(TAG, "ensureFullscreenMode: fullscreen mode ensured")

            } catch (e: Exception) {
                Log.e(TAG, "Error ensuring fullscreen mode", e)
            }
        }

        /**
         * Проверяет, поддерживается ли полноэкранный режим на устройстве
         * @param activity Активность для проверки
         * @return true если поддерживается, false в противном случае
         */
        fun isFullscreenSupported(activity: Activity): Boolean {
            return try {
                // Проверяем доступность системных функций
                activity.window != null &&
                        activity.window.decorView != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            } catch (e: Exception) {
                Log.e(TAG, "Error checking fullscreen support", e)
                false
            }
        }

        /**
         * Восстанавливает нормальный режим (показывает системные панели)
         * @param activity Активность для настройки
         */
        fun showSystemUI(activity: Activity) {
            try {
                Log.d(TAG, "showSystemUI: showing system UI")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+)
                    activity.window.insetsController?.let { controller ->
                        controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    }
                } else {
                    // Android 10 и ниже
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }

                // Убираем флаг полноэкранного режима
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

                Log.d(TAG, "showSystemUI: system UI shown successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error showing system UI", e)
            }
        }
    }
}


