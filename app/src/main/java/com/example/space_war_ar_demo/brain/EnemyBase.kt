// EnemyBase.kt (исправленная версия)
package com.example.space_war_ar_demo.enemy

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

abstract class EnemyBase(
    val id: String,
    var position: Vector3,
    var velocity: Vector3 = Vector3.zero(),
    var health: Int = 100,
    val maxHealth: Int = 100,
    var modelNode: Any? = null
) {
    var currentRotation: Quaternion = Quaternion.identity()
    var targetRotation: Quaternion = Quaternion.identity()
    var isRotating: Boolean = false

    abstract fun update(
        deltaTime: Float,
        playerPosition: Vector3,
        playerVelocity: Vector3
    )
    abstract fun takeDamage(damage: Int)
    abstract fun shouldAttack(playerPosition: Vector3): Boolean
}