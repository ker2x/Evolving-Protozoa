package protoevo.env

import protoevo.biology.Cell
import protoevo.biology.MeatCell
import protoevo.biology.PlantCell
import protoevo.biology.Protozoan
import protoevo.core.ChunkManager
import protoevo.core.Settings
import protoevo.core.Simulation
import protoevo.env.RockGeneration.generateRingOfRocks
import protoevo.utils.FileIO.appendLine
import protoevo.utils.Vector2
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

class Tank : Iterable<Cell?>, Serializable {
    @JvmField
	val radius = Settings.tankRadius
    var elapsedTime: Float
        private set
    @JvmField
	val cellCounts = ConcurrentHashMap<Class<out Cell>, Int>(3, 1f)
    @JvmField
	val cellCapacities = ConcurrentHashMap<Class<out Cell>, Int>(3, 1f)
    @JvmField
	val chunkManager: ChunkManager
    @JvmField
	var chemicalSolution: ChemicalSolution? = null
    val rocks: MutableList<Rock?>
    var generation: Long = 1
        private set
    private var protozoaBorn: Long = 0
    private var totalCellsAdded: Long = 0
    private var crossoverEvents: Long = 0
    private var genomeFile: String? = null
    private val genomesToWrite: MutableList<String> = ArrayList()
    private val entitiesToAdd: MutableList<Cell> = ArrayList()
    private var hasInitialised: Boolean

    init {
        val chunkSize = 2 * radius / Settings.numChunkBreaks
        chunkManager = ChunkManager(-radius, radius, -radius, radius, chunkSize)
        chemicalSolution = if (Settings.enableChemicalField) {
            val chemicalGridSize = 2 * radius / Settings.numChemicalBreaks
            ChemicalSolution(-radius, radius, -radius, radius, chemicalGridSize)
        } else {
            null
        }
        rocks = ArrayList()
        elapsedTime = 0f
        hasInitialised = false
    }

    fun initialise() {
        chemicalSolution?.initialise()
        if (!hasInitialised) {
            var clusterCentres: Array<Vector2?>? = null
            if (Settings.initialPopulationClustering) {
                clusterCentres = arrayOfNulls(Settings.numRingClusters)
                for (i in 0 until Settings.numPopulationClusters) {
                    clusterCentres[i] = randomPosition(Settings.populationClusterRadius)
                    generateRingOfRocks(
                        this, clusterCentres[i],
                        5 * Settings.populationClusterRadius
                    )
                }
                for (j in Settings.numPopulationClusters until clusterCentres.size) {
                    clusterCentres[j] = randomPosition(Settings.populationClusterRadius)
                    val maxR = 5 * (Settings.populationClusterRadius + Settings.populationClusterRadiusRange)
                    val minR =
                        Math.max(0.1f, 5 * (Settings.populationClusterRadius - Settings.populationClusterRadiusRange))
                    val radius = Simulation.RANDOM.nextFloat() * (maxR - minR) + minR
                    RockGeneration.generateRingOfRocks(this, clusterCentres[j], radius, 0.05f)
                }
            }
            RockGeneration.generateRocks(this)
            rocks.forEach(Consumer { rock: Rock? -> chunkManager.allocateToChunk(rock!!) })
            if (clusterCentres != null) initialisePopulation(
                Arrays.copyOfRange(
                    clusterCentres,
                    0,
                    Settings.numPopulationClusters
                )
            ) else initialisePopulation()
            flushEntitiesToAdd()
            if (Settings.writeGenomes && genomeFile != null) writeGenomeHeaders()
            hasInitialised = true
        }
    }

    fun writeGenomeHeaders() {
        val protozoan = chunkManager.allCells
            .stream()
            .filter { cell: Cell? -> cell is Protozoan }
            .map { cell: Cell -> cell as Protozoan }
            .findAny()
            .orElseThrow { RuntimeException("No initial population present") }
        val headerStr = StringBuilder()
        headerStr.append("Generation,Time Elapsed,Parent 1 ID,Parent 2 ID,ID,")
        for (gene in protozoan.genome.genes) headerStr.append(gene.traitName).append(",")
        appendLine(genomeFile, headerStr.toString())
    }

    fun hasBeenInitialised(): Boolean {
        return hasInitialised
    }

    fun initialisePopulation(clusterCentres: Array<Vector2?>?) {
        var findPlantPosition = Function { entityRadius: Float -> this.randomPosition(entityRadius) }
        var findProtozoaPosition = Function { entityRadius: Float -> this.randomPosition(entityRadius) }
        if (clusterCentres != null) {
            findPlantPosition = Function { r: Float -> randomPosition(1.5f * r, clusterCentres) }
            findProtozoaPosition = Function { r: Float -> randomPosition(r, clusterCentres) }
        }
        for (i in 0 until Settings.numInitialPlantPellets) addRandom(PlantCell(this), findPlantPosition)
        for (i in 0 until Settings.numInitialProtozoa) {
            try {
                addRandom(Protozoan(this), findProtozoaPosition)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initialisePopulation() {
        val clusterCentres = arrayOfNulls<Vector2>(Settings.numPopulationClusters)
        for (i in clusterCentres.indices) clusterCentres[i] = randomPosition(Settings.populationClusterRadius)
        initialisePopulation(clusterCentres)
    }

    fun randomPosition(entityRadius: Float, clusterCentres: Array<Vector2?>): Vector2 {
        val clusterIdx = Simulation.RANDOM.nextInt(clusterCentres.size)
        val clusterCentre = clusterCentres[clusterIdx]
        return randomPosition(entityRadius, clusterCentre, Settings.populationClusterRadius)
    }

    @JvmOverloads
    fun randomPosition(entityRadius: Float, centre: Vector2? = Vector2.ZERO, clusterRadius: Float = radius): Vector2 {
        val rad = clusterRadius - 4 * entityRadius
        val t = (2 * Math.PI * Simulation.RANDOM.nextDouble()).toFloat()
        val r = 2 * entityRadius + rad * Simulation.RANDOM.nextFloat()
        return Vector2((r * Math.cos(t.toDouble())).toFloat(), (r * Math.sin(t.toDouble())).toFloat()).add(
            centre!!
        )
    }

    fun handleTankEdge(e: Cell) {
        val rPos = e.pos!!.len()
        if (Settings.sphericalTank && rPos - e.radius > radius) e.pos!!.setLength(-0.98f * radius) else if (rPos + e.radius > radius) {
            e.pos!!.setLength(radius - e.radius)
            val normal = e.pos!!.unit().scale(-1f)
            e.getVel().translate(normal.mul(-2 * normal.dot(e.getVel())))
        }
    }

    fun updateCell(e: Cell, delta: Float) {
        e.handleInteractions(delta)
        e.update(delta)
        handleTankEdge(e)
    }

    private fun flushEntitiesToAdd() {
        entitiesToAdd.forEach(Consumer { e: Cell? -> chunkManager.add(e) })
        entitiesToAdd.clear()
        chunkManager.update()
    }

    private fun flushWrites() {
        val genomeWritesHandled: MutableList<String> = ArrayList()
        for (line in genomesToWrite) {
            appendLine(genomeFile, line)
            genomeWritesHandled.add(line)
        }
        genomesToWrite.removeAll(genomeWritesHandled)
    }

    fun update(delta: Float) {
        elapsedTime += delta
        flushEntitiesToAdd()
        flushWrites()
        val cells = chunkManager.allCells
        cells.parallelStream().forEach { obj: Cell -> obj.resetPhysics() }
        cells.parallelStream().forEach { cell: Cell -> updateCell(cell, delta) }
        cells.parallelStream().forEach { cell: Cell -> cell.physicsUpdate(delta) }
        cells.parallelStream().forEach { e: Cell -> handleDeadEntities(e) }
        updateCounts(cells)
        chemicalSolution?.update(delta, cells)
    }

    private fun updateCounts(entities: Collection<Cell>) {
        cellCounts.clear()
        for (e in entities) cellCounts[e.javaClass] = 1 + cellCounts.getOrDefault(e.javaClass, 0)
    }

    private fun handleDeadEntities(e: Cell) {
        if (!e.isDead) return
        e.handleDeath()
    }

    private fun handleNewProtozoa(p: Protozoan) {
        protozoaBorn++
        generation = Math.max(generation, p.generation.toLong())
        if (genomeFile != null && Settings.writeGenomes) {
            val genomeLine = p.generation.toString() + "," + elapsedTime + "," + p.genome.toString()
            genomesToWrite.add(genomeLine)
        }
    }

    fun add(e: Cell) {
        if (cellCounts.getOrDefault(e.javaClass, 0)
            >= cellCapacities.getOrDefault(e.javaClass, 0)
        ) return
        totalCellsAdded++
        entitiesToAdd.add(e)
        if (e is Protozoan) handleNewProtozoa(e)
    }

    val entities: Collection<Cell>
        get() = chunkManager.allCells

    fun getStats(includeProtozoaStats: Boolean): Map<String, Float> {
        val stats: MutableMap<String, Float> = TreeMap()
        stats["Protozoa"] = numberOfProtozoa().toFloat()
        stats["Plants"] = cellCounts.getOrDefault(PlantCell::class.java, 0).toFloat()
        stats["Meat Pellets"] = cellCounts.getOrDefault(MeatCell::class.java, 0).toFloat()
        stats["Max Generation"] = generation.toFloat()
        stats["Time Elapsed"] = elapsedTime
        stats["Protozoa Born"] = protozoaBorn.toFloat()
        stats["Total Entities Born"] = totalCellsAdded.toFloat()
        stats["Crossover Events"] = crossoverEvents.toFloat()
        if (includeProtozoaStats) stats.putAll(protozoaStats)
        return stats
    }

    val stats: Map<String, Float>
        get() = getStats(false)
    val protozoaStats: Map<String, Float>
        get() {
            val stats: MutableMap<String, Float> = TreeMap()
            val protozoa: Collection<Protozoan> = chunkManager.allCells
                .stream()
                .filter { cell: Cell? -> cell is Protozoan }
                .map { cell: Cell -> cell as Protozoan }
                .collect(Collectors.toSet())
            for (e in protozoa) {
                for ((key1, value) in e.stats) {
                    val key = "Sum $key1"
                    val currentValue = stats.getOrDefault(key, 0f)
                    stats[key] = value + currentValue
                }
            }
            val numProtozoa = protozoa.size
            for (e in protozoa) {
                for ((key, value) in e.stats) {
                    val sumValue = stats.getOrDefault("Sum $key", 0f)
                    val mean = sumValue / numProtozoa
                    stats["Mean $key"] = mean
                    val currVar = stats.getOrDefault("Var $key", 0f)
                    val deltaVar = Math.pow((value - mean).toDouble(), 2.0).toFloat() / numProtozoa
                    stats["Var $key"] = currVar + deltaVar
                }
            }
            return stats
        }

    fun numberOfProtozoa(): Int {
        return cellCounts.getOrDefault(Protozoan::class.java, 0)
    }

    fun numberOfPellets(): Int {
        var nPellets = cellCounts.getOrDefault(PlantCell::class.java, 0)
        nPellets += cellCounts.getOrDefault(MeatCell::class.java, 0)
        return nPellets
    }

    override fun iterator(): Iterator<Cell> {
        return chunkManager!!.allCells!!.iterator()
    }

    fun isCollidingWithAnything(e: Cell): Boolean {
        return if (chunkManager.allCells.stream()
                .anyMatch { other: Cell? -> e.isCollidingWith(other) }
        ) true else rocks.stream().anyMatch { rock: Rock? -> e.isCollidingWith(rock) }
    }

    fun addRandom(e: Cell, findPosition: Function<Float, Vector2>) {
        for (i in 0..4) {
            e.pos = findPosition.apply(e.radius)
            if (!isCollidingWithAnything(e)) {
                add(e)
                return
            }
        }
    }

    fun setGenomeFile(genomeFile: String?) {
        this.genomeFile = genomeFile
    }

    fun registerCrossoverEvent() {
        crossoverEvents++
    }

    companion object {
        private const val serialVersionUID = 2804817237950199223L
    }
}