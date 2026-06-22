package com.example.space_war_ar_demo.boss_brain.config

import com.example.space_war_ar_demo.boss_brain.ability.BossAbility
import com.example.space_war_ar_demo.boss_brain.attack.BossAttack
import com.example.space_war_ar_demo.boss_brain.behavior.BossBehavior

/**
 * Конфигурация босса для уровня.
 * Определяет поведение, атаки, способности и параметры босса.
 */
data class BossConfig(
    val health: Int,
    val maxHealth: Int,
    val damage: Int,
    val behavior: BossBehavior,
    val attacks: List<BossAttack>,
    val abilities: List<BossAbility>,
    val bossName: String = "Boss"
)












