package com.example.space_war_ar_demo.boss_brain.ability

interface BossAbility {
    val name: String get() = this::class.java.simpleName
    val abilityName: String get() = name
    val isActive: Boolean get() = false
    val cooldown: Long get() = 0L
    var lastUsedTime: Long
    fun activate(boss: Any) {}
    fun deactivate(boss: Any) {}
    fun update(boss: Any, deltaTime: Float) {}
}
