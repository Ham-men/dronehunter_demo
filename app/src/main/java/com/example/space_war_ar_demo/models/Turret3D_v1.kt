package com.example.space_war_ar_demo.models

import android.content.Context
import android.util.Log
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.Vertex

/**
 * Класс для создания 3D модели турели из простых фигур.
 * Фигуры: шар, куб, пирамида, параллелепипед.
 */
class Turret3D_v1(
    private val context: Context,
    private val scale: Float = 1f // Переменная для регулировки масштаба модели
) : Node() {
    // Основные материалы
    private var baseMaterial: Material? = null
    private var accentMaterial: Material? = null
    private var glassMaterial: Material? = null

    init {
        Log.d("Turret3D", "Start creating materials")
        MaterialFactory.makeOpaqueWithColor(context, Color(0.2f, 0.2f, 0.8f))
            .thenAccept { mat ->
                baseMaterial = mat
                Log.d("Turret3D", "Base material created")
                MaterialFactory.makeOpaqueWithColor(context, Color(0.8f, 0.8f, 0.2f))
                    .thenAccept { accent ->
                        accentMaterial = accent
                        Log.d("Turret3D", "Accent material created")
                        MaterialFactory.makeTransparentWithColor(
                            context,
                            Color(0.5f, 0.8f, 1.0f, 0.5f)
                        )
                            .thenAccept { glass ->
                                glassMaterial = glass
                                Log.d("Turret3D", "Glass material created, building ship")
                                try {
                                    buildShip()
                                    Log.d("Turret3D", "Ship built successfully")
                                } catch (e: Exception) {
                                    Log.e("Turret3D", "Error building ship", e)
                                }
                            }
                            .exceptionally { throwable ->
                                Log.e("Turret3D", "Error creating glass material", throwable)
                                null
                            }
                    }
                    .exceptionally { throwable ->
                        Log.e("Turret3D", "Error creating accent material", throwable)
                        null
                    }
            }
            .exceptionally { throwable ->
                Log.e("Turret3D", "Error creating base material", throwable)
                null
            }
    }

    /**
     * Собирает турель из простых фигур.
     */
    private fun buildShip() {

        // 1. Центральный корпус (плоский горизонтальный круг)
        val body =
            ShapeFactory.makeCylinder(0.24f * scale, 0.05f * scale, Vector3.zero(), baseMaterial)
        val bodyNode = Node().apply {
            renderable = body
            localPosition = Vector3(0f, 0f, 0f)
        }
        addChild(bodyNode)

        val block1 = ShapeFactory.makeCube(
            Vector3(0.2f * scale, 0.1f * scale, 0.1f * scale),
            Vector3.zero(),
            accentMaterial
        )
        val block1Node = Node().apply {
            renderable = block1
            localPosition = Vector3(0f, 0.05f * scale, 0f)
        }

        val block2 = ShapeFactory.makeCube(
            Vector3(0.5f * scale, 0.2f * scale, 0.1f * scale),
            Vector3.zero(),
            accentMaterial
        )
        val block2node = Node().apply {
            renderable = block2
            localPosition = Vector3(0f, 0.2f * scale, 0f)
        }
        //пушка слева и справа
        val block3 = ShapeFactory.makeCube(
            Vector3(0.1f * scale, 0.1f * scale, 0.3f * scale),
            Vector3.zero(),
            accentMaterial
        )
        val block3node = Node().apply {
            renderable = block3
            localPosition = Vector3(-0.2f * scale, 0.2f * scale, 0f)
        }
        val block4node = Node().apply {
            renderable = block3
            localPosition = Vector3(+0.2f * scale, 0.2f * scale, 0f)
        }

        addChild(block1Node)
        addChild(block2node)
        addChild(block3node)
        addChild(block4node)

    }

    /**
     * Генерация пирамиды (4-сторонней) как ModelRenderable.
     * @param size размеры пирамиды (ширина, высота, глубина)
     * @param material материал
     */
    private fun makePyramid(size: Vector3, material: Material?): ModelRenderable? {
        if (material == null) return null
        val w = size.x / 2f
        val h = size.y
        val d = size.z / 2f
        // Вершины
        val positions = listOf(
            Vector3(-w, 0f, -d), // 0
            Vector3(w, 0f, -d),  // 1
            Vector3(w, 0f, d),   // 2
            Vector3(-w, 0f, d),  // 3
            Vector3(0f, h, 0f)   // 4 (верхушка)
        )
        val vertices = positions.map { pos ->
            Vertex.builder().setPosition(pos).setNormal(Vector3(0f, 1f, 0f)).build()
        }
        // Индексы треугольников
        val triangleIndices = listOf(
            // Боковые грани
            0, 1, 4,
            1, 2, 4,
            2, 3, 4,
            3, 0, 4,
            // Основание (два треугольника)
            0, 1, 2,
            0, 2, 3
        )
        val submesh = RenderableDefinition.Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material)
            .build()
        val definition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(listOf(submesh))
            .build()
        return ModelRenderable.builder()
            .setSource(definition)
            .build()
            .get() // блокирующе, т.к. вызывается после создания материала
    }

    /**
     * Получить текущий масштаб модели
     * @return текущий масштаб
     */
    fun getScale(): Float = scale

    /**
     * Установить масштаб модели (требует пересборки модели)
     * @param newScale новый масштаб
     */
    fun setScale(newScale: Float) {
        // Очищаем текущие дочерние элементы
        children.clear()
        // Пересобираем модель с новым масштабом
        buildShip()
    }
} 