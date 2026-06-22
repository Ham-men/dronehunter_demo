// AggressiveDroneBehavior.kt - агрессивное поведение (исправленная версия)
package com.example.space_war_ar_demo.enemy.behavior.types

import com.example.space_war_ar_demo.brain.DroneCommand
import com.example.space_war_ar_demo.brain.DroneCommandType
import com.example.space_war_ar_demo.enemy.behavior.DroneBehavior
import com.example.space_war_ar_demo.enemy.types.DroneEnemy
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AggressiveDroneBehavior : DroneBehavior() {
    private var currentState: DroneState = DroneState.PATROL
    private var lastStateChange: Long = 0L
    private val stateChangeCooldown = 1500L
    private var retreatingUntil: Long = 0L
    private var isRetreating: Boolean = false

    private var attackSnapActiveUntil: Long = 0L
    private var hitEvadeUntil: Long = 0L
    private var hitEvadeVerticalSign: Float = 1f
    private var lastAimDirection: Vector3 = Vector3(0f, 0f, 1f)
    private var lastPlayerSpeed: Float = 0f
    private var lastKnownPlayerPos: Vector3 = Vector3.zero()

    private var playMode: PlayMode = PlayMode.NONE
    private var playModeUntil: Long = 0L
    private var spinDirection: Float = 1f
    private var spinPhase: Float = 0f
    private var directionalBias: Vector3 = Vector3(0f, 0f, 0f)
    private var inspectTarget: Vector3? = null

    private var shotRecoilUntil: Long = 0L
    private var shotShakeUntil: Long = 0L
    private var shotRecoilDir: Vector3 = Vector3(0f, 0f, 1f)

    private var commandMode: CommandMode = CommandMode.NONE
    private var commandModeUntil: Long = 0L
    private var commandIntensity: Float = 1f
    private var shieldAvoidUntil: Long = 0L
    private var shockwavePanicUntil: Long = 0L
    private var lastDistanceToPlayer: Float = 0f
    private var attackIntentUntil: Long = 0L

    private var friendlyFollowUntil: Long = 0L
    private var swayPhase: Float = Random.nextFloat() * (PI.toFloat() * 2f)
    private var swaySpeed: Float = Random.nextFloat() * 1.5f + 0.8f
    private var chargeStage: ChargeStage = ChargeStage.NONE
    private var chargeEntryPoint: Vector3 = Vector3.zero()
    private var chargeExitPoint: Vector3 = Vector3.zero()
    private var chargeAttackFired: Boolean = false
    
    // Астероиды для укрытий
    private var asteroids: List<com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo> = emptyList()
    private var currentCoverAsteroid: com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo? = null
    private var seekingCoverUntil: Long = 0L
    private val COVER_SEARCH_RADIUS = 3.5f
    private val COVER_USAGE_DURATION_MS = 3000L
    
    // Союзные цели (транспорт и станции)
    private var alliedTargets: List<com.example.space_war_ar_demo.brain.BrainBot_v4.AlliedTargetInfo> = emptyList()
    private var currentAlliedTarget: com.example.space_war_ar_demo.brain.BrainBot_v4.AlliedTargetInfo? = null
    private val ALLIED_TARGET_PRIORITY_MULTIPLIER = 2.5f // Приоритет выше чем у игрока

    fun updateAsteroids(newAsteroids: List<com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo>) {
        asteroids = newAsteroids
    }
    
    fun updateAlliedTargets(newAlliedTargets: List<com.example.space_war_ar_demo.brain.BrainBot_v4.AlliedTargetInfo>) {
        alliedTargets = newAlliedTargets
    }
    
    override fun update(
        drone: DroneEnemy,
        playerPosition: Vector3,
        playerVelocity: Vector3,
        deltaTime: Float
    ) {
        lastKnownPlayerPos = playerPosition
        
        // Определяем приоритетную цель (союзные цели имеют приоритет)
        val targetInfo = selectPriorityTarget(drone.position, playerPosition)
        val targetPosition = targetInfo.position
        val isAlliedTarget = targetInfo.isAllied
        
        val predictedTarget = if (isAlliedTarget) {
            targetPosition // Союзные цели не двигаются быстро, не нужно предсказывать
        } else {
            predictLeadPosition(drone.position, playerPosition, playerVelocity)
        }
        
        val directionToTarget = Vector3.subtract(predictedTarget, drone.position)
        val distanceToPlayer = directionToTarget.length()
        lastDistanceToPlayer = distanceToPlayer
        if (playerContext.isShieldActive) {
            shieldAvoidUntil = now() + 450L
        }
        if (playerContext.isRepulsing) {
            shockwavePanicUntil = now() + 320L
        }
        if (distanceToPlayer > 0.0001f) {
            lastAimDirection = directionToTarget.normalized()
        }
        lastPlayerSpeed = playerVelocity.length()
        maybeTriggerAttackIntent(distanceToPlayer)

        // Проверяем, нужно ли искать укрытие за астероидом
        val coverBehavior = handleCoverBehavior(drone, playerPosition, deltaTime)
        
        val playfulFocus = handlePlayfulness(drone, predictedTarget, playerPosition, deltaTime)
        val charging = handleChargeRun(drone, playerPosition, deltaTime)
        if (charging) {
            applyHitEvasion(drone, deltaTime)
            applyShotRecoil(drone, deltaTime)
            applyAltitudeHold(drone, deltaTime)
            applyDamping(drone)
            limitSpeed(drone)
            applyMovement(drone, playerPosition, deltaTime)
            syncUiState(drone)
            return
        }
        
        // Если дрон ищет укрытие, применяем специальное поведение
        if (coverBehavior.isSeekingCover) {
            performCoverSeeking(drone, coverBehavior.coverPosition, deltaTime)
            applyHitEvasion(drone, deltaTime)
            applyShotRecoil(drone, deltaTime)
            applyAltitudeHold(drone, deltaTime)
            applyDamping(drone)
            limitSpeed(drone)
            applyMovement(drone, playerPosition, deltaTime)
            syncUiState(drone)
            return
        }

        updateRetreatState()
        updateCommandMode()
        updateState(drone, distanceToPlayer, playfulFocus, isAlliedTarget)

        if (isShieldHazard(distanceToPlayer)) {
            startRetreat()
            currentState = DroneState.EVASION
        }

        if (isAttackSnapActive()) {
            quickAlignToTarget(drone, playfulFocus)
        }

        val handledRetreat = isRetreating && currentState != DroneState.EVASION
        if (handledRetreat) {
            performRetreat(drone, playfulFocus, deltaTime)
        } else {
            when (currentState) {
                DroneState.PATROL -> performPatrol(drone, playfulFocus, playerVelocity, deltaTime)
                DroneState.PURSUIT -> performPursuit(drone, playfulFocus, playerVelocity, deltaTime)
                DroneState.ATTACK -> performAttack(drone, playfulFocus, playerVelocity, playerPosition, deltaTime)
                DroneState.EVASION -> performEvasion(drone, playfulFocus, deltaTime)
            }
        }

        applyHitEvasion(drone, deltaTime)
        applyShotRecoil(drone, deltaTime)
        applyAltitudeHold(drone, deltaTime)
        applyDamping(drone)
        limitSpeed(drone)
        applyFriendlyDrift(drone, deltaTime)
        applyMovement(drone, playerPosition, deltaTime)
        syncUiState(drone)
    }

    override fun onDamageTaken(drone: DroneEnemy) {
        triggerHitAggression(drone)
    }

    override fun onAttackExecuted(drone: DroneEnemy) {
        shotRecoilUntil = now() + 220L
        shotShakeUntil = now() + 180L
        shotRecoilDir = lastAimDirection
        attackSnapActiveUntil = now() + 220L
        quickAlignToTarget(drone, Vector3.add(drone.position, lastAimDirection))
    }

    override fun onBossCommand(command: DroneCommand) {
        commandModeUntil = now() + command.durationMs
        commandIntensity = command.intensity
        commandMode = when (command.type) {
            DroneCommandType.EVADE_TO_RADIUS -> {
                startRetreat()
                CommandMode.EVACUATE
            }
            DroneCommandType.FOCUS_FIRE -> {
                currentState = DroneState.ATTACK
                attackSnapActiveUntil = now() + 500L
                CommandMode.FOCUS
            }
            DroneCommandType.HOLD_FORMATION -> {
                currentState = DroneState.PATROL
                CommandMode.HOLD
            }
            DroneCommandType.CHARGE_OVER_PLAYER -> {
                prepareChargeRun(command)
                CommandMode.CHARGE
            }
            else -> CommandMode.NONE
        }
    }

    private fun updateState(drone: DroneEnemy, distanceToPlayer: Float, focusPoint: Vector3, isAlliedTarget: Boolean = false) {
        if (commandMode == CommandMode.CHARGE) return
        val currentTime = now()
        if (currentTime < attackIntentUntil && commandMode != CommandMode.EVACUATE) {
            if (currentState != DroneState.ATTACK) {
                currentState = DroneState.ATTACK
                lastStateChange = currentTime
                onStateChanged(drone, DroneState.ATTACK, focusPoint)
            }
            return
        }
        if (currentTime - lastStateChange < stateChangeCooldown) return

        // Для союзных целей используем увеличенную дистанцию обнаружения и атаки
        val attackRangeMultiplier = if (isAlliedTarget) ALLIED_TARGET_PRIORITY_MULTIPLIER else 1f
        val dynamicAttack = effectiveAttackRange() * attackRangeMultiplier
        val dynamicDetect = effectiveDetectionRange() * attackRangeMultiplier
        val newState = when {
            drone.health < drone.maxHealth * 0.3f -> DroneState.EVASION
            distanceToPlayer <= dynamicAttack -> DroneState.ATTACK
            distanceToPlayer <= dynamicDetect -> DroneState.PURSUIT
            else -> DroneState.PATROL
        }

        if (newState != currentState) {
            currentState = newState
            lastStateChange = now()
            onStateChanged(drone, newState, focusPoint)
        }
    }

    private fun performPatrol(
        drone: DroneEnemy,
        focusPoint: Vector3,
        playerVelocity: Vector3,
        deltaTime: Float
    ) {
        val commandScale = if (commandMode == CommandMode.HOLD) 0.2f else 1f
        val randomMovement = adaptiveJitter(0.45f * commandScale, deltaTime)
        val gentlePull = Vector3.subtract(focusPoint, drone.position)
            .normalized()
            .scaled(maxSpeed * 0.35f * deltaTime)
        val drift = playerVelocity.scaled(0.2f * deltaTime)
        val bias = directionalBias.scaled(0.4f * maxSpeed * deltaTime * commandScale)
        drone.velocity = Vector3.add(
            drone.velocity,
            Vector3.add(Vector3.add(Vector3.add(randomMovement, gentlePull), drift), bias)
        )
        setTargetRotation(drone, Vector3.add(drone.position, gentlePull))
    }

    private fun performPursuit(
        drone: DroneEnemy,
        focusPoint: Vector3,
        playerVelocity: Vector3,
        deltaTime: Float
    ) {
        val direction = Vector3.subtract(focusPoint, drone.position).normalized()
        val orbit = Vector3.cross(direction, Vector3(0f, 1f, 0f)).normalized()
            .scaled(maxSpeed * 0.75f * deltaTime)
        val velocityAddition = direction.scaled(maxSpeed * 1.15f * deltaTime)
        val leadCompensation = playerVelocity.scaled(0.5f * deltaTime)
        val jitter = adaptiveJitter(0.3f, deltaTime)
        drone.velocity = Vector3.add(
            drone.velocity,
            Vector3.add(
                Vector3.add(velocityAddition, orbit),
                Vector3.add(leadCompensation, Vector3.add(jitter, directionalBias.scaled(0.2f * maxSpeed * deltaTime)))
            )
        )
        setTargetRotation(drone, focusPoint)
    }

    private fun performAttack(
        drone: DroneEnemy,
        focusPoint: Vector3,
        playerVelocity: Vector3,
        playerPosition: Vector3,
        deltaTime: Float
    ) {
        val direction = Vector3.subtract(focusPoint, drone.position).normalized()
        val focusBoost = if (commandMode == CommandMode.FOCUS) 1f + 0.2f * commandIntensity else 1f
        val forwardScale = if (isDefensePressureActive()) 0.55f else 1.25f
        val orbitScale = if (isDefensePressureActive()) 1.25f else 0.95f
        val forward = direction.scaled(maxSpeed * forwardScale * deltaTime * focusBoost)
        val orbit = Vector3.cross(direction, Vector3.up()).normalized().scaled(maxSpeed * orbitScale * deltaTime)
        val anticipatory = playerVelocity.scaled(0.65f * deltaTime * focusBoost)
        val jitter = adaptiveJitter(0.3f, deltaTime)
        drone.velocity = Vector3.add(
            drone.velocity,
            Vector3.add(Vector3.add(forward, orbit), Vector3.add(anticipatory, jitter))
        )
        setTargetRotation(drone, focusPoint)

        // Атакуем приоритетную цель (союзные цели или игрока)
        val attackTarget = if (currentAlliedTarget != null) {
            currentAlliedTarget!!.position
        } else {
            playerPosition
        }
        
        // Для союзных целей используем увеличенную дистанцию атаки
        val attackRange = if (currentAlliedTarget != null) {
            effectiveAttackRange() * ALLIED_TARGET_PRIORITY_MULTIPLIER
        } else {
            effectiveAttackRange()
        }
        
        val distanceToTarget = Vector3.subtract(drone.position, attackTarget).length()
        // Проверяем готовность к атаке (cooldown и дистанция)
        val canAttack = if (currentAlliedTarget != null) {
            // Для союзных целей используем увеличенную дистанцию
            distanceToTarget <= attackRange && drone.shouldAttack(attackTarget)
        } else {
            // Для игрока используем стандартную проверку
            drone.shouldAttack(attackTarget)
        }
        
        if (!isDefensePressureActive() && canAttack) {
            drone.attack()
            if (shouldStartRetreat()) {
                startRetreat()
            }
        }
    }

    private fun performEvasion(drone: DroneEnemy, focusPoint: Vector3, deltaTime: Float) {
        val direction = Vector3.subtract(drone.position, focusPoint).normalized()
        val lateral = Vector3.cross(direction, Vector3(0f, 1f, 0f)).normalized()
        val velocityAddition = direction.scaled(maxSpeed * 1.15f * deltaTime)
        val lateralDodge = lateral.scaled(maxSpeed * 0.9f * deltaTime * if (Random.nextBoolean()) 1f else -1f)
        val jitter = adaptiveJitter(0.4f, deltaTime)
        drone.velocity = Vector3.add(
            drone.velocity,
            Vector3.add(Vector3.add(velocityAddition, lateralDodge), jitter)
        )
        setTargetRotation(drone, Vector3.add(drone.position, direction))
    }

    private fun performRetreat(drone: DroneEnemy, focusPoint: Vector3, deltaTime: Float) {
        val away = Vector3.subtract(drone.position, focusPoint).normalized()
        val strafe = Vector3.cross(away, Vector3(0f, 1f, 0f)).normalized()
        val retreatImpulse = away.scaled(maxSpeed * 1.3f * deltaTime)
        val strafeImpulse = strafe.scaled(maxSpeed * 0.9f * deltaTime * if (Random.nextBoolean()) 1f else -1f)
        val defenseLift = if (isDefensePressureActive()) playerContext.defenseIntensity.coerceAtLeast(0.5f) else 0f
        val verticalShift = Vector3(
            0f,
            (if (Random.nextBoolean()) 1f else -1f) + defenseLift,
            0f
        ).scaled(maxSpeed * 0.6f * deltaTime)
        val jitter = adaptiveJitter(0.4f, deltaTime)
        drone.velocity = Vector3.add(
            drone.velocity,
            Vector3.add(
                Vector3.add(retreatImpulse, strafeImpulse),
                Vector3.add(verticalShift, jitter)
            )
        )
        setTargetRotation(drone, focusPoint)
    }

    private fun setTargetRotation(drone: DroneEnemy, target: Vector3) {
        drone.targetRotation = calculateRotationToTarget(drone.position, target)
        drone.isRotating = true
    }

    private fun quickAlignToTarget(drone: DroneEnemy, target: Vector3) {
        val rotation = calculateRotationToTarget(drone.position, target)
        drone.forceRotation(rotation)
    }

    private fun applyDamping(drone: DroneEnemy) {
        drone.velocity = drone.velocity.scaled(0.88f)
    }

    private fun limitSpeed(drone: DroneEnemy) {
        if (drone.velocity.length() > maxSpeed) {
            drone.velocity = drone.velocity.normalized().scaled(maxSpeed)
        }
    }

    private fun applyMovement(drone: DroneEnemy, playerPosition: Vector3, deltaTime: Float) {
        drone.position = Vector3.add(drone.position, drone.velocity)
        if (commandMode != CommandMode.CHARGE) {
            applyLeash(drone, playerPosition, deltaTime, currentState == DroneState.ATTACK)
            applyShieldBuffer(drone, playerPosition, deltaTime)
        }
    }

    private fun applyShieldBuffer(drone: DroneEnemy, playerPosition: Vector3, deltaTime: Float) {
        if (!playerContext.isShieldActive) return
        val offset = Vector3.subtract(drone.position, playerPosition)
        val horizontal = Vector3(offset.x, 0f, offset.z)
        val distance = horizontal.length()
        val minRadius = playerContext.shieldRadius.coerceAtLeast(0.8f) + 0.2f
        if (distance < minRadius && distance > 0.001f) {
            val push = horizontal.normalized().scaled((minRadius - distance) * maxSpeed * 1.4f * deltaTime)
            drone.velocity = Vector3.add(drone.velocity, push)
        }
    }

    private fun startRetreat() {
        if (commandMode == CommandMode.CHARGE) return
        isRetreating = true
        attackIntentUntil = 0L
        val duration = if (commandMode == CommandMode.EVACUATE) {
            (commandModeUntil - now()).coerceAtLeast(600L)
        } else {
            Random.nextLong(900L, 1700L)
        }
        retreatingUntil = now() + duration
    }

    private fun randomJitter(intensity: Float, deltaTime: Float): Vector3 {
        val scale = intensity * deltaTime
        return Vector3(
            (Random.nextFloat() - 0.5f) * scale,
            (Random.nextFloat() - 0.5f) * scale * 0.4f,
            (Random.nextFloat() - 0.5f) * scale
        )
    }

    private fun updateRetreatState() {
        if (isRetreating && now() > retreatingUntil) {
            isRetreating = false
        }
    }

    private fun updateCommandMode() {
        if (commandMode == CommandMode.NONE) return
        if (now() > commandModeUntil) {
            if (commandMode == CommandMode.CHARGE) {
                endChargeMode()
            } else {
                commandMode = CommandMode.NONE
                commandIntensity = 1f
            }
            return
        }
        if (commandMode == CommandMode.EVACUATE) {
            isRetreating = true
            retreatingUntil = commandModeUntil
        }
        if (commandMode == CommandMode.HOLD) {
            directionalBias = Vector3.zero()
        }
    }

    private fun syncUiState(drone: DroneEnemy) {
        drone.uiState = when {
            commandMode == CommandMode.FOCUS || commandMode == CommandMode.CHARGE -> DroneEnemy.UiState.CHARGING
            isRetreating || commandMode == CommandMode.EVACUATE -> DroneEnemy.UiState.RETREAT
            else -> DroneEnemy.UiState.NORMAL
        }
    }

    override fun onPlayerShockwave(strength: Float) {
        shockwavePanicUntil = now() + (250L + (strength * 450f)).toLong()
        startRetreat()
    }

    private fun adaptiveJitter(intensity: Float, deltaTime: Float): Vector3 {
        val boost = 1f + lastPlayerSpeed.coerceAtMost(2f) * 0.5f
        return randomJitter(intensity * boost, deltaTime)
    }

    private fun shouldStartRetreat(): Boolean {
        if (commandMode == CommandMode.FOCUS || commandMode == CommandMode.CHARGE) return false
        if (commandMode == CommandMode.EVACUATE) return true
        return Random.nextFloat() <= retreatChance
    }

    private fun triggerHitAggression(drone: DroneEnemy) {
        hitEvadeUntil = now() + 350L
        hitEvadeVerticalSign = if (Random.nextBoolean()) 1f else -1f
        val angleJuke = Vector3.cross(lastAimDirection, Vector3(0f, 1f, 0f)).normalized()
        val burst = angleJuke.scaled(maxSpeed * 1.4f)
        val heightKick = Vector3(0f, hitEvadeVerticalSign * maxSpeed * 0.9f, 0f)
        val forwardSurge = lastAimDirection.scaled(maxSpeed * 1.2f)
        drone.velocity = Vector3.add(drone.velocity, Vector3.add(burst, Vector3.add(heightKick, forwardSurge)))
        currentState = DroneState.ATTACK
        attackSnapActiveUntil = now() + 400L
        attackIntentUntil = now() + 1600L
        isRetreating = false
        if (drone.shouldAttack(lastKnownPlayerPos)) {
            drone.attack()
        }
    }

    private fun applyHitEvasion(drone: DroneEnemy, deltaTime: Float) {
        if (now() > hitEvadeUntil) return
        val jitter = randomJitter(0.7f, deltaTime)
        val vertical = Vector3(0f, hitEvadeVerticalSign * maxSpeed * 1.1f * deltaTime, 0f)
        drone.velocity = Vector3.add(drone.velocity, Vector3.add(jitter, vertical))
    }

    private fun onStateChanged(drone: DroneEnemy, state: DroneState, focusPoint: Vector3) {
        if (state == DroneState.ATTACK) {
            attackSnapActiveUntil = now() + 350L
            quickAlignToTarget(drone, focusPoint)
        }
    }

    private fun handlePlayfulness(
        drone: DroneEnemy,
        defaultTarget: Vector3,
        playerPosition: Vector3,
        deltaTime: Float
    ): Vector3 {
        val now = now()
        if (playMode == PlayMode.NONE && commandMode != CommandMode.HOLD && Random.nextFloat() < 0.02f) {
            startPlayfulness(drone)
        } else if (playMode != PlayMode.NONE && now > playModeUntil) {
            playMode = PlayMode.NONE
            inspectTarget = null
        }

        return when (playMode) {
            PlayMode.NONE -> defaultTarget
            PlayMode.SPIN -> {
                spinPhase += deltaTime * 360f * spinDirection
                val rotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), spinPhase)
                drone.forceRotation(rotation)
                drone.velocity = drone.velocity.scaled(0.7f)
                Vector3.add(drone.position, lastAimDirection)
            }
            PlayMode.DECIDE -> {
                directionalBias = directionalBias.scaled(0.98f).let {
                    Vector3.add(it, randomHorizontalDirection().scaled(0.05f))
                }
                defaultTarget
            }
            PlayMode.INSPECT -> {
                inspectTarget = drone.getNeighborPosition() ?: inspectTarget
                inspectTarget ?: defaultTarget
            }
        }
    }

    private fun startPlayfulness(drone: DroneEnemy) {
        playMode = when (Random.nextFloat()) {
            in 0f..0.3f -> PlayMode.SPIN
            in 0.3f..0.65f -> PlayMode.DECIDE
            else -> PlayMode.INSPECT
        }
        playModeUntil = now() + when (playMode) {
            PlayMode.SPIN -> 700L
            PlayMode.DECIDE -> 1000L
            PlayMode.INSPECT -> 1100L
            else -> 0L
        }
        when (playMode) {
            PlayMode.SPIN -> {
                spinDirection = if (Random.nextBoolean()) 1f else -1f
                spinPhase = 0f
            }
            PlayMode.DECIDE -> {
                directionalBias = randomHorizontalDirection()
            }
            PlayMode.INSPECT -> {
                inspectTarget = drone.getNeighborPosition()
                if (inspectTarget == null) {
                    playMode = PlayMode.NONE
                }
            }
            else -> {}
        }
    }

    private fun applyShotRecoil(drone: DroneEnemy, deltaTime: Float) {
        val now = now()
        if (now < shotRecoilUntil) {
            val recoil = shotRecoilDir.scaled(-maxSpeed * 0.6f * deltaTime)
            val shake = randomJitter(0.2f, deltaTime)
            drone.velocity = Vector3.add(drone.velocity, Vector3.add(recoil, shake))
        }
        if (now < shotShakeUntil) {
            val yaw = (Random.nextFloat() - 0.5f) * 6f
            val rotation = Quaternion.multiply(
                drone.currentRotation,
                Quaternion.axisAngle(Vector3(0f, 1f, 0f), yaw)
            )
            drone.forceRotation(rotation)
        }
    }

    private fun applyAltitudeHold(drone: DroneEnemy, deltaTime: Float) {
        if (!hasAltitudeTarget()) return
        val target = getTargetAltitude(drone.position.y)
        val error = target - drone.position.y
        val lift = error.coerceIn(-1.5f, 1.5f) * (maxSpeed * 2.5f)
        if (lift != 0f) {
            drone.velocity = Vector3.add(drone.velocity, Vector3(0f, lift * deltaTime, 0f))
        }
    }

    private fun isShieldHazard(distance: Float): Boolean {
        val radius = playerContext.shieldRadius.coerceAtLeast(0.8f)
        return playerContext.isShieldActive && distance < radius + 0.25f
    }

    private fun isDefensePressureActive(): Boolean =
        now() < shieldAvoidUntil || now() < shockwavePanicUntil

    private fun randomHorizontalDirection(): Vector3 {
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val dir = Vector3(Math.cos(angle.toDouble()).toFloat(), 0f, Math.sin(angle.toDouble()).toFloat())
        return dir.normalized()
    }

    private fun isAttackSnapActive(): Boolean = now() < attackSnapActiveUntil

    private fun now() = System.currentTimeMillis()

    private enum class PlayMode { NONE, SPIN, DECIDE, INSPECT }
    private enum class CommandMode { NONE, EVACUATE, FOCUS, HOLD, CHARGE }
    private enum class ChargeStage { NONE, ASSEMBLE, RUN, EXIT }

    enum class DroneState { PATROL, PURSUIT, ATTACK, EVASION }

    private fun maybeTriggerAttackIntent(distanceToPlayer: Float) {
        val currentTime = now()
        if (currentTime < attackIntentUntil) return
        if (isRetreating || commandMode == CommandMode.EVACUATE || commandMode == CommandMode.CHARGE) return
        val triggerRange = effectiveDetectionRange() * 1.1f
        if (distanceToPlayer <= triggerRange && Random.nextFloat() < 0.035f) {
            attackIntentUntil = currentTime + Random.nextLong(800L, 1600L)
        }
    }

    private fun applyFriendlyDrift(drone: DroneEnemy, deltaTime: Float) {
        if (commandMode == CommandMode.CHARGE) return
        val currentTime = now()
        if (currentTime >= friendlyFollowUntil && Random.nextFloat() < 0.015f) {
            friendlyFollowUntil = currentTime + Random.nextLong(1200L, 2400L)
        }
        val swayCycle = (PI.toFloat() * 2f)
        swayPhase = (swayPhase + deltaTime * swaySpeed) % swayCycle
        val lateralRaw = Vector3.cross(lastAimDirection, Vector3.up())
        val lateralDir = if (lateralRaw.length() > 0.0001f) lateralRaw.normalized() else Vector3(1f, 0f, 0f)
        val swayAmount = sin(swayPhase) * maxSpeed * 0.35f * deltaTime
        drone.velocity = Vector3.add(drone.velocity, lateralDir.scaled(swayAmount))

        val followTarget = if (currentTime < friendlyFollowUntil) drone.getNeighborPosition() else null
        if (followTarget == null && currentTime < friendlyFollowUntil) {
            friendlyFollowUntil = currentTime
        }
        followTarget?.let { anchor ->
            val toAnchor = Vector3.subtract(anchor, drone.position)
            val distance = toAnchor.length()
            if (distance > 0.05f) {
                val dir = toAnchor.normalized()
                val follow = dir.scaled(maxSpeed * 0.6f * deltaTime)
                val spacing = (distance - 0.45f).coerceAtLeast(0f)
                val spacingAdjust = dir.scaled(spacing * 0.25f * deltaTime)
                drone.velocity = Vector3.add(drone.velocity, Vector3.add(follow, spacingAdjust))
            }
        }
    }

    private fun applyLeash(
        drone: DroneEnemy,
        playerPosition: Vector3,
        deltaTime: Float,
        isAggressive: Boolean
    ) {
        val offset = Vector3.subtract(drone.position, playerPosition)
        val horizontal = Vector3(offset.x, 0f, offset.z)
        val distance = horizontal.length().coerceAtLeast(0.0001f)
        val outward = horizontal.normalized()
        val minRadius = if (isAggressive) orbitRadius * 0.12f else orbitRadius * 0.35f
        val maxRadius = if (isAggressive) orbitRadius * 0.9f else orbitRadius * 0.95f
        var correction = Vector3.zero()
        if (distance > maxRadius) {
            val strength = ((distance - maxRadius) / maxRadius).coerceIn(0.1f, 1.2f)
            correction = outward.scaled(-maxSpeed * 1.4f * strength * deltaTime)
        } else if (distance < minRadius) {
            val strength = ((minRadius - distance) / minRadius).coerceIn(0.1f, 1f)
            correction = outward.scaled(maxSpeed * 0.9f * strength * deltaTime)
        }
        val verticalOffset = drone.position.y - playerPosition.y
        val verticalRange = if (isAggressive) 0.6f else 1.1f
        val verticalCorrection = when {
            verticalOffset > verticalRange -> Vector3(
                0f,
                -maxSpeed * ((verticalOffset - verticalRange) / verticalRange).coerceIn(0.1f, 1.2f) * deltaTime,
                0f
            )
            verticalOffset < -verticalRange -> Vector3(
                0f,
                maxSpeed * ((-verticalOffset - verticalRange) / verticalRange).coerceIn(0.1f, 1.2f) * deltaTime,
                0f
            )
            else -> Vector3.zero()
        }
        drone.velocity = Vector3.add(drone.velocity, Vector3.add(correction, verticalCorrection))
    }

    private fun dot(a: Vector3, b: Vector3): Float = a.x * b.x + a.y * b.y + a.z * b.z

    private fun prepareChargeRun(command: DroneCommand) {
        chargeStage = ChargeStage.ASSEMBLE
        val fallbackEntry = Vector3.add(lastKnownPlayerPos, Vector3(orbitRadius, 0f, orbitRadius))
        val fallbackExit = Vector3.add(lastKnownPlayerPos, Vector3(-orbitRadius, 0f, -orbitRadius))
        chargeEntryPoint = command.entryPoint ?: fallbackEntry
        chargeExitPoint = command.exitPoint ?: fallbackExit
        chargeAttackFired = false
        isRetreating = false
        attackIntentUntil = 0L
    }

    private fun endChargeMode() {
        commandMode = CommandMode.NONE
        commandIntensity = 1f
        chargeStage = ChargeStage.NONE
        chargeAttackFired = false
    }

    private fun handleChargeRun(
        drone: DroneEnemy,
        playerPosition: Vector3,
        deltaTime: Float
    ): Boolean {
        if (commandMode != CommandMode.CHARGE || chargeStage == ChargeStage.NONE) return false
        val currentTime = now()
        if (currentTime > commandModeUntil) {
            endChargeMode()
            return false
        }
        val entry = chargeEntryPoint
        val exit = chargeExitPoint
        when (chargeStage) {
            ChargeStage.ASSEMBLE -> {
                val toEntry = Vector3.subtract(entry, drone.position)
                val distance = toEntry.length()
                if (distance < 0.35f) {
                    chargeStage = ChargeStage.RUN
                } else if (distance > 0.001f) {
                    val push = toEntry.normalized().scaled(maxSpeed * 1.3f * deltaTime)
                    val cohesion = Vector3.subtract(playerPosition, drone.position)
                        .normalized()
                        .scaled(maxSpeed * 0.4f * deltaTime)
                    drone.velocity = Vector3.add(drone.velocity, Vector3.add(push, cohesion))
                    setTargetRotation(drone, entry)
                }
            }
            ChargeStage.RUN -> {
                val toPlayer = Vector3.subtract(playerPosition, drone.position)
                val distance = toPlayer.length()
                if (distance > 0.001f) {
                    val direction = toPlayer.normalized()
                    val thrust = direction.scaled(maxSpeed * 1.7f * deltaTime)
                    val orbit = Vector3.cross(direction, Vector3.up()).normalized()
                        .scaled(maxSpeed * 0.45f * deltaTime)
                    drone.velocity = Vector3.add(drone.velocity, Vector3.add(thrust, orbit))
                    setTargetRotation(drone, playerPosition)
                }
                if (!chargeAttackFired && drone.shouldAttack(playerPosition)) {
                    drone.attack()
                    chargeAttackFired = true
                }
                val entryToExit = Vector3.subtract(exit, entry)
                val entryToDrone = Vector3.subtract(drone.position, entry)
                val passedPlayer = dot(entryToDrone, entryToExit) > 0f &&
                        entryToDrone.length() >= entryToExit.length() * 0.45f
                if (distance < 0.25f || passedPlayer) {
                    chargeStage = ChargeStage.EXIT
                }
            }
            ChargeStage.EXIT -> {
                val toExit = Vector3.subtract(exit, drone.position)
                val distance = toExit.length()
                if (distance < 0.35f) {
                    endChargeMode()
                } else if (distance > 0.001f) {
                    val push = toExit.normalized().scaled(maxSpeed * 1.1f * deltaTime)
                    val loft = Vector3(0f, 0.2f * maxSpeed * deltaTime, 0f)
                    drone.velocity = Vector3.add(drone.velocity, Vector3.add(push, loft))
                    setTargetRotation(drone, exit)
                }
            }
            else -> {}
        }
        currentState = DroneState.ATTACK
        return true
    }
    
    // === МЕТОДЫ ДЛЯ РАБОТЫ С АСТЕРОИДАМИ И СОЮЗНЫМИ ЦЕЛЯМИ ===
    
    data class TargetInfo(
        val position: Vector3,
        val isAllied: Boolean
    )
    
    private fun selectPriorityTarget(dronePosition: Vector3, playerPosition: Vector3): TargetInfo {
        // Ищем ближайшую союзную цель
        var closestAlliedTarget: com.example.space_war_ar_demo.brain.BrainBot_v4.AlliedTargetInfo? = null
        var closestAlliedDistance = Float.MAX_VALUE
        
        for (target in alliedTargets) {
            val distance = Vector3.subtract(dronePosition, target.position).length()
            if (distance < closestAlliedDistance) {
                closestAlliedDistance = distance
                closestAlliedTarget = target
            }
        }
        
        // Если есть союзная цель в пределах досягаемости, выбираем её
        val maxAlliedRange = effectiveDetectionRange() * ALLIED_TARGET_PRIORITY_MULTIPLIER
        if (closestAlliedTarget != null && closestAlliedDistance <= maxAlliedRange) {
            currentAlliedTarget = closestAlliedTarget
            return TargetInfo(closestAlliedTarget.position, isAllied = true)
        }
        
        // Иначе атакуем игрока
        currentAlliedTarget = null
        return TargetInfo(playerPosition, isAllied = false)
    }
    
    data class CoverBehaviorResult(
        val isSeekingCover: Boolean,
        val coverPosition: Vector3
    )
    
    private fun handleCoverBehavior(
        drone: DroneEnemy,
        playerPosition: Vector3,
        deltaTime: Float
    ): CoverBehaviorResult {
        val currentTime = now()
        
        // Если дрон уже использует укрытие, проверяем, не истекло ли время
        if (currentCoverAsteroid != null) {
            if (currentTime > seekingCoverUntil) {
                // Время укрытия истекло, покидаем укрытие
                currentCoverAsteroid = null
            } else {
                // Продолжаем использовать укрытие
                val coverPos = calculateCoverPosition(currentCoverAsteroid!!, playerPosition)
                return CoverBehaviorResult(true, coverPos)
            }
        }
        
        // Проверяем, нужно ли искать укрытие (низкое здоровье или под огнем)
        val shouldSeekCover = drone.health < drone.maxHealth * 0.5f || 
                              (currentTime < attackIntentUntil && Random.nextFloat() < 0.4f)
        
        if (!shouldSeekCover) {
            return CoverBehaviorResult(false, Vector3.zero())
        }
        
        // Ищем ближайший подходящий астероид
        val nearbyAsteroid = findNearbyCoverAsteroid(drone.position, playerPosition)
        if (nearbyAsteroid != null) {
            currentCoverAsteroid = nearbyAsteroid
            seekingCoverUntil = currentTime + COVER_USAGE_DURATION_MS
            val coverPos = calculateCoverPosition(nearbyAsteroid, playerPosition)
            return CoverBehaviorResult(true, coverPos)
        }
        
        return CoverBehaviorResult(false, Vector3.zero())
    }
    
    private fun findNearbyCoverAsteroid(
        dronePosition: Vector3,
        playerPosition: Vector3
    ): com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo? {
        var bestAsteroid: com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo? = null
        var bestScore = Float.MIN_VALUE
        
        for (asteroid in asteroids) {
            val distanceToAsteroid = Vector3.subtract(dronePosition, asteroid.position).length()
            
            // Астероид должен быть в пределах поиска
            if (distanceToAsteroid > COVER_SEARCH_RADIUS) continue
            
            // Проверяем, что астероид находится между дроном и игроком (укрывает от игрока)
            val toAsteroid = Vector3.subtract(asteroid.position, dronePosition)
            val toPlayer = Vector3.subtract(playerPosition, dronePosition)
            val dotProduct = toAsteroid.x * toPlayer.x + toAsteroid.z * toPlayer.z
            
            // Астероид должен быть между дроном и игроком (dot product > 0)
            if (dotProduct <= 0) continue
            
            // Вычисляем оценку: ближе к дрону и дальше от игрока = лучше
            val distanceToPlayer = Vector3.subtract(asteroid.position, playerPosition).length()
            val score = distanceToPlayer / (distanceToAsteroid + 0.1f)
            
            if (score > bestScore) {
                bestScore = score
                bestAsteroid = asteroid
            }
        }
        
        return bestAsteroid
    }
    
    private fun calculateCoverPosition(
        asteroid: com.example.space_war_ar_demo.brain.BrainBot_v4.AsteroidInfo,
        playerPosition: Vector3
    ): Vector3 {
        // Позиция укрытия: за астероидом относительно игрока
        val toAsteroid = Vector3.subtract(asteroid.position, playerPosition)
        val distance = toAsteroid.length()
        
        if (distance < 0.001f) {
            return asteroid.position
        }
        
        val direction = toAsteroid.normalized()
        val coverOffset = direction.scaled(asteroid.radius + 0.3f) // Немного за астероидом
        return Vector3.add(asteroid.position, coverOffset)
    }
    
    private fun performCoverSeeking(
        drone: DroneEnemy,
        coverPosition: Vector3,
        deltaTime: Float
    ) {
        val toCover = Vector3.subtract(coverPosition, drone.position)
        val distance = toCover.length()
        
        if (distance < 0.2f) {
            // Достигли укрытия, остаемся на месте или слегка двигаемся
            val jitter = adaptiveJitter(0.15f, deltaTime)
            drone.velocity = Vector3.add(drone.velocity, jitter)
        } else {
            // Двигаемся к укрытию
            val direction = toCover.normalized()
            val moveSpeed = maxSpeed * 0.8f // Немного медленнее при движении к укрытию
            val movement = direction.scaled(moveSpeed * deltaTime)
            drone.velocity = Vector3.add(drone.velocity, movement)
            setTargetRotation(drone, coverPosition)
        }
    }
}