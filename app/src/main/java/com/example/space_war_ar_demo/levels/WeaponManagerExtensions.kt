package com.example.space_war_ar_demo.levels

import com.google.ar.sceneform.Node

fun Node.callOnHit(damage: Int) {
    android.util.Log.d("WeaponManagerExtensions", "callOnHit with damage $damage on ${this.hashCode()}")
    val gameActivity = (this.scene?.view?.context as? com.example.space_war_ar_demo.GameActivityVer2)
    if (gameActivity == null) {
        android.util.Log.e("WeaponManagerExtensions", "gameActivity is null!")
        return
    }
    val lvl = gameActivity.getCurrentLevel
    if (lvl == null) {
        android.util.Log.e("WeaponManagerExtensions", "lvl is null!")
        return
    }
    when (lvl) {
        is NewLvl2 -> {
            val physicsNode = this as? com.example.space_war_ar_demo.physics.PhysicsNode
            if (physicsNode == null) {
                android.util.Log.w("WeaponManagerExtensions", "[Lvl2] Hit target is not PhysicsNode, damage ignored")
                return
            }
            lvl.damageDefender(physicsNode, damage)
        }
        else -> {
            android.util.Log.w("WeaponManagerExtensions", "Unhandled level type: ${lvl.javaClass.simpleName}")
        }
    }
}
