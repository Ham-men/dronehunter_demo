package com.example.space_war_ar_demo.physics

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable

object ProjectileFactory {

    data class ProjectileData(val type: String, val damage: Int)

    fun createBullet(
        material: ModelRenderable,
        position: Vector3,
        forward: Vector3,
        damage: Int
    ): PhysicsNode {
        return PhysicsNode().apply {
            shapeType = ShapeType.SPHERE
            radius = 0.1f
            mass = 1f
            renderable = material
            worldPosition = position
            velocity = forward.scaled(15f)
            entityTag = ProjectileData("Bullet", damage)
        }
    }

    fun createGravityBall(
        material: ModelRenderable,
        position: Vector3,
        forward: Vector3
    ): PhysicsNode {
        return PhysicsNode().apply {
            shapeType = ShapeType.SPHERE
            radius = 0.3f
            mass = 10f // Тяжелый шар для толкания объектов
            renderable = material
            worldPosition = position
            velocity = forward.scaled(8f)
            entityTag = ProjectileData("GravityBall", 0) // Не наносит прямого урона
        }
    }
} 