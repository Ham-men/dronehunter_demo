package com.example.space_war_ar_demo.event

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.space_war_ar_demo.EventType
import com.example.space_war_ar_demo.GameEvent
import com.example.space_war_ar_demo.R
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Менеджер для отображения уведомлений о событиях.
 * Обеспечивает показ кастомных уведомлений с очередью и анимациями.
 */
object EventManager {
    private const val TAG = "EventManager"

    private val listeners = mutableListOf<(GameEvent) -> Unit>()
    private val eventQueue = ArrayDeque<Pair<Context, GameEvent>>()
    private val isNotificationShowing = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Константы для анимаций
    private const val ANIMATION_DURATION_SHOW = 250L
    private const val ANIMATION_DURATION_HIDE = 300L
    private const val NOTIFICATION_DISPLAY_TIME = 2500L
    private const val MAX_QUEUE_SIZE = 10

    /**
     * Показ события с уведомлением
     */
    fun showEvent(context: Context, event: GameEvent) {
        Log.d(TAG, "showEvent: showing event: ${event.type} - ${event.message}")
        try {
            // Если уже показывается уведомление — ставим в очередь
            if (isNotificationShowing.get()) {
                addToQueue(context, event)
                return
            }

            isNotificationShowing.set(true)

            // Кастомное уведомление только если context — Activity
            if (context is Activity) {
                showCustomNotification(context, event)
            } else {
                showToastNotification(context, event)
            }

            // Уведомляем слушателей
            notifyListeners(event)

            Log.d(TAG, "showEvent: event shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing event", e)
            isNotificationShowing.set(false)
            // Показываем следующее уведомление из очереди
            processNextEvent()
        }
    }

    /**
     * Показ кастомного уведомления в Activity
     */
    private fun showCustomNotification(activity: Activity, event: GameEvent) {
        Log.d(TAG, "showCustomNotification: showing custom notification")
        try {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val inflater = LayoutInflater.from(activity)
            val notifView = inflater.inflate(R.layout.custom_notification, rootView, false)

            val card = notifView.findViewById<CardView>(R.id.notificationCard)
            val icon = notifView.findViewById<ImageView>(R.id.notificationIcon)
            val text = notifView.findViewById<TextView>(R.id.notificationText)

            // Устанавливаем текст уведомления
            text.text = event.message

            // Настраиваем цвет и иконку по типу события
            configureNotificationAppearance(activity, card, icon, event)

            // Добавляем уведомление в корневое представление
            notifView.alpha = 0f
            rootView.addView(notifView)

            // Анимация появления
            notifView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION_SHOW)
                .start()

            // Автоматическое скрытие через заданное время
            mainHandler.postDelayed({
                hideCustomNotification(notifView, rootView)
            }, NOTIFICATION_DISPLAY_TIME)

            Log.d(TAG, "showCustomNotification: custom notification displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing custom notification", e)
            // Fallback к Toast
            showToastNotification(activity, event)
        }
    }

    /**
     * Настройка внешнего вида уведомления
     */
    private fun configureNotificationAppearance(
        context: Context,
        card: CardView,
        icon: ImageView,
        event: GameEvent
    ) {
        try {
            when (event.type) {
                EventType.REWARD, EventType.ACHIEVEMENT -> {
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.holo_green_dark
                        )
                    )
                    icon.setImageResource(event.iconRes ?: R.drawable.ic_money)
                }

                EventType.ERROR, EventType.LEVEL_LOSE -> {
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.holo_red_dark
                        )
                    )
                    icon.setImageResource(event.iconRes ?: R.drawable.ic_shield)
                }

                EventType.WARNING -> {
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.holo_orange_dark
                        )
                    )
                    icon.setImageResource(event.iconRes ?: android.R.drawable.ic_dialog_alert)
                }

                EventType.LEVEL_WIN -> {
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.holo_blue_dark
                        )
                    )
                    icon.setImageResource(event.iconRes ?: R.drawable.ic_health)
                }

                else -> {
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.darker_gray
                        )
                    )
                    icon.setImageResource(event.iconRes ?: R.drawable.ic_minimap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring notification appearance", e)
        }
    }

    /**
     * Скрытие кастомного уведомления
     */
    private fun hideCustomNotification(notifView: android.view.View, rootView: ViewGroup) {
        try {
            notifView.animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION_HIDE)
                .withEndAction {
                    try {
                        rootView.removeView(notifView)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing notification view", e)
                    } finally {
                        isNotificationShowing.set(false)
                        processNextEvent()
                    }
                }
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding custom notification", e)
            isNotificationShowing.set(false)
            processNextEvent()
        }
    }

    /**
     * Показ Toast уведомления
     */
    private fun showToastNotification(context: Context, event: GameEvent) {
        Log.d(TAG, "showToastNotification: showing toast notification")
        try {
            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            isNotificationShowing.set(false)
            processNextEvent()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast notification", e)
            isNotificationShowing.set(false)
            processNextEvent()
        }
    }

    /**
     * Добавление события в очередь
     */
    private fun addToQueue(context: Context, event: GameEvent) {
        try {
            if (eventQueue.size >= MAX_QUEUE_SIZE) {
                // Удаляем самое старое событие если очередь переполнена
                eventQueue.pollFirst()
                Log.w(TAG, "addToQueue: queue full, removed oldest event")
            }

            eventQueue.add(context to event)
            Log.d(TAG, "addToQueue: event added to queue, queue size: ${eventQueue.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event to queue", e)
        }
    }

    /**
     * Обработка следующего события из очереди
     */
    private fun processNextEvent() {
        try {
            val nextEvent = eventQueue.pollFirst()
            if (nextEvent != null) {
                Log.d(TAG, "processNextEvent: processing next event from queue")
                showEvent(nextEvent.first, nextEvent.second)
            } else {
                Log.d(TAG, "processNextEvent: no more events in queue")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing next event", e)
        }
    }

    /**
     * Добавление слушателя событий
     */
    fun addListener(listener: (GameEvent) -> Unit) {
        try {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
                Log.d(TAG, "addListener: listener added, total listeners: ${listeners.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding listener", e)
        }
    }

    /**
     * Удаление слушателя событий
     */
    fun removeListener(listener: (GameEvent) -> Unit) {
        try {
            val removed = listeners.remove(listener)
            if (removed) {
                Log.d(TAG, "removeListener: listener removed, total listeners: ${listeners.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing listener", e)
        }
    }

    /**
     * Уведомление всех слушателей
     */
    private fun notifyListeners(event: GameEvent) {
        try {
            listeners.forEach { listener ->
                try {
                    listener(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying listener", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying listeners", e)
        }
    }

    /**
     * Очистка очереди событий
     */
    fun clearQueue() {
        try {
            val queueSize = eventQueue.size
            eventQueue.clear()
            Log.d(TAG, "clearQueue: cleared $queueSize events from queue")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing queue", e)
        }
    }

    /**
     * Получение размера очереди
     */
    fun getQueueSize(): Int {
        return try {
            eventQueue.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting queue size", e)
            0
        }
    }

    /**
     * Проверка, показывается ли уведомление
     */
    fun isShowingNotification(): Boolean {
        return isNotificationShowing.get()
    }

    /**
     * Принудительное завершение показа уведомлений
     */
    fun forceStopNotifications() {
        try {
            isNotificationShowing.set(false)
            clearQueue()
            Log.d(TAG, "forceStopNotifications: notifications stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error forcing stop notifications", e)
        }
    }
} 