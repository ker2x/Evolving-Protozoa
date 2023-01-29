package protoevo.core

import com.google.common.collect.Iterators
import protoevo.biology.Cell
import protoevo.env.Rock
import protoevo.utils.Vector2
import java.io.Serializable
import java.util.function.Consumer
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

class ChunkManager(
    private val xMin: Float, private val xMax: Float,
    private val yMin: Float, private val yMax: Float,
    val chunkSize: Float
) : Serializable {
    private val nYChunks: Int
    private val nXChunks: Int
    val chunks: Array<Chunk?>
    private val entities: MutableList<Cell> = ArrayList()

    init {
        nXChunks = 2 + ((xMax - xMin) / chunkSize).toInt()
        nYChunks = 2 + ((yMax - yMin) / chunkSize).toInt()
        chunks = arrayOfNulls(nXChunks * nYChunks)
        for (i in 0 until nXChunks) for (j in 0 until nYChunks) chunks[toChunkID(i, j)] = Chunk(i, j, this)
    }

    private fun <T : Collidable?> broadScan(
        pos: Vector2,
        range: Float,
        scanner: Function<Chunk?, Iterator<T>?>
    ): Iterator<T> {
        val x = pos.x
        val y = pos.y
        val iMin = toChunkX(x - range)
        val iMax = toChunkX(x + range)
        val jMin = toChunkY(y - range)
        val jMax = toChunkY(y + range)
        val iterators: MutableList<Iterator<T>?> = ArrayList((iMax - iMin + 1) * (jMax - jMin + 1))
        for (i in iMin..iMax) {
            for (j in jMin..jMax) {
                val chunk = getChunk(toChunkID(i, j))
                iterators.add(scanner.apply(chunk))
            }
        }
        return Iterators.concat(iterators.iterator())
    }

    fun broadCollisionDetection(pos: Vector2, range: Float): Iterator<Collidable?> {
        return broadScan(pos, range) { obj: Chunk? -> obj!!.collidables }
    }

    fun broadEntityDetection(pos: Vector2, range: Float): Iterator<Cell?> {
        return broadScan(pos, range) { chunk: Chunk? -> chunk!!.cells.iterator() }
    }

    private fun toChunkX(tankX: Float): Int {
        val i = (1 + (tankX - xMin) / chunkSize).toInt()
        if (i < 0) return 0
        return if (i >= nXChunks) nXChunks - 1 else i
    }

    private fun toChunkY(tankY: Float): Int {
        val j = (1 + (tankY - yMin) / chunkSize).toInt()
        if (j < 0) return 0
        return if (j >= nYChunks) nYChunks - 1 else j
    }

    private fun toChunkID(i: Int, j: Int): Int {
        val id = i + j * nYChunks
        if (id < 0) return 0
        return if (id >= nXChunks * nYChunks) nXChunks * nYChunks - 1 else id % (nXChunks * nYChunks)
    }

    private fun toChunkID(x: Float, y: Float): Int {
        val i = (1 + (x - xMin) / chunkSize).toInt()
        val j = (1 + (y - yMin) / chunkSize).toInt()
        return toChunkID(i, j)
    }

    fun toTankCoords(chunkCoords: Vector2): Vector2 {
        val x = (chunkCoords.x - 1) * chunkSize + xMin
        val y = (chunkCoords.y - 1) * chunkSize + yMin
        return Vector2(x, y)
    }

    private fun getChunk(e: Cell): Chunk? {
        return getChunk(e.pos)
    }

    private fun getChunk(pos: Vector2?): Chunk? {
        val chunkID = this.toChunkID(pos!!.x, pos.y)
        return getChunk(chunkID)
    }

    private fun getChunk(chunkID: Int): Chunk? {
        return chunks[chunkID]
    }

    private fun allocateToChunk(e: Cell) {
        val chunk = getChunk(e)
        chunk!!.addEntity(e)
    }

    fun add(e: Cell?) {
        if (e != null) entities.add(e)
    }

    val allCells: Collection<Cell>
        get() = entities

    fun update() {
        for (chunk in chunks) chunk!!.clear()
        entities.removeIf { obj: Cell -> obj.isDead() }
        entities.forEach(Consumer { e: Cell -> this.allocateToChunk(e) })
    }

    fun allocateToChunk(rock: Rock) {
        var iMax = Int.MIN_VALUE
        var iMin = Int.MAX_VALUE
        var jMax = Int.MIN_VALUE
        var jMin = Int.MAX_VALUE
        for (p in rock.points) {
            val chunk = getChunk(p)
            val i = chunk!!.chunkCoords.x.toInt()
            val j = chunk.chunkCoords.y.toInt()
            iMax = max(i, iMax)
            iMin = min(i, iMin)
            jMax = max(j, jMax)
            jMin = min(j, jMin)
        }
        for (i in iMin..iMax) for (j in jMin..jMax) chunks[toChunkID(i, j)]!!.addRock(rock)
    }

    companion object {
        const val serialVersionUID = 1L
    }
}