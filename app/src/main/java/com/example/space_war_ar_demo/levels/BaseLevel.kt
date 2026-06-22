package com.example.space_war_ar_demo.levels

import android.content.Context
import android.media.MediaPlayer
import com.example.space_war_ar_demo.BulletParams
import com.example.space_war_ar_demo.EventType
import com.example.space_war_ar_demo.GameEvent
import com.example.space_war_ar_demo.GravityBallParams
import com.example.space_war_ar_demo.LaserParams
import com.example.space_war_ar_demo.MissileParams
import com.example.space_war_ar_demo.R
import com.example.space_war_ar_demo.event.EventManager
import com.example.space_war_ar_demo.physics.PhysicsEngine
import com.example.space_war_ar_demo.physics.PhysicsNode
import com.google.ar.sceneform.ArSceneView

interface LevelEventListener {
    fun onWaveStart(wave: Int) {}
    fun onWaveEnd(wave: Int) {}
    fun onEnemyDefeated(enemy: PhysicsNode) {}
    fun onLevelWin() {}
    fun onLevelLose() {}
}

open class WaveManager(var maxWaves: Int = 5) {
    var currentWave: Int = 0
    var listener: LevelEventListener? = null
    fun startWaves() {
        currentWave = 1
        listener?.onWaveStart(currentWave)
    }

    fun nextWave() {
        if (currentWave < maxWaves) {
            currentWave++
            listener?.onWaveStart(currentWave)
        } else {
            listener?.onWaveEnd(currentWave)
        }
    }
}

open class EnemyManager {
    val enemies = mutableListOf<PhysicsNode>()
    var listener: LevelEventListener? = null
    fun spawnEnemy(enemy: PhysicsNode) {
        enemies.add(enemy)
    }

    fun removeEnemy(enemy: PhysicsNode) {
        enemies.remove(enemy)
        listener?.onEnemyDefeated(enemy)
    }

    fun getAliveEnemies(): List<PhysicsNode> = enemies.toList()
}

abstract class BaseLevel : LevelEventListener {
    private var lives = 20
    private var score = 0
    private var isGameOver = false

    // Базовая переменная для музыки
    protected var backgroundMusic: MediaPlayer? = null

    // === СИСТЕМА СТАТИСТИКИ И ДЕНЕГ ===
    var levelStartTime: Long = 0L
    var destroyedDrones: Int = 0
    var destroyedBosses: Int = 0
    var destroyedBases: Int = 0
    var totalEarnedCredits: Int = 0

    // Физический движок для всех уровней
    internal open val physicsEngine = PhysicsEngine()
    protected val waveManager = WaveManager()
    protected val enemyManager = EnemyManager()
    var sceneView: ArSceneView? = null

    // Флаг: игрок получил урон на уровне
    protected var playerTookDamage: Boolean = false

    open fun setLives(value: Int) {
        lives = value
    }

    open fun setScore(value: Int) {
        score = value
    }

    open fun setIsGameOver(value: Boolean) {
        isGameOver = value
    }

    // Методы для статистики
    open fun startLevelTimer() {
        levelStartTime = System.currentTimeMillis()
        destroyedDrones = 0
        destroyedBosses = 0
        destroyedBases = 0
        totalEarnedCredits = 0
    }

    open fun addDestroyedDrone() {
        destroyedDrones++
        val credits = 1000
        totalEarnedCredits += credits
        addCreditsToPlayer(credits)
        android.util.Log.d(
            "BaseLevel",
            "Drone destroyed! +$credits credits. Total earned: $totalEarnedCredits"
        )

    }

    open fun addDestroyedBoss() {
        destroyedBosses++
        val credits = 10000
        totalEarnedCredits += credits
        addCreditsToPlayer(credits)
        android.util.Log.d(
            "BaseLevel",
            "Boss destroyed! +$credits credits. Total earned: $totalEarnedCredits"
        )
    }

    open fun addDestroyedBase() {
        destroyedBases++
        val credits = 10000
        totalEarnedCredits += credits
        addCreditsToPlayer(credits)
        android.util.Log.d(
            "BaseLevel",
            "Base destroyed! +$credits credits. Total earned: $totalEarnedCredits"
        )
    }

    fun addCreditsToPlayer(amount: Int) {
        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2
        activity?.shipData?.addCredits(amount)
    }

    open fun getLevelTime(): Long {
        return if (levelStartTime > 0) System.currentTimeMillis() - levelStartTime else 0L
    }

    open fun getCurrentWave(): Int {
        return waveManager.currentWave
    }

    open fun getTimeToNextWave(): Int {
        // По умолчанию возвращаем 0, чтобы показывать общее время уровня
        // Уровни с волнами должны переопределить этот метод
        return 0
    }

    open fun getLevelTimeFormatted(): String {
        val timeMs = getLevelTime()
        val seconds = (timeMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    // Убираем дублирующий метод getDestroyedDronesCount(), так как свойство destroyedDrones уже доступно
    // Убираем дублирующий метод getTotalEarnedCredits(), так как свойство totalEarnedCredits уже доступно

    abstract fun initialize(sceneView: ArSceneView)
    abstract fun update()

    // Методы для паузы
    open fun pause() {
        // Останавливаем фоновую музыку
        backgroundMusic?.pause()

        // Останавливаем все таймеры и анимации
        // Конкретная реализация в наследниках
    }

    open fun resume() {
        // Возобновляем фоновую музыку
        backgroundMusic?.start()

        // Возобновляем все таймеры и анимации
        // Конкретная реализация в наследниках
    }

    // Общая реализация стрельбы для всех уровней
    open fun onFire() {
        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2 ?: return
        val camera = sceneView?.scene?.camera ?: return
        val selectedWeapon = activity.shipData.selectedWeapon

        android.util.Log.d("BaseLevel", "onFire called with weapon: $selectedWeapon")

        // Получаем выбранного дрона для ракеты
        val targetedDrone = try {
            val field = activity.javaClass.getDeclaredField("targetedDrone")
            field.isAccessible = true
            field.get(activity) as? PhysicsNode
        } catch (e: Exception) {
            null
        }

        when (selectedWeapon) {
            "Гравити пушка" -> {
                android.util.Log.d("BaseLevel", "Firing gravity gun")
                activity.weaponManager.fireGravityBall(
                    camera.worldPosition,
                    camera.forward,
                    getGravityBallParams()
                )
            }

            "Пистолет" -> {
                android.util.Log.d("BaseLevel", "Firing pistol")
                activity.weaponManager.fireBullet(
                    camera.worldPosition,
                    camera.forward,
                    getBulletParams()
                )
            }

            "Лазер" -> {
                android.util.Log.d("BaseLevel", "Firing laser")
                activity.weaponManager.fireLaser(
                    camera.worldPosition,
                    camera.forward,
                    getLaserParams()
                )
            }

            "Ракета" -> {
                android.util.Log.d("BaseLevel", "Firing missile")
                activity.weaponManager.fireMissile(
                    camera.worldPosition,
                    camera.forward,
                    getMissileParams(),
                    targetedDrone
                )
            }

            else -> {
                android.util.Log.d(
                    "BaseLevel",
                    "Unknown weapon: $selectedWeapon, using pistol as default"
                )
                activity.weaponManager.fireBullet(
                    camera.worldPosition,
                    camera.forward,
                    getBulletParams()
                )
            }
        }
    }

    open fun cleanup() {
        try {
            // Восстанавливаем здоровье игрока
            val context = sceneView?.context
            if (context is com.example.space_war_ar_demo.GameActivityVer2) {
                val shipData = context.shipData
                shipData.fullHeal()
                android.util.Log.d(
                    "BaseLevel",
                    "Health restored in cleanup: ${shipData.health}/${shipData.calculateHealth()}"
                )
            }

            // Останавливаем фоновую музыку
            stopBackgroundMusic()

            // Очищаем физический движок
            physicsEngine.cleanup()

            // Очищаем все объекты сцены
            val scene = sceneView?.scene
            if (scene != null) {
                val children = scene.children.toList()
                children.forEach { child ->
                    scene.removeChild(child)
                }
            }

            // Останавливаем все таймеры и обработчики
            waveManager.listener = null
            enemyManager.listener = null

            android.util.Log.d("BaseLevel", "Level cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("BaseLevel", "Error during cleanup", e)
        }
    }

    abstract fun getDronesForTurret(): List<PhysicsNode>
    open fun damageDefender(targetNode: PhysicsNode, damage: Int) {}

    fun getLives(): Int = lives
    fun getScore(): Int = score
    fun isGameOver(): Boolean = isGameOver

    open fun isWin(): Boolean = false
    open fun getTime(): Int = 0
    open fun getReward(): Int = 0

    fun decreaseLives() {
        if (lives > 0) {
            lives--
            if (lives <= 0) {
                isGameOver = true
            }
        }
    }

    fun increaseScore() {
        score++
    }

    // Публичный метод для добавления узлов в физический движок
    fun addPhysicsNode(node: PhysicsNode) {
        physicsEngine.addNode(node)
    }

    override fun onWaveStart(wave: Int) {
        val context = (sceneView?.context ?: return)
        EventManager.showEvent(context, GameEvent(EventType.NOTIFICATION, context.getString(com.example.space_war_ar_demo.R.string.notification_wave_start_format, wave)))
    }

    override fun onLevelWin() {
        val context = (sceneView?.context ?: return)
        stopBackgroundMusic()
        EventManager.showEvent(context, GameEvent(EventType.LEVEL_WIN, context.getString(com.example.space_war_ar_demo.R.string.notification_level_win)))


        // Показываем диалог победы
        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2
        activity?.showWinDialog()
    }

    override fun onLevelLose() {
        val context = (sceneView?.context ?: return)
        stopBackgroundMusic()
        EventManager.showEvent(context, GameEvent(EventType.LEVEL_LOSE, context.getString(com.example.space_war_ar_demo.R.string.notification_level_lose)))

        // Показываем диалог поражения
        val activity = sceneView?.context as? com.example.space_war_ar_demo.GameActivityVer2
        activity?.showLoseDialog()
    }

    open fun getBulletParams(): BulletParams {
        val params = BulletParams()
        android.util.Log.d("BaseLevel", "getBulletParams called: $params")
        return params
    }

    open fun getLaserParams(): LaserParams {
        val params = LaserParams()
        android.util.Log.d("BaseLevel", "getLaserParams called: $params")
        return params
    }

    open fun getGravityBallParams(): GravityBallParams = GravityBallParams()
    open fun getMissileParams(): MissileParams {
        val params = MissileParams()
        android.util.Log.d("BaseLevel", "getMissileParams called: $params")
        return params
    }

    // Метод для удаления всех дочерних элементов сцены
    fun removeAllChildren() {
        val scene = sceneView?.scene
        if (scene != null) {
            val children = scene.children.toList()
            children.forEach { child ->
                scene.removeChild(child)
            }
        }
    }

    // Базовые методы для управления музыкой
    protected open fun startBackgroundMusic(
        context: Context,
        musicResId: Int = R.raw.untitled_project_target_locked
    ) {
        try {
            backgroundMusic = MediaPlayer.create(context, musicResId)

            backgroundMusic?.isLooping = true
            // apply volume from settings
            val volPercent = com.example.space_war_ar_demo.SettingsManager.getMusicVolume(context)
            val vol = (volPercent.coerceIn(0, 100)) / 100f
            backgroundMusic?.setVolume(vol, vol)
            backgroundMusic?.start()
            android.util.Log.d(
                "BaseLevel",
                "Background music started successfully with resource ID: $musicResId"
            )
        } catch (e: Exception) {
            android.util.Log.e("BaseLevel", "Error starting background music: ${e.message}")
        }
    }

    protected open fun stopBackgroundMusic() {
        try {
            backgroundMusic?.stop()
            backgroundMusic?.release()
            backgroundMusic = null
            android.util.Log.d("BaseLevel", "Background music stopped successfully")
        } catch (e: Exception) {
            android.util.Log.e("BaseLevel", "Error stopping background music: ${e.message}")
        }
    }
}