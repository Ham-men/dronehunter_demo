// DroneBehavior.kt (исправленная версия)
package com.example.space_war_ar_demo.enemy.behavior

import com.example.space_war_ar_demo.brain.DroneCommand
import com.example.space_war_ar_demo.brain.PlayerContext
import com.example.space_war_ar_demo.enemy.types.DroneEnemy
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.atan2

abstract class DroneBehavior {

    data class BehaviorConfig(
        val detectionRange: Float = 5f,
        val attackRange: Float = 5f,
        val maxSpeed: Float = 0.02f,
        val rotationSpeed: Float = 2f,
        val orbitRadius: Float = 4f,
        val attackCooldownMs: Long = 500L,
        val retreatChance: Float = 0.3f
    )
//    data class BehaviorConfig( //старая версия
//        val detectionRange: Float = 4f,
//        val attackRange: Float = 4f,
//        val maxSpeed: Float = 0.2f,
//        val rotationSpeed: Float = 45f,
//        val orbitRadius: Float = 5f,
//        val attackCooldownMs: Long = 1500L,
//        val retreatChance: Float = 0.3f
//    )

    protected var behaviorConfig: BehaviorConfig = BehaviorConfig()
    protected var playerContext: PlayerContext = PlayerContext()
    private var altitudeTarget: Float? = null

    open val detectionRange: Float get() = behaviorConfig.detectionRange
    open val attackRange: Float get() = behaviorConfig.attackRange
    open val maxSpeed: Float get() = behaviorConfig.maxSpeed
    open val rotationSpeed: Float get() = behaviorConfig.rotationSpeed
    open val orbitRadius: Float get() = behaviorConfig.orbitRadius
    open val attackCooldownMs: Long get() = behaviorConfig.attackCooldownMs
    open val retreatChance: Float get() = behaviorConfig.retreatChance

    open fun configure(config: BehaviorConfig) {
        behaviorConfig = config
    }

    abstract fun update(
        drone: DroneEnemy,
        playerPosition: Vector3,
        playerVelocity: Vector3,
        deltaTime: Float
    )
    abstract fun onDamageTaken(drone: DroneEnemy)

    open fun onSpawn(drone: DroneEnemy) {}
    open fun onDestroyed(drone: DroneEnemy) {}
    open fun onAttackExecuted(drone: DroneEnemy) {}
    open fun onBossCommand(command: DroneCommand) {}
    open fun onPlayerShockwave(strength: Float) {}

    open fun updatePlayerContext(context: PlayerContext) {
        playerContext = context
    }

    open fun setTargetAltitude(height: Float) {
        altitudeTarget = height
    }

    protected fun getTargetAltitude(currentY: Float): Float =
        altitudeTarget ?: currentY

    protected fun hasAltitudeTarget(): Boolean = altitudeTarget != null

    protected fun calculateRotationToTarget(currentPos: Vector3, targetPos: Vector3): Quaternion {
        val flatTarget = Vector3(targetPos.x, currentPos.y, targetPos.z)
        val direction = Vector3.subtract(flatTarget, currentPos).normalized()
        val angle = atan2(direction.x, direction.z)
        return Quaternion.axisAngle(
            Vector3(0f, 1f, 0f),
            Math.toDegrees(angle.toDouble()).toFloat()
        )
    }

    protected fun predictLeadPosition(
        origin: Vector3,
        playerPosition: Vector3,
        playerVelocity: Vector3,
        maxLookAhead: Float = 0.9f
    ): Vector3 {
        val toPlayer = Vector3.subtract(playerPosition, origin)
        val distance = toPlayer.length().coerceAtLeast(0.001f)
        val lookAhead = (distance / (maxSpeed * 6f)).coerceIn(0f, maxLookAhead)
        val projected = Vector3.add(playerPosition, playerVelocity.scaled(lookAhead))
        return projected
    }

    protected fun effectiveDetectionRange(): Float =
        detectionRange * playerContext.threatLevel.coerceIn(0.7f, 1.5f)

    protected fun effectiveAttackRange(): Float =
        attackRange * playerContext.threatLevel.coerceIn(0.8f, 1.4f)
}