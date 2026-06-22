package com.example.space_war_ar_demo.boss_brain.attack

import com.example.space_war_ar_demo.boss_brain.BossBase
import com.google.ar.sceneform.math.Vector3

/**
 * Интерфейс для атак босса.
 * Определяет различные типы атак, которые может использовать босс.
 */
interface BossAttack {
    /**
     * Выполнить атаку
     * @param boss - экземпляр босса
     * @param targetPosition - позиция цели (обычно игрок)
     * @param damage - урон атаки
     */
    fun execute(boss: BossBase, targetPosition: Vector3, damage: Int)
    
    /**
     * Проверка, можно ли выполнить атаку (кулдаун прошел)
     */
    fun canExecute(boss: BossBase): Boolean
    
    /**
     * Кулдаун атаки в миллисекундах
     */
    val cooldown: Long
    
    /**
     * Время последнего выполнения атаки
     */
    var lastExecutionTime: Long
    
    /**
     * Название типа атаки (для логирования и отладки)
     */
    val attackTypeName: String
}












