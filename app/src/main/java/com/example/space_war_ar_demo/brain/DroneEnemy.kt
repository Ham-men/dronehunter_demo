package com.example.space_war_ar_demo.enemy.types

import com.example.space_war_ar_demo.brain.DroneCommand
import com.example.space_war_ar_demo.brain.PlayerContext
import com.example.space_war_ar_demo.enemy.EnemyBase
import com.example.space_war_ar_demo.enemy.behavior.DroneBehavior
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

class DroneEnemy(
    id: String,
    position: Vector3,
    health: Int = 100,
    maxHealth: Int = health,
    behavior: DroneBehavior,
    private var attackCallback: ((DroneEnemy) -> Unit)? = null
) : EnemyBase(id, position, health = health, maxHealth = maxHealth) {

    enum class UiState { NORMAL, RETREAT, CHARGING }

    private val baseBehavior: DroneBehavior = behavior
    private var overrideBehavior: DroneBehavior? = null
    private var overrideTimer = 0f
    private var attackCooldownTimer = 0f
    private var destroyed = false
    private val modelRotationOffset = Quaternion.multiply(
        Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f),
        Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
    )
    private var neighborPositionProvider: (() -> Vector3?)? = null
    private var latestPlayerContext: PlayerContext = PlayerContext()

    var uiState: UiState = UiState.NORMAL
    var onDestroyed: ((DroneEnemy) -> Unit)? = null

    init {
        baseBehavior.onSpawn(this)
    }

    override fun update(
        deltaTime: Float,
        playerPosition: Vector3,
        playerVelocity: Vector3
    ) {
        tickOverrideTimer(deltaTime)
        attackCooldownTimer = (attackCooldownTimer - deltaTime).coerceAtLeast(0f)
        currentBehavior().update(this, playerPosition, playerVelocity, deltaTime)
        updateRotation(deltaTime)
    }

    fun updatePlayerContext(context: PlayerContext) {
        latestPlayerContext = context
        baseBehavior.updatePlayerContext(context)
        overrideBehavior?.updatePlayerContext(context)
    }

    override fun takeDamage(damage: Int) {
        if (destroyed) {
            android.util.Log.d("dron_hp2", "[takeDamage] Drone $id: already destroyed, ignoring damage=$damage")
            return
        }
        val healthBefore = health
        health = (health - damage).coerceAtLeast(0)
        val healthAfter = health
        android.util.Log.d("dron_hp2", "[takeDamage] Drone $id: healthBefore=$healthBefore, damage=$damage, healthAfter=$healthAfter, destroyed=${health <= 0}")
        currentBehavior().onDamageTaken(this)
        if (health <= 0 && !destroyed) {
            destroyed = true
            android.util.Log.d("dron_hp2", "[takeDamage] Drone $id: MARKED AS DESTROYED")
            onDestroyed?.invoke(this)
        }
    }

    override fun shouldAttack(playerPosition: Vector3): Boolean {
        val distance = Vector3.subtract(position, playerPosition).length()
        return distance <= currentBehavior().attackRange && attackCooldownTimer <= 0f
    }

    fun attack() {
        attackCooldownTimer = currentBehavior().attackCooldownMs / 1000f
        currentBehavior().onAttackExecuted(this)
        attackCallback?.invoke(this)
    }

    fun setAttackCallback(callback: ((DroneEnemy) -> Unit)?) {
        attackCallback = callback
    }

    fun applyBossCommand(command: DroneCommand) {
        currentBehavior().onBossCommand(command)
    }

    fun notifyPlayerShockwave(strength: Float) {
        currentBehavior().onPlayerShockwave(strength)
    }

    fun applyImpulse(impulse: Vector3) {
        velocity = Vector3.add(velocity, impulse)
    }

    fun setNeighborProvider(provider: (() -> Vector3?)?) {
        neighborPositionProvider = provider
    }

    fun getNeighborPosition(): Vector3? = neighborPositionProvider?.invoke()

    fun overrideBehavior(tempBehavior: DroneBehavior, durationMs: Long = 1500L) {
        tempBehavior.updatePlayerContext(latestPlayerContext)
        tempBehavior.onSpawn(this)
        overrideBehavior?.onDestroyed(this)
        overrideBehavior = tempBehavior
        overrideTimer = durationMs / 1000f
    }

    fun clearBehaviorOverride() {
        overrideBehavior?.onDestroyed(this)
        overrideBehavior = null
        overrideTimer = 0f
    }

    fun setInitialRotation(rotation: Quaternion) {
        currentRotation = rotation
        targetRotation = rotation
        applyRotationToModel()
    }

    fun forceRotation(rotation: Quaternion) {
        currentRotation = rotation
        targetRotation = rotation
        isRotating = false
        applyRotationToModel()
    }

    private fun updateRotation(deltaTime: Float) {
        if (isRotating) {
            currentRotation = Quaternion.slerp(
                currentRotation,
                targetRotation,
                currentBehavior().rotationSpeed * deltaTime
            )
            if (areQuaternionsClose(currentRotation, targetRotation, 0.01f)) {
                isRotating = false
            }
        }
        applyRotationToModel()
    }

    private fun applyRotationToModel() {
        val node = modelNode as? Node ?: return
        val adjustedRotation = Quaternion.multiply(currentRotation, modelRotationOffset)
        node.localRotation = adjustedRotation
    }

    private fun areQuaternionsClose(q1: Quaternion, q2: Quaternion, tolerance: Float): Boolean {
        return (kotlin.math.abs(q1.x - q2.x) < tolerance &&
                kotlin.math.abs(q1.y - q2.y) < tolerance &&
                kotlin.math.abs(q1.z - q2.z) < tolerance &&
                kotlin.math.abs(q1.w - q2.w) < tolerance)
    }

    fun dispose() {
        overrideBehavior?.onDestroyed(this)
        baseBehavior.onDestroyed(this)
    }

    private fun currentBehavior(): DroneBehavior = overrideBehavior ?: baseBehavior
    
    fun updateAsteroidsAndAlliedTargets(
        asteroids: List<com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo>,
        alliedTargets: List<com.example.space_war_ar_demo.brain.BrainBot_v4.AlliedTargetInfo>
    ) {
        val behavior = currentBehavior()
        if (behavior is com.example.space_war_ar_demo.enemy.behavior.types.AggressiveDroneBehavior) {
            behavior.updateAsteroids(asteroids)
            behavior.updateAlliedTargets(alliedTargets)
        }
    }

    private fun tickOverrideTimer(deltaTime: Float) {
        if (overrideBehavior == null) return
        overrideTimer = (overrideTimer - deltaTime).coerceAtLeast(0f)
        if (overrideTimer == 0f) {
            overrideBehavior?.onDestroyed(this)
            overrideBehavior = null
        }
    }
}
// DroneEnemy.kt (исправленная версия)