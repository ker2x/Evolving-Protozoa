package protoevo.core

import com.google.common.collect.Iterators
import protoevo.biology.Cell
import protoevo.env.Rock
import protoevo.utils.Vector2
import java.io.Serializable

class Chunk(private val x: Int, private val y: Int, private val chunkManager: ChunkManager) : Serializable {
    private val entities: MutableList<Cell?>
    private val rocks: MutableList<Rock>

    init {
        entities = ArrayList()
        rocks = ArrayList()
    }

    val chunkCoords: Vector2
        get() = Vector2(x.toFloat(), y.toFloat())
    val tankCoords: Vector2
        get() = chunkManager.toTankCoords(chunkCoords)
    val cells: Collection<Cell?>
        get() = entities

    fun addEntity(e: Cell?) {
        entities.add(e)
    }

    fun addRock(rock: Rock) {
        rocks.add(rock)
    }

    operator fun contains(rock: Rock): Boolean {
        return rocks.contains(rock)
    }

    override fun equals(o: Any?): Boolean {
        if (o is Chunk) {
            val otherChunk = o
            return otherChunk.x == x && otherChunk.y == y
        }
        return false
    }

    fun clear() {
        entities.clear()
    }

    val collidables: Iterator<Collidable?>
        get() = Iterators.concat<Collidable?>(entities.iterator(), rocks.iterator())

    fun getRocks(): Collection<Rock> {
        return rocks
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + chunkManager.hashCode()
        result = 31 * result + entities.hashCode()
        result = 31 * result + rocks.hashCode()
        return result
    }

    companion object {
        const val serialVersionUID = 4697424153087580763L
    }
}