package com.example.space_war_ar_demo.boss_brain.utils

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

object BossMovementHelper {
    fun moveTowards(current: Vector3, target: Vector3, maxSpeed: Float): Vector3 {
        val dir = Vector3.subtract(target, current)
        val dist = dir.length()
        if (dist < 0.01f) return current
        val step = if (dist < maxSpeed) dist else maxSpeed
        return Vector3.add(current, dir.normalized().scaled(step))
    }

    fun calculateDirectionToPlayer(bossPos: Vector3, playerPos: Vector3): Vector3 {
        return Vector3.subtract(playerPos, bossPos).normalized()
    }

    fun isInRange(bossPos: Vector3, playerPos: Vector3, range: Float): Boolean {
        return Vector3.subtract(bossPos, playerPos).length() <= range
    }

    fun calculateChaseVelocity(
        from: Vector3,
        to: Vector3,
        maxSpeed: Float,
        deltaTime: Float
    ): Vector3 {
        val dir = Vector3.subtract(to, from).normalized()
        return dir.scaled(maxSpeed * deltaTime * 60f)
    }

    fun calculateRotationToTarget(from: Vector3, to: Vector3): Quaternion {
        val dir = Vector3.subtract(to, from)
        if (dir.length() < 0.001f) return Quaternion.identity()
        return Quaternion.lookRotation(dir.normalized(), Vector3.up())
    }
}
