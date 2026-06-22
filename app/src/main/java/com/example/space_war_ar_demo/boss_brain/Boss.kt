package com.example.space_war_ar_demo.boss_brain

import com.example.space_war_ar_demo.boss_brain.ability.BossAbility
import com.example.space_war_ar_demo.boss_brain.attack.BossAttack
import com.example.space_war_ar_demo.boss_brain.behavior.BossBehavior
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

/**
 * Конкретная реализация босса.
 * Объединяет поведение, атаки и способности.
 */
class Boss(
    id: String,
    position: Vector3,
    health: Int,
    maxHealth: Int,
    private val behavior: BossBehavior,
    private val attacks: List<BossAttack> = emptyList(),
    private val abilities: List<BossAbility> = emptyList(),
    val damage: Int = 10 // Урон босса для атак
) : BossBase(id, position, health, maxHealth) {
    
    private var onDestroyedCallback: ((Boss) -> Unit)? = null
    
    init {
        behavior.onSpawn(this)
        abilities.forEach { it.activate(this) }
    }
    
    override fun update(
        deltaTime: Float,
        playerPosition: Vector3,
        playerVelocity: Vector3
    ) {
        if (!isAlive) return
        
        // Обновляем способности
        abilities.forEach { ability ->
            if (ability.isActive) {
                ability.update(this, deltaTime)
            }
        }
        
        // Обновляем поведение
        val behaviorResult = behavior.update(this, playerPosition, deltaTime)
        
        // Применяем движение
        velocity = behaviorResult.velocity
        position = Vector3.add(position, velocity)
        targetRotation = behaviorResult.targetRotation
        
        // Обновляем поворот
        updateRotation(deltaTime)
        
        // Применяем поворот к модели
        applyRotationToModel()
        
        // Выполняем атаку, если нужно
        if (behaviorResult.shouldAttack) {
            // Выбираем первую доступную атаку, если тип не указан
            val attack = if (behaviorResult.attackType != null) {
                attacks.find { it.attackTypeName == behaviorResult.attackType }
            } else {
                attacks.firstOrNull()
            }
            if (attack != null && attack.canExecute(this)) {
                attack.execute(this, playerPosition, damage)
                attack.lastExecutionTime = System.currentTimeMillis()
            }
        }
    }
    
    private fun updateRotation(deltaTime: Float) {
        if (isRotating) {
            val rotationSpeed = 2f // радиан в секунду
            currentRotation = Quaternion.slerp(
                currentRotation,
                targetRotation,
                rotationSpeed * deltaTime
            )
            if (areQuaternionsClose(currentRotation, targetRotation, 0.01f)) {
                isRotating = false
            }
        }
    }
    
    private fun applyRotationToModel() {
        val node = modelNode ?: return
        // Применяем поворот с учетом offset для горизонтальной ориентации (как у дронов)
        val modelRotationOffset = Quaternion.multiply(
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f),
            Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
        )
        val adjustedRotation = Quaternion.multiply(currentRotation, modelRotationOffset)
        node.localRotation = adjustedRotation
    }
    
    private fun areQuaternionsClose(q1: Quaternion, q2: Quaternion, tolerance: Float): Boolean {
        return (kotlin.math.abs(q1.x - q2.x) < tolerance &&
                kotlin.math.abs(q1.y - q2.y) < tolerance &&
                kotlin.math.abs(q1.z - q2.z) < tolerance &&
                kotlin.math.abs(q1.w - q2.w) < tolerance)
    }
    
    override fun onDestroyed() {
        behavior.onDestroyed(this)
        abilities.forEach { it.deactivate(this) }
        onDestroyedCallback?.invoke(this)
    }
    
    fun setOnDestroyedCallback(callback: (Boss) -> Unit) {
        onDestroyedCallback = callback
    }
    
    /**
     * Получить активную способность по имени
     */
    fun getAbility(name: String): BossAbility? {
        return abilities.find { it.abilityName == name }
    }
    
    /**
     * Получить атаку по имени
     */
    fun getAttack(name: String): BossAttack? {
        return attacks.find { it.attackTypeName == name }
    }
}



