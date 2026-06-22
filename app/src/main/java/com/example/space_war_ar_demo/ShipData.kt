package com.example.space_war_ar_demo

import android.content.Context
import android.content.SharedPreferences

class ShipData(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ship_data", Context.MODE_PRIVATE)

    var credits: Int
        get() = prefs.getInt("credits", 5000)
        set(value) = prefs.edit().putInt("credits", value).apply()

    var health: Int
        get() = prefs.getInt("health", 100)
        set(value) = prefs.edit().putInt("health", value).apply()

    var shield: Int
        get() = prefs.getInt("shield", 1)
        set(value) = prefs.edit().putInt("shield", value).apply()

    var damage: Int
        get() = prefs.getInt("damage", 10)
        set(value) = prefs.edit().putInt("damage", value).apply()

    var turretCount: Int
        get() = 999
        set(value) = prefs.edit().putInt("turretCount", value).apply()
    var sonarCount: Int
        get() = 999
        set(value) = prefs.edit().putInt("sonarCount", value).apply()

    var selectedWeapon: String
        get() = prefs.getString("selectedWeapon", "Пистолет") ?: "Пистолет"
        set(value) = prefs.edit().putString("selectedWeapon", value).apply()

    fun useTurret() {
        if (turretCount > 0) {
            turretCount--
        }
    }

    fun calculateHealth(): Int = 100

    fun takeDamage(damageAmount: Int): Int {
        val actualDamage = when {
            shield > 0 -> {
                val absorbedDamage = (damageAmount * 0.8f).toInt()
                val remainingDamage = damageAmount - absorbedDamage
                shield = (shield - 1).coerceAtLeast(0)
                if (remainingDamage > 0) {
                    health -= remainingDamage
                    remainingDamage
                } else {
                    0
                }
            }
            else -> {
                health -= damageAmount
                damageAmount
            }
        }
        if (health < 0) health = 0
        return actualDamage
    }

    fun fullHeal(): Boolean {
        val maxHealth = calculateHealth()
        val oldHealth = health
        health = maxHealth
        shield = 1
        return oldHealth < maxHealth
    }

    fun addCredits(amount: Int) {
        credits += amount
    }
}
