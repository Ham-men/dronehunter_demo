package com.example.space_war_ar_demo.brain

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

object DroneVisuals {

    fun playSpawnMaterialize(
        node: Node,
        handler: Handler = Handler(Looper.getMainLooper()),
        durationMs: Long = 350L
    ) {
        val steps = 18
        val minScale = 0.05f
        val maxScale = 1f
        node.localScale = Vector3(minScale, minScale, minScale)
        for (step in 0..steps) {
            val delay = (durationMs * step) / steps
            handler.postDelayed({
                if (node.scene == null) return@postDelayed
                val progress = step / steps.toFloat()
                val eased = (0.5 - 0.5 * cos(PI * progress.toDouble())).toFloat()
                val scale = minScale + (maxScale - minScale) * eased
                node.localScale = Vector3(scale, scale, scale)
            }, delay)
        }
    }

    fun playExplosionDebris(
        scene: Scene,
        context: Context,
        handler: Handler = Handler(Looper.getMainLooper()),
        position: Vector3
    ) {
        MaterialFactory.makeOpaqueWithColor(context, Color(1f, 0.86f, 0.4f, 1f)).thenAccept { material ->
            val flashRenderable = ShapeFactory.makeSphere(0.08f, Vector3.zero(), material.makeCopy())
            val flashNode = Node().apply {
                renderable = flashRenderable
                worldPosition = position
                localScale = Vector3(0.2f, 0.2f, 0.2f)
            }
            scene.addChild(flashNode)
            val flashSteps = 14
            val flashDuration = 260L
            for (step in 0..flashSteps) {
                val delay = (flashDuration * step) / flashSteps
                handler.postDelayed({
                    if (flashNode.scene == null) return@postDelayed
                    val progress = step / flashSteps.toFloat()
                    val scale = 0.2f + 0.6f * progress
                    flashNode.localScale = Vector3(scale, scale, scale)
                    val alpha = (1f - progress).coerceAtLeast(0f)
                    flashNode.renderable?.material?.setFloat4(
                        "baseColorTint",
                        Color(1f, 0.86f, 0.4f, alpha)
                    )
                    if (progress >= 1f) {
                        scene.removeChild(flashNode)
                    }
                }, delay)
            }

            repeat(3) {
                val debrisRenderable =
                    ShapeFactory.makeCube(Vector3(0.05f, 0.05f, 0.05f), Vector3.zero(), material.makeCopy())
                val debrisNode = Node().apply {
                    renderable = debrisRenderable
                    worldPosition = position
                }
                scene.addChild(debrisNode)
                val velocity = Vector3(
                    (Random.nextFloat() - 0.5f) * 0.8f,
                    Random.nextFloat() * 0.9f,
                    (Random.nextFloat() - 0.5f) * 0.8f
                )
                val debrisSteps = 16
                val duration = 380L
                for (step in 0..debrisSteps) {
                    val delay = (duration * step) / debrisSteps
                    handler.postDelayed({
                        if (debrisNode.scene == null) return@postDelayed
                        val progress = step / debrisSteps.toFloat()
                        val offset = velocity.scaled(progress)
                        debrisNode.worldPosition = Vector3.add(position, offset)
                        val alpha = (1f - progress).coerceAtLeast(0f)
                        debrisNode.renderable?.material?.setFloat4(
                            "baseColorTint",
                            Color(1f, 0.75f, 0.35f, alpha)
                        )
                        if (progress >= 1f) {
                            scene.removeChild(debrisNode)
                        }
                    }, delay)
                }
            }
        }
    }
}

