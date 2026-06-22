package com.example.space_war_ar_demo.models

import android.content.Context
import android.util.Log
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * 3D модель на основе параметров из dron2_m.txt
 * Содержит только кубы
 */
class Ship3D_dron2(private val context: Context, private val initialRotation: Vector3 = Vector3.zero()) : Node() {

    private var material001: Material? = null
    private var material: Material? = null
    private var material002: Material? = null
    private var material003: Material? = null

    init {
        // Создаем материалы с соответствующими цветами из файла
        MaterialFactory.makeOpaqueWithColor(context, Color(0.80010176f, 0.5297813f, 0.02001291f))
            .thenAccept { material001 ->
                this.material001 = material001
                MaterialFactory.makeOpaqueWithColor(context, Color(0.01727098f, 0.07654593f, 0.8002826f))
                    .thenAccept { material ->
                        this.material = material
                        MaterialFactory.makeOpaqueWithColor(context, Color(0.800023f, 0.0073050363f, 0.1920853f))
                            .thenAccept { material002 ->
                                this.material002 = material002
                                MaterialFactory.makeOpaqueWithColor(context, Color(0.0f, 0.80029976f, 0.00570001f))
                                    .thenAccept { material003 ->
                                        this.material003 = material003
                                        material003.setFloat("metallic", 0.0f)
                                        material003.setFloat("roughness", 1.0f)
                                        material002.setFloat("metallic", 0.0f)
                                        material002.setFloat("roughness", 1.0f)
                                        material001.setFloat("metallic", 0.0f)
                                        material001.setFloat("roughness", 1.0f)
                                        material.setFloat("metallic", 0.0f)
                                        material.setFloat("roughness", 1.0f)
                                        try {
                                            buildShipFromPrimitives()
                                        } catch (e: Exception) {
                                            Log.e("Ship3D_dron2", "Error building ship", e)
                                        }
                                    }
                            }
                    }
            }
    }

    private fun buildShipFromPrimitives() {
        // Масштабирующий коэффициент для приведения размеров к подходящему масштабу AR
        val scaleFactor = 0.1f

        // Куб
        val cube = ShapeFactory.makeCube(
            Vector3(0.53652f * scaleFactor, 1.83561f * scaleFactor, 0.22316f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cubeNode = Node().apply {
            renderable = cube
            localPosition = Vector3(0.41716f * scaleFactor, 0.91989f * scaleFactor, -0.15774f * scaleFactor)
        }
        addChild(cubeNode)

        // Куб.001
        val cube1 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 1.42769f * scaleFactor, 0.22316f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube1Node = Node().apply {
            renderable = cube1
            localPosition = Vector3(0.79849f * scaleFactor, 0.41399f * scaleFactor, -0.15774f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 0.0f, 1.0f), 
                174.68534f
            )
        }
        addChild(cube1Node)

        // Куб.002
        val cube2 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 0.40791f * scaleFactor, 0.46864f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube2Node = Node().apply {
            renderable = cube2
            localPosition = Vector3(-0.67284f * scaleFactor, -0.93822f * scaleFactor, -0.38712f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 1.0f, 0.0f), 
                -29.72252f
            )
        }
        addChild(cube2Node)

        // Куб.003
        val cube3 = ShapeFactory.makeCube(
            Vector3(0.53652f * scaleFactor, 1.42769f * scaleFactor, 0.39053f * scaleFactor),
            Vector3.zero(),
            material
        )
        val cube3Node = Node().apply {
            renderable = cube3
            localPosition = Vector3(0.01893f * scaleFactor, -0.41187f * scaleFactor, 0.01097f * scaleFactor)
        }
        addChild(cube3Node)

        // Куб.004
        val cube4 = ShapeFactory.makeCube(
            Vector3(0.32191f * scaleFactor, 1.01978f * scaleFactor, 0.33474f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube4Node = Node().apply {
            renderable = cube4
            localPosition = Vector3(0.48244f * scaleFactor, -1.06045f * scaleFactor, 0.08833f * scaleFactor)
        }
        addChild(cube4Node)

        // Куб.005
        val cube5 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 0.61187f * scaleFactor, 0.44633f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube5Node = Node().apply {
            renderable = cube5
            localPosition = Vector3(0.87513f * scaleFactor, -0.94546f * scaleFactor, 0.08833f * scaleFactor)
        }
        addChild(cube5Node)

        // Куб.006
        val cube6 = ShapeFactory.makeCube(
            Vector3(0.26826f * scaleFactor, 0.81582f * scaleFactor, 0.44633f * scaleFactor),
            Vector3.zero(),
            material002
        )
        val cube6Node = Node().apply {
            renderable = cube6
            localPosition = Vector3(0.44898f * scaleFactor, 0.23675f * scaleFactor, -0.15774f * scaleFactor)
        }
        addChild(cube6Node)

        // Куб.007
        val cube7 = ShapeFactory.makeCube(
            Vector3(0.21461f * scaleFactor, 0.40791f * scaleFactor, 0.22316f * scaleFactor),
            Vector3.zero(),
            material002
        )
        val cube7Node = Node().apply {
            renderable = cube7
            localPosition = Vector3(0.55863f * scaleFactor, 2.04586f * scaleFactor, -0.15774f * scaleFactor)
        }
        addChild(cube7Node)

        // Куб.008
        val cube8 = ShapeFactory.makeCube(
            Vector3(0.26826f * scaleFactor, 0.61187f * scaleFactor, 0.10042f * scaleFactor),
            Vector3.zero(),
            material003
        )
        val cube8Node = Node().apply {
            renderable = cube8
            localPosition = Vector3(-0.44885f * scaleFactor, -1.06045f * scaleFactor, 0.30967f * scaleFactor)
        }
        addChild(cube8Node)

        // Куб.009
        val cube9 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 0.40791f * scaleFactor, 0.46864f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube9Node = Node().apply {
            renderable = cube9
            localPosition = Vector3(-0.67284f * scaleFactor, -0.93822f * scaleFactor, 0.5591f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 1.0f, 0.0f), 
                29.72252f
            )
        }
        addChild(cube9Node)

        // Куб.010
        val cube10 = ShapeFactory.makeCube(
            Vector3(0.21461f * scaleFactor, 0.40791f * scaleFactor, 0.22316f * scaleFactor),
            Vector3.zero(),
            material002
        )
        val cube10Node = Node().apply {
            renderable = cube10
            localPosition = Vector3(-0.52503f * scaleFactor, 2.04586f * scaleFactor, -0.15774f * scaleFactor)
        }
        addChild(cube10Node)

        // Куб.011
        val cube11 = ShapeFactory.makeCube(
            Vector3(0.26826f * scaleFactor, 0.81582f * scaleFactor, 0.44633f * scaleFactor),
            Vector3.zero(),
            material002
        )
        val cube11Node = Node().apply {
            renderable = cube11
            localPosition = Vector3(-0.41538f * scaleFactor, 0.23675f * scaleFactor, -0.15774f * scaleFactor)
        }
        addChild(cube11Node)

        // Куб.012
        val cube12 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 0.61187f * scaleFactor, 0.44633f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube12Node = Node().apply {
            renderable = cube12
            localPosition = Vector3(-0.84153f * scaleFactor, -0.94546f * scaleFactor, 0.08833f * scaleFactor)
        }
        addChild(cube12Node)

        // Куб.013
        val cube13 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 0.40791f * scaleFactor, 0.46864f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube13Node = Node().apply {
            renderable = cube13
            localPosition = Vector3(0.70643f * scaleFactor, -0.93822f * scaleFactor, 0.5591f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 1.0f, 0.0f), 
                -29.72252f
            )
        }
        addChild(cube13Node)

        // Куб.014
        val cube14 = ShapeFactory.makeCube(
            Vector3(0.32191f * scaleFactor, 1.01978f * scaleFactor, 0.33474f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube14Node = Node().apply {
            renderable = cube14
            localPosition = Vector3(-0.44885f * scaleFactor, -1.06045f * scaleFactor, 0.08833f * scaleFactor)
        }
        addChild(cube14Node)

        // Куб.015
        val cube15 = ShapeFactory.makeCube(
            Vector3(0.26826f * scaleFactor, 0.61187f * scaleFactor, 0.10042f * scaleFactor),
            Vector3.zero(),
            material003
        )
        val cube15Node = Node().apply {
            renderable = cube15
            localPosition = Vector3(0.48244f * scaleFactor, -1.06045f * scaleFactor, 0.30967f * scaleFactor)
        }
        addChild(cube15Node)

        // Куб.016
        val cube16 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 1.42769f * scaleFactor, 0.22316f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube16Node = Node().apply {
            renderable = cube16
            localPosition = Vector3(-0.7649f * scaleFactor, 0.41399f * scaleFactor, -0.15774f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 0.0f, 1.0f), 
                185.31466f
            )
        }
        addChild(cube16Node)

        // Куб.017
        val cube17 = ShapeFactory.makeCube(
            Vector3(0.1073f * scaleFactor, 0.40791f * scaleFactor, 0.46864f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube17Node = Node().apply {
            renderable = cube17
            localPosition = Vector3(0.70643f * scaleFactor, -0.93822f * scaleFactor, -0.38712f * scaleFactor)
            localRotation = com.google.ar.sceneform.math.Quaternion.axisAngle(
                Vector3(0.0f, 1.0f, 0.0f), 
                29.72252f
            )
        }
        addChild(cube17Node)

        // Куб.018
        val cube18 = ShapeFactory.makeCube(
            Vector3(0.53652f * scaleFactor, 1.83561f * scaleFactor, 0.22316f * scaleFactor),
            Vector3.zero(),
            material001
        )
        val cube18Node = Node().apply {
            renderable = cube18
            localPosition = Vector3(-0.38257f * scaleFactor, 0.91989f * scaleFactor, -0.15774f * scaleFactor)
        }
        addChild(cube18Node)

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
     * Функция для установки вращения модели в градусах
     */
    fun setRotation(rotation: Vector3) {
        localRotation = com.google.ar.sceneform.math.Quaternion.eulerAngles(
            Vector3(
                Math.toRadians(rotation.x.toDouble()).toFloat(),
                Math.toRadians(rotation.y.toDouble()).toFloat(),
                Math.toRadians(rotation.z.toDouble()).toFloat()
            )
        )
    }

    /**
     * Функция для установки вращения модели с использованием кватерниона
     */
    fun setRotation(quaternion: com.google.ar.sceneform.math.Quaternion) {
        localRotation = quaternion
    }



    /**
     * Функция для получения текущего вращения в виде кватерниона
     */
    fun getRotationQuaternion(): com.google.ar.sceneform.math.Quaternion {
        return localRotation
    }
}