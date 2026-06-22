package com.example.space_war_ar_demo

import android.os.Bundle

enum class EventType {
    NOTIFICATION,
    WARNING,
    ERROR,
    REWARD,
    ACHIEVEMENT,
    LEVEL_WIN,
    LEVEL_LOSE,
    CUSTOM
}

data class GameEvent(
    val type: EventType,
    val message: String,
    val iconRes: Int? = null,
    val extraData: Bundle? = null
) 