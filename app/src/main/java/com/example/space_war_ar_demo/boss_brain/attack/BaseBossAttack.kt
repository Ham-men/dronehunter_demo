package com.example.space_war_ar_demo.boss_brain.attack

import com.example.space_war_ar_demo.boss_brain.BossBase

/**
 * Базовый класс для атак босса.
 * Предоставляет общую функциональность для проверки кулдауна.
 */
abstract class BaseBossAttack(
    override val cooldown: Long,
    override val attackTypeName: String
) : BossAttack {
    
    override var lastExecutionTime: Long = 0L
    
    override fun canExecute(boss: BossBase): Boolean {
        if (!boss.isAlive) return false
        val now = System.currentTimeMillis()
        return (now - lastExecutionTime) >= cooldown
    }
}












