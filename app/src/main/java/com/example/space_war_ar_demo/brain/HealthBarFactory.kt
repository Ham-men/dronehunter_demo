package com.example.space_war_ar_demo.brain

import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

class HealthBarFactory(private val context: android.content.Context) {

	data class HealthBar(val root: Node, val bar: Node, val maxWidth: Float, val barHeight: Float)

	fun createAbove(target: Node, width: Float = 0.6f, height: Float = 0.06f, yOffset: Float = 0.6f, color: Color = Color(0f, 1f, 0f)): HealthBar {
		val root = Node()
		val barNode = Node()
		MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { mat ->
			val renderable = ShapeFactory.makeCube(Vector3(width, height, 0.01f), Vector3.zero(), mat)
			barNode.renderable = renderable
			barNode.localPosition = Vector3(0f, 0f, 0f)
		}
		root.localPosition = Vector3(0f, yOffset, 0f)
		root.addChild(barNode)
		target.addChild(root)
		return HealthBar(root, barNode, width, height)
	}

	fun update(healthBar: HealthBar, current: Int, max: Int, color: Color? = null) {
		val ratio = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
		val width = (healthBar.maxWidth * ratio).coerceAtLeast(0.01f)
		val ctx = context
		MaterialFactory.makeOpaqueWithColor(ctx, color ?: Color(0f, 1f, 0f)).thenAccept { mat ->
			val renderable = ShapeFactory.makeCube(Vector3(width, healthBar.barHeight, 0.01f), Vector3.zero(), mat)
			healthBar.bar.renderable = renderable
		}
	}

	fun faceTowards(healthBar: HealthBar, targetPosition: Vector3) {
		val root = healthBar.root
		val hbPos = root.worldPosition
		val horizontalTarget = Vector3(targetPosition.x, hbPos.y, targetPosition.z)
		val direction = Vector3.subtract(horizontalTarget, hbPos)
		if (direction.lengthSquared() < 1e-4f) return
		val normalized = direction.normalized()
		val yaw = kotlin.math.atan2(normalized.x, normalized.z)
		val rotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), Math.toDegrees(yaw.toDouble()).toFloat())
		root.worldRotation = rotation
	}
}
