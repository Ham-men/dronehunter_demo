package com.example.space_war_ar_demo.physics

import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import kotlin.math.max
import kotlin.math.min

/**
 * Перечисление для типов физических форм.
 */
enum class ShapeType {
    SPHERE, BOX
}

/**
 * Узел сцены, обладающий физическими свойствами.
 * Может быть статическим или динамическим, иметь массу, скорость и определенную форму.
 */
open class PhysicsNode : Node() {
    var shapeType = ShapeType.SPHERE
    var dimensions = Vector3(1f, 1f, 1f) // Для BOX: полные размеры (ширина, высота, глубина)
    var radius = 0.5f                    // Для SPHERE

    var velocity = Vector3.zero()
    var mass = 1.0f
    var isStatic = false
    var isPhysicsActive = true
    var shouldBeRemoved = false // Флаг для удаления объекта

    // Индивидуальная гравитация для объекта (если null, используется глобальная гравитация)
    var individualGravity: Vector3? = null

    // Кастомный тэг для хранения игровых данных (например, HP, тип объекта)
    var entityTag: Any? = null

    // Множество тегов для отслеживания состояния объекта
    private val tags = mutableSetOf<String>()

    fun addTag(tag: String) {
        tags.add(tag)
    }

    fun removeTag(tag: String) {
        tags.remove(tag)
    }

    fun hasTag(tag: String): Boolean {
        return tags.contains(tag)
    }

    fun clearTags() {
        tags.clear()
    }

    val minExtents: Vector3
        get() = if (shapeType == ShapeType.BOX) Vector3.subtract(
            worldPosition,
            dimensions.scaled(0.5f)
        ) else Vector3.subtract(worldPosition, Vector3(radius, radius, radius))

    val maxExtents: Vector3
        get() = if (shapeType == ShapeType.BOX) Vector3.add(
            worldPosition,
            dimensions.scaled(0.5f)
        ) else Vector3.add(worldPosition, Vector3(radius, radius, radius))
}

/**
 * Интерфейс для прослушивания событий столкновений.
 * Уровни будут реализовывать его для обработки урона, очков и т.д.
 */
interface CollisionListener {
    fun onCollision(nodeA: PhysicsNode, nodeB: PhysicsNode)
}

/**
 * Основной класс физического движка.
 * Управляет всеми физическими объектами, обновляет их состояние и обрабатывает столкновения.
 */
class PhysicsEngine {
    private val dynamicNodes = ArrayList<PhysicsNode>()
    private val staticNodes = ArrayList<PhysicsNode>()
    var listener: CollisionListener? = null
    var gravity = Vector3(0f, -9.8f, 0f)

    fun addNode(node: PhysicsNode) {
        if (node.isStatic) {
            staticNodes.add(node)
        } else {
            dynamicNodes.add(node)
        }
    }

    fun removeNode(node: PhysicsNode) {
        dynamicNodes.remove(node)
        staticNodes.remove(node)
        node.parent?.removeChild(node)
    }

    fun clear() {
        dynamicNodes.clear()
        staticNodes.clear()
    }

    fun cleanup() {
        // Очищаем все узлы
        val allNodes = getAllNodes().toList()
        for (node in allNodes) {
            node.parent?.removeChild(node)
        }
        clear()

        // Очищаем слушатель
        listener = null

        android.util.Log.d("PhysicsEngine", "Physics engine cleanup completed")
    }

    fun getAllNodes(): List<PhysicsNode> {
        return dynamicNodes + staticNodes
    }

    fun getDynamicNodeCount(): Int {
        return dynamicNodes.size
    }

    fun update(deltaTime: Float) {
        // 1. Обновление позиций динамических объектов
        dynamicNodes.forEach { node ->
            // Используем индивидуальную гравитацию или глобальную
            val nodeGravity = node.individualGravity ?: gravity
            node.velocity = Vector3.add(node.velocity, nodeGravity.scaled(deltaTime))
            node.worldPosition = Vector3.add(node.worldPosition, node.velocity.scaled(deltaTime))
        }

        // 2. Обработка столкновений
        // Динамические со статическими
        for (dynamicNode in dynamicNodes.toList()) {
            for (staticNode in staticNodes.toList()) {
                handleCollision(dynamicNode, staticNode)
            }
        }
        // Динамические друг с другом - используем toList() для безопасной итерации
        val dynamicNodesCopy = dynamicNodes.toList()
        for (i in 0 until dynamicNodesCopy.size) {
            for (j in i + 1 until dynamicNodesCopy.size) {
                // Проверяем, что объекты все еще существуют в оригинальном списке
                if (i < dynamicNodes.size && j < dynamicNodes.size &&
                    dynamicNodes[i] == dynamicNodesCopy[i] &&
                    dynamicNodes[j] == dynamicNodesCopy[j]
                ) {
                    handleCollision(dynamicNodes[i], dynamicNodes[j])
                }
            }
        }

        // 3. Удаление объектов с флагом shouldBeRemoved
        removeMarkedNodes()
    }

    private fun removeMarkedNodes() {
        val nodesToRemove = mutableListOf<PhysicsNode>()

        // Собираем все объекты для удаления
        dynamicNodes.forEach { if (it.shouldBeRemoved) nodesToRemove.add(it) }
        staticNodes.forEach { if (it.shouldBeRemoved) nodesToRemove.add(it) }

        // Удаляем их
        nodesToRemove.forEach { removeNode(it) }
    }

    private fun handleCollision(obj1: PhysicsNode, obj2: PhysicsNode) {
        val normal: Vector3
        val depth: Float

        when {
            // Коробка-Коробка
            obj1.shapeType == ShapeType.BOX && obj2.shapeType == ShapeType.BOX -> {
                if (!aabbIntersects(obj1, obj2)) return
                val details = getAabbCollisionDetails(obj1, obj2)
                normal = details.first
                depth = details.second
            }
            // Коробка-Сфера
            obj1.shapeType == ShapeType.BOX && obj2.shapeType == ShapeType.SPHERE -> {
                val details = sphereAabbIntersects(obj2, obj1)
                if (!details.first) return
                normal = details.second
                depth = details.third
            }
            // Сфера-Коробка
            obj1.shapeType == ShapeType.SPHERE && obj2.shapeType == ShapeType.BOX -> {
                val details = sphereAabbIntersects(obj1, obj2)
                if (!details.first) return
                normal = details.second.negated() // Инвертируем нормаль
                depth = details.third
            }
            // Сфера-Сфера
            obj1.shapeType == ShapeType.SPHERE && obj2.shapeType == ShapeType.SPHERE -> {
                val details = sphereIntersects(obj1, obj2)
                if (!details.first) return
                normal = details.second
                depth = details.third
            }

            else -> return
        }

        // Если столкновение произошло, разрешаем его
        resolveCollision(obj1, obj2, normal, depth)
        // Уведомляем слушателя
        listener?.onCollision(obj1, obj2)
    }

    private fun resolveCollision(
        obj1: PhysicsNode,
        obj2: PhysicsNode,
        normal: Vector3,
        depth: Float
    ) {
        if (obj1.isStatic && obj2.isStatic) return

        // 1. Разделение
        val separation = normal.scaled(depth)
        when {
            obj1.isStatic -> obj2.worldPosition = Vector3.add(obj2.worldPosition, separation)
            obj2.isStatic -> obj1.worldPosition = Vector3.subtract(obj1.worldPosition, separation)
            else -> {
                obj1.worldPosition = Vector3.subtract(obj1.worldPosition, separation.scaled(0.5f))
                obj2.worldPosition = Vector3.add(obj2.worldPosition, separation.scaled(0.5f))
            }
        }

        // 2. Импульс
        val relativeVelocity = Vector3.subtract(obj2.velocity, obj1.velocity)
        if (Vector3.dot(relativeVelocity, normal) > 0) return

        val restitution = 0.4f // Коэффициент упругости
        val j = -(1f + restitution) * Vector3.dot(relativeVelocity, normal)
        val invMass1 = if (obj1.isStatic) 0f else 1f / obj1.mass
        val invMass2 = if (obj2.isStatic) 0f else 1f / obj2.mass
        val impulse = normal.scaled(j / (invMass1 + invMass2))

        if (!obj1.isStatic) obj1.velocity =
            Vector3.subtract(obj1.velocity, impulse.scaled(invMass1))
        if (!obj2.isStatic) obj2.velocity = Vector3.add(obj2.velocity, impulse.scaled(invMass2))
    }

    private fun aabbIntersects(box1: PhysicsNode, box2: PhysicsNode): Boolean =
        box1.minExtents.x <= box2.maxExtents.x && box1.maxExtents.x >= box2.minExtents.x &&
                box1.minExtents.y <= box2.maxExtents.y && box1.maxExtents.y >= box2.minExtents.y &&
                box1.minExtents.z <= box2.maxExtents.z && box1.maxExtents.z >= box2.minExtents.z

    private fun sphereAabbIntersects(
        sphere: PhysicsNode,
        box: PhysicsNode
    ): Triple<Boolean, Vector3, Float> {
        val closestPoint = Vector3(
            max(box.minExtents.x, min(sphere.worldPosition.x, box.maxExtents.x)),
            max(box.minExtents.y, min(sphere.worldPosition.y, box.maxExtents.y)),
            max(box.minExtents.z, min(sphere.worldPosition.z, box.maxExtents.z))
        )
        val toSphere = Vector3.subtract(sphere.worldPosition, closestPoint)
        val distanceSq = toSphere.lengthSquared()

        if (distanceSq >= sphere.radius * sphere.radius) return Triple(false, Vector3.zero(), 0f)

        val distance = kotlin.math.sqrt(distanceSq)
        val normal = if (distance > 0.0001f) toSphere.normalized() else Vector3.up()
        val depth = sphere.radius - distance
        return Triple(true, normal, depth)
    }

    private fun sphereIntersects(
        sphere1: PhysicsNode,
        sphere2: PhysicsNode
    ): Triple<Boolean, Vector3, Float> {
        val toSphere2 = Vector3.subtract(sphere2.worldPosition, sphere1.worldPosition)
        val distanceSq = toSphere2.lengthSquared()
        val combinedRadius = sphere1.radius + sphere2.radius

        if (distanceSq >= combinedRadius * combinedRadius) return Triple(false, Vector3.zero(), 0f)

        val distance = kotlin.math.sqrt(distanceSq)
        val normal = if (distance > 0.0001f) toSphere2.normalized() else Vector3.up()
        val depth = combinedRadius - distance
        return Triple(true, normal, depth)
    }

    private fun getAabbCollisionDetails(
        box1: PhysicsNode,
        box2: PhysicsNode
    ): Pair<Vector3, Float> {
        val dist1 = Vector3.subtract(box2.maxExtents, box1.minExtents)
        val dist2 = Vector3.subtract(box1.maxExtents, box2.minExtents)

        var depth = Float.MAX_VALUE
        var normal = Vector3.zero()

        if (dist1.x < depth) {
            depth = dist1.x; normal = Vector3(-1f, 0f, 0f)
        }
        if (dist1.y < depth) {
            depth = dist1.y; normal = Vector3(0f, -1f, 0f)
        }
        if (dist1.z < depth) {
            depth = dist1.z; normal = Vector3(0f, 0f, -1f)
        }
        if (dist2.x < depth) {
            depth = dist2.x; normal = Vector3(1f, 0f, 0f)
        }
        if (dist2.y < depth) {
            depth = dist2.y; normal = Vector3(0f, 1f, 0f)
        }
        if (dist2.z < depth) {
            depth = dist2.z; normal = Vector3(0f, 0f, 1f)
        }

        return Pair(normal, depth)
    }
} 