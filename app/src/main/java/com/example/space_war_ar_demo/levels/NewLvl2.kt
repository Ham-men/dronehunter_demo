// NewLvl2.kt (мигрирован на новую систему боссов)
package com.example.space_war_ar_demo.levels

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.space_war_ar_demo.EventType
import com.example.space_war_ar_demo.GameEvent
import com.example.space_war_ar_demo.ShipData
import com.example.space_war_ar_demo.brain.DroneVisuals
import com.example.space_war_ar_demo.brain.HealthBarFactory
import com.example.space_war_ar_demo.boss_brain.BossManager
import com.example.space_war_ar_demo.boss_brain.BossVisuals
import com.example.space_war_ar_demo.boss_brain.config.BossConfigFactory
import com.example.space_war_ar_demo.enemy.types.DroneEnemy
import com.example.space_war_ar_demo.event.EventManager
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlin.random.Random

class NewLvl2 : BaseLevel(), com.example.space_war_ar_demo.physics.CollisionListener {
    private val TAG = "NewLvl2_2"

    private val levelData = com.example.space_war_ar_demo.levels.LevelBalance.getLevelData(2)

    private var waveNumber: Int = 0
    private var bossSpawned: Boolean = false
    private var timeToNextWave: Int = 0
    private var waveTimerHandler: Handler? = null
    private var waveTimerRunnable: Runnable? = null
    private val effectsHandler = Handler(Looper.getMainLooper())

    private var healthBarFactory: HealthBarFactory? = null

    private data class SimpleDrone(
        val node: com.example.space_war_ar_demo.physics.PhysicsNode,
        val id: String,
        var hp: Int = 1,
        var maxHp: Int = 1,
        var hb: HealthBarFactory.HealthBar? = null
    )
    private val drones = mutableListOf<SimpleDrone>()
    private val brainBot = com.example.space_war_ar_demo.brain.BrainBot_v4()
    private val enemyIdCounter = mutableMapOf<com.example.space_war_ar_demo.physics.PhysicsNode, String>()
    private var enemyIdCounterValue = 0
    private var playerLowHpWarned: Boolean = false
    private var bossLowHpWarned: Boolean = false

    // Новая система боссов
    private lateinit var bossManager: BossManager
    private lateinit var bossConfigFactory: BossConfigFactory
    private var bossId: String? = null
    private var bossPhysicsNode: com.example.space_war_ar_demo.physics.PhysicsNode? = null
    private var bossHpBar: HealthBarFactory.HealthBar? = null
    private val bossVisuals = BossVisuals()
    private var levelActive: Boolean = false

    override fun getCurrentWave(): Int = waveNumber
    override fun getTimeToNextWave(): Int = timeToNextWave

    override fun initialize(sceneView: ArSceneView) {
        this.sceneView = sceneView
        levelActive = true
        startLevelTimer()
        
        // Инициализация новой системы боссов
        bossManager = BossManager()
        bossConfigFactory = BossConfigFactory(
            context = sceneView.context,
            sceneView = sceneView,
            physicsEngine = physicsEngine,
            registerPhysicsNode = { node -> physicsEngine.addNode(node) }
        )
        
        healthBarFactory = HealthBarFactory(sceneView.context)
        brainBot.onEnemyDestroyed = { enemy ->
            Handler(Looper.getMainLooper()).post { handleBrainBotDroneDestroyed(enemy.id) }
        }

        val activity = sceneView.context as? com.example.space_war_ar_demo.GameActivityVer2
        activity?.weaponManager?.setDroneProvider { drones.map { it.node } }
        activity?.weaponManager?.setBossProvider { bossPhysicsNode }
        activity?.weaponManager?.setPhysicsEngineProvider { physicsEngine }

        physicsEngine.gravity = Vector3.zero()
        physicsEngine.listener = this

        waveNumber = 0
        startWaveCountdown()

        startBackgroundMusic(sceneView.context, com.example.space_war_ar_demo.R.raw.kill_streak_red_eye_storm)

        val context = sceneView.context
        activity?.showCutsceneDialog(
            title = context.getString(com.example.space_war_ar_demo.R.string.level_title_format, 2, LevelBalance.getLevelName(context, 2)),
            description = LevelBalance.getLevelDescription(context, 2),
            imageRes = com.example.space_war_ar_demo.R.drawable.enemy2,
            onOk = { }
        )
    }

    private fun startWaveCountdown() {
        timeToNextWave = if (waveNumber == 0) 0 else 15
        waveTimerHandler?.let { waveTimerRunnable?.let { r -> it.removeCallbacks(r) } }
        waveTimerHandler = Handler(Looper.getMainLooper())
        waveTimerRunnable = object : Runnable {
            override fun run() {
                if (!levelActive) return
                val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2
                activity?.runOnUiThread { activity.updateAllStats() }
                if (timeToNextWave > 0) {
                    timeToNextWave--
                    waveTimerHandler?.postDelayed(this, 1000)
                } else {
                    if (waveNumber < 3) {
                        waveNumber++
                        spawnWave(waveNumber)
                        if (waveNumber >= 3) {
                            timeToNextWave = 0
                            return
                        } else {
                            timeToNextWave = 15
                            waveTimerHandler?.postDelayed(this, 1000)
                        }
                    }
                }
            }
        }
        waveTimerHandler?.post(waveTimerRunnable!!)
    }

    private fun spawnWave(wave: Int) {
        val scene = sceneView?.scene ?: return
        val camera = scene.camera ?: return
        val dronesToSpawn = 5 + wave
        val context = sceneView?.context ?: return
        notifyEvent(EventType.NOTIFICATION, context.getString(com.example.space_war_ar_demo.R.string.notification_wave_start_format, wave))
        if (wave > 1) {
            notifyEvent(EventType.NOTIFICATION, context.getString(com.example.space_war_ar_demo.R.string.notification_drones_enhanced))
        }
        if (wave == levelData.bossAppearsOnWave && !bossSpawned) {
            createBoss()
            bossSpawned = true
        }
        val basePos = Vector3.add(camera.worldPosition, camera.forward.scaled(2.5f))
        val radius = 2.0
        for (i in 0 until dronesToSpawn) {
            val angle = (2 * Math.PI * i) / dronesToSpawn
            val x = basePos.x + (radius * Math.cos(angle)).toFloat()
            val verticalOffset = (0.5f + Random.nextFloat() * 0.5f) * if (Random.nextBoolean()) 1 else -1
            val y = (basePos.y + verticalOffset).coerceAtLeast(0.1f)
            val z = basePos.z + (radius * Math.sin(angle)).toFloat()
            val droneModel = com.example.space_war_ar_demo.models.Ship3D_dron2(sceneView?.context!!)
            val dronePhysicsNode = com.example.space_war_ar_demo.physics.PhysicsNode()
            dronePhysicsNode.worldPosition = Vector3(x, y, z)
            dronePhysicsNode.entityTag = "Enemy"
            dronePhysicsNode.addChild(droneModel)
            scene.addChild(dronePhysicsNode)
            physicsEngine.addNode(dronePhysicsNode)
            DroneVisuals.playSpawnMaterialize(droneModel, effectsHandler)
            val enemyId = "drone_${enemyIdCounterValue++}"
            enemyIdCounter[dronePhysicsNode] = enemyId
            val initialRotation = calculateRotationToTarget(Vector3(x, y, z), camera.worldPosition)
            val drone = brainBot.addDrone(
                id = enemyId,
                position = dronePhysicsNode.worldPosition,
                health = levelData.droneHealth,
                modelNode = droneModel,
                targetAltitude = y
            )
            drone.setInitialRotation(initialRotation)
            val simple = SimpleDrone(
                node = dronePhysicsNode,
                id = enemyId,
                hp = levelData.droneHealth,
                maxHp = levelData.droneHealth,
                hb = null
            )
            simple.hb = healthBarFactory?.createAbove(dronePhysicsNode, width = 0.3f, height = 0.04f, yOffset = 0.5f, color = Color(0f, 1f, 0f))
            drones.add(simple)
            enemyManager.spawnEnemy(dronePhysicsNode)
        }
        Log.d(TAG, "Spawned wave $wave with $dronesToSpawn drones")
        (sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2)?.let { activity ->
            activity.runOnUiThread { activity.updateAllStats() }
        }
    }

    private fun calculateRotationToTarget(currentPos: Vector3, targetPos: Vector3): Quaternion {
        val direction = Vector3.subtract(targetPos, currentPos).normalized()
        val yaw = kotlin.math.atan2(direction.x, direction.z)
        return Quaternion.axisAngle(Vector3(0f, 1f, 0f), Math.toDegrees(yaw.toDouble()).toFloat())
    }

    private fun createBoss() {
        val scene = sceneView?.scene ?: return
        val camera = scene.camera ?: return
        val ctx = sceneView?.context ?: return
        
        // Получаем конфигурацию босса для уровня 2
        val bossConfig = bossConfigFactory.createConfigForLevel(2) ?: return
        
        val bossModel = com.example.space_war_ar_demo.models.Ship3D_boss1(ctx)
        val bossPosition = Vector3.add(camera.worldPosition, camera.forward.scaled(3f))
        bossModel.worldPosition = bossPosition
        
        // Создаем физический узел для босса
        val bossPhysNode = com.example.space_war_ar_demo.physics.PhysicsNode()
        bossPhysNode.worldPosition = bossPosition
        bossPhysNode.entityTag = "Boss"
        bossPhysNode.addChild(bossModel)
        scene.addChild(bossPhysNode)
        physicsEngine.addNode(bossPhysNode)
        bossPhysicsNode = bossPhysNode
        
        // Создаем босса через новую систему
        val boss = bossManager.createBoss(
            config = bossConfig,
            position = bossPosition,
            modelNode = bossModel as Node
        )
        bossId = boss.id
        
        // Настройка колбэков
        boss.setOnDestroyedCallback { destroyedBoss ->
            bossHpBar?.root?.parent?.removeChild(bossHpBar!!.root)
            scene.removeChild(bossPhysNode)
            physicsEngine.removeNode(bossPhysNode)
            enemyManager.removeEnemy(bossPhysNode)
            addDestroyedBoss()
            bossId = null
            bossPhysicsNode = null
        }
        
        // Создаем полоску здоровья
        bossHpBar = healthBarFactory?.createAbove(bossModel, width = 0.8f, height = 0.06f, yOffset = 0.9f, color = Color(1f, 0f, 0f))
        
        Log.d(TAG, "Boss spawned with ${boss.health} HP")
        val context = sceneView?.context ?: return
        notifyEvent(EventType.WARNING, context.getString(com.example.space_war_ar_demo.R.string.notification_boss_hyperjump))
    }

    override fun update() {
        if (!levelActive) return
        physicsEngine.update(0.016f)
        val camera = sceneView?.scene?.camera ?: return
        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2
        activity?.shipData?.let { checkLowHpWarnings(it) }
        brainBot.updatePlayerData(position = camera.worldPosition, velocity = camera.forward, isMoving = true)
        brainBot.updateAI(0.016f)
        for (drone in drones) {
            val enemy = brainBot.getEnemy(drone.id) as? DroneEnemy ?: continue
            drone.node.worldPosition = enemy.position
            drone.hp = enemy.health
            drone.hb?.let { hb ->
                val color = resolveHealthBarColor(enemy.uiState)
                healthBarFactory?.update(hb, drone.hp, drone.maxHp, color)
                healthBarFactory?.faceTowards(hb, camera.worldPosition)
            }
            if (enemy.shouldAttack(camera.worldPosition)) {
                enemy.attack()
                fireDroneBullet(drone.node, camera.worldPosition)
            }
        }
        // Обновляем боссов через новую систему
        bossManager.updateBosses(
            deltaTime = 0.016f,
            playerPosition = camera.worldPosition,
            playerVelocity = camera.forward
        )
        
        // Обновляем позицию физического узла босса
        bossId?.let { id ->
            val boss = bossManager.getBoss(id)
            if (boss != null && boss.isAlive) {
                bossPhysicsNode?.worldPosition = boss.position
                bossHpBar?.let {
                    healthBarFactory?.update(it, boss.health, boss.maxHealth, Color(1f, 0f, 0f))
                    healthBarFactory?.faceTowards(it, camera.worldPosition)
                }
            }
        }
        checkWinLoseConditions()
    }

    private fun fireDroneBullet(fromNode: com.example.space_war_ar_demo.physics.PhysicsNode, targetPos: Vector3) {
        val scene = sceneView?.scene ?: return
        MaterialFactory.makeOpaqueWithColor(sceneView?.context!!, Color(1f, 1f, 0.2f)).thenAccept { material ->
            val renderable = ShapeFactory.makeSphere(0.04f, Vector3.zero(), material)
            val dir = Vector3.subtract(targetPos, fromNode.worldPosition).normalized()
            val phys = com.example.space_war_ar_demo.physics.ProjectileFactory.createBullet(
                material = renderable,
                position = fromNode.worldPosition,
                forward = dir,
                damage = levelData.droneDamage
            )
            phys.entityTag = com.example.space_war_ar_demo.physics.ProjectileFactory.ProjectileData("EnemyBullet", levelData.droneDamage)
            scene.addChild(phys)
            physicsEngine.addNode(phys)
            // Проверяем попадание по игроку (камера) — так как игрок не физический узел
            val h = Handler(Looper.getMainLooper())
            val r = object : Runnable {
                override fun run() {
                    if (phys.scene == null) return
                    val cam = scene.camera ?: return
                    val dist = Vector3.subtract(phys.worldPosition, cam.worldPosition).length()
                    if (dist < 0.4f) {
                        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2
                        activity?.shipData?.takeDamage(levelData.droneDamage)
                        scene.removeChild(phys)
                        return
                    }
                    if (Vector3.subtract(phys.worldPosition, fromNode.worldPosition).length() > 40f) {
                        scene.removeChild(phys)
                        return
                    }
                    h.postDelayed(this, 16)
                }
            }
            h.post(r)
        }
    }

    override fun onCollision(nodeA: com.example.space_war_ar_demo.physics.PhysicsNode, nodeB: com.example.space_war_ar_demo.physics.PhysicsNode) {
        val scene = sceneView?.scene ?: return
        fun isPlayerProjectile(node: com.example.space_war_ar_demo.physics.PhysicsNode): Boolean {
            if (node.entityTag is com.example.space_war_ar_demo.physics.ProjectileFactory.ProjectileData) {
                val pd = node.entityTag as com.example.space_war_ar_demo.physics.ProjectileFactory.ProjectileData
                return !pd.type.startsWith("Enemy")
            }
            return when (node.entityTag) {
                "Laser", "Missile", "PlayerBullet", "Bullet" -> true
                else -> false
            }
        }
        fun projectileDamage(node: com.example.space_war_ar_demo.physics.PhysicsNode): Int {
            val pd = node.entityTag as? com.example.space_war_ar_demo.physics.ProjectileFactory.ProjectileData
            if (pd != null) return pd.damage
            return when (node.entityTag) {
                "Laser" -> getLaserParams().damage
                "Missile" -> getMissileParams().damage
                "PlayerBullet", "Bullet" -> getBulletParams().damage
                else -> 0
            }
        }
        val (proj, target) = when {
            isPlayerProjectile(nodeA) && (nodeB.entityTag == "Enemy" || nodeB.entityTag == "Boss") -> nodeA to nodeB
            isPlayerProjectile(nodeB) && (nodeA.entityTag == "Enemy" || nodeA.entityTag == "Boss") -> nodeB to nodeA
            else -> return
        }
        val dmg = projectileDamage(proj)
        if (dmg > 0) damageDefender(target, dmg)
        scene.removeChild(proj); physicsEngine.removeNode(proj)
    }

    override fun damageDefender(targetNode: com.example.space_war_ar_demo.physics.PhysicsNode, damage: Int) {
        val simple = drones.find { it.node == targetNode }
        if (simple != null) {
            brainBot.damageEnemy(simple.id, damage)
            val enemy = brainBot.getEnemy(simple.id) as? DroneEnemy
            simple.hp = enemy?.health ?: (simple.hp - damage)
            if (simple.hp > 0) {
                val color = enemy?.uiState?.let { resolveHealthBarColor(it) } ?: Color(0f, 1f, 0f)
                simple.hb?.let { healthBarFactory?.update(it, simple.hp, simple.maxHp, color) }
            }
            return
        }
        // Обработка урона боссу через новую систему
        if (bossPhysicsNode == targetNode && bossId != null) {
            bossManager.damageBoss(bossId!!, damage)
            val boss = bossManager.getBoss(bossId!!)
            if (boss != null) {
                // Визуальный эффект урона
                boss.modelNode?.let { bossVisuals.startDamageFlash(it) }
                bossHpBar?.let { healthBarFactory?.update(it, boss.health, boss.maxHealth, Color(1f, 0f, 0f)) }
                if (!boss.isAlive) {
                    // Удаляем босса из сцены и фиксируем победу
                    sceneView?.scene?.removeChild(targetNode)
                    physicsEngine.removeNode(targetNode)
                    enemyManager.removeEnemy(targetNode)
                    bossId = null
                    bossPhysicsNode = null
                    onLevelWin()
                }
            }
        }
    }

    private fun checkWinLoseConditions() {
        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2 ?: return
        val shipData = activity.shipData
        if (shipData.health <= 0) {
            onLevelLose()
        } else if (bossSpawned && bossId == null && drones.isEmpty()) {
            onLevelWin()
        }
    }

    override fun cleanup() {
        super.cleanup()
        waveTimerRunnable?.let { waveTimerHandler?.removeCallbacks(it) }
        waveTimerHandler = null
        waveTimerRunnable = null
        effectsHandler.removeCallbacksAndMessages(null)
        physicsEngine.listener = null
        drones.forEach { dn -> dn.hb?.root?.parent?.removeChild(dn.hb!!.root) }
        drones.clear()
        brainBot.clearAllEnemies()
        bossManager.clear()
        bossHpBar?.root?.parent?.removeChild(bossHpBar!!.root)
        bossHpBar = null
        bossId = null
        bossPhysicsNode = null
        playerLowHpWarned = false
        bossLowHpWarned = false
        levelActive = false
    }

    override fun getDronesForTurret(): List<com.example.space_war_ar_demo.physics.PhysicsNode> = drones.map { it.node }

    private fun handleBrainBotDroneDestroyed(enemyId: String) {
        val iterator = drones.iterator()
        while (iterator.hasNext()) {
            val drone = iterator.next()
            if (drone.id == enemyId) {
                val scene = sceneView?.scene
                val context = sceneView?.context
                if (scene != null && context != null) {
                    DroneVisuals.playExplosionDebris(
                        scene = scene,
                        context = context,
                        handler = effectsHandler,
                        position = drone.node.worldPosition
                    )
                }
                drone.hb?.root?.parent?.removeChild(drone.hb!!.root)
                sceneView?.scene?.removeChild(drone.node)
                physicsEngine.removeNode(drone.node)
                enemyManager.removeEnemy(drone.node)
                iterator.remove()
                addDestroyedDrone()
                break
            }
        }
    }


    private fun notifyEvent(type: EventType, message: String) {
        val ctx = sceneView?.context ?: return
        EventManager.showEvent(ctx, GameEvent(type, message))
    }

    private fun checkLowHpWarnings(shipData: ShipData) {
        val max = shipData.calculateHealth().coerceAtLeast(1)
        val context = sceneView?.context ?: return
        if (!playerLowHpWarned && shipData.health <= (max * 0.25f)) {
            notifyEvent(EventType.WARNING, context.getString(com.example.space_war_ar_demo.R.string.notification_ship_critical))
            playerLowHpWarned = true
        } else if (playerLowHpWarned && shipData.health > (max * 0.4f)) {
            playerLowHpWarned = false
        }

        bossId?.let { id ->
            val boss = bossManager.getBoss(id)
            if (boss != null && !bossLowHpWarned && boss.health <= (boss.maxHealth * 0.25f)) {
                notifyEvent(EventType.NOTIFICATION, context.getString(com.example.space_war_ar_demo.R.string.notification_boss_weakened))
                bossLowHpWarned = true
            } else if (boss == null || boss.health > (boss.maxHealth * 0.4f)) {
                bossLowHpWarned = false
            }
        }
    }

    private fun resolveHealthBarColor(state: DroneEnemy.UiState): Color = when (state) {
        DroneEnemy.UiState.RETREAT -> Color(1f, 0.75f, 0f)
        DroneEnemy.UiState.CHARGING -> Color(0.9f, 0f, 1f)
        else -> Color(0f, 1f, 0f)
    }
}