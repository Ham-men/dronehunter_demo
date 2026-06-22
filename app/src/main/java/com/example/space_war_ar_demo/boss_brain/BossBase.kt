package com.example.space_war_ar_demo.boss_brain

import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

/**
 * Базовый класс для всех боссов в игре.
 * Предоставляет общую функциональность: позиция, здоровье, поворот, модель.
 */
abstract class BossBase(
    val id: String,
    var position: Vector3,
    var health: Int,
    val maxHealth: Int
) {
    var modelNode: Node? = null
    var velocity: Vector3 = Vector3.zero()
    var currentRotation: Quaternion = Quaternion.identity()
    var targetRotation: Quaternion = Quaternion.identity()
    var isRotating: Boolean = false
    
    var isAlive: Boolean = true
        private set
    
    /**
     * Обновление логики босса каждый кадр
     */
    abstract fun update(
        deltaTime: Float,
        playerPosition: Vector3,
        playerVelocity: Vector3
    )
    
    /**
     * Обработка получения урона
     */
    open fun takeDamage(damage: Int) {
        if (!isAlive) return
        health = (health - damage).coerceAtLeast(0)
        if (health <= 0) {
            isAlive = false
            onDestroyed()
        }
    }
    
    /**
     * Вызывается при уничтожении босса
     */
    abstract fun onDestroyed()
    
    /**
     * Получить процент здоровья (0.0 - 1.0)
     */
    fun getHealthPercent(): Float {
        return if (maxHealth > 0) (health.toFloat() / maxHealth.toFloat()).coerceIn(0f, 1f) else 0f
    }
    
    /**
     * Проверка, жив ли босс
     */
    fun isDead(): Boolean = !isAlive || health <= 0
}












