package com.example.space_war_ar_demo.boss_brain

import com.example.space_war_ar_demo.boss_brain.config.BossConfig
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3

/**
 * Менеджер для управления всеми боссами в игре.
 */
class BossManager {
    private val bosses = mutableMapOf<String, Boss>()
    
    /**
     * Создать босса по конфигурации
     */
    fun createBoss(
        config: BossConfig,
        position: Vector3,
        modelNode: Node?
    ): Boss {
        val boss = Boss(
            id = "boss_${System.currentTimeMillis()}",
            position = position,
            health = config.health,
            maxHealth = config.maxHealth,
            behavior = config.behavior,
            attacks = config.attacks,
            abilities = config.abilities,
            damage = config.damage
        ).apply {
            this.modelNode = modelNode
        }
        
        bosses[boss.id] = boss
        return boss
    }
    
    /**
     * Обновить всех боссов
     */
    fun updateBosses(
        deltaTime: Float,
        playerPosition: Vector3,
        playerVelocity: Vector3
    ) {
        bosses.values.forEach { boss ->
            if (boss.isAlive) {
                boss.update(deltaTime, playerPosition, playerVelocity)
            }
        }
        
        // Удаляем уничтоженных боссов
        val destroyedBosses = bosses.values.filter { !it.isAlive }
        destroyedBosses.forEach { removeBoss(it.id) }
    }
    
    /**
     * Нанести урон боссу
     */
    fun damageBoss(bossId: String, damage: Int) {
        bosses[bossId]?.takeDamage(damage)
    }
    
    /**
     * Удалить босса
     */
    fun removeBoss(bossId: String) {
        bosses.remove(bossId)
    }
    
    /**
     * Получить босса по ID
     */
    fun getBoss(bossId: String): Boss? = bosses[bossId]
    
    /**
     * Получить всех боссов
     */
    fun getAllBosses(): List<Boss> = bosses.values.toList()
    
    /**
     * Очистить всех боссов
     */
    fun clear() {
        bosses.clear()
    }
}



