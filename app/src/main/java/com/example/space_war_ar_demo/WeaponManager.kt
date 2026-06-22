package com.example.space_war_ar_demo

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.space_war_ar_demo.levels.callOnHit
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

// Параметры для разных видов оружия
// Можно расширять по необходимости

data class BulletParams(
    val damage: Int = 1,
    val color: Color = Color(1.0f, 0.0f, 0.0f),
    val speed: Float = 0.2f
)

data class LaserParams(
    val damage: Int = 1,
    val color: Color = Color(0.0f, 1.0f, 1.0f), // Циан для лучшей видимости
    val length: Float = 10f
)

data class GravityBallParams(
    val damage: Int = 0,
    val color: Color = Color(0.2f, 0.1f, 0.5f),
    val speed: Float = 0.2f,
    val radius: Float = 0.3f
)

data class MissileParams(
    val damage: Int = 10,
    val color: Color = Color(1.0f, 0.5f, 0.0f), // Более яркий оранжевый
    val speed: Float = 0.1f, // Увеличиваем скорость
    val homing: Boolean = true
)

class WeaponManager(private val sceneView: ArSceneView, private val activity: GameActivityVer2?) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastShotTime: Long = 0L
    var isPaused: Boolean = false

    fun getLastShotTime(): Long = lastShotTime

    // Список активных снарядов игрока для песчаных бурь
    private val activeProjectiles = mutableListOf<ProjectileData>()
    
    // Список активных runnable для остановки при паузе
    private val activeRunnables = mutableListOf<Runnable>()

    data class ProjectileData(
        val node: Node,
        var forward: Vector3,
        var velocity: Vector3,
        var updateRunnable: Runnable? = null
    )

    fun getActiveProjectiles(): List<ProjectileData> {
        return activeProjectiles.toList()
    }

    private fun addProjectile(node: Node, forward: Vector3, velocity: Vector3, updateRunnable: Runnable? = null) {
        val projectileData = ProjectileData(node, forward, velocity, updateRunnable)
        activeProjectiles.add(projectileData)
        Log.d("WeaponManager", "Projectile added: ${node.name}, total projectiles: ${activeProjectiles.size}")
        
        if (updateRunnable != null) {
            activeRunnables.add(updateRunnable)
            Log.d("WeaponManager", "Runnable added for ${node.name}, total runnables: ${activeRunnables.size}")
        }

        // Удаляем снаряд через 5 секунд
        handler.postDelayed({
            activeProjectiles.remove(projectileData)
            if (projectileData.updateRunnable != null) {
                activeRunnables.remove(projectileData.updateRunnable)
            }
            // Также удаляем узел снаряда со сцены и из физики, чтобы не засорять память
            try {
                if (projectileData.node.scene != null) {
                    sceneView.scene.removeChild(projectileData.node)
                }
                val phys = projectileData.node as? com.example.space_war_ar_demo.physics.PhysicsNode
                if (phys != null) {
                    (physicsEngineProvider?.invoke() as? com.example.space_war_ar_demo.physics.PhysicsEngine)?.removeNode(
                        phys
                    )
                }
            } catch (_: Exception) {
                // безопасно игнорируем
            }
        }, 5000)
    }

    // Метод для получения обновленной скорости снаряда
    private fun getUpdatedVelocity(node: Node): Vector3? {
        val projectileData = activeProjectiles.find { it.node == node }
        return projectileData?.velocity
    }

    // Метод для обновления скорости PhysicsNode
    private fun updatePhysicsNodeVelocity(node: Node) {
        val projectileData = activeProjectiles.find { it.node == node }
        if (projectileData != null && node is com.example.space_war_ar_demo.physics.PhysicsNode) {
            node.velocity = projectileData.velocity
        }
    }

    // Позволяет получать актуальный список дронов из уровня
    private var droneProvider: (() -> List<Node>)? = null
    fun setDroneProvider(provider: () -> List<Node>) {
        droneProvider = provider
    }

    private var bossProvider: (() -> Node?)? = null
    fun setBossProvider(provider: () -> Node?) {
        bossProvider = provider
    }

    private var physicsEngineProvider: (() -> Any?)? = null
    fun setPhysicsEngineProvider(provider: () -> Any?) {
        physicsEngineProvider = provider
    }

    fun fireBullet(cameraPos: Vector3, cameraForward: Vector3, params: BulletParams) {
        lastShotTime = System.currentTimeMillis()
        Log.d("WeaponManager", "=== fireBullet START ===")
        Log.d("WeaponManager", "Current pause state - local: $isPaused, activity: ${activity?.isPaused}")
        Log.d(
            "WeaponManager",
            "fireBullet called: pos=$cameraPos, forward=$cameraForward, params=$params"
        )
        Log.d(
            "WeaponManager",
            "bossProvider: ${bossProvider != null}"
        )

        MaterialFactory.makeOpaqueWithColor(sceneView.context, params.color)
            .thenAccept { material ->
                val bullet = ShapeFactory.makeSphere(0.04f, Vector3.zero(), material)
                val bulletNode = Node().apply {
                    renderable = bullet
                    worldPosition = cameraPos
                    this.setParent(sceneView.scene)
                    this.name = "PlayerBullet"
                }
                Log.d("WeaponManager", "Bullet node created at ${bulletNode.worldPosition}")
                val velocity = cameraForward.normalized().scaled(params.speed)

                var justCreated = true
                val updateRunnable = object : Runnable {
                    override fun run() {
                        // Проверяем паузу - если игра на паузе, полностью останавливаем runnable
                        if (isPaused || activity?.isPaused == true) {
                            Log.d("WeaponManager", "Bullet runnable stopped due to pause (local: $isPaused, activity: ${activity?.isPaused})")
                            return
                        }
                        
                        val currentVelocity = getUpdatedVelocity(bulletNode)
                        if (currentVelocity != null) {
                            bulletNode.worldPosition =
                                Vector3.add(bulletNode.worldPosition, currentVelocity)
                            Log.d(
                                "WeaponManager",
                                "Bullet node moved to ${bulletNode.worldPosition}"
                            )
                        }
                        // Проверка попадания по дронам
                        val drones = droneProvider?.invoke() ?: emptyList()
                        for (drone in drones) {
                            if (drone.isActive && Vector3.subtract(
                                    bulletNode.worldPosition,
                                    drone.worldPosition
                                ).length() < 0.13f
                            ) {
                                Log.d(
                                    "WeaponManager",
                                    "Bullet hit drone: ${drone.hashCode()}, pos=${drone.worldPosition}"
                                )
                                drone.callOnHit(params.damage)
                                sceneView.scene.removeChild(bulletNode)
                                Log.d(
                                    "WeaponManager",
                                    "Bullet node removed from scene (hit drone) at ${bulletNode.worldPosition}"
                                )
                                return
                            }
                        }
                        // Проверка попадания по боссу (Lvl15, Lvl16)
                        val boss = bossProvider?.invoke()
                        if (boss != null) {
                            val dist =
                                Vector3.subtract(bulletNode.worldPosition, boss.worldPosition)
                                    .length()
                            Log.d(
                                "WeaponManager",
                                "Checking boss hit: bulletPos=${bulletNode.worldPosition}, bossPos=${boss.worldPosition}, dist=$dist"
                            )
                            if (dist < 1.0f) { // Увеличиваем радиус попадания для босса
                                Log.d("WeaponManager", "Boss hit! Applying damage: $params.damage")
                                boss.callOnHit(params.damage)
                                sceneView.scene.removeChild(bulletNode)
                                Log.d(
                                    "WeaponManager",
                                    "Bullet node removed from scene (hit boss) at ${bulletNode.worldPosition}"
                                )
                                return
                            }
                        }
                        // Проверка попадания по базе/боссу (уже перемещена выше)
                        if (bulletNode.worldPosition.z < -20f || bulletNode.worldPosition.z > 20f) {
                            sceneView.scene.removeChild(bulletNode)
                            Log.d(
                                "WeaponManager",
                                "Bullet node removed from scene (out of bounds) at ${bulletNode.worldPosition}"
                            )
                        } else {
                            handler.postDelayed(this, 16)
                        }
                    }
                }
                // Добавляем снаряд в список активных для песчаных бурь
                addProjectile(bulletNode, cameraForward.normalized(), velocity, updateRunnable)
                Log.d("WeaponManager", "Bullet added to active projectiles list")
                
                sceneView.scene.addChild(bulletNode)
                Log.d("WeaponManager", "Bullet node added to scene")
                Log.d("WeaponManager", "Bullet updateRunnable started")
                Log.d("WeaponManager", "=== fireBullet END ===")
                handler.post(updateRunnable)
            }
    }

    fun fireLaser(cameraPos: Vector3, cameraForward: Vector3, params: LaserParams) {
        lastShotTime = System.currentTimeMillis()
        Log.d(
            "WeaponManager",
            "fireLaser called: pos=$cameraPos, forward=$cameraForward, params=$params"
        )

        val laserColor = Color(1.0f, 0.0f, 0.0f) // Ярко-красный, непрозрачный
        val laserRadius = 0.1f // Толще
        val laserLength = params.length
        Log.d(
            "WeaponManager",
            "Laser visual params: color=$laserColor, radius=$laserRadius, length=$laserLength"
        )

        MaterialFactory.makeOpaqueWithColor(sceneView.context, laserColor)
            .thenAccept { material ->
                Log.d("WeaponManager", "Laser material created successfully")
                val laser =
                    ShapeFactory.makeCylinder(laserRadius, laserLength, Vector3.zero(), material)
                Log.d(
                    "WeaponManager",
                    "Laser cylinder created: radius=$laserRadius, length=$laserLength"
                )

                // Новый способ: строим лазер от точки под камерой вперед
                val laserStart = Vector3.add(cameraPos, Vector3(0f, -0.2f, 0f))
                val laserDir = cameraForward.normalized()
                val laserEnd = Vector3.add(laserStart, laserDir.scaled(laserLength))
                val laserCenter = Vector3.lerp(laserStart, laserEnd, 0.5f)
                val laserRotation = Quaternion.lookRotation(laserDir, Vector3.up())
                val extraRotation =
                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 90f) // 90 градусов вокруг X
                val finalRotation = Quaternion.multiply(laserRotation, extraRotation)
                Log.d(
                    "WeaponManager",
                    "Laser start: $laserStart, end: $laserEnd, center: $laserCenter, rotation: $finalRotation"
                )

                val laserNode = Node().apply {
                    renderable = laser
                    worldPosition = laserCenter
                    localRotation = finalRotation
                    sceneView.scene.addChild(this)
                    this.name = "Laser"
                }
                Log.d(
                    "WeaponManager",
                    "Laser node created and added to scene: pos=${laserNode.worldPosition}, parent after add: ${laserNode.parent?.javaClass?.simpleName}, rotation=${laserNode.localRotation}"
                )

                handler.postDelayed({
                    Log.d(
                        "WeaponManager",
                        "Removing laser node from scene. Parent before remove: ${laserNode.parent?.javaClass?.simpleName}"
                    )
                    sceneView.scene.removeChild(laserNode)
                }, 300)
            }
            .exceptionally { throwable ->
                Log.e("WeaponManager", "Error creating laser material", throwable)
                null
            }

        // Логика урона - выполняем после создания визуального эффекта
        handler.postDelayed({
            // Проверяем паузу - если игра на паузе, не обрабатываем урон
            if (isPaused || activity?.isPaused == true) {
                Log.d("WeaponManager", "Laser damage processing skipped - game is paused (local: $isPaused, activity: ${activity?.isPaused})")
                return@postDelayed
            }
            
            Log.d("WeaponManager", "Processing laser damage logic")
            val laserStart = Vector3.add(cameraPos, Vector3(0f, -0.2f, 0f))
            val laserDir = cameraForward.normalized()
            val hitThreshold = 0.5f // Увеличиваем порог попадания
            val drones = droneProvider?.invoke() ?: emptyList()
            Log.d("WeaponManager", "Checking ${drones.size} drones for laser hit")

            for (drone in drones) {
                val toDrone = Vector3.subtract(drone.worldPosition, laserStart)
                val proj = Vector3.dot(toDrone, laserDir)
                Log.d(
                    "WeaponManager",
                    "Drone ${drone.hashCode()}: pos=${drone.worldPosition}, toDrone=${toDrone.length()}, proj=$proj"
                )
                if (proj > 0 && proj < laserLength) {
                    val closestPoint = Vector3.add(laserStart, laserDir.scaled(proj))
                    val dist = Vector3.subtract(drone.worldPosition, closestPoint).length()
                    Log.d(
                        "WeaponManager",
                        "Drone ${drone.hashCode()}: proj=$proj, dist=$dist, threshold=$hitThreshold"
                    )
                    if (dist < hitThreshold) {
                        Log.d(
                            "WeaponManager",
                            "Laser hit drone ${drone.hashCode()} with damage ${params.damage}"
                        )
                        drone.callOnHit(params.damage)
                    }
                }
            }

            // Попадание по боссу
            val boss = bossProvider?.invoke()
            if (boss != null) {
                val toBoss = Vector3.subtract(boss.worldPosition, laserStart)
                val proj = Vector3.dot(toBoss, laserDir)
                Log.d("WeaponManager", "Boss check: pos=${boss.worldPosition}, proj=$proj")
                if (proj > 0 && proj < laserLength) {
                    val closestPoint = Vector3.add(laserStart, laserDir.scaled(proj))
                    val dist = Vector3.subtract(boss.worldPosition, closestPoint).length()
                    Log.d(
                        "WeaponManager",
                        "Boss: proj=$proj, dist=$dist, threshold=${hitThreshold + 0.8f}"
                    )
                    if (dist < hitThreshold + 0.8f) {
                        Log.d("WeaponManager", "Laser hit boss with damage ${params.damage}")
                        boss.callOnHit(params.damage)
                    }
                }
            }

        }, 50)
    }

    fun fireGravityBall(cameraPos: Vector3, cameraForward: Vector3, params: GravityBallParams) {
        lastShotTime = System.currentTimeMillis()
        Log.d(
            "WeaponManager",
            "fireGravityBall called: pos=$cameraPos, forward=$cameraForward, params=$params"
        )

        MaterialFactory.makeOpaqueWithColor(sceneView.context, params.color)
            .thenAccept { material ->
                val gravityBall = ShapeFactory.makeSphere(0.3f, Vector3.zero(), material)
                // Немного смещаем точку спауна вперед от игрока, и добавляем небольшой наклон вниз к скорости
                val spawnPos = Vector3.add(cameraPos, cameraForward.normalized().scaled(0.6f))
                val initialForward =
                    Vector3.add(cameraForward.normalized(), Vector3(0f, -0.25f, 0f)).normalized()
                val gravityBallNode = com.example.space_war_ar_demo.physics.PhysicsNode().apply {
                    shapeType = com.example.space_war_ar_demo.physics.ShapeType.SPHERE
                    radius = 0.3f
                    mass = 5f
                    renderable = gravityBall
                    worldPosition = spawnPos
                    velocity = initialForward.scaled(params.speed)
                    entityTag = "GravityBall"
                }
                sceneView.scene.addChild(gravityBallNode)
                Log.d(
                    "WeaponManager",
                    "GravityBall added to scene at $spawnPos with velocity ${gravityBallNode.velocity}"
                )

                // Создаем updateRunnable для гравити шара
                val updateRunnable = object : Runnable {
                    override fun run() {
                        // Проверяем паузу - если игра на паузе, полностью останавливаем runnable
                        if (isPaused || activity?.isPaused == true) {
                            Log.d("WeaponManager", "GravityBall runnable stopped due to pause (local: $isPaused, activity: ${activity?.isPaused})")
                            return
                        }
                        
                        val currentVelocity = getUpdatedVelocity(gravityBallNode)
                        if (currentVelocity != null) {
                            gravityBallNode.worldPosition = Vector3.add(gravityBallNode.worldPosition, currentVelocity)
                            Log.d("WeaponManager", "GravityBall moved to ${gravityBallNode.worldPosition}")
                        }
                        
                        // Проверка границ - удаляем если улетел далеко
                        if (gravityBallNode.worldPosition.z < -20f || gravityBallNode.worldPosition.z > 20f ||
                            gravityBallNode.worldPosition.x < -20f || gravityBallNode.worldPosition.x > 20f ||
                            gravityBallNode.worldPosition.y < -20f || gravityBallNode.worldPosition.y > 20f) {
                            sceneView.scene.removeChild(gravityBallNode)
                            Log.d("WeaponManager", "GravityBall removed from scene (out of bounds)")
                        } else {
                            handler.postDelayed(this, 16)
                        }
                    }
                }
                
                // Добавляем снаряд в список активных для песчаных бурь
                addProjectile(gravityBallNode, initialForward, gravityBallNode.velocity, updateRunnable)

                // Запускаем updateRunnable для гравити шара
                handler.post(updateRunnable)
                Log.d("WeaponManager", "GravityBall updateRunnable started")
                
                // Добавить в physicsEngine
                val physicsEngine =
                    physicsEngineProvider?.invoke() as? com.example.space_war_ar_demo.physics.PhysicsEngine
                if (physicsEngine != null) {
                    physicsEngine.addNode(gravityBallNode)
                    Log.d("WeaponManager", "GravityBall added to physicsEngine successfully")
                } else {
                    Log.e(
                        "WeaponManager",
                        "physicsEngineProvider returned null, cannot add GravityBall to physics"
                    )
                }
            }
    }

    fun fireMissile(
        cameraPos: Vector3,
        cameraForward: Vector3,
        params: MissileParams,
        target: Node?
    ) {
        lastShotTime = System.currentTimeMillis()
        Log.d("WeaponManager_missile", "Current pause state - local: $isPaused, activity: ${activity?.isPaused}")
        Log.d(
            "WeaponManager_missile",
            "fireMissile called: pos=$cameraPos, forward=$cameraForward, params=$params"
        )
        // Логируем все возможные цели для ракеты
        val drones = droneProvider?.invoke() ?: emptyList()
        for (drone in drones) {
            Log.d(
                "WeaponManager_missile",
                "[MissileTargeting] Candidate: hash=${drone.hashCode()}, entityTag=${(drone as? com.example.space_war_ar_demo.physics.PhysicsNode)?.entityTag}, type=${drone::class.java.simpleName}, renderable=${drone.renderable}"
            )
        }
        Log.d(
            "WeaponManager_missile",
            "[MissileTargeting] Selected target: ${target?.hashCode()}, type=${target?.javaClass?.simpleName}, renderable=${target?.renderable}"
        )

        if (target == null) {
            Log.d("WeaponManager_missile", "No target selected, creating simple missile")
            // Если цель не выбрана, создаем простую ракету без наведения
            MaterialFactory.makeOpaqueWithColor(sceneView.context, params.color)
                .thenAccept { material ->
                    val missileRenderable = ShapeFactory.makeSphere(0.13f, Vector3.zero(), material)
                    Log.d(
                        "WeaponManager_missile",
                        "Simple missile material created: $missileRenderable"
                    )
                    val missileNode = Node().apply {
                        renderable = missileRenderable
                        worldPosition = Vector3.add(
                            cameraPos,
                            cameraForward.scaled(0.5f)
                        ) // старт чуть дальше камеры
                        this.setParent(sceneView.scene)
                        this.name = "Missile"
                        if (this is com.example.space_war_ar_demo.physics.PhysicsNode) {
                            this.entityTag = "Missile"
                        }
                    }
                    Log.d(
                        "WeaponManager_missile",
                        "Simple missile node created: pos=${missileNode.worldPosition}"
                    )

                    val velocity = cameraForward.normalized().scaled(params.speed)
                    Log.d("WeaponManager_missile", "Simple missile velocity: $velocity")

                    var justCreated = true
                    val updateRunnable = object : Runnable {
                        override fun run() {
                            // Проверяем паузу - если игра на паузе, полностью останавливаем runnable
                            if (isPaused || activity?.isPaused == true) {
                                Log.d("WeaponManager_missile", "Missile runnable stopped due to pause (local: $isPaused, activity: ${activity?.isPaused})")
                                return
                            }
                            
                            val currentVelocity = getUpdatedVelocity(missileNode)
                            if (currentVelocity != null) {
                                missileNode.worldPosition =
                                    Vector3.add(missileNode.worldPosition, currentVelocity)
                                Log.d(
                                    "WeaponManager_missile",
                                    "Simple missile position: ${missileNode.worldPosition}"
                                )
                            }

                            // Проверка попадания по дронам
                            val drones = droneProvider?.invoke() ?: emptyList()
                            for (drone in drones) {
                                if (drone.isActive && Vector3.subtract(
                                        missileNode.worldPosition,
                                        drone.worldPosition
                                    ).length() < 0.28f
                                ) {
                                    Log.d(
                                        "WeaponManager_missile",
                                        "Simple missile hit drone ${drone.hashCode()}"
                                    )
                                    drone.callOnHit(params.damage)
                                    sceneView.scene.removeChild(missileNode)
                                    Log.d(
                                        "WeaponManager_missile",
                                        "Simple missile node removed from scene (hit drone) at ${missileNode.worldPosition}"
                                    )
                                    return
                                }
                            }
                            // Проверка попадания по боссу
                            val boss = bossProvider?.invoke()
                            if (boss != null && Vector3.subtract(
                                    missileNode.worldPosition,
                                    boss.worldPosition
                                ).length() < 0.2f
                            ) {
                                Log.d("WeaponManager_missile", "Simple missile hit boss")
                                boss.callOnHit(params.damage)
                                sceneView.scene.removeChild(missileNode)
                                Log.d(
                                    "WeaponManager_missile",
                                    "Simple missile node removed from scene (hit boss) at ${missileNode.worldPosition}"
                                )
                                return
                            }
                            // Проверка попадания по боссу (Lvl15) - используем существующую логику выше
                            if (missileNode.worldPosition.z < -40f || missileNode.worldPosition.z > 40f) {
                                Log.d(
                                    "WeaponManager_missile",
                                    "Simple missile out of bounds, removing at ${missileNode.worldPosition}"
                                )
                                sceneView.scene.removeChild(missileNode)
                                Log.d(
                                    "WeaponManager_missile",
                                    "Simple missile node removed from scene (out of bounds) at ${missileNode.worldPosition}"
                                )
                            } else {
                                handler.postDelayed(this, 16)
                            }
                        }
                    }
                    // Добавляем снаряд в список активных для песчаных бурь
                    addProjectile(missileNode, cameraForward.normalized(), velocity, updateRunnable)
                    Log.d("WeaponManager_missile", "Missile added to active projectiles list")
                    
                    sceneView.scene.addChild(missileNode)
                    Log.d("WeaponManager_missile", "Simple missile node added to scene")
                    Log.d("WeaponManager_missile", "Missile updateRunnable started")
                    handler.post(updateRunnable)
                }
                .exceptionally { throwable ->
                    Log.e("WeaponManager", "Error creating simple missile material", throwable)
                    null
                }
            return
        }

        Log.d("WeaponManager_missile", "Creating homing missile for target ${target.hashCode()}")
        val missileNode = HomingMissile(sceneView, cameraPos, target, params, this)
        sceneView.scene.addChild(missileNode)
        
        // Создаем runnable для гоминг ракеты, чтобы его можно было остановить при паузе
        val homingMissileRunnable = object : Runnable {
            override fun run() {
                // Проверяем паузу - если игра на паузе, полностью останавливаем runnable
                if (isPaused || activity?.isPaused == true) {
                    Log.d("WeaponManager_missile", "Homing missile runnable stopped due to pause (local: $isPaused, activity: ${activity?.isPaused})")
                    return
                }
                
                // Этот runnable будет вызывать update() у гоминг ракеты
                if (missileNode.isAlive()) {
                    missileNode.forceUpdate()
                    handler.postDelayed(this, 16)
                } else {
                    Log.d("WeaponManager_missile", "Homing missile is no longer alive, stopping runnable")
                }
            }
        }
        
        // Добавляем гоминг ракету в список активных снарядов для системы паузы
        addProjectile(missileNode, cameraForward.normalized(), Vector3.zero(), homingMissileRunnable)
        Log.d("WeaponManager_missile", "Homing missile added to scene and active projectiles")
        
        // Запускаем runnable для гоминг ракеты
        handler.post(homingMissileRunnable)
    }

    /**
     * Останавливает все активные снаряды игрока
     */
    fun pause() {
        Log.d("WeaponManager", "=== PAUSE START ===")
        Log.d("WeaponManager", "pause: stopping all projectiles")
        Log.d("WeaponManager", "Active projectiles before pause: ${activeProjectiles.size}")
        Log.d("WeaponManager", "Active runnables before pause: ${activeRunnables.size}")
        
        isPaused = true
        
        // Останавливаем все активные runnable
        activeRunnables.forEachIndexed { index, runnable ->
            handler.removeCallbacks(runnable)
            Log.d("WeaponManager", "Removed runnable #$index")
        }
        
        // Останавливаем все активные снаряды, устанавливая их скорость в ноль
        activeProjectiles.forEachIndexed { index, projectile ->
            val nodeName = projectile.node.name ?: "Unknown"
            val nodeType = projectile.node.javaClass.simpleName
            Log.d("WeaponManager", "Stopping projectile #$index: $nodeName ($nodeType)")
            projectile.velocity = Vector3.zero()
            // Если это PhysicsNode, также останавливаем его
            if (projectile.node is com.example.space_war_ar_demo.physics.PhysicsNode) {
                (projectile.node as com.example.space_war_ar_demo.physics.PhysicsNode).velocity = Vector3.zero()
                Log.d("WeaponManager", "PhysicsNode velocity set to zero for projectile #$index")
            }
            // Если это HomingMissile, логируем дополнительную информацию
            if (projectile.node is HomingMissile) {
                Log.d("WeaponManager", "HomingMissile #$index paused: alive=${(projectile.node as HomingMissile).isAlive()}")
            }
        }
        
        Log.d("WeaponManager", "pause: ${activeProjectiles.size} projectiles stopped, ${activeRunnables.size} runnables stopped")
        Log.d("WeaponManager", "=== PAUSE END ===")
    }

    /**
     * Возобновляет движение снарядов
     */
    fun resume() {
        Log.d("WeaponManager", "=== RESUME START ===")
        Log.d("WeaponManager", "resume: resuming all projectiles")
        Log.d("WeaponManager", "Active projectiles before resume: ${activeProjectiles.size}")
        
        isPaused = false
        
        // Восстанавливаем скорость для всех активных снарядов
        activeProjectiles.forEachIndexed { index, projectile ->
            val nodeName = projectile.node.name ?: "Unknown"
            val nodeType = projectile.node.javaClass.simpleName
            Log.d("WeaponManager", "Resuming projectile #$index: $nodeName ($nodeType)")
            
            // Восстанавливаем исходную скорость снаряда
            projectile.velocity = projectile.forward.scaled(getProjectileSpeed(projectile.node))
            
            // Если это PhysicsNode, также восстанавливаем его скорость
            if (projectile.node is com.example.space_war_ar_demo.physics.PhysicsNode) {
                (projectile.node as com.example.space_war_ar_demo.physics.PhysicsNode).velocity = projectile.velocity
                Log.d("WeaponManager", "PhysicsNode velocity restored for projectile #$index: ${projectile.velocity}")
            }
            
            // Если это HomingMissile, логируем дополнительную информацию
            if (projectile.node is HomingMissile) {
                Log.d("WeaponManager", "HomingMissile #$index resumed: alive=${(projectile.node as HomingMissile).isAlive()}")
            }
            
            // Перезапускаем runnable если он есть
            if (projectile.updateRunnable != null) {
                handler.post(projectile.updateRunnable!!)
                Log.d("WeaponManager", "Runnable restarted for projectile #$index")
            }
        }
        
        Log.d("WeaponManager", "resume: ${activeProjectiles.size} projectiles resumed")
        Log.d("WeaponManager", "=== RESUME END ===")
    }
    
    /**
     * Получает скорость снаряда в зависимости от его типа
     */
    private fun getProjectileSpeed(node: Node): Float {
        return when {
            node.name == "PlayerBullet" -> 0.2f // Скорость пули
            node.name == "Missile" -> 0.1f // Скорость ракеты
            node.name == "GravityBall" -> 0.1f // Скорость гравити шара
            node is HomingMissile -> 0.1f // Скорость гоминг ракеты
            else -> 0.2f // По умолчанию
        }
    }

    /**
     * Полностью очищает все активные снаряды
     */
    fun clearAllProjectiles() {
        Log.d("WeaponManager", "clearAllProjectiles: removing ${activeProjectiles.size} projectiles")
        
        // Останавливаем все runnable
        activeRunnables.forEach { runnable ->
            handler.removeCallbacks(runnable)
        }
        
        activeProjectiles.forEach { projectile ->
            try {
                // Удаляем узел со сцены
                if (projectile.node.scene != null) {
                    sceneView.scene.removeChild(projectile.node)
                }
                // Удаляем из физики если это PhysicsNode
                val phys = projectile.node as? com.example.space_war_ar_demo.physics.PhysicsNode
                if (phys != null) {
                    (physicsEngineProvider?.invoke() as? com.example.space_war_ar_demo.physics.PhysicsEngine)?.removeNode(phys)
                }
            } catch (e: Exception) {
                Log.e("WeaponManager", "Error removing projectile: ${e.message}")
            }
        }
        
        activeProjectiles.clear()
        activeRunnables.clear()
        Log.d("WeaponManager", "clearAllProjectiles: all projectiles and runnables removed")
    }
}

// Вспомогательные extension-функции для Node
fun Node.setTag(tag: String) {
    this.name = tag
} // для entityTag

// Класс гоминг-ракеты
class HomingMissile(
    private val sceneView: ArSceneView,
    startPos: Vector3,
    private val target: Node,
    private val params: MissileParams,
    private val weaponManager: WeaponManager? = null
) : Node() {
    private val handler = Handler(Looper.getMainLooper())
    private var alive = true
    private var velocity: Vector3 = Vector3.zero()
    private val turnRate: Float = 6f // можно сделать параметром
    private var lastKnownTargetPos = target.worldPosition
    private var materialCreated = false

    init {
        Log.d(
            "HomingMissile",
            "Initializing homing missile: startPos=$startPos, target=${target.hashCode()}"
        )
        worldPosition = startPos
        this.setTag("Missile")
        MaterialFactory.makeOpaqueWithColor(sceneView.context, params.color)
            .thenAccept { material ->
                Log.d("HomingMissile", "Missile material created")
                renderable = ShapeFactory.makeSphere(0.12f, Vector3.zero(), material)
                Log.d("HomingMissile", "Missile renderable created")
                materialCreated = true
                // Обновление будет запущено через внешний runnable
            }
            .exceptionally { throwable ->
                Log.e("HomingMissile", "Error creating missile material", throwable)
                null
            }
        // Начальная скорость в сторону цели
        velocity = Vector3.subtract(target.worldPosition, startPos).normalized()
            .scaled(params.speed * 0.5f)
        Log.d("HomingMissile", "Initial velocity: $velocity")
        // Таймер жизни ракеты
        handler.postDelayed({
            if (alive) {
                Log.d("HomingMissile", "Missile lifetime expired, removing from scene")
                this.setParent(null)
                alive = false
            }
        }, 5000)
    }

    fun isAlive(): Boolean = alive
    
    fun forceUpdate() {
        update()
    }

    private fun update() {
        if (!alive || !materialCreated) {
            Log.d("HomingMissile", "Update stopped: alive=$alive, materialCreated=$materialCreated")
            return
        }

        if (scene == null) {
            Log.d("HomingMissile", "Scene is null, waiting...")
            return
        }
        
        // Проверяем паузу - если игра на паузе, не обновляем позицию
        val activity = sceneView.context as? com.example.space_war_ar_demo.GameActivityVer2
        val isGamePaused = activity?.isPaused == true || weaponManager?.isPaused == true
        if (isGamePaused) {
            Log.d("HomingMissile", "Update paused due to game pause (local: ${weaponManager?.isPaused}, activity: ${activity?.isPaused})")
            return
        }

        // Обновляем позицию цели
        if (target.scene != null) {
            lastKnownTargetPos = target.worldPosition
        }

        // Гоминг: плавно корректируем направление к цели
        val toTarget = Vector3.subtract(lastKnownTargetPos, worldPosition)
        Log.d(
            "HomingMissile",
            "Update: missilePos=${worldPosition}, targetPos=$lastKnownTargetPos, toTargetLen=${toTarget.length()}, targetHash=${target.hashCode()}, targetType=${target::class.java.simpleName}, targetRenderable=${target.renderable}"
        )

        if (toTarget.length() < 0.1f) {
            // Цель достигнута
            Log.d(
                "HomingMissile",
                "Target reached, dealing damage ${params.damage} to target hash=${target.hashCode()}, type=${target::class.java.simpleName}"
            )
            target.callOnHit(params.damage)
            Log.d("HomingMissile", "Missile hit target, removing from scene")
            this.setParent(null)
            alive = false
            return
        }

        val toTargetDir = toTarget.normalized()
        val currentDir = velocity.normalized()

        // Интерполяция направления для плавного поворота
        val newDir = lerp(currentDir, toTargetDir, 0.16f * turnRate).normalized()
        velocity = newDir.scaled(params.speed * 0.5f)

        worldPosition = Vector3.add(worldPosition, velocity)
        Log.d("HomingMissile", "New position: ${worldPosition}, velocity: $velocity")

        // Удаление если вышла за границы
        if (worldPosition.z < -40f || worldPosition.z > 40f ||
            worldPosition.x < -40f || worldPosition.x > 40f ||
            worldPosition.y < -40f || worldPosition.y > 40f
        ) {
            Log.d("HomingMissile", "Missile out of bounds, removing")
            this.setParent(null)
            alive = false
            return
        }
    }

    // Линейная интерполяция между двумя векторами
    private fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
        return Vector3(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        )
    }

}


