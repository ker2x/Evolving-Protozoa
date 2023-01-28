package protoevo.utils

import java.io.Serializable

class Vector2(x: Float, y: Float) : Serializable {
    @JvmField
    var x = 0f
    @JvmField
    var y = 0f

    init {
        this.x = x
        this.y = y
    }

    fun len2(): Float {
        return x * x + y * y
    }

    fun len(): Float {
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    fun copy(): Vector2 {
        return Vector2(x, y)
    }

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

    fun add(b: Vector2): Vector2 {
        return Vector2(x + b.x, y + b.y)
    }

    fun sub(b: Vector2): Vector2 {
        return Vector2(x - b.x, y - b.y)
    }

    fun mul(s: Float): Vector2 {
        return Vector2(s * x, s * y)
    }

    fun dot(b: Vector2?): Float {
        return x * b!!.x + y * b.y
    }

    fun perp(): Vector2 {
        return Vector2(-y, x)
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
        val yNew = x * s + y * c
        x = xNew
        y = yNew
        return this
    }

    fun unit(): Vector2 {
        val len = len()
        return if (len == 0f) Vector2(0f, 0f) else Vector2(x / len, y / len)
    }

    fun nor(): Vector2 {
        val len = len()
        if (len == 0f) return this
        x /= len
        y /= len
        return this
    }

    fun angle(): Float {
        return Math.atan2(y.toDouble(), x.toDouble()).toFloat()
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