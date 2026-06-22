package com.example.space_war_ar_demo.boss_brain.behavior

import com.example.space_war_ar_demo.boss_brain.BossBase
import com.example.space_war_ar_demo.boss_brain.utils.BossMovementHelper
import com.google.ar.sceneform.math.Vector3

/**
 * Преследование игрока.
 * Босс движется прямо к игроку.
 */
class ChaseBehavior(
    private val maxSpeed: Float = 0.02f,
    private val attackRange: Float = 8f
) : BossBehavior {
    
    override fun update(boss: BossBase, playerPosition: Vector3, deltaTime: Float): BehaviorResult {
        val bossPos = boss.position
        val distance = Vector3.subtract(playerPosition, bossPos).length()
        
        val velocity = BossMovementHelper.calculateChaseVelocity(
            bossPos,
            playerPosition,
            maxSpeed,
            deltaTime
        )
        
        val targetRotation = BossMovementHelper.calculateRotationToTarget(bossPos, playerPosition)
        
        val shouldAttack = distance <= attackRange
        
        return BehaviorResult(
            velocity = velocity,
            targetRotation = targetRotation,
            shouldAttack = shouldAttack
        )
    }
}












