package protoevo.utils

import java.io.Serializable
import kotlin.math.atan2
import kotlin.math.sqrt

class Vector2(x: Float, y: Float) : Serializable {

    @JvmField
    var x : Float = 0f
    @JvmField
    var y : Float = 0f

    init {
        this.x = x
        this.y = y
    }

    fun len2() :Float = x * x + y * y
    fun len() :Float = sqrt(x * x + y * y)
    fun copy(): Vector2 = Vector2(x, y)

    fun add(b: Vector2): Vector2 = Vector2(x + b.x, y + b.y)
    fun sub(b: Vector2): Vector2 = Vector2(x - b.x, y - b.y)
    fun mul(s: Float): Vector2 = Vector2(x * s, y * s)
    fun dot(b: Vector2?): Float = x * b!!.x + y * b.y
    fun perp(): Vector2 = Vector2(-y, x)
    fun angle(): Float = atan2(y.toDouble(), x.toDouble()).toFloat()

    fun unit(): Vector2 = if (len() == 0f) Vector2(0f, 0f) else Vector2(x / len(), y / len())

    fun translate(dv: Vector2): Vector2 {
        x += dv.x
        y += dv.y
        return this
    }

    fun translate(dx: Float, dy: Float): Vector2 {
        x += dx
        y += dy
        return this
    }

    fun scale(s: Float): Vector2 {
        x *= s
        y *= s
        return this
    }

    fun rotate(angle: Float): Vector2 {
        val c = CachedMath.cos(angle)
        val s = CachedMath.sin(angle)
        return Vector2(x * c - y * s, x * s + y * c)
    }

    fun turn(angle: Float): Vector2 {
        val c = CachedMath.cos(angle)
        val s = CachedMath.sin(angle)
        val xNew = x * c - y * s
        y = x * s + y * c
        x = xNew
        return this
    }

    fun nor(): Vector2 {
        val len = len()
        if (len != 0f) {
            x /= len
            y /= len
        }
        return this
    }



    fun setLength(targetLen: Float): Vector2 {
        val currentLen = len()
        x *= targetLen / currentLen
        y *= targetLen / currentLen
        return this
    }

    fun setDir(other: Vector2): Vector2 {
        return other.setLength(len())
    }

    override fun toString(): String {
        return "($x, $y)"
    }

    fun angleBetween(other: Vector2): Float {
        val a = len2()
        val b = other.len2()
        return Math.acos(dot(other) / Math.sqrt((a * b).toDouble())).toFloat()
    }

    fun distanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun squareDistanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    override fun equals(o: Any?): Boolean {
        if (o is Vector2) {
            val v = o
            return v.x == x && v.y == y
        }
        return false
    }

    fun moveAway(other: Vector2, amount: Float) {
        val dx = x - other.x
        val dy = y - other.y
        val l = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        x = other.x + dx * amount / l
        y = other.y + dy * amount / l
    }

    operator fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(v: Vector2): Vector2 {
        x = v.x
        y = v.y
        return this
    }

    fun take(pos: Vector2): Vector2 {
        x -= pos.x
        y -= pos.y
        return this
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    companion object {
        @JvmField
        val ZERO = Vector2(0f, 0f)
        private const val serialVersionUID = 8642244552320036511L
        @JvmStatic
        fun fromAngle(angle: Float): Vector2 {
            return Vector2(Math.cos(angle.toDouble()).toFloat(), Math.sin(angle.toDouble()).toFloat())
        }
    }
}