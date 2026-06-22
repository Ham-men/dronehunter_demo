package com.example.space_war_ar_demo

object WeaponCatalog {
    data class Weapon(
        val name: String,
        val description: String,
        val iconRes: Int,
        val price: Int
    )

    val weapons = listOf(
        Weapon("Пистолет", "Стандартное оружие, базовый урон.", R.drawable.ic_minimap, 2000),
        Weapon("Лазер", "Мощный луч, пробивает щиты.", R.drawable.ic_energy, 3000),
        Weapon(
            "Гравити пушка",
            "Отталкивает врагов, наносит урон по площади.",
            R.drawable.ic_shield,
            4000
        ),
        Weapon("Ракета", "Высокий урон по одной цели.", R.drawable.ic_turret, 5000),
        Weapon("Турели", "Автоматический огонь по врагам.", R.drawable.ic_turret, 0),
        Weapon(
            "Сонар",
            "Импульс подсветки врагов и облаков. Можно купить до 10 раз.",
            R.drawable.ic_sonar,
            9000
        )
    )

    fun getByName(name: String) = weapons.find { it.name == name }
} 