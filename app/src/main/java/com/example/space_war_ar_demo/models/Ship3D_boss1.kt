package com.example.space_war_ar_demo.models

import android.content.Context
import android.util.Log
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * 3D модель на основе параметров из boss1_m.txt
 * Содержит конусы, цилиндры и сферы
 */
class Ship3D_boss1(private val context: Context, private val initialRotation: Vector3 = Vector3.zero()) : Node() {

    private var material: Material? = null
    private var material001: Material? = null
    private var material002: Material? = null
    private var material003: Material? = null
    private var material004: Material? = null

    init {
        // Создаем материалы с соответствующими цветами из boss1_m.txt
        MaterialFactory.makeOpaqueWithColor(context, Color(0.22022f, 0.08665f, 0.80008f)) // Материал
            .thenAccept { material ->
                this.material = material
                material.setFloat("metallic", 0.0f)
                material.setFloat("roughness", 1.0f)
                MaterialFactory.makeTransparentWithColor(
                    context,
                    Color(0.09810f, 0.20137f, 0.80005f)
                ).thenAccept { material001 ->
                    this.material001 = material001
                    material001.setFloat("metallic", 0.0f)
                    material001.setFloat("roughness", 1.0f)

                    MaterialFactory.makeOpaqueWithColor(context, Color(0.80007f, 0.27408f, 0.03928f)) // Материал.002
                        .thenAccept { material002 ->
                            this.material002 = material002
                            material002.setFloat("metallic", 0.0f)
                            material002.setFloat("roughness", 1.0f)
                            MaterialFactory.makeOpaqueWithColor(context, Color(0.07570f, 0.80001f, 0.02066f)) // Материал.003
                                .thenAccept { material003 ->
                                    this.material003 = material003
                                    material003.setFloat("metallic", 0.0f)
                                    material003.setFloat("roughness", 1.0f)
                                    MaterialFactory.makeOpaqueWithColor(context, Color(0.80002f, 0.08664f, 0.03556f)) // Материал.004
                                        .thenAccept { material004 ->
                                            this.material004 = material004
                                            material004.setFloat("metallic", 0.0f)
                                            material004.setFloat("roughness", 1.0f)
                                            try {
                                                buildShipFromPrimitives()
                                            } catch (e: Exception) {
                                                Log.e("Ship3D_boss1", "Error building ship", e)
                                            }
                                        }
                                }
                        }
                }
            }
    }

    private fun buildShipFromPrimitives() {
        // Масштабирующий коэффициент для приведения размеров к подходящему масштабу AR
        val scaleFactor = 0.1f

        // Центральная позиция для сфер
        val sphereCenter = Vector3(0.01158f * scaleFactor, 1.19389f * scaleFactor, 0.16231f * scaleFactor)

        // Конус - перемещаем ближе к центру сферы
        createCustomCone(
            baseRadius = 1.19923f * scaleFactor / 2,
            height = 1.19923f * scaleFactor,
            position = Vector3(
                sphereCenter.x,
                sphereCenter.y + 1.1f * scaleFactor, // Смещаем конус немного выше центра сферы
                sphereCenter.z
            ),
            rotation = Vector3(90.0f, 0.0f, -180.0f),
            material = material002
        )

        // Цилиндр
        val cylinder1 = ShapeFactory.makeCylinder(
            0.34047f * scaleFactor / 2, // Радиус = половина диаметра
            2f * scaleFactor,    // Высота
            Vector3.zero(),
            material002
        )
        val cylinder1Node = Node().apply {
            renderable = cylinder1
            localPosition = Vector3(-0.00333f * scaleFactor, 3.6f * scaleFactor, 0.08782f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(1.0f, 0.0f, 0.0f),
                0f
            )
        }
        addChild(cylinder1Node)

        // Цилиндр.001
        val cylinder2 = ShapeFactory.makeCylinder(
            1.04152f * scaleFactor / 2,
            2.12773f * scaleFactor,
            Vector3.zero(),
            material004
        )
        val cylinder2Node = Node().apply {
            renderable = cylinder2
            localPosition = Vector3(-0.00304f * scaleFactor, -1.88234f * scaleFactor, 1.17246f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(1.0f, 0.0f, 0.0f),
                0f
            )
        }
        addChild(cylinder2Node)

        // Цилиндр.002
        val cylinder3 = ShapeFactory.makeCylinder(
            1.04152f * scaleFactor / 2,
            2.12773f * scaleFactor,
            Vector3.zero(),
            material004
        )
        val cylinder3Node = Node().apply {
            renderable = cylinder3
            localPosition = Vector3(-0.92003f * scaleFactor, -1.88234f * scaleFactor, -0.44196f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(1.0f, 0.0f, 0.0f),
                0.0f
            )
        }
        addChild(cylinder3Node)

        // Цилиндр.003
        val cylinder4 = ShapeFactory.makeCylinder(
            1.04152f * scaleFactor / 2,
            2.12773f * scaleFactor,
            Vector3.zero(),
            material004
        )
        val cylinder4Node = Node().apply {
            renderable = cylinder4
            localPosition = Vector3(0.90104f * scaleFactor, -1.88234f * scaleFactor, -0.44196f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(1.0f, 0.0f, 0.0f),
                0f
            )
        }
        addChild(cylinder4Node)

        // Цилиндр.004
        val cylinder5 = ShapeFactory.makeCylinder(
            1.82763f * scaleFactor / 2,
            0.28057f * scaleFactor,
            Vector3.zero(),
            material
        )
        val cylinder5Node = Node().apply {
            renderable = cylinder5
            localPosition = Vector3(-1.5f * scaleFactor, 0.91994f * scaleFactor, -0.83396f * scaleFactor)
            localRotation = Quaternion.eulerAngles(
                Vector3(90f, -30f, 0f) // pitch (X), yaw (Y), roll (Z)
            )
        }
        addChild(cylinder5Node)

        // Цилиндр.005
        val cylinder6 = ShapeFactory.makeCylinder(
            1.82763f * scaleFactor / 2,
            0.28057f * scaleFactor,
            Vector3.zero(),
            material
        )
        val cylinder6Node = Node().apply {
            renderable = cylinder6
            localPosition = Vector3(1.5f * scaleFactor, 0.91994f * scaleFactor, -0.83396f * scaleFactor)
            localRotation = Quaternion.eulerAngles(
                Vector3(90f, 30f, 0f) // pitch (X), yaw (Y), roll (Z)
            )
        }
        addChild(cylinder6Node)

        // Цилиндр.006
        val cylinder7 = ShapeFactory.makeCylinder(
            1.82763f * scaleFactor / 2,
            0.28057f * scaleFactor,
            Vector3.zero(),
            material
        )
        val cylinder7Node = Node().apply {
            renderable = cylinder7
            localPosition = Vector3(-0.00041f * scaleFactor, 0.91994f * scaleFactor, 1.50600f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 0.0f, 1.0f),
                90.0f
            )
        }
        addChild(cylinder7Node)

        // Цилиндр.007
        val cylinder8 = ShapeFactory.makeCylinder(
            2.04903f * scaleFactor / 2,
            1.09079f * scaleFactor,
            Vector3.zero(),
            material003
        )
        val cylinder8Node = Node().apply {
            renderable = cylinder8
            localPosition = Vector3(-0.04285f * scaleFactor, -0.77384f * scaleFactor, 0.15459f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(1.0f, 0.0f, 0.0f),
                0f
            )
        }
        addChild(cylinder8Node)

//        // Сфера (меньшая, внутренняя)
//        val sphere1 = ShapeFactory.makeSphere(
//            1.94290f * scaleFactor / 2, // Радиус = половина диаметра
//            Vector3.zero(),
//            material001
//        )
//        val sphere1Node = Node().apply {
//            renderable = sphere1
//            localPosition = sphereCenter
//        }
//        addChild(sphere1Node)

        // Сфера.001 (большая, внешняя) - делаем полупрозрачной
        val sphere2 = ShapeFactory.makeSphere(
            2.70126f * scaleFactor / 2, // Радиус = половина диаметра
            Vector3.zero(),
            material001
        )
        val sphere2Node = Node().apply {
            renderable = sphere2
            localPosition = sphereCenter
        }
        addChild(sphere2Node)

        // Применяем начальный поворот к корневому узлу модели
        if (initialRotation != Vector3.zero()) {
            localRotation = com.google.ar.sceneform.math.Quaternion.eulerAngles(
                Vector3(
                    Math.toRadians(initialRotation.x.toDouble()).toFloat(),
                    Math.toRadians(initialRotation.y.toDouble()).toFloat(),
                    Math.toRadians(initialRotation.z.toDouble()).toFloat()
                )
            )
        }

    }

    /**
     * Создает конус используя комбинацию примитивов
     */
    private fun createCustomCone(
        baseRadius: Float,
        height: Float,
        position: Vector3,
        rotation: Vector3,
        material: Material?
    ) {
        // Используем пирамиду из нескольких цилиндров разного радиуса для аппроксимации конуса
        val segments = 8 // Количество сегментов для аппроксимации конуса
        val segmentHeight = height / segments

        for (i in 0 until segments) {
            val segmentRadius = baseRadius * (1 - i.toFloat() / segments)
            val segmentPosition = Vector3(
                position.x,
                position.y + i * segmentHeight,
                position.z
            )

            val cylinder = ShapeFactory.makeCylinder(
                segmentRadius,
                segmentHeight,
                Vector3.zero(),
                material
            )
            val cylinderNode = Node().apply {
                renderable = cylinder
                localPosition = segmentPosition
                localRotation = com.google.ar.sceneform.math.Quaternion.eulerAngles(
                    Vector3(
                        Math.toRadians(rotation.x.toDouble()).toFloat(),
                        Math.toRadians(rotation.y.toDouble()).toFloat(),
                        Math.toRadians(rotation.z.toDouble()).toFloat()
                    )
                )
            }
            addChild(cylinderNode)
        }

        // Добавляем сферу на вершину для более гладкого кончика
        val tipSphere = ShapeFactory.makeSphere(
            baseRadius * 0.1f,
            Vector3.zero(),
            material
        )
        val tipNode = Node().apply {
            renderable = tipSphere
            localPosition = Vector3(
                position.x,
                position.y + height,
                position.z
            )
            localRotation = com.google.ar.sceneform.math.Quaternion.eulerAngles(
                Vector3(
                    Math.toRadians(rotation.x.toDouble()).toFloat(),
                    Math.toRadians(rotation.y.toDouble()).toFloat(),
                    Math.toRadians(rotation.z.toDouble()).toFloat()
                )
            )
        }
        addChild(tipNode)
    }
}