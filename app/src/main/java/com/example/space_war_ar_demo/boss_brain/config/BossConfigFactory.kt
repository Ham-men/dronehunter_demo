package com.example.space_war_ar_demo.boss_brain.config

import android.content.Context
import com.example.space_war_ar_demo.boss_brain.attack.ProjectileAttack
import com.example.space_war_ar_demo.boss_brain.behavior.ChaseBehavior
import com.example.space_war_ar_demo.levels.LevelBalance
import com.example.space_war_ar_demo.physics.PhysicsEngine
import com.google.ar.sceneform.ArSceneView

class BossConfigFactory(
    private val context: Context,
    private val sceneView: ArSceneView,
    private val physicsEngine: PhysicsEngine,
    private val registerPhysicsNode: (com.example.space_war_ar_demo.physics.PhysicsNode) -> Unit
) {
    fun createConfigForLevel(levelNumber: Int): BossConfig? {
        val levelData = LevelBalance.getLevelData(levelNumber)
        return when (levelNumber) {
            2 -> createLevel2Config(levelData)
            else -> null
        }
    }

    private fun createLevel2Config(levelData: LevelBalance.LevelData): BossConfig {
        val behavior = ChaseBehavior(maxSpeed = 0.02f, attackRange = 6f)
        val attacks = listOf(
            ProjectileAttack(context, sceneView, registerPhysicsNode, cooldown = 2000L)
        )

        return BossConfig(
            health = 30,
            maxHealth = 30,
            damage = levelData.bossDamage,
            behavior = behavior,
            attacks = attacks,
            abilities = emptyList<com.example.space_war_ar_demo.boss_brain.ability.BossAbility>(),
            bossName = "Предатель"
        )
    }
}
