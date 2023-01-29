package protoevo.env

import protoevo.biology.Cell
import protoevo.core.Collidable
import protoevo.core.Simulation
import protoevo.utils.Vector2
import java.awt.Color
import java.io.Serializable

class Rock(p1: Vector2?, p2: Vector2?, p3: Vector2?) : Collidable(), Serializable {
    @JvmField
    val points: Array<Vector2?>
    val edges: Array<Array<Vector2?>>
    private val edgeAttachStates: BooleanArray
    val centre: Vector2
    val normals: Array<Vector2?>
    private val boundingBox: Array<Vector2?>?
    private val colour: Color
    private fun computeCentre(): Vector2 {
        val c = Vector2(0f, 0f)
        for (p in points) c.translate(p!!)
        return c.scale(1f / points.size)
    }

    private fun computeBounds(): Array<Vector2?> {
        val minX = Math.min(points[0]!!.x, Math.min(points[1]!!.x, points[2]!!.x))
        val minY = Math.min(points[0]!!.y, Math.min(points[1]!!.y, points[2]!!.y))
        val maxX = Math.max(points[0]!!.x, Math.max(points[1]!!.x, points[2]!!.x))
        val maxY = Math.max(points[0]!!.y, Math.max(points[1]!!.y, points[2]!!.y))
        return arrayOf(Vector2(minX, minY), Vector2(maxX, maxY))
    }


    override fun handlePotentialCollision(other: Collidable?, delta: Float): Boolean {
        return if (other is Cell) other.handlePotentialCollision(this, delta) else false
    }

    fun isEdgeAttached(edgeIdx: Int): Boolean {
        return edgeAttachStates[edgeIdx]
    }

    fun setEdgeAttached(edgeIdx: Int) {
        edgeAttachStates[edgeIdx] = true
    }

    private fun computeNormals(): Array<Vector2?> {
        val normals = arrayOfNulls<Vector2>(3)
        val edges = edges
        for (i in edges.indices) normals[i] = normal(edges[i][0], edges[i][1])
        return normals
    }

    private fun normal(p1: Vector2?, p2: Vector2?): Vector2 {
        val n = p1!!.sub(p2!!).perp().unit()
        if (n.dot(p1.sub(centre)) < 0) n.scale(-1f)
        return n
    }

    fun getEdge(i: Int): Array<Vector2?> {
        val edges = edges
        return edges[i]
    }

    private fun sign(p1: Vector2, p2: Vector2?, p3: Vector2?): Float {
        return ((p1.x - p3!!.x) * (p2!!.y - p3.y)
                - (p2.x - p3.x) * (p1.y - p3.y))
    }

    override fun pointInside(x: Vector2?): Boolean {
        val d1 = sign(x!!, points[0], points[1])
        val d2 = sign(x, points[1], points[2])
        val d3 = sign(x, points[2], points[0])
        val hasNeg = d1 < 0 || d2 < 0 || d3 < 0
        val hasPos = d1 > 0 || d2 > 0 || d3 > 0
        return !(hasNeg && hasPos)
    }

    override fun rayIntersects(start: Vector2?, end: Vector2?): Boolean {
        return false
    }

    private val intersectTs = FloatArray(2)

    init {
        points = arrayOf(p1, p2, p3)
        edges = arrayOf(
            arrayOf(points[0], points[1]), arrayOf(
                points[1], points[2]
            ), arrayOf(points[0], points[2])
        )
        edgeAttachStates = booleanArrayOf(false, false, false)
        centre = computeCentre()
        normals = computeNormals()
        colour = randomRockColour()
        boundingBox = computeBounds()
    }

    override fun rayCollisions(start: Vector2?, end: Vector2?, collisions: Array<Collision>) {
        for (collision in collisions) collision.collided = false
        val dirRay = end!!.take(start!!)
        var bestT = Float.MAX_VALUE
        for (i in edges.indices) {
            if (isEdgeAttached(i)) continue
            val edge = edges[i]
            val dirEdge = edge[1]!!.sub(edge[0]!!)
            val coefs = edgesIntersectCoef(start, dirRay, edge[0], dirEdge, intersectTs)
            if (coefs != null && edgeIntersectCondition(coefs)) {
                val t = coefs[0]
                if (t > bestT) continue
                bestT = t
                collisions[0].point.set(start)
                    .translate(dirRay.x * t, dirRay.y * t)
                collisions[0].collided = true
            }
        }
    }

    override fun getColor(): Color {
        return colour
    }

    override fun getMass(): Float {
        TODO("Not yet implemented")
    }

    override fun getBoundingBox(): Array<Vector2?>? {
        return boundingBox
    }

    fun intersectsWith(otherRock: Rock?): Boolean {
        for (e1 in otherRock!!.edges) for (e2 in edges) if (edgesIntersect(e1, e2)) return true
        return false
    }

    fun allEdgesAttached(): Boolean {
        return isEdgeAttached(0) && isEdgeAttached(1) && isEdgeAttached(2)
    }

    companion object {
        const val serialVersionUID = 1L
        fun edgesIntersectCoef(
            start1: Vector2?,
            dir1: Vector2,
            start2: Vector2?,
            dir2: Vector2,
            ts: FloatArray
        ): FloatArray? {
            val coef00 = dir1.len2()
            val coef01 = -dir1.dot(dir2)
            val coef10 = -dir2.dot(dir1)
            val coef11 = dir2.len2()
            val const0 = start2!!.dot(dir1) - start1!!.dot(dir1)
            val const1 = start1.dot(dir2) - start2.dot(dir2)
            val det = coef00 * coef11 - coef10 * coef01
            if (det == 0f) return null
            val t1 = (const0 * coef11 - const1 * coef01) / det
            val t2 = (-const0 * coef10 + const1 * coef00) / det
            ts[0] = t1
            ts[1] = t2
            return ts
        }

        fun edgeIntersectCondition(coefs: FloatArray): Boolean {
            val t1 = coefs[0]
            val t2 = coefs[1]
            return 0f < t1 && t1 < 1f && 0f < t2 && t2 < 1f
        }

        fun edgesIntersect(start1: Vector2?, dir1: Vector2, start2: Vector2?, dir2: Vector2): Boolean {
            val coefs = edgesIntersectCoef(start1, dir1, start2, dir2, FloatArray(2)) ?: return false
            return edgeIntersectCondition(coefs)
        }

        fun edgesIntersect(e1: Array<Vector2?>?, e2: Array<Vector2?>): Boolean {
            val dir1 = e1!![1]!!.sub(e1[0]!!)
            val dir2 = e2[1]!!.sub(e2[0]!!)
            return edgesIntersect(e1[0], dir1, e2[0], dir2)
        }

        fun randomRockColour(): Color {
            val tone = 80 + Simulation.RANDOM.nextInt(20)
            val yellowing = Simulation.RANDOM.nextInt(20)
            return Color(tone + yellowing, tone + yellowing, tone)
        }
    }
}