package com.example.space_war_ar_demo.brain

import com.google.ar.sceneform.math.Vector3

data class PlayerContext(
    val id: Int = 0,
    val position: Vector3 = Vector3.zero(),
    val velocity: Vector3 = Vector3.zero(),
    val isMoving: Boolean = false,
    val shieldRadius: Float = 0f,
    val threatLevel: Float = 0f,
    val isShieldActive: Boolean = false,
    val isRepulsing: Boolean = false,
    val repulseStrength: Float = 0f,
    val defenseIntensity: Float = 0f
)
