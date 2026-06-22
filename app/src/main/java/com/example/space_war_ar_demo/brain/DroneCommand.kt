package com.example.space_war_ar_demo.brain

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.Node

enum class DroneCommandType {
    MOVE_TO, ATTACK_TARGET, EVADE, RETREAT, PATROL, DEFEND, IDLE, CHASE, FOLLOW,
    EVADE_TO_RADIUS, FOCUS_FIRE, HOLD_FORMATION, CHARGE_OVER_PLAYER
}

data class DroneCommand(
    val id: Int = 0,
    val type: DroneCommandType,
    val targetPosition: Vector3? = null,
    val targetNode: Node? = null,
    val priority: Int = 0,
    val durationMs: Long = 0L,
    val intensity: Float = 1.0f,
    val entryPoint: Vector3? = null,
    val exitPoint: Vector3? = null
)
