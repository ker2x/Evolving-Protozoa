package protoevo.env

import protoevo.biology.Cell
import protoevo.biology.PlantCell
import protoevo.core.Particle
import protoevo.core.Settings
import protoevo.utils.Vector2
import java.io.Serializable
import java.util.*

class ChemicalSolution(
    private val xMin: Float, private val xMax: Float,
    private val yMin: Float, private val yMax: Float,
    val gridSize: Float
) : Serializable {
    val nYChunks: Int
    val nXChunks: Int
    private val chemicalGrid: Array<Array<Chemical?>>
    private var timeSinceUpdate = 0f

    init {
        nXChunks = 2 + ((xMax - xMin) / gridSize).toInt()
        nYChunks = 2 + ((yMax - yMin) / gridSize).toInt()
        chemicalGrid = Array(nXChunks) { arrayOfNulls(nYChunks) }
        for (i in 0 until nXChunks) for (j in 0 until nYChunks) chemicalGrid[i][j] = Chemical()
    }

    fun initialise() {
        for (i in 1 until nXChunks - 1) for (j in 1 until nYChunks - 1) chemicalGrid[i][j]!!.setNeighbours(
            chemicalGrid[i][j - 1]!!,
            chemicalGrid[i][j + 1]!!,
            chemicalGrid[i - 1][j]!!,
            chemicalGrid[i + 1][j]!!
        )
    }

    fun toTankCoords(i: Int, j: Int): Vector2 {
        val x = (i - 1) * gridSize + xMin
        val y = (j - 1) * gridSize + yMin
        return Vector2(x, y)
    }

    fun toChemicalGridX(x: Float): Int {
        val i = (1 + (x - xMin) / gridSize).toInt()
        if (i < 0) return 0
        return if (i >= nXChunks) nXChunks - 1 else i
    }

    fun toChemicalGridY(y: Float): Int {
        val j = (1 + (y - yMin) / gridSize).toInt()
        if (j < 0) return 0
        return if (j >= nYChunks) nYChunks - 1 else j
    }

    fun depositChemicals(delta: Float, e: Cell) {
        if (e is PlantCell && !e.isDead()) {
            val i = toChemicalGridX(e.pos!!.x)
            val j = toChemicalGridY(e.pos!!.y)
            val k = Settings.plantPheromoneDeposit
            chemicalGrid[i][j]!!.currentPlantPheromoneDensity += delta * k * e.getRadius() * e.getHealth()
        }
    }

    fun update(delta: Float, entities: Collection<Cell>) {
        timeSinceUpdate += delta
        if (timeSinceUpdate >= Settings.chemicalsUpdateTime) {
            entities.parallelStream().forEach { e: Cell -> depositChemicals(timeSinceUpdate, e) }
            Arrays.stream(chemicalGrid).parallel().forEach { row: Array<Chemical?>? ->
                Arrays.stream(row).forEach { chemical: Chemical? -> chemical!!.propagate(timeSinceUpdate) }
            }
            Arrays.stream(chemicalGrid).parallel()
                .forEach { row: Array<Chemical?>? -> Arrays.stream(row).forEach { obj: Chemical? -> obj!!.update() } }
            timeSinceUpdate = 0f
        }
    }

    fun getPlantPheromoneGradientX(i: Int, j: Int): Float {
        return if (i < 1 || i >= nXChunks - 1) 0f else chemicalGrid[i - 1][j]!!.currentPlantPheromoneDensity - chemicalGrid[i + 1][j]!!.currentPlantPheromoneDensity
    }

    fun getPlantPheromoneGradientY(i: Int, j: Int): Float {
        return if (j < 1 || j >= nYChunks - 1) 0f else chemicalGrid[i][j - 1]!!.currentPlantPheromoneDensity - chemicalGrid[i][j + 1]!!.currentPlantPheromoneDensity
    }

    fun getPlantPheromoneDensity(i: Int, j: Int): Float {
        return chemicalGrid[i][j]!!.currentPlantPheromoneDensity
    }

    companion object {
        const val serialVersionUID = 1L
    }
}