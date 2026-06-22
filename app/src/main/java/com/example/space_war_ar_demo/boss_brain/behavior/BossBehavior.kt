package com.example.space_war_ar_demo.boss_brain.behavior

import com.example.space_war_ar_demo.boss_brain.BossBase
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

/**
 * Интерфейс для поведения босса.
 * Определяет, как босс движется и реагирует на игрока.
 */
interface BossBehavior {
    /**
     * Обновление поведения босса
     * @param boss - экземпляр босса
     * @param playerPosition - позиция игрока
     * @param deltaTime - время с последнего кадра
     * @return результат поведения (скорость, поворот, нужно ли атаковать)
     */
    fun update(boss: BossBase, playerPosition: Vector3, deltaTime: Float): BehaviorResult
    
    /**
     * Вызывается при появлении босса
     */
    fun onSpawn(boss: BossBase) {}
    
    /**
     * Вызывается при уничтожении босса
     */
    fun onDestroyed(boss: BossBase) {}
}

/**
 * Результат работы поведения босса
 */
data class BehaviorResult(
    val velocity: Vector3,                    // Скорость движения
    val targetRotation: Quaternion,            // Целевой поворот
    val shouldAttack: Boolean = false,        // Нужно ли атаковать
    val attackType: String? = null            // Тип атаки (если shouldAttack = true)
)












