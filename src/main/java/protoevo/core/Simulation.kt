package protoevo.core

import com.github.javafaker.Faker
import protoevo.biology.MeatCell
import protoevo.biology.PlantCell
import protoevo.biology.Protozoan
import protoevo.env.Tank
import protoevo.utils.FileIO.appendLine
import protoevo.utils.FileIO.load
import protoevo.utils.FileIO.save
import protoevo.utils.Utils.timeSeconds
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors

class Simulation {
    var tank: Tank? = null
        private set
    private var simulate: Boolean
    private var pause = false
    var timeDilation = 1f
    private var timeSinceSave = 0f
    private var timeSinceSnapshot = 0f
    private var updateDelay = Application.refreshDelay / 1000.0
    private var lastUpdateTime = 0.0
    private var debug = false
    private var delayUpdate = true
    private val name: String
    private val genomeFile: String
    private val historyFile: String
    private var statsNames: List<String>? = null

    constructor() {
        simulate = true
        name = generateSimName()
        println("Created new simulation named: $name")
        genomeFile = "saves/$name/genomes.csv"
        historyFile = "saves/$name/history.csv"
        settingsPath = "saves/$name/settings.yaml"
        newSaveDir()
        newDefaultTank()
        loadSettings()
        RANDOM = Random(Settings.simulationSeed)
    }

    constructor(name: String) {
        simulate = true
        this.name = name
        genomeFile = "saves/$name/genomes.csv"
        historyFile = "saves/$name/history.csv"
        settingsPath = "saves/$name/settings.yaml"
        newSaveDir()
        loadMostRecentTank()
        loadSettings()
        RANDOM = Random(Settings.simulationSeed)
    }

    constructor(name: String, save: String) {
        simulate = true
        this.name = name
        genomeFile = "saves/$name/genomes.csv"
        historyFile = "saves/$name/history.csv"
        newSaveDir()
        loadTank("saves/$name/tank/$save")
        loadSettings()
        RANDOM = Random(Settings.simulationSeed)
    }

    private fun loadSettings() {
        tank!!.cellCapacities[Protozoan::class.java] = Settings.maxProtozoa
        tank!!.cellCapacities[PlantCell::class.java] = Settings.maxPlants
        tank!!.cellCapacities[MeatCell::class.java] = Settings.maxMeat
    }

    private fun newSaveDir() {
        try {
            val saveDir = Paths.get("saves/$name")
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir)
                Files.createDirectories(Paths.get("saves/$name/tank"))
                val original = File(defaultSettingsPath)
                val copied = File(settingsPath)
                com.google.common.io.Files.copy(original, copied)
            }
            val genomePath = Paths.get(genomeFile)
            if (!Files.exists(genomePath)) Files.createFile(genomePath)
            val historyPath = Paths.get(historyFile)
            if (!Files.exists(historyPath)) Files.createFile(historyPath)
            val seedFile = "saves/$name/seed.txt"
            val seedPath = Paths.get(seedFile)
            if (!Files.exists(seedPath)) {
                try {
                    Files.createFile(seedPath)
                    appendLine(seedFile, Settings.simulationSeed.toString() + "")
                } catch (e: IOException) {
                    println("Failed to create seed file.")
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun newDefaultTank() {
        tank = Tank()
        loadSettings()
        tank!!.setGenomeFile(genomeFile)
    }

    fun setupTank() {
        tank!!.initialise()
    }

    fun loadTank(filename: String) {
        try {
            tank = load(filename) as Tank
            println("Loaded tank at: $filename")
        } catch (e: IOException) {
            println("Unable to load tank at " + filename + " because: " + e.message)
            newDefaultTank()
        } catch (e: ClassNotFoundException) {
            println("Unable to load tank at " + filename + " because: " + e.message)
            newDefaultTank()
        }
    }

    fun loadMostRecentTank() {
        val dir = Paths.get("saves/$name/tank")
        if (Files.exists(dir)) try {
            Files.list(dir).use { pathStream ->
                val lastFilePath = pathStream
                    .filter { f: Path? -> !Files.isDirectory(f) }
                    .max(Comparator.comparingLong { f: Path -> f.toFile().lastModified() })
                lastFilePath.ifPresentOrElse(
                    { path: Path -> loadTank(path.toString().replace(".dat", "")) }) { newDefaultTank() }
            }
        } catch (e: IOException) {
            newDefaultTank()
        } else newDefaultTank()
    }

    fun simulate() {
        setupTank()
        makeHistorySnapshot()
        while (simulate) {
            if (pause) continue
            if (delayUpdate && updateDelay > 0) {
                val currTime = timeSeconds
                if (currTime - lastUpdateTime > updateDelay) {
                    update()
                    lastUpdateTime = currTime
                }
            } else {
                update()
            }
            if (tank!!.numberOfProtozoa() <= 0 && Settings.finishOnProtozoaExtinction) {
                simulate = false
                println()
                println("Finished simulation. All protozoa died.")
                printStats()
            }
        }
    }

    fun printStats() {
        tank!!.getStats(true).forEach { (k: String?, v: Float?) -> System.out.printf("%s: %.5f\n", k, v) }
    }

    fun update() {
        val delta = timeDilation * Settings.simulationUpdateDelta
        synchronized(tank!!) { tank!!.update(delta) }
        timeSinceSave += delta
        if (timeSinceSave > Settings.timeBetweenSaves) {
            timeSinceSave = 0f
            saveTank()
        }
        timeSinceSnapshot += delta
        if (timeSinceSnapshot > Settings.historySnapshotTime) {
            timeSinceSnapshot = 0f
            makeHistorySnapshot()
        }
    }

    fun close() {
        simulate = false
        println()
        println("Closing simulation.")
        saveTank()
    }

    fun saveTank() {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
        val fileName = "saves/$name/tank/$timeStamp"
        save(tank, fileName)
    }

    fun makeHistorySnapshot() {
        val stats = tank!!.getStats(true)
        if (statsNames == null) {
            statsNames = ArrayList(tank!!.getStats(true).keys)
            val statsCsvHeader = java.lang.String.join(",", statsNames)
            appendLine(historyFile, statsCsvHeader)
        }
        val statsString = statsNames!!.stream()
            .map { k: String -> String.format("%.5f", stats[k]) }
            .collect(Collectors.joining(","))
        appendLine(historyFile, statsString)
    }

    fun toggleDebug() {
        debug = !debug
    }

    fun togglePause() {
        pause = !pause
    }

    fun inDebugMode(): Boolean {
        return debug
    }

    val generation: Long
        get() = tank!!.generation
    val elapsedTime: Float
        get() = tank!!.elapsedTime

    fun setUpdateDelay(updateDelay: Float) {
        this.updateDelay = updateDelay.toDouble()
    }

    fun toggleUpdateDelay() {
        delayUpdate = !delayUpdate
    }

    companion object {
        var defaultSettingsPath = "config/default_settings.yaml"
        @JvmField
		var settingsPath = defaultSettingsPath
        lateinit var RANDOM: Random
        fun generateSimName(): String {
            val faker = Faker()
            return String.format(
                "%s-%s-%s",
                faker.ancient().primordial().lowercase(Locale.getDefault()).replace(" ".toRegex(), "-"),
                faker.pokemon().name().lowercase(Locale.getDefault()).replace(" ".toRegex(), "-"),
                faker.lorem().word().lowercase(Locale.getDefault()).replace(" ".toRegex(), "-")
            )
        }
    }
}