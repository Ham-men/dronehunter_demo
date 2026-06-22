package com.example.space_war_ar_demo

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.space_war_ar_demo.levels.BaseLevel
import com.example.space_war_ar_demo.levels.BrainTurret_v1
import com.example.space_war_ar_demo.levels.NewLvl2
import com.example.space_war_ar_demo.models.Turret3D_v1
import com.example.space_war_ar_demo.physics.PhysicsNode
import com.example.space_war_ar_demo.physics.ShapeType
import android.content.Context
import com.example.space_war_ar_demo.utils.FullscreenManager
import com.example.space_war_ar_demo.utils.LanguageHelper
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable

class GameActivityVer2 : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "GameActivityVer2"
        private const val TARGET_LOCK_TIME = 2000L
        private const val TARGET_FRAME_SIZE = 0.525f
        private const val BLINK_INTERVAL = 300L
    }

    private lateinit var sceneView: ArSceneView
    private var arSession: Session? = null
    private var userRequestedInstall: Boolean = true

    private lateinit var playerHealth: TextView
    private lateinit var playerCredits: TextView
    private lateinit var playerArmor: TextView
    private lateinit var playerDamage: TextView
    private lateinit var playerTurrets: TextView
    private lateinit var enemyHealth: TextView
    private lateinit var enemyArmor: TextView
    private lateinit var enemyDamage: TextView
    private lateinit var enemyDrones: TextView
    private lateinit var waveCount: TextView
    private lateinit var waveTimer: TextView
    private lateinit var turretSpinner: Spinner
    private lateinit var weaponSpinner: Spinner
    private lateinit var fireButton: Button
    private lateinit var launchTurretButton: Button
    private lateinit var pauseButton: Button
    private lateinit var evacuationProgressText: TextView
    
    private var movementButtonsPanel: LinearLayout? = null
    private var moveLeftButton: Button? = null
    private var moveRightButton: Button? = null
    private lateinit var waterOverlay: View
    private lateinit var blindOverlay: View

    private lateinit var droneIndicatorRight: View
    private lateinit var droneIndicatorLeft: View
    private lateinit var droneIndicatorTop: View
    private lateinit var droneIndicatorBottom: View

    lateinit var shipData: ShipData
    lateinit var weaponManager: WeaponManager
    private var currentLevel: BaseLevel? = null
    val getCurrentLevel: BaseLevel? get() = currentLevel
    var currentLevelId: Int = -1

    private var selectedTurret: String = ""
    var isPaused = false

    private var isInitialized = false

    private val turretBrains = mutableListOf<BrainTurret_v1>()
    private val handler = Handler(Looper.getMainLooper())

    private var targetedDrone: PhysicsNode? = null
    private var targetingStartTime: Long = 0L
    private var isTargetLocked: Boolean = false
    private var targetFrameNode: com.google.ar.sceneform.Node? = null
    private var targetFrameBlinkState: Boolean = false
    private var lastFrameBlinkTime: Long = 0L

    private lateinit var gravityGunSound: MediaPlayer
    private lateinit var laserSound: MediaPlayer
    private lateinit var pistolSound: MediaPlayer
    private lateinit var missileSound: MediaPlayer

    private var onPlayerFireListener: (() -> Unit)? = null

    private var lastDialogTypeForTest: String? = null
    fun getLastDialogTypeForTest(): String? = lastDialogTypeForTest

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: start")
        try {
            super.onCreate(savedInstanceState)

            FullscreenManager.setupFullscreenMode(this)
            Log.d(TAG, "onCreate: fullscreen mode setup completed")

            setContentView(R.layout.activity_game_ver2)

            if (savedInstanceState != null) {
                restoreState(savedInstanceState)
            }
            Log.d(TAG, "onCreate: calling hideSystemUI")
            FullscreenManager.hideSystemUI(this)
            Log.d(TAG, "onCreate: hideSystemUI completed")

            initializeViews()
            initializeGameData()
            setupSpinners()
            setupButtons()
            initializeWeaponSounds()
            
            Log.d(TAG, "About to call initializeMovementButtons()")
            initializeMovementButtons()
            Log.d(TAG, "initializeMovementButtons() completed")

            val isTestMode = intent?.getBooleanExtra("TEST_MODE", false) == true
            if (isTestMode) {
                setupTestMode()
            } else {
                setupNormalMode()
            }

            isInitialized = true
            Log.d(TAG, "onCreate: completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            Toast.makeText(this, "Критическая ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG,  "Критическая ошибка: ${e.message}")
            finish()
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "initializeViews: start")
        try {
            sceneView = findViewById(R.id.arSceneView)
                ?: throw IllegalStateException("arSceneView not found")
            
            setupTouchHandling()
            
            try {
                Log.d(TAG, "About to call setupMovementButtons()")
                setupMovementButtons()
                Log.d(TAG, "setupMovementButtons() completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup movement buttons: ${e.message}")
                e.printStackTrace()
            }

            playerCredits = findViewById(R.id.playerCredits)
                ?: throw IllegalStateException("playerCredits not found")
            playerHealth = findViewById(R.id.playerHealth)
                ?: throw IllegalStateException("playerHealth not found")
            playerArmor = findViewById(R.id.playerArmor)
                ?: throw IllegalStateException("playerArmor not found")
            playerDamage = findViewById(R.id.playerDamage)
                ?: throw IllegalStateException("playerDamage not found")
            playerTurrets = findViewById(R.id.playerTurrets)
                ?: throw IllegalStateException("playerTurrets not found")

            enemyHealth = findViewById(R.id.enemyHealth)
                ?: throw IllegalStateException("enemyHealth not found")
            enemyArmor = findViewById(R.id.enemyArmor)
                ?: throw IllegalStateException("enemyArmor not found")
            enemyDamage = findViewById(R.id.enemyDamage)
                ?: throw IllegalStateException("enemyDamage not found")
            enemyDrones = findViewById(R.id.enemyDrones)
                ?: throw IllegalStateException("enemyDrones not found")

            waveCount = findViewById(R.id.waveCount)
                ?: throw IllegalStateException("waveCount not found")
            waveTimer = findViewById(R.id.waveTimer)
                ?: throw IllegalStateException("waveTimer not found")

            turretSpinner = findViewById(R.id.turretSpinner)
                ?: throw IllegalStateException("turretSpinner not found")
            weaponSpinner = findViewById(R.id.weaponSpinner)
                ?: throw IllegalStateException("weaponSpinner not found")
            fireButton = findViewById(R.id.fireButton)
                ?: throw IllegalStateException("fireButton not found")
            launchTurretButton = findViewById(R.id.launchTurretButton)
                ?: throw IllegalStateException("launchTurretButton not found")

            pauseButton = findViewById(R.id.pauseButton)
                ?: throw IllegalStateException("pauseButton not found")

            evacuationProgressText = findViewById(R.id.evacuationProgressText)
                ?: throw IllegalStateException("evacuationProgressText not found")
            try {
                Log.d(TAG, "Attempting to initialize movement buttons")
                movementButtonsPanel = findViewById<LinearLayout>(R.id.movementButtonsPanel)
                moveLeftButton = findViewById<Button>(R.id.moveLeftButton)
                moveRightButton = findViewById<Button>(R.id.moveRightButton)
                
                Log.d(TAG, "Movement buttons found - panel: $movementButtonsPanel, left: $moveLeftButton, right: $moveRightButton")
                
                if (movementButtonsPanel != null && moveLeftButton != null && moveRightButton != null) {
                    Log.d(TAG, "Movement buttons initialized successfully")
                } else {
                    Log.w(TAG, "Some movement buttons are null")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Movement buttons not found, they will be null: ${e.message}")
                e.printStackTrace()
                movementButtonsPanel = null
                moveLeftButton = null
                moveRightButton = null
            }
            waterOverlay = findViewById(R.id.waterOverlay)
                ?: throw IllegalStateException("waterOverlay not found")
            blindOverlay = findViewById(R.id.blindOverlay)
                ?: throw IllegalStateException("blindOverlay not found")


            droneIndicatorRight = findViewById(R.id.droneIndicatorRight)
                ?: throw IllegalStateException("droneIndicatorRight not found")
            droneIndicatorLeft = findViewById(R.id.droneIndicatorLeft)
                ?: throw IllegalStateException("droneIndicatorLeft not found")
            droneIndicatorTop = findViewById(R.id.droneIndicatorTop)
                ?: throw IllegalStateException("droneIndicatorTop not found")
            droneIndicatorBottom = findViewById(R.id.droneIndicatorBottom)
                ?: throw IllegalStateException("droneIndicatorBottom not found")

            Log.d(TAG, "initializeViews: views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun initializeGameData() {
        Log.d(TAG, "initializeGameData: start")
        try {
            shipData = ShipData(this)
            weaponManager = WeaponManager(sceneView, this)

            fireButton.isEnabled = false

            Log.d(TAG, "initializeGameData: game data initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing game data", e)
            throw e
        }
    }

    private fun setupTestMode() {
        Log.d(TAG, "setupTestMode: start")
        try {
            sceneView.scene.addOnUpdateListener {
                if (!isPaused) {
                    updateGameLoop()
                }
            }
            sceneView.post { startLevelFromIntent() }

            Log.d(TAG, "setupTestMode: test mode setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up test mode", e)
            throw e
        }
    }

    private fun setupNormalMode() {
        Log.d(TAG, "setupNormalMode: start")
        try {
            if (checkCameraPermission()) {
                setupAR()
            } else {
                requestCameraPermission()
            }

            Log.d(TAG, "setupNormalMode: normal mode setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up normal mode", e)
            throw e
        }
    }

    private fun updateGameLoop() {
        try {
            currentLevel?.update()
            updateAllStats()
            updateDroneIndicators()

            if (shipData.selectedWeapon == "Ракета") {
                handleDroneTargeting()
            } else {
                if (targetedDrone != null) clearTargetFrame()
                fireButton.isEnabled = true
            }

            updateTurrets()

        } catch (e: Exception) {
            Log.e(TAG, "Error in game loop update", e)
        }
    }


    private fun updateTurrets() {
        try {
            turretBrains.forEach { brain ->
                val drones = currentLevel?.getDronesForTurret() ?: emptyList()
                val targetData = drones.map { drone ->
                    BrainTurret_v1.TargetData(node = drone, id = drone.hashCode().toString())
                }
                brain.updateTargets(targetData)
                brain.update()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating turrets", e)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAR()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required for AR features",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun setupAR() {
        Log.d(TAG, "setupAR: start")
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (availability.isTransient) {
                Handler(Looper.getMainLooper()).postDelayed({ setupAR() }, 200)
                return
            }
            if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                val installStatus: InstallStatus =
                    ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)
                if (installStatus == InstallStatus.INSTALL_REQUESTED) {
                    userRequestedInstall = false
                    Log.d(TAG, "ARCore install requested; waiting for user action")
                    return
                }
            }

            arSession = Session(this)
            val config = Config(arSession)
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            arSession?.configure(config)

            sceneView.setupSession(arSession)

            sceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST

            sceneView.scene.addOnUpdateListener {
                if (!isPaused) {
                    updateGameLoop()
                }
            }

            arSession?.resume()
            sceneView.resume()
            Log.d(TAG, "AR session initialized successfully")

            waitForSceneViewReady()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AR: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun waitForSceneViewReady() {
        if (sceneView.width > 0 && sceneView.height > 0) {
            Log.d(TAG, "SceneView ready, starting level")
            startLevelFromIntent()
        } else {
            Log.d(TAG, "SceneView not ready yet, waiting...")
            sceneView.post { waitForSceneViewReady() }
        }
    }

    private fun setupTouchHandling() {
        sceneView.setOnTouchListener { _, _ -> false }
    }

    private fun setupMovementButtons() {
        Log.d(TAG, "setupMovementButtons: starting setup")
        Log.d(TAG, "Current button states - moveLeftButton: $moveLeftButton, moveRightButton: $moveRightButton, movementButtonsPanel: $movementButtonsPanel")
        
        if (moveLeftButton == null || moveRightButton == null || movementButtonsPanel == null) {
            Log.w(TAG, "Movement buttons not available, attempting to find them again")
            try {
                movementButtonsPanel = findViewById<LinearLayout>(R.id.movementButtonsPanel)
                moveLeftButton = findViewById<Button>(R.id.moveLeftButton)
                moveRightButton = findViewById<Button>(R.id.moveRightButton)
                Log.d(TAG, "After re-finding - moveLeftButton: $moveLeftButton, moveRightButton: $moveRightButton, movementButtonsPanel: $movementButtonsPanel")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to find movement buttons: ${e.message}")
                e.printStackTrace()
            }
        }
        
        if (moveLeftButton == null || moveRightButton == null || movementButtonsPanel == null) {
            Log.w(TAG, "Movement buttons still not available after re-finding, skipping setup")
            return
        }
        
        Log.d(TAG, "Movement buttons found, setting up click listeners")
        
        try {
            val testButton = findViewById<Button>(R.id.fireButton)
            testButton?.setOnClickListener {
                Log.d(TAG, "TEST: fireButton clicked - this proves click listeners work!")
            }
            Log.d(TAG, "TEST: Set click listener on fireButton successfully")
        } catch (e: Exception) {
            Log.e(TAG, "TEST: Failed to set click listener on fireButton: ${e.message}")
        }
        
        moveLeftButton?.setOnClickListener {
            Log.d(TAG, "moveLeftButton clicked!")
            Log.w(TAG, "Movement buttons not available in demo")
        }
        
        moveRightButton?.setOnClickListener {
            Log.d(TAG, "moveRightButton clicked!")
            Log.w(TAG, "Movement buttons not available in demo")
        }
        
        movementButtonsPanel?.visibility = View.GONE
        Log.d(TAG, "Movement buttons setup completed successfully")
    }

    private fun updateMovementButtonsVisibility(level: BaseLevel) {
        Log.d(TAG, "updateMovementButtonsVisibility: level = ${level.javaClass.simpleName}")
        movementButtonsPanel?.visibility = View.GONE
    }

    private fun initializeMovementButtons() {
        Log.d(TAG, "initializeMovementButtons: starting")
        try {
            if (movementButtonsPanel == null) {
                Log.d(TAG, "Movement buttons panel is null, attempting to find views")
                movementButtonsPanel = findViewById<LinearLayout>(R.id.movementButtonsPanel)
                moveLeftButton = findViewById<Button>(R.id.moveLeftButton)
                moveRightButton = findViewById<Button>(R.id.moveRightButton)
                
                Log.d(TAG, "Found views - panel: $movementButtonsPanel, left: $moveLeftButton, right: $moveRightButton")
                
                if (movementButtonsPanel != null && moveLeftButton != null && moveRightButton != null) {
                    setupMovementButtons()
                    Log.d(TAG, "Movement buttons re-initialized successfully")
                } else {
                    Log.w(TAG, "Movement buttons still not available after re-initialization")
                    Log.w(TAG, "Panel: $movementButtonsPanel, Left: $moveLeftButton, Right: $moveRightButton")
                }
            } else {
                Log.d(TAG, "Movement buttons already initialized")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to re-initialize movement buttons: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initializeWeaponSounds() {
        Log.d(TAG, "initializeWeaponSounds: start")
        try {
            gravityGunSound = MediaPlayer.create(this, R.raw.shut_gravity)
            laserSound = MediaPlayer.create(this, R.raw.shut_laser)
            pistolSound = MediaPlayer.create(this, R.raw.shut_pistol)
            missileSound = MediaPlayer.create(this, R.raw.shut_raketa)
            applySoundVolumesFromSettings()

            gravityGunSound.isLooping = false
            laserSound.isLooping = false
            pistolSound.isLooping = false
            missileSound.isLooping = false

            Log.d(TAG, "initializeWeaponSounds: weapon sounds initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing weapon sounds: ${e.message}")
        }
    }

    private fun applySoundVolumesFromSettings() {
        try {
            val volPercent = SettingsManager.getSoundVolume(this)
            val vol = (volPercent.coerceIn(0, 100)) / 100f
            gravityGunSound.setVolume(vol, vol)
            laserSound.setVolume(vol, vol)
            pistolSound.setVolume(vol, vol)
            missileSound.setVolume(vol, vol)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying sound volumes", e)
        }
    }

    private fun playWeaponSound() {
        try {
            if (!::gravityGunSound.isInitialized || !::laserSound.isInitialized ||
                !::pistolSound.isInitialized || !::missileSound.isInitialized
            ) {
                Log.w(TAG, "Weapon sounds not initialized yet")
                return
            }

            val selectedWeapon = shipData.selectedWeapon
            when (selectedWeapon) {
                "Гравити пушка" -> {
                    if (gravityGunSound.isPlaying) {
                        gravityGunSound.stop()
                        gravityGunSound.prepare()
                    }
                    gravityGunSound.start()
                }

                "Лазер" -> {
                    if (laserSound.isPlaying) {
                        laserSound.stop()
                        laserSound.prepare()
                    }
                    laserSound.start()
                }

                "Пистолет" -> {
                    if (pistolSound.isPlaying) {
                        pistolSound.stop()
                        pistolSound.prepare()
                    }
                    pistolSound.start()
                }

                "Ракета" -> {
                    if (missileSound.isPlaying) {
                        missileSound.stop()
                        missileSound.prepare()
                    }
                    missileSound.start()
                }

                else -> {
                    if (pistolSound.isPlaying) {
                        pistolSound.stop()
                        pistolSound.prepare()
                    }
                    pistolSound.start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing weapon sound: ${e.message}")
        }
    }

    private fun setupSpinners() {
        Log.d(TAG, "setupSpinners: start")
        try {
            val localizedWeapons = getAvailableWeapons()
            val weaponAdapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, localizedWeapons)
            weaponAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            weaponSpinner.adapter = weaponAdapter

            val turretNames = arrayOf("Турель", "Сонар")
            val turrets = arrayOf(
                getString(R.string.weapon_turret),
                getString(R.string.weapon_sonar)
            )
            val turretAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, turrets)
            turretAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            turretSpinner.adapter = turretAdapter
            
            if (selectedTurret.isEmpty()) {
                selectedTurret = turretNames[0]
            }

            setupSpinnerDropdownDirection(weaponSpinner)
            setupSpinnerDropdownDirection(turretSpinner)

            weaponSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (localizedWeapons.isNotEmpty() && position < localizedWeapons.size) {
                        shipData.selectedWeapon = localizedWeapons[position]
                        Log.d(TAG, "Selected weapon: ${shipData.selectedWeapon}")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            turretSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position < turretNames.size) {
                        selectedTurret = turretNames[position]
                        Log.d(TAG, "Selected turret: $selectedTurret")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            Log.d(TAG, "setupSpinners: spinners configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up spinners", e)
        }
    }

    private fun setupSpinnerDropdownDirection(spinner: Spinner) {
        try {
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            val location = IntArray(2)
            spinner.getLocationOnScreen(location)
            val spinnerY = location[1]
            val spinnerHeight = spinner.height

            val spaceBelow = screenHeight - spinnerY - spinnerHeight
            val spaceAbove = spinnerY

            if (spaceBelow < spaceAbove) {
                spinner.dropDownVerticalOffset = -spinnerHeight
                Log.d(TAG, "Spinner ${spinner.id} configured to open upward")
            } else {
                spinner.dropDownVerticalOffset = 0
                Log.d(TAG, "Spinner ${spinner.id} configured to open downward")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up spinner dropdown direction", e)
        }
    }

    private fun getAvailableWeapons(): Array<String> {
        Log.d(TAG, "getAvailableWeapons: start")
        try {
            val availableWeapons = mutableListOf<String>()
            // В демо доступно всё оружие
            for (w in WeaponCatalog.weapons) {
                if (w.name != "Турели" && w.name != "Сонар") {
                    availableWeapons.add(w.name)
                }
            }
            return availableWeapons.toTypedArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available weapons", e)
            return arrayOf(getString(R.string.weapon_pistol))
        }
    }

    private fun setupButtons() {
        Log.d(TAG, "setupButtons: start")
        try {
            fireButton.setOnClickListener {
                handleFireButtonClick()
            }

            pauseButton.setOnClickListener {
                handlePauseButtonClick()
            }

            launchTurretButton.setOnClickListener {
                handleLaunchTurretButtonClick()
            }

            Log.d(TAG, "setupButtons: buttons configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buttons", e)
        }
    }

    private fun handleFireButtonClick() {
        Log.d(TAG, "Fire button clicked. Selected weapon: ${shipData.selectedWeapon}")
        try {
            playWeaponSound()

            if (shipData.selectedWeapon == "Ракета") {
                if (isTargetLocked && targetedDrone != null) {
                    Log.d(TAG, "Launching homing missile at targeted drone")
                    launchHomingMissile(targetedDrone!!)
                    clearTargetFrame()
                } else {
                    Log.d(TAG, "No target locked for missile")
                    Toast.makeText(this, "Нет заблокированной цели для ракеты", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
            } else {
                currentLevel?.onFire()
            }

            updateAllStats()

            Log.d(TAG, "Fire button click handled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling fire button click", e)
        }
    }

    private fun handlePauseButtonClick() {
        Log.d(TAG, "Pause button clicked")
        try {
            togglePause()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pause button click", e)
        }
    }

    private fun handleLaunchTurretButtonClick() {
        Log.d(
            TAG,
            "Launch turret button clicked. selectedTurret=$selectedTurret, turretCount=${shipData.turretCount}, sonarCount=${shipData.sonarCount}"
        )
        try {
            if (selectedTurret == "Турель") {
                if (shipData.turretCount > 0) {
                    Log.d(TAG, "Trying to fire turret...")
                    fireTurret()
                    shipData.useTurret()
                    updateAllStats()
                    setupSpinners()
                } else {
                    Log.d(TAG, "Нет доступных турелей (turretCount=0)")
                    Toast.makeText(this, getString(R.string.game_no_turrets), Toast.LENGTH_SHORT).show()
                }
            } else if (selectedTurret == "Сонар") {
                fireTurret()
                updateAllStats()
            } else {
                Log.d(TAG, "selectedTurret != 'Турель' ($selectedTurret)")
                Toast.makeText(this, getString(R.string.game_no_turrets), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling launch turret button click", e)
        }
    }

    private fun fireTurret() {
        Log.d(TAG, "fireTurret: start")
        try {
            val scene = sceneView.scene
            if (scene == null) {
                Log.e(TAG, "fireTurret: scene is null!")
                return
            }
            val camera = scene.camera
            if (camera == null) {
                Log.e(TAG, "fireTurret: camera is null!")
                return
            }

            Log.d(TAG, "fireTurret: scene and camera are OK. Spawning turret...")
            val turretPhysicsNode = PhysicsNode().apply {
                shapeType = ShapeType.BOX
                dimensions = com.google.ar.sceneform.math.Vector3(0.3f, 0.3f, 0.3f)
                mass = 50f
                isStatic = false
                worldPosition = camera.worldPosition
                entityTag = "Turret"
                velocity = camera.forward.scaled(5f)
            }
            val turretModel = Turret3D_v1(this)
            turretPhysicsNode.addChild(turretModel)
            scene.addChild(turretPhysicsNode)
            currentLevel?.addPhysicsNode(turretPhysicsNode)
            val brain = BrainTurret_v1()
            brain.initialize(turretPhysicsNode, camera.worldPosition)
            brain.onFire = { turret, target, damage ->
                fireTurretProjectile(turret, target, damage)
            }
            turretBrains.add(brain)
            Log.d(TAG, "fireTurret: turret spawned and brain added.")

        } catch (e: Exception) {
            Log.e(TAG, "Error firing turret", e)
        }
    }

    private fun fireTurretProjectile(
        turretNode: PhysicsNode,
        targetNode: PhysicsNode,
        damage: Int
    ) {
        Log.d(TAG, "fireTurretProjectile: start")
        try {
            val scene = sceneView.scene ?: return
            com.google.ar.sceneform.rendering.MaterialFactory.makeOpaqueWithColor(
                this,
                com.google.ar.sceneform.rendering.Color(0f, 1f, 0f, 0f)
            )
                .thenAccept { material ->
                    val projectile = com.google.ar.sceneform.rendering.ShapeFactory.makeSphere(
                        0.08f,
                        com.google.ar.sceneform.math.Vector3.zero(),
                        material
                    )
                    val projectileNode = com.google.ar.sceneform.Node().apply {
                        renderable = projectile
                        worldPosition = turretNode.worldPosition
                    }
                    scene.addChild(projectileNode)
                    val handler = Handler(Looper.getMainLooper())
                    val updateRunnable = object : Runnable {
                        override fun run() {
                            if (projectileNode.scene == null || targetNode.scene == null) {
                                if (projectileNode.scene != null) scene.removeChild(projectileNode)
                                return
                            }
                            val direction = com.google.ar.sceneform.math.Vector3.subtract(
                                targetNode.worldPosition,
                                projectileNode.worldPosition
                            ).normalized()
                            val speed = 10f * 0.016f
                            projectileNode.worldPosition = com.google.ar.sceneform.math.Vector3.add(
                                projectileNode.worldPosition,
                                direction.scaled(speed)
                            )
                            val distance = com.google.ar.sceneform.math.Vector3.subtract(
                                projectileNode.worldPosition,
                                targetNode.worldPosition
                            ).length()
                            if (distance < 0.2f) {
                                currentLevel?.damageDefender(targetNode, damage)
                                scene.removeChild(projectileNode)
                            } else {
                                handler.postDelayed(this, 16)
                            }
                        }
                    }
                    handler.post(updateRunnable)
                }
            Log.d(TAG, "fireTurretProjectile: projectile launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error firing turret projectile", e)
        }
    }

    private fun togglePause() {
        Log.d(TAG, "togglePause: current state isPaused=$isPaused")
        try {
            isPaused = !isPaused
            if (isPaused) {
                pauseGame()
            } else {
                resumeGame()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling pause", e)
        }
    }

    private fun pauseGame() {
        Log.d(TAG, "pauseGame: start")
        try {
            isPaused = true
            pauseButton.text = "▶"
            currentLevel?.pause()
            weaponManager.pause()
            showPauseDialog()
            Log.d(TAG, "pauseGame: game paused successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing game", e)
        }
    }

    private fun resumeGame() {
        Log.d(TAG, "resumeGame: start")
        try {
            isPaused = false
            pauseButton.text = "⏸"
            currentLevel?.resume()
            weaponManager.resume()
            Log.d(TAG, "resumeGame: game resumed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming game", e)
        }
    }

    private fun showPauseDialog() {
        Log.d(TAG, "showPauseDialog: start")
        try {
            val dialog = android.app.Dialog(this)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_pause, null)
            dialog.setContentView(view)
            dialog.setCancelable(false)

            view.findViewById<Button>(R.id.continueButton)?.setOnClickListener {
                dialog.dismiss()
                resumeGame()
            }

            view.findViewById<Button>(R.id.menuButton)?.setOnClickListener {
                dialog.dismiss()
                finishLevel()
            }



            dialog.show()
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )

            Log.d(TAG, "showPauseDialog: pause dialog shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing pause dialog", e)
        }
    }

    fun showWinDialog() {
        Log.d(TAG, "showWinDialog: start")
        try {
            lastDialogTypeForTest = "win"

            val levelId = intent.getIntExtra("Level", 101)
            val levelProgressManager = LevelProgressManager(this)
            levelProgressManager.markLevelCompleted(levelId)
            Log.d(TAG, "showWinDialog: level $levelId marked as completed")

            if (!isPaused) {
                Log.d(TAG, "showWinDialog: setting game to paused state")
                isPaused = true
                currentLevel?.pause()
                Log.d(TAG, "showWinDialog: calling weaponManager.pause()")
                weaponManager.pause()
            }
            val dialog = android.app.Dialog(this)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_win, null)
            dialog.setContentView(view)
            dialog.setCancelable(false)

            val rewardText = view.findViewById<TextView>(R.id.rewardText)
            val totalCredits = currentLevel?.totalEarnedCredits ?: 0
            rewardText?.text = getString(R.string.dialog_rewards_format, totalCredits)

            val statsText = view.findViewById<TextView>(R.id.statsText)
            val levelTime = currentLevel?.getLevelTimeFormatted() ?: "0:00"
            val destroyedDrones = currentLevel?.destroyedDrones ?: 0
            statsText?.text = getString(R.string.dialog_stats_format, levelTime, destroyedDrones)


            view.findViewById<Button>(R.id.menuButton)?.setOnClickListener {
                dialog.dismiss()
                finishLevel()
            }

            dialog.show()
            Log.d(TAG, "showWinDialog: win dialog shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing win dialog", e)
        }
    }

    fun showLoseDialog() {
        Log.d(TAG, "showLoseDialog: start")
        try {
            lastDialogTypeForTest = "lose"
            if (!isPaused) {
                Log.d(TAG, "showLoseDialog: setting game to paused state")
                isPaused = true
                currentLevel?.pause()
                Log.d(TAG, "showLoseDialog: calling weaponManager.pause()")
                weaponManager.pause()
            }
            val dialog = android.app.Dialog(this)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_lose, null)
            dialog.setContentView(view)
            dialog.setCancelable(false)

            val statsText = view.findViewById<TextView>(R.id.statsText)
            val levelTime = currentLevel?.getLevelTimeFormatted() ?: "0:00"
            val destroyedDrones = currentLevel?.destroyedDrones ?: 0
            statsText?.text = getString(R.string.dialog_stats_format, levelTime, destroyedDrones)


            view.findViewById<Button>(R.id.menuButton)?.setOnClickListener {
                dialog.dismiss()
                finishLevel()
            }

            dialog.show()
            Log.d(TAG, "showLoseDialog: lose dialog shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing lose dialog", e)
        }
    }

    fun setOnPlayerFireListener(listener: () -> Unit) {
        onPlayerFireListener = listener
    }

    fun triggerPlayerFire() {
        Log.d(TAG, "triggerPlayerFire called, onPlayerFireListener=${onPlayerFireListener != null}")
        try {
            onPlayerFireListener?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering player fire", e)
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        Log.d(TAG, "restoreState: start")
        try {
            Log.d(TAG, "restoreState: state restored")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring state", e)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState: start")
        try {
            super.onSaveInstanceState(outState)
            Log.d(TAG, "onSaveInstanceState: state saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving state", e)
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume: start")
        try {
            super.onResume()
            arSession?.resume()
            sceneView.resume()
            
            if (::weaponSpinner.isInitialized && ::turretSpinner.isInitialized) {
                setupSpinners()
            }
            
            Log.d(TAG, "onResume: completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.d(TAG, "onWindowFocusChanged: hasFocus = $hasFocus")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isInitialized) {
            Log.d(TAG, "onWindowFocusChanged: calling ensureFullscreenMode")
            FullscreenManager.ensureFullscreenMode(this)
            Log.d(TAG, "onWindowFocusChanged: ensureFullscreenMode completed")

            if (::weaponSpinner.isInitialized && ::turretSpinner.isInitialized) {
                setupSpinnerDropdownDirection(weaponSpinner)
                setupSpinnerDropdownDirection(turretSpinner)
                Log.d(TAG, "onWindowFocusChanged: spinner directions configured")
            }
        }
        Log.d(TAG, "onWindowFocusChanged: completed")
    }

    override fun onPause() {
        Log.d(TAG, "onPause: start")
        try {
            super.onPause()
            arSession?.pause()
            sceneView.pause()
            Log.d(TAG, "onPause: completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause", e)
        }
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy: start")
        try {
            super.onDestroy()
            cleanupResources()
            Log.d(TAG, "onDestroy: completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    private fun cleanupResources() {
        try {
            if (::gravityGunSound.isInitialized) gravityGunSound.release()
            if (::laserSound.isInitialized) laserSound.release()
            if (::pistolSound.isInitialized) pistolSound.release()
            if (::missileSound.isInitialized) missileSound.release()

            turretBrains.clear()

            Log.d(TAG, "cleanupResources: resources cleaned successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
    }

    fun showCutsceneDialog(title: String, description: String, imageRes: Int, onOk: () -> Unit) {
        Log.d("GameActivityVer2", "showCutsceneDialog called: $title")
        runOnUiThread {
            try {
                val dialog = android.app.Dialog(this)
                val inflater = layoutInflater
                val view = inflater.inflate(R.layout.dialog_cutscene, null)
                dialog.setContentView(view)
                dialog.setCancelable(false)
                view.findViewById<TextView>(R.id.cutsceneTitle)?.text =
                    title.ifBlank { "..." }
                view.findViewById<TextView>(R.id.cutsceneDescription)?.text =
                    description.ifBlank { " " }
                view.findViewById<android.widget.ImageView>(R.id.cutsceneImage)
                    ?.setImageResource(imageRes)
                view.findViewById<Button>(R.id.cutsceneOkButton)
                    ?.setOnClickListener {
                        dialog.dismiss()
                        onOk()
                    }
                dialog.window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.95).toInt(),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
                )
                dialog.show()
            } catch (e: Exception) {
                Log.e("GameActivityVer2", "Exception in showCutsceneDialog", e)
            }
        }
    }

    private fun launchHomingMissile(target: PhysicsNode) {
        Log.d(TAG, "Launching homing missile at target: ${target.hashCode()}")
        val camera = sceneView.scene?.camera ?: return

        val missileParams = com.example.space_war_ar_demo.MissileParams(
            damage = shipData.damage,
            color = com.google.ar.sceneform.rendering.Color(1.0f, 1.0f, 0.0f, 1.0f),
            speed = 0.1f,
            homing = true
        )
        
        Log.d(TAG, "Using WeaponManager.fireMissile for homing missile")
        weaponManager.fireMissile(
            camera.worldPosition,
            camera.forward,
            missileParams,
            target
        )
    }

    fun updateAllStats() {
        try {
            playerHealth.text = getString(R.string.game_health_dynamic_format, shipData.health, shipData.calculateHealth())
            playerCredits.text = getString(R.string.game_money_dynamic_format, shipData.credits)
            playerArmor.text = getString(R.string.game_armor_dynamic_format, shipData.shield)
            playerDamage.text = getString(R.string.game_damage_dynamic_format, shipData.damage)
            playerTurrets.text = getString(R.string.game_turrets_dynamic_format, shipData.turretCount)

            currentLevel?.let { level ->
                val levelNumber = if (currentLevelId in 101..120) currentLevelId - 100 else 0
                if (levelNumber in 1..20) {
                    try {
                        val data =
                            com.example.space_war_ar_demo.levels.LevelBalance.getLevelData(levelNumber)
                        enemyHealth.text = getString(R.string.game_drones_hp_format, data.droneHealth)
                        enemyArmor.text = getString(R.string.game_drones_armor_format, 0)
                        enemyDamage.text = getString(R.string.game_drones_damage_format, data.droneDamage)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error reading LevelBalance for level $levelNumber",
                            e
                        )
                    }
                } else {
                    enemyHealth.text = getString(R.string.game_enemy_health_format, 0)
                    enemyArmor.text = getString(R.string.game_enemy_armor_format, 0)
                    enemyDamage.text = getString(R.string.game_enemy_damage_format, 0)
                }

                val alive = try {
                    level.getDronesForTurret().size
                } catch (_: Exception) {
                    0
                }
                enemyDrones.text = getString(R.string.game_enemy_drones_format, alive)

                waveCount.text = getString(R.string.game_wave_format, level.getCurrentWave())

                val timeToNextWave = level.getTimeToNextWave()
                if (timeToNextWave > 0) {
                    val minutes = timeToNextWave / 60
                    val seconds = timeToNextWave % 60
                    waveTimer.text = getString(R.string.game_wave_timer_format, String.format("%02d:%02d", minutes, seconds))
                } else {
                    waveTimer.text = getString(R.string.game_time_format, level.getLevelTimeFormatted())
                }
            }


        } catch (e: Exception) {
            Log.e(TAG, "Error updating stats", e)
        }
    }

    fun finishLevel() {
        try {
            shipData.fullHeal()
            Log.d(
                TAG,
                "Health restored on level finish: ${shipData.health}/${shipData.calculateHealth()}"
            )

            currentLevel?.let { level ->
                level.cleanup()
            }

            currentLevel?.removeAllChildren()

            handler.removeCallbacksAndMessages(null)

            weaponManager.clearAllProjectiles()

            Log.d(TAG, "Level cleanup completed")

            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing level", e)
            finish()
        }
    }

    private fun updateDroneIndicators() {
        try {
            currentLevel?.let { level ->
                val drones = level.getDronesForTurret()
                updateIndicatorVisibilityForAllTargets(drones)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating drone indicators", e)
        }
    }

    private fun updateIndicatorVisibilityForAllTargets(droneTargets: List<PhysicsNode>) {
        val cam = sceneView.scene?.camera ?: return
        val playerPos = cam.worldPosition
        val camForward = cam.forward
        val camRight = cam.right
        val camUp = cam.up

        val priorityTargets = getPriorityTargets(currentLevel)

        val redSides = computeOffscreenSides(droneTargets, playerPos, camForward, camRight, camUp)
        val yellowSides =
            computeOffscreenSides(priorityTargets, playerPos, camForward, camRight, camUp)

        if (!redSides.any() && !yellowSides.any()) {
            hideAllDroneIndicators()
            return
        }

        applySideIndicator(droneIndicatorRight, red = redSides.right, yellow = yellowSides.right)
        applySideIndicator(droneIndicatorLeft, red = redSides.left, yellow = yellowSides.left)
        applySideIndicator(droneIndicatorTop, red = redSides.top, yellow = yellowSides.top)
        applySideIndicator(droneIndicatorBottom, red = redSides.bottom, yellow = yellowSides.bottom)
    }

    private data class Sides(
        val left: Boolean,
        val right: Boolean,
        val top: Boolean,
        val bottom: Boolean
    ) {
        fun any(): Boolean = left || right || top || bottom
    }

    private fun computeOffscreenSides(
        targets: List<PhysicsNode>,
        playerPos: com.google.ar.sceneform.math.Vector3,
        camForward: com.google.ar.sceneform.math.Vector3,
        camRight: com.google.ar.sceneform.math.Vector3,
        camUp: com.google.ar.sceneform.math.Vector3
    ): Sides {
        var left = false
        var right = false
        var top = false
        var bottom = false
        if (targets.isEmpty()) return Sides(left, right, top, bottom)

        val cosHalfFov = 0.866f
        for (t in targets) {
            val dir = com.google.ar.sceneform.math.Vector3.subtract(t.worldPosition, playerPos)
            val len = dir.length()
            if (len < 0.0001f) continue
            val n = com.google.ar.sceneform.math.Vector3(dir.x / len, dir.y / len, dir.z / len)
            val dotForward = com.google.ar.sceneform.math.Vector3.dot(n, camForward)
            val offscreen = dotForward < cosHalfFov
            if (!offscreen) continue
            val dotRight = com.google.ar.sceneform.math.Vector3.dot(n, camRight)
            val dotUp = com.google.ar.sceneform.math.Vector3.dot(n, camUp)
            if (dotRight > 0f) right = true else if (dotRight < 0f) left = true
            if (dotUp > 0f) top = true else if (dotUp < 0f) bottom = true
        }
        return Sides(left, right, top, bottom)
    }

    private fun applySideIndicator(view: View, red: Boolean, yellow: Boolean) {
        if (yellow) {
            view.visibility = View.VISIBLE
            view.setBackgroundColor(android.graphics.Color.YELLOW)
        } else if (red) {
            view.visibility = View.VISIBLE
            view.setBackgroundColor(android.graphics.Color.RED)
        } else {
            view.visibility = View.GONE
        }
    }

    private fun getPriorityTargets(level: BaseLevel?): List<PhysicsNode> {
        if (level == null) return emptyList()
        val nodes = mutableListOf<PhysicsNode>()
        val nodeFieldNames = listOf(
            "bossNode", "baseNode", "generatorNode", "cryogenicWeaponNode", "installationNode"
        )
        val aliveFieldCandidates = listOf(
            "bossAlive", "baseAlive", "generatorAlive", "cryogenicWeaponAlive", "installationAlive"
        )
        val hpFieldCandidates = listOf(
            "bossHp", "baseHp", "generatorHp", "cryogenicWeaponHp", "installationHp"
        )
        for (name in nodeFieldNames) {
            try {
                val f = level.javaClass.getDeclaredField(name)
                f.isAccessible = true
                val value = f.get(level)
                val node = value as? PhysicsNode ?: continue
                var isAlive = true
                for (aliveName in aliveFieldCandidates) {
                    try {
                        val af = level.javaClass.getDeclaredField(aliveName)
                        af.isAccessible = true
                        val av = af.get(level) as? Boolean
                        if (av != null) {
                            isAlive = isAlive && av
                        }
                    } catch (_: Throwable) {
                    }
                }
                for (hpName in hpFieldCandidates) {
                    try {
                        val hf = level.javaClass.getDeclaredField(hpName)
                        hf.isAccessible = true
                        val hv = hf.get(level) as? Int
                        if (hv != null) {
                            isAlive = isAlive && hv > 0
                        }
                    } catch (_: Throwable) {
                    }
                }
                if (isAlive && node.scene != null) nodes.add(node)
            } catch (_: Throwable) {
            }
        }
        return nodes
    }

    private fun hideAllDroneIndicators() {
        try {
            droneIndicatorRight.visibility = View.GONE
            droneIndicatorLeft.visibility = View.GONE
            droneIndicatorTop.visibility = View.GONE
            droneIndicatorBottom.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding drone indicators", e)
        }
    }

    private fun handleDroneTargeting() {
        try {
            if (shipData.selectedWeapon != "Ракета") {
                if (targetedDrone != null) clearTargetFrame()
                fireButton.isEnabled = true
                return
            }

            val scene = sceneView.scene ?: return
            val camera = scene.camera ?: return
            val drones = currentLevel?.getDronesForTurret() ?: emptyList()

            if (drones.isEmpty()) {
                if (targetedDrone != null) clearTargetFrame()
                fireButton.isEnabled = false
                return
            }

            val cameraPos = camera.worldPosition
            val cameraForward = camera.forward.normalized()
            val maxTargetDist = 20f
            val crosshairThreshold = 1.0f

            Log.d(TAG, "[Targeting] Camera pos: $cameraPos, forward: $cameraForward")
            Log.d(TAG, "[Targeting] Checking ${drones.size} drones")

            var foundDrone: PhysicsNode? = null
            var minDist = Float.MAX_VALUE

            for (drone in drones) {
                val toDrone =
                    com.google.ar.sceneform.math.Vector3.subtract(drone.worldPosition, cameraPos)
                val proj = com.google.ar.sceneform.math.Vector3.dot(toDrone, cameraForward)
                val closestPoint =
                    com.google.ar.sceneform.math.Vector3.add(cameraPos, cameraForward.scaled(proj))
                val dist =
                    com.google.ar.sceneform.math.Vector3.subtract(drone.worldPosition, closestPoint)
                        .length()

                Log.d(
                    TAG,
                    "[Targeting] Drone ${drone.hashCode()} pos=${drone.worldPosition} proj=$proj dist=$dist"
                )

                if (proj > 0 && proj < maxTargetDist && dist < crosshairThreshold && dist < minDist) {
                    foundDrone = drone
                    minDist = dist
                    Log.d(TAG, "[Targeting] Drone ${drone.hashCode()} is under crosshair!")
                }
            }

            val now = System.currentTimeMillis()
            Log.d(
                TAG,
                "[Targeting] foundDrone=${foundDrone?.hashCode()} targetedDrone=${targetedDrone?.hashCode()}"
            )
            val isSameTarget =
                targetedDrone != null && foundDrone != null && targetedDrone === foundDrone

            if (foundDrone != null) {
                if (isSameTarget) {
                    if (!isTargetLocked) {
                        if (now - targetingStartTime >= TARGET_LOCK_TIME) {
                            isTargetLocked = true
                            Log.d(
                                TAG,
                                "[Targeting] LOCKED: смена цвета на ЗЕЛЕНЫЙ (2 сек) drone=${foundDrone.hashCode()}"
                            )
                            setTargetFrameColor(0f, 1f, 0f, 0.7f)
                            fireButton.isEnabled = true
                        } else {
                            if (now - lastFrameBlinkTime > BLINK_INTERVAL) {
                                targetFrameBlinkState = !targetFrameBlinkState
                                Log.d(
                                    TAG,
                                    "[Targeting] BLINK: мигаем ЖЕЛТЫМ drone=${foundDrone.hashCode()} blinkState=$targetFrameBlinkState time=${now - targetingStartTime}"
                                )
                                setTargetFrameColor(
                                    1f,
                                    1f,
                                    0f,
                                    if (targetFrameBlinkState) 0.7f else 0.2f
                                )
                                lastFrameBlinkTime = now
                            }
                            fireButton.isEnabled = false
                        }
                    } else {
                        Log.d(
                            TAG,
                            "[Targeting] ALREADY LOCKED: зелёная рамка drone=${foundDrone.hashCode()}"
                        )
                        setTargetFrameColor(0f, 1f, 0f, 0.7f)
                        fireButton.isEnabled = true
                    }
                } else {
                    Log.d(
                        TAG,
                        "[Targeting] NEW DRONE: hash=${foundDrone.hashCode()} (prev=${targetedDrone?.hashCode()})"
                    )
                    if (targetedDrone != null) clearTargetFrame()
                    targetedDrone = foundDrone
                    targetingStartTime = now
                    isTargetLocked = false
                    addTargetFrameToDrone(foundDrone)
                    setTargetFrameColor(1f, 1f, 0f, 0.7f)
                    targetFrameBlinkState = true
                    lastFrameBlinkTime = now
                    fireButton.isEnabled = false
                }
            } else {
                if (targetedDrone != null) Log.d(
                    TAG,
                    "[Targeting] LOST DRONE: hash=${targetedDrone?.hashCode()}"
                )
                clearTargetFrame()
                fireButton.isEnabled = false
            }

            fireButton.isEnabled = isTargetLocked

        } catch (e: Exception) {
            Log.e(TAG, "Error handling drone targeting", e)
        }
    }


    private fun addTargetFrameToDrone(drone: PhysicsNode) {
        try {
            val frameNode = com.google.ar.sceneform.Node()
            frameNode.setParent(drone)
            frameNode.localPosition = com.google.ar.sceneform.math.Vector3(0f, 0f, 0f)

            com.google.ar.sceneform.rendering.MaterialFactory.makeTransparentWithColor(
                sceneView.context, com.google.ar.sceneform.rendering.Color(1f, 1f, 0f, 0.7f)
            ).thenAccept { material ->
                val box = com.google.ar.sceneform.rendering.ShapeFactory.makeCube(
                    com.google.ar.sceneform.math.Vector3(
                        TARGET_FRAME_SIZE,
                        TARGET_FRAME_SIZE,
                        0.01f
                    ),
                    com.google.ar.sceneform.math.Vector3(0f, 0f, 0f),
                    material
                )
                frameNode.renderable = box
            }
            targetFrameNode = frameNode
            Log.d(TAG, "Target frame added to drone")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding target frame to drone", e)
        }
    }

    private fun setTargetFrameColor(r: Float, g: Float, b: Float, a: Float) {
        try {
            val frameNode = targetFrameNode ?: return
            Log.d(
                TAG,
                "[Targeting] setTargetFrameColor: r=$r g=$g b=$b a=$a node=${frameNode.hashCode()}"
            )
            com.google.ar.sceneform.rendering.MaterialFactory.makeTransparentWithColor(
                sceneView.context, com.google.ar.sceneform.rendering.Color(r, g, b, a)
            ).thenAccept { material ->
                (frameNode.renderable as? com.google.ar.sceneform.rendering.ModelRenderable)?.material =
                    material
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting target frame color", e)
        }
    }

    private fun clearTargetFrame() {
        try {
            Log.d(TAG, "[Targeting] clearTargetFrame: targetedDrone=${targetedDrone?.hashCode()}")
            targetFrameNode?.setParent(null)
            targetFrameNode = null
            targetedDrone = null
            isTargetLocked = false
            targetingStartTime = 0L
            Log.d(TAG, "Target frame cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing target frame", e)
        }
    }

    private fun startLevelFromIntent() {
        try {
            val levelId = intent.getIntExtra("Level", 102)
            Log.d(TAG, "Starting level: $levelId")
            currentLevel = createLevel(levelId)
            currentLevelId = levelId

            currentLevel?.let { level ->
                level.initialize(sceneView)
                Log.d(TAG, "Level initialized successfully: ${level.javaClass.simpleName}")
                
                try {
                    updateMovementButtonsVisibility(level)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update movement buttons visibility: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting level from intent", e)
        }
    }

    private fun createLevel(levelId: Int): BaseLevel? {
        return try {
            hideEvacuationPanel()

            when (levelId) {
                102 -> NewLvl2()
                else -> {
                    Log.w(TAG, "Unknown level ID: $levelId, using NewLvl2")
                    NewLvl2()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating level $levelId", e)
            null
        }
    }



    fun updateLevelTimer(timeLeft: Int) {
        try {
            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)
            waveTimer.text = getString(R.string.game_time_format, timeString)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating level timer", e)
        }
    }

    fun showEvacuationPanel() {
        try {
            val evacuationPanel = findViewById<android.widget.LinearLayout>(R.id.evacuationPanel)
            evacuationPanel?.visibility = View.VISIBLE
            Log.d(TAG, "Evacuation panel shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing evacuation panel", e)
        }
    }

    fun hideEvacuationPanel() {
        try {
            val evacuationPanel = findViewById<android.widget.LinearLayout>(R.id.evacuationPanel)
            evacuationPanel?.visibility = View.GONE
            Log.d(TAG, "Evacuation panel hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding evacuation panel", e)
        }
    }

    fun updateEvacuationTimer(timeLeft: Int) {
        try {
            evacuationProgressText.text = getString(R.string.game_evacuation_format, timeLeft)
            Log.d(TAG, "Evacuation timer updated: $timeLeft seconds left")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating evacuation timer", e)
        }
    }
}
