package com.example.space_war_ar_demo.levels

import android.content.Context
import com.example.space_war_ar_demo.R

object LevelBalance {

    data class LevelData(
        val levelNumber: Int,
        val droneHealth: Int,
        val droneDamage: Int,
        val bossHealth: Int,
        val bossDamage: Int,
        val maxWaves: Int,
        val droneCountPerWave: Int,
        val bossAppearsOnWave: Int? = null,
        val specialMechanics: List<String> = emptyList(),
        val bossName: String? = null,
        val storyline: String = "",
        val objective: String = ""
    )

    fun getLevelData(levelNumber: Int): LevelData {
        return LevelData(
            2, 5, 1, 300, 2, 4, 4, 3,
            listOf("Первые враги", "Увеличенное количество"),
            bossName = null,
            storyline = "Один из земных кораблей перешёл на сторону врага. Остановите предателя и его флот. Отбивайтесь от дронов до 3 волны. Далее появится предатель (фиолетовый корабль), которого нужно уничтожить.",
            objective = "Отбиваться от флота и уничтожить предателя после 3 волны."
        )
    }

    fun getLevelName(context: Context, levelNumber: Int): String {
        return context.getString(R.string.level_name_2)
    }

    fun getLevelDescription(context: Context, levelNumber: Int): String {
        val story = context.getString(R.string.level_storyline_2)
        val obj = context.getString(R.string.level_objective_2)
        return "$story\n${context.getString(R.string.level_objective_label)}: $obj"
    }
}
