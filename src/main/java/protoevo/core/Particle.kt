package protoevo.core

import protoevo.env.Rock
import protoevo.env.Tank
import protoevo.utils.Geometry.circleIntersectLineCoefficients
import protoevo.utils.Geometry.doesLineIntersectCircle
import protoevo.utils.Geometry.getSphereVolume
import protoevo.utils.Geometry.isPointInsideCircle
import protoevo.utils.Geometry.lineIntersectCondition
import protoevo.utils.Vector2
import java.awt.Color
import java.io.Serializable
import kotlin.math.sqrt

open class Particle(@JvmField val tank: Tank) : Collidable(), Serializable {
    @JvmField
    var pos: Vector2? = null
    private var prevPos: Vector2? = null
    private var vel: Vector2? = null
    private val acc = Vector2(0f, 0f)
    private var radius = 0f
    var recentRigidCollisions = 0
        private set

    fun resetPhysics() {
        acc[0f] = 0f
        recentRigidCollisions = 0
    }

    fun physicsUpdate(delta: Float) {
        val subStepDelta = delta / Settings.physicsSubSteps
        for (i in 0 until Settings.physicsSubSteps) physicsStep(subStepDelta)
    }

    open fun physicsStep(delta: Float) {
        val chunkManager = tank.chunkManager
        val entities = chunkManager.broadCollisionDetection(pos!!, radius)
        entities.forEachRemaining { o: Collidable? -> handlePotentialCollision(o, delta) }
        if (prevPos == null) prevPos = pos!!.copy()
        vel = if (delta != 0f) pos!!.sub(prevPos!!).scale(1 / delta) else return
        move(delta)
    }

    private fun move(delta: Float) {
        val verletVel = pos!!.sub(prevPos!!).scale(1f - Settings.tankFluidResistance)
        val dx = verletVel.translate(acc.mul(delta * delta))
        if (dx.len2() > Settings.maxParticleSpeed * Settings.maxParticleSpeed) dx.setLength(Settings.maxParticleSpeed)
        prevPos!!.set(pos!!)
        pos!!.translate(dx)
    }

    fun handleBindingConstraint(attached: Particle) {
        val axis = pos!!.sub(attached.pos!!)
        val dist = axis.len()
        val targetDist = 1.1f * (getRadius() + attached.getRadius())
        val offset = targetDist - dist
        val axisNorm = axis.unit()
        val myMass = mass
        val theirMass = attached.mass
        val p = myMass / (myMass + theirMass)
        pos!!.translate(axisNorm.mul((1 - p) * offset))
        attached.pos!!.translate(axisNorm.mul(-p * offset))
    }

    fun accelerate(da: Vector2?) {
        acc.translate(da!!)
    }

    override fun pointInside(p: Vector2?): Boolean {
        return isPointInsideCircle(pos!!, getRadius(), p!!)
    }

    override fun rayIntersects(start: Vector2?, end: Vector2?): Boolean {
        return false
    }

    private val tmp = Vector2(0f, 0f)
    private val collision = arrayOf(
        Vector2(0f, 0f), Vector2(0f, 0f)
    )

    override fun rayCollisions(start: Vector2?, end: Vector2?, collisions: Array<Collision>) {
        for (collision in collisions) collision.collided = false
        val ray = end!!.take(start!!).nor()
        val p = collisions[0].point.set(pos!!).take(start)
        val a = ray.len2()
        val b = -2 * ray.dot(p)
        val c = p.len2() - getRadius() * getRadius()
        val d = b * b - 4 * a * c
        val doesIntersect = d != 0f
        if (!doesIntersect) return
        val l1 = ((-b + sqrt(d.toDouble())) / (2 * a)).toFloat()
        val l2 = ((-b - sqrt(d.toDouble())) / (2 * a)).toFloat()
        if (l1 > 0) {
            collisions[0].collided = true
            collisions[0].point.set(start).translate(ray.x * l1, ray.y * l1)
        } else if (l2 > 0) {
            collisions[1].collided = true
            collisions[1].point.set(start).translate(ray.x * l2, ray.y * l2)
        }
    }

    override fun handlePotentialCollision(other: Collidable?, delta: Float): Boolean {
        if (other is Particle) return handlePotentialCollision(
            other,
            delta
        ) else if (other is Rock) return handlePotentialCollision(
            other, delta
        )
        return false
    }

    open fun onParticleCollisionCallback(p: Particle?, delta: Float) {}
    private val tmp1 = Vector2(0f, 0f)
    private fun handleParticleCollision(p: Particle, delta: Float) {
        val mr = p.mass / (p.mass + mass)
        val axis = tmp1.set(pos!!).take(p.pos!!)
        val dist = axis.len()
        val targetDist = getRadius() + p.getRadius()
        val offset = targetDist - dist
        val axisNorm = axis.nor()
        pos!!.translate(axisNorm.scale(mr * offset))
        p.pos!!.translate(axisNorm.scale(-(1 - mr) / mr))
        onParticleCollisionCallback(p, delta)
    }

    open fun handlePotentialCollision(e: Particle, delta: Float): Boolean {
        if (e === this) return false
        val sqDist = e.pos!!.squareDistanceTo(pos!!)
        val r = getRadius() + e.getRadius()
        if (sqDist < r * r) handleParticleCollision(e, delta)
        return true
    }

    private fun onRockCollisionCallback(rock: Rock?, delta: Float) {}
    open fun handlePotentialCollision(rock: Rock, delta: Float): Boolean {
        val edges = rock.edges
        val pos = pos
        val r = getRadius()
        for (i in edges.indices) {
            val edge = edges[i]
            val normal = rock.normals[i]
            val dir = edge[1]!!.sub(edge[0]!!)
            val x = pos!!.sub(edge[0]!!)
            if (dir.dot(normal) > 0) continue
            val coefs = circleIntersectLineCoefficients(dir, x, r)
            if (lineIntersectCondition(coefs)) {
                val t1 = coefs!![0]
                val t2 = coefs[1]
                val t = (t1 + t2) / 2f
                val offset = r - x.sub(dir.mul(t)).len()
                pos.translate(normal!!.mul(offset))
                recentRigidCollisions++
                onRockCollisionCallback(rock, delta)
                return true
            }
        }
        return false
    }

    fun isCollidingWith(other: Collidable?): Boolean {
        if (other is Particle) return isCollidingWith(other) else if (other is Rock) return isCollidingWith(other)
        return false
    }

    fun isCollidingWith(rock: Rock): Boolean {
        val edges = rock.edges
        val r = getRadius()
        val pos = pos
        if (rock.pointInside(pos!!)) return true
        for (edge in edges) {
            if (doesLineIntersectCircle(edge, pos, r)) return true
        }
        return false
    }

    fun isCollidingWith(other: Particle): Boolean {
        if (other === this) return false
        val r = getRadius() + other.getRadius()
        return other.pos!!.squareDistanceTo(pos!!) < r * r
    }

    fun getVel(): Vector2 {
        return vel ?: Vector2(0f, 0f)
    }

    val speed: Float
        get() = if (vel == null) 0f else vel!!.len()
    open val mass: Float
        get() = getMass(getRadius())

    fun getMass(r: Float): Float {
        return getMass(r, 0f)
    }

    fun getMass(r: Float, extraMass: Float): Float {
        return getSphereVolume(r) * massDensity + extraMass
    }

    val massDensity: Float
        get() = 1000f

    override fun getBoundingBox(): Array<Vector2?>? {
        val x = pos!!.x
        val y = pos!!.y
        val r = getRadius()
        return arrayOf(Vector2(x - r, y - r), Vector2(x + r, y + r))
    }

    fun getRadius(): Float {
        return radius
    }

    fun setRadius(radius: Float) {
        this.radius = radius
        if (this.radius > Settings.maxParticleRadius) this.radius = Settings.maxParticleRadius
        if (this.radius < Settings.minParticleRadius) this.radius = Settings.minParticleRadius
    }

    override fun getColor(): Color? {
        return Color.WHITE.darker()
    }

    companion object {
        private const val serialVersionUID = -4333766895269415282L
    }
}