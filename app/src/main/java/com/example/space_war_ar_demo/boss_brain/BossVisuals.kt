package com.example.space_war_ar_demo.boss_brain

import android.os.Handler
import android.os.Looper
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3

/**
 * Визуальные эффекты для боссов.
 * Объединяет функционал из старого BossVisualEffects.kt
 */
class BossVisuals {
    private val handler = Handler(Looper.getMainLooper())
    // Запоминаем исходный масштаб для каждого узла, чтобы не накапливать увеличение
    private val originalScales = mutableMapOf<Node, Vector3>()
    
    /**
     * Эффект урона - мигание/пульсация
     */
    fun startDamageFlash(bossNode: Node, duration: Long = 500L) {
        // Берём базовый масштаб, сохранённый при первом попадании
        val originalScale = originalScales.getOrPut(bossNode) { Vector3(bossNode.localScale) }
        
        // Быстрая пульсация при получении урона
        bossNode.localScale = Vector3(originalScale.x * 0.8f, originalScale.y * 0.8f, originalScale.z * 0.8f)
        
        handler.postDelayed({
            bossNode.localScale = Vector3(
                originalScale.x * 1.1f,
                originalScale.y * 1.1f,
                originalScale.z * 1.1f
            )
        }, 100)
        
        handler.postDelayed({
            bossNode.localScale = originalScale
        }, 200)
        
        // Сброс после полной длительности
        handler.postDelayed({
            bossNode.localScale = originalScale
            // После завершения анимации возвращаем узел к исходному размеру
            // и забываем сохранённый масштаб
            originalScales.remove(bossNode)
        }, duration)
    }
    
    /**
     * Эффект смены фазы
     */
    fun startPhaseChangeEffect(bossNode: Node, phaseNumber: Int) {
        val originalScale = Vector3(bossNode.localScale)
        
        // Пульсация при смене фазы
        when (phaseNumber) {
            2 -> startShieldEffect(bossNode)
            3 -> startInvisibilityEffect(bossNode)
        }
        
        // Общая пульсация при смене фазы
        for (i in 0..2) {
            handler.postDelayed({
                bossNode.localScale = Vector3(originalScale.x * 1.2f, originalScale.y * 1.2f, originalScale.z * 1.2f)
            }, i * 100L)
            handler.postDelayed({
                bossNode.localScale = originalScale
            }, i * 100L + 50L)
        }
    }
    
    /**
     * Эффект щита (при переходе во 2 фазу)
     */
    private fun startShieldEffect(bossNode: Node) {
        // Визуальный эффект активации щита
        // Может быть расширен для показа щита
    }
    
    /**
     * Эффект невидимости (при переходе в 3 фазу)
     */
    private fun startInvisibilityEffect(bossNode: Node) {
        // Визуальный эффект невидимости
        // Может быть расширен для показа эффекта исчезновения
    }
}



