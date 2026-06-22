package com.example.space_war_ar_demo.levels

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.space_war_ar_demo.physics.PhysicsNode
import com.google.ar.sceneform.math.Vector3

/**
 * Класс для управления поведением стационарной турели.
 * Версия 1.0 - нацеливание на ближайшего врага и стрельба.
 */
class BrainTurret_v1 {

    companion object {
        private const val TAG = "BrainTurret_v1"
        private const val INITIAL_DELAY = 1000L // 1 секунда задержки перед началом стрельбы
        private const val FIRE_RATE = 2000L     // 2 секунды между выстрелами
    }

    // === ДАННЫЕ О ТУРЕЛИ ===
    data class TurretData(
        val node: PhysicsNode,
        var currentTarget: TargetData? = null,
        var lastFireTime: Long = 0L,
        var isReady: Boolean = false
    )

    // === ДАННЫЕ О ЦЕЛИ (ДРОНЕ) ===
    data class TargetData(
        val node: PhysicsNode,
        val id: String
    )

    private var turret: TurretData? = null
    private var allTargets = mutableListOf<TargetData>()
    private val handler = Handler(Looper.getMainLooper())
    private var spawnPosition: Vector3? = null

    /**
     * Callback для выстрела. Будет реализован в классе, управляющем уровнем.
     * Передает турель, цель и урон.
     */
    var onFire: ((turret: PhysicsNode, target: PhysicsNode, damage: Int) -> Unit)? = null

    /**
     * Инициализация турели.
     * @param turretNode Узел турели, который будет управляться этим мозгом.
     * @param spawnPos Позиция, в которой была создана турель.
     */
    fun initialize(turretNode: PhysicsNode, spawnPos: Vector3) {
        this.turret = TurretData(node = turretNode)
        this.spawnPosition = spawnPos
        Log.d(TAG, "Турель ${turretNode.hashCode()} инициализирована. Ожидание 1 секунду.")

        // Запускаем таймер на 1 секунду перед активацией
        handler.postDelayed({
            turret?.isReady = true
            Log.d(TAG, "Турель ${turretNode.hashCode()} готова к бою.")
        }, INITIAL_DELAY)
    }

    /**
     * Обновление списка всех возможных целей (дронов).
     * @param targets Список всех дронов на уровне.
     */
    fun updateTargets(targets: List<TargetData>) {
        allTargets.clear()
        allTargets.addAll(targets)
    }

    /**
     * Основной метод обновления, который должен вызываться в игровом цикле.
     */
    fun update() {
        val currentTurret = turret ?: return
        val sp = spawnPosition ?: return

        // Ограничиваем движение турели
        val distance = Vector3.subtract(currentTurret.node.worldPosition, sp).length()
        if (distance > 2.0f) {
            currentTurret.node.velocity = Vector3.zero()
        }

        if (!currentTurret.isReady || onFire == null) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - currentTurret.lastFireTime < FIRE_RATE) {
            return // Перезарядка
        }

        // Если текущая цель уничтожена или ее нет, ищем новую
        if (currentTurret.currentTarget == null || !isTargetAlive(currentTurret.currentTarget!!)) {
            currentTurret.currentTarget = findNearestTarget(currentTurret.node)
        }

        // Если есть цель, стреляем
        currentTurret.currentTarget?.let { target ->
            Log.d(TAG, "Турель ${currentTurret.node.hashCode()} стреляет в цель ${target.id}")
            onFire?.invoke(currentTurret.node, target.node, 5) // Урон 5
            currentTurret.lastFireTime = currentTime
        }
    }

    /**
     * Поиск ближайшей цели (дрона) к турели.
     * @param turretNode Узел турели.
     * @return Данные о ближайшей цели или null, если целей нет.
     */
    private fun findNearestTarget(turretNode: PhysicsNode): TargetData? {
        if (allTargets.isEmpty()) return null

        var nearestTarget: TargetData? = null
        var minDistance = Float.MAX_VALUE

        for (target in allTargets) {
            if (!isTargetAlive(target)) continue // Пропускаем уничтоженные цели

            val distance =
                Vector3.subtract(target.node.worldPosition, turretNode.worldPosition).length()
            if (distance < minDistance) {
                minDistance = distance
                nearestTarget = target
            }
        }
        return nearestTarget
    }

    /**
     * Проверяет, "жива" ли цель.
     * В данном контексте просто проверяем, есть ли узел на сцене.
     * В реальной игре здесь может быть проверка HP.
     */
    private fun isTargetAlive(target: TargetData): Boolean {
        // Простая проверка. В идеале, нужно проверять, активен ли узел в сцене.
        return target.node.scene != null
    }

    /**
     * Очистка ресурсов, когда турель уничтожена или уровень завершен.
     */
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        turret = null
        allTargets.clear()
        Log.d(TAG, "BrainTurret_v1 очищен.")
    }
} 