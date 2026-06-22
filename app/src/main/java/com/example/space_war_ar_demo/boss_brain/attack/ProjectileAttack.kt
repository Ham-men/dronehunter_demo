package com.example.space_war_ar_demo.boss_brain.attack

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.space_war_ar_demo.boss_brain.BossBase
import com.example.space_war_ar_demo.physics.PhysicsNode
import com.example.space_war_ar_demo.physics.ProjectileFactory
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * Обычная атака снарядом.
 * Выстреливает один снаряд в направлении цели.
 */
class ProjectileAttack(
    private val context: Context,
    private val sceneView: ArSceneView,
    private val registerPhysicsNode: (PhysicsNode) -> Unit,
    cooldown: Long = 1500L,
    private val bulletSpeed: Float = 0.2f,
    private val bulletSize: Float = 0.08f,
    private val bulletColor: Color = Color(1f, 0.2f, 0.2f)
) : BaseBossAttack(cooldown, "PROJECTILE") {
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun execute(boss: BossBase, targetPosition: Vector3, damage: Int) {
        val scene = sceneView.scene ?: return
        val bossPos = boss.position
        
        MaterialFactory.makeOpaqueWithColor(context, bulletColor).thenAccept { mat ->
            val dir = Vector3.subtract(targetPosition, bossPos).normalized()
            val renderable = ShapeFactory.makeSphere(bulletSize, Vector3.zero(), mat)
            val phys = ProjectileFactory.createBullet(
                material = renderable,
                position = bossPos,
                forward = dir,
                damage = damage
            )
            scene.addChild(phys)
            registerPhysicsNode(phys)
            
            // Проверка попадания по игроку
            val h = Handler(Looper.getMainLooper())
            val r = object : Runnable {
                override fun run() {
                    if (phys.scene == null) return
                    val cam = scene.camera ?: return
                    val dist = Vector3.subtract(phys.worldPosition, cam.worldPosition).length()
                    if (dist < 0.5f) {
                        val activity = sceneView.context as? com.example.space_war_ar_demo.GameActivityVer2
                        activity?.shipData?.takeDamage(damage)
                        scene.removeChild(phys)
                        return
                    }
                    // Снаряд ушёл далеко — удаляем
                    if (Vector3.subtract(phys.worldPosition, bossPos).length() > 40f) {
                        scene.removeChild(phys)
                        return
                    }
                    h.postDelayed(this, 16)
                }
            }
            h.post(r)
        }
        
        lastExecutionTime = System.currentTimeMillis()
    }
}












