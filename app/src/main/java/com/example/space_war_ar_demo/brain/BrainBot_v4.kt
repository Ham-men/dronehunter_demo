// BrainBot_v3.kt (оптимизированная версия)
package com.example.space_war_ar_demo.brain

import com.example.space_war_ar_demo.enemy.EnemyBase
import com.example.space_war_ar_demo.enemy.behavior.DroneBehavior
import com.example.space_war_ar_demo.enemy.behavior.types.AggressiveDroneBehavior
import com.example.space_war_ar_demo.enemy.types.DroneEnemy
import com.google.ar.sceneform.math.Vector3
import kotlin.random.Random

class BrainBot_v4 {
    companion object {
        private const val TAG = "BrainBot_v4"
        private const val COHESION_WEIGHT = 0.18f
        private const val ALIGNMENT_WEIGHT = 0.12f
        private const val SEPARATION_WEIGHT = 0.35f
        private const val FLOW_WEIGHT = 0.08f
        private const val BOUNDARY_WEIGHT = 0.35f
        private const val LEASH_WEIGHT = 0.45f
        private const val DESIRED_SEPARATION = 0.7f
        private const val FLOCK_RADIUS = 3.0f
        private const val MAX_STEER_FORCE = 0.18f
        private const val FLOOR_OFFSET = 0.6f
        private const val CEILING_OFFSET = 1.4f
        private const val FRIEND_LINK_PROBABILITY = 0.55f
        private const val ATTACK_RUN_MIN_DELAY_MS = 6500L
        private const val ATTACK_RUN_MAX_DELAY_MS = 11000L
    }
    
    private val enemies = mutableMapOf<String, EnemyBase>()
    private val friendLinks = mutableMapOf<String, String>()
    private var playerPosition: Vector3 = Vector3.zero()
    private var playerVelocity: Vector3 = Vector3.zero()
    private var playerIsMoving: Boolean = false
    private var playerContext: PlayerContext = PlayerContext()
    private var nextAttackRunAtMs: Long = 0L
    private var attackRunCounter: Long = 0L
    
    // Астероиды для укрытий
    private val asteroids = mutableListOf<AsteroidInfo>()
    
    // Союзные цели (транспорт и станции)
    private val alliedTargets = mutableListOf<AlliedTargetInfo>()
    
    // === ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ ИНТЕГРАЦИИ С УРОВНЯМИ ===
    
    var onEnemySpawned: ((EnemyBase) -> Unit)? = null
    var onEnemyDestroyed: ((EnemyBase) -> Unit)? = null
    
    fun addDrone(
        id: String,
        position: Vector3,
        health: Int = 100,
        maxHealth: Int = health,
        modelNode: Any? = null,
        behavior: DroneBehavior = AggressiveDroneBehavior(),
        behaviorConfig: DroneBehavior.BehaviorConfig = DroneBehavior.BehaviorConfig(),
        attackCallback: ((DroneEnemy) -> Unit)? = null,
        targetAltitude: Float? = null
    ): DroneEnemy {
        behavior.configure(behaviorConfig)
        val drone = DroneEnemy(
            id = id,
            position = position,
            health = health,
            maxHealth = maxHealth,
            behavior = behavior,
            attackCallback = attackCallback
        ).apply {
            this.modelNode = modelNode
            setNeighborProvider { pickNeighborPosition(id) }
            onDestroyed = { handleDroneDestroyed(it) }
            updatePlayerContext(playerContext)
        }
        behavior.updatePlayerContext(playerContext)
        behavior.setTargetAltitude(targetAltitude ?: position.y)
        enemies[id] = drone
        assignFriendlyLink(id)
        onEnemySpawned?.invoke(drone)
        return drone
    }
    
    fun updatePlayerData(
        position: Vector3,
        velocity: Vector3,
        isMoving: Boolean,
        isShieldActive: Boolean = playerContext.isShieldActive,
        isRepulsing: Boolean = playerContext.isRepulsing,
        threatLevel: Float = playerContext.threatLevel,
        shieldRadius: Float = playerContext.shieldRadius,
        repulseStrength: Float = playerContext.repulseStrength
    ) {
        playerPosition = position
        playerVelocity = velocity
        playerIsMoving = isMoving
        playerContext = playerContext.copy(
            isMoving = isMoving,
            isShieldActive = isShieldActive,
            isRepulsing = isRepulsing,
            threatLevel = threatLevel,
            shieldRadius = shieldRadius,
            repulseStrength = repulseStrength
        )
    }

    fun updatePlayerContext(context: PlayerContext) {
        playerContext = context
    }
    
    fun damageEnemy(enemyId: String, damage: Int) {
        enemies[enemyId]?.takeDamage(damage)
    }
    
    fun getEnemy(enemyId: String): EnemyBase? = enemies[enemyId]
    fun getAllEnemies(): List<EnemyBase> = enemies.values.toList()
    fun getAliveEnemies(): List<EnemyBase> = enemies.values.filter { it.health > 0 }
    
    fun clearAllEnemies() {
        enemies.values.toList().forEach { disposeEnemy(it) }
        enemies.clear()
        friendLinks.clear()
    }

    fun broadcastCommand(command: DroneCommand) {
        enemies.values.filterIsInstance<DroneEnemy>().forEach { drone ->
            drone.applyBossCommand(command)
        }
    }

    fun overrideDroneBehavior(
        enemyId: String,
        tempBehavior: DroneBehavior,
        durationMs: Long = 1500L
    ) {
        (enemies[enemyId] as? DroneEnemy)?.overrideBehavior(tempBehavior, durationMs)
    }

    fun clearDroneBehaviorOverride(enemyId: String) {
        (enemies[enemyId] as? DroneEnemy)?.clearBehaviorOverride()
    }

    fun applyShockwave(center: Vector3, radius: Float, force: Float) {
        enemies.values.filterIsInstance<DroneEnemy>().forEach { drone ->
            val offset = Vector3.subtract(drone.position, center)
            val distance = offset.length()
            if (distance <= radius && distance > 0.0001f) {
                val falloff = 1f - (distance / radius)
                val impulse = offset.normalized().scaled(force * falloff)
                drone.applyImpulse(impulse)
                drone.notifyPlayerShockwave(force * falloff)
            }
        }
    }
    
    // === МЕТОДЫ ДЛЯ РЕГИСТРАЦИИ АСТЕРОИДОВ И СОЮЗНЫХ ЦЕЛЕЙ ===
    
    data class AsteroidInfo(
        val position: Vector3,
        val radius: Float = 0.5f
    )
    
    data class AlliedTargetInfo(
        val position: Vector3,
        val type: AlliedTargetType,
        val radius: Float = 1.2f
    )
    
    enum class AlliedTargetType {
        TRANSPORT,
        STATION
    }
    
    fun registerAsteroid(position: Vector3, radius: Float = 0.5f) {
        asteroids.add(AsteroidInfo(position, radius))
    }
    
    fun updateAsteroidPosition(index: Int, newPosition: Vector3) {
        if (index in asteroids.indices) {
            asteroids[index] = asteroids[index].copy(position = newPosition)
        }
    }
    
    fun removeAsteroid(index: Int) {
        if (index in asteroids.indices) {
            asteroids.removeAt(index)
        }
    }
    
    fun clearAsteroids() {
        asteroids.clear()
    }
    
    fun registerAlliedTarget(position: Vector3, type: AlliedTargetType, radius: Float = 1.2f) {
        alliedTargets.add(AlliedTargetInfo(position, type, radius))
    }
    
    fun updateAlliedTargetPosition(index: Int, newPosition: Vector3) {
        if (index in alliedTargets.indices) {
            alliedTargets[index] = alliedTargets[index].copy(position = newPosition)
        }
    }
    
    fun removeAlliedTarget(index: Int) {
        if (index in alliedTargets.indices) {
            alliedTargets.removeAt(index)
        }
    }
    
    fun clearAlliedTargets() {
        alliedTargets.clear()
    }
    
    fun getAsteroids(): List<AsteroidInfo> = asteroids.toList()
    
    fun getAlliedTargets(): List<AlliedTargetInfo> = alliedTargets.toList()
    
    fun updateAI(deltaTime: Float) {
        // Обновляем контекст для всех дронов с информацией об астероидах и союзных целях
        enemies.values.toList().forEach { enemy ->
            (enemy as? DroneEnemy)?.apply {
                updatePlayerContext(playerContext)
                // Передаем информацию об астероидах и союзных целях в поведение
                updateAsteroidsAndAlliedTargets(asteroids, alliedTargets)
            }
            enemy.update(deltaTime, playerPosition, playerVelocity)
        }
        updateCoordination(deltaTime)
        maybeStartAttackRun()
    }
    
    // === ПРИВАТНЫЕ МЕТОДЫ ===
    
    private fun updateCoordination(deltaTime: Float) {
        val drones = enemies.values.filterIsInstance<DroneEnemy>().filter { it.health > 0 }
        if (drones.size <= 1) return

        val count = drones.size.toFloat()
        val center = drones.map { it.position }.reduce { acc, v -> Vector3.add(acc, v) }.scaled(1f / count)
        val averageVelocity = drones.map { it.velocity }.reduce { acc, v -> Vector3.add(acc, v) }.scaled(1f / count)

        drones.forEach { drone ->
            val cohesion = Vector3.subtract(center, drone.position).scaled(COHESION_WEIGHT)
            val alignment = Vector3.subtract(averageVelocity, drone.velocity).scaled(ALIGNMENT_WEIGHT)

            var separation = Vector3.zero()
            drones.forEach { other ->
                if (other === drone) return@forEach
                val diff = Vector3.subtract(drone.position, other.position)
                val distance = diff.length()
                if (distance < DESIRED_SEPARATION && distance > 0.0001f) {
                    separation = Vector3.add(
                        separation,
                        diff.normalized().scaled(SEPARATION_WEIGHT / distance.coerceAtLeast(0.1f))
                    )
                }
            }

            val flow = if (playerIsMoving) playerVelocity.scaled(FLOW_WEIGHT) else Vector3.zero()
            val boundary = boundarySteer(drone.position)
            val visibility = visibilitySteer(drone.position)

            val steering = Vector3.add(
                Vector3.add(cohesion, alignment),
                Vector3.add(separation, Vector3.add(flow, Vector3.add(boundary, visibility)))
            )
            val limitedSteering = limitVector(steering, MAX_STEER_FORCE)
            drone.velocity = Vector3.add(drone.velocity, limitedSteering.scaled(deltaTime))
        }
    }
    
    private fun disposeEnemy(enemy: EnemyBase) {
        (enemy as? DroneEnemy)?.apply {
            onDestroyed = null
            dispose()
        }
    }
    
    private fun notifyEnemyDestroyed(enemy: EnemyBase) {
        onEnemyDestroyed?.invoke(enemy)
    }

    private fun handleDroneDestroyed(drone: DroneEnemy) {
        val removed = enemies.remove(drone.id)
        if (removed != null) {
            disposeEnemy(drone)
            clearFriendlyLinksFor(drone.id)
            notifyEnemyDestroyed(drone)
        }
    }

    private fun pickNeighborPosition(requesterId: String): Vector3? {
        friendLinks[requesterId]?.let { leaderId ->
            val leader = enemies[leaderId] as? DroneEnemy
            if (leader != null && leader.health > 0) {
                return leader.position
            } else {
                friendLinks.remove(requesterId)
            }
        }
        val peers = enemies.values.filterIsInstance<DroneEnemy>()
            .filter { it.id != requesterId && it.health > 0 }
        if (peers.isEmpty()) return null
        val peer = peers.random()
        return peer.position
    }

    private fun maybeStartAttackRun() {
        val now = System.currentTimeMillis()
        if (now < nextAttackRunAtMs) return
        val drones = enemies.values.filterIsInstance<DroneEnemy>().filter { it.health > 0 }
        if (drones.size < 2) {
            nextAttackRunAtMs = now + ATTACK_RUN_MIN_DELAY_MS
            return
        }
        val groupSize = Random.nextInt(2, minOf(5, drones.size) + 1)
        val group = drones.shuffled().take(groupSize)
        if (group.isEmpty()) {
            nextAttackRunAtMs = now + ATTACK_RUN_MIN_DELAY_MS
            return
        }
        val entryOffset = randomCornerOffset()
        val exitOffset = Vector3(-entryOffset.x, entryOffset.y, -entryOffset.z)
        val entryPoint = Vector3.add(playerPosition, entryOffset)
        val exitPoint = Vector3.add(playerPosition, exitOffset)
        val command = DroneCommand(
            id = (++attackRunCounter).toInt(),
            type = DroneCommandType.CHARGE_OVER_PLAYER,
            durationMs = 4600L,
            intensity = 4f,
            entryPoint = entryPoint,
            exitPoint = exitPoint
        )
        group.forEach { drone ->
            drone.applyBossCommand(command)
        }
        val nextDelay = Random.nextLong(ATTACK_RUN_MIN_DELAY_MS, ATTACK_RUN_MAX_DELAY_MS)
        nextAttackRunAtMs = now + nextDelay
    }

    private fun randomCornerOffset(): Vector3 {
        val horizontal = Vector3(
            if (Random.nextBoolean()) 1f else -1f,
            0f,
            if (Random.nextBoolean()) 1f else -1f
        ).normalized()
        val distance = FLOCK_RADIUS + 1.4f
        val vertical = (Random.nextFloat() - 0.5f) * 0.8f
        return Vector3(
            horizontal.x * distance,
            vertical,
            horizontal.z * distance
        )
    }

    private fun boundarySteer(position: Vector3): Vector3 {
        val horizontal = Vector3(position.x - playerPosition.x, 0f, position.z - playerPosition.z)
        val horizontalDistance = horizontal.length()
        val boundaryForce = if (horizontalDistance > FLOCK_RADIUS) {
            val normal = if (horizontalDistance > 0.0001f) horizontal.normalized() else Vector3(0f, 0f, 1f)
            normal.scaled(-BOUNDARY_WEIGHT * (horizontalDistance - FLOCK_RADIUS))
        } else {
            Vector3.zero()
        }

        val minY = playerPosition.y - FLOOR_OFFSET
        val maxY = playerPosition.y + CEILING_OFFSET
        val verticalForce = when {
            position.y < minY -> Vector3(0f, (minY - position.y) * BOUNDARY_WEIGHT, 0f)
            position.y > maxY -> Vector3(0f, (maxY - position.y) * -BOUNDARY_WEIGHT, 0f)
            else -> Vector3.zero()
        }

        return Vector3.add(boundaryForce, verticalForce)
    }

    private fun visibilitySteer(position: Vector3): Vector3 {
        val offset = Vector3.subtract(position, playerPosition)
        val horizontal = Vector3(offset.x, 0f, offset.z)
        val distance = horizontal.length()
        if (distance == 0f) return Vector3.zero()
        val dir = horizontal.normalized()
        val minRadius = 0.9f
        val maxRadius = (FLOCK_RADIUS - 0.2f).coerceAtLeast(minRadius + 0.1f)
        val horizontalForce = when {
            distance < minRadius -> dir.scaled((minRadius - distance) * LEASH_WEIGHT)
            distance > maxRadius -> dir.scaled(-(distance - maxRadius) * LEASH_WEIGHT)
            else -> Vector3.zero()
        }
        val verticalOffset = position.y - playerPosition.y
        val verticalForce = when {
            verticalOffset > CEILING_OFFSET -> Vector3(0f, -(verticalOffset - CEILING_OFFSET) * LEASH_WEIGHT * 0.5f, 0f)
            verticalOffset < -FLOOR_OFFSET -> Vector3(0f, (-verticalOffset - FLOOR_OFFSET) * LEASH_WEIGHT * 0.5f, 0f)
            else -> Vector3.zero()
        }
        return Vector3.add(horizontalForce, verticalForce)
    }

    private fun assignFriendlyLink(newId: String) {
        if (Random.nextFloat() > FRIEND_LINK_PROBABILITY) return
        val candidates = enemies.keys.filter { it != newId }
        if (candidates.isEmpty()) return
        friendLinks[newId] = candidates.random()
    }

    private fun clearFriendlyLinksFor(id: String) {
        val iterator = friendLinks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == id || entry.value == id) {
                iterator.remove()
            }
        }
    }

    private fun limitVector(vector: Vector3, maxMagnitude: Float): Vector3 {
        val length = vector.length()
        if (length <= maxMagnitude || length == 0f) return vector
        return vector.normalized().scaled(maxMagnitude)
    }

}