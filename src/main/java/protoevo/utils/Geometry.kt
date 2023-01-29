package protoevo.utils

object Geometry {
    @JvmStatic
    fun circleIntersectLineCoefficients(dir: Vector2?, x: Vector2?, r: Float): FloatArray? {
        val a = dir!!.len2()
        val b = -2 * dir.dot(x)
        val c = x!!.len2() - r * r
        val disc = b * b - 4 * a * c
        if (disc < 0) return null
        val t1 = ((-b + Math.sqrt(disc.toDouble())) / (2 * a)).toFloat()
        val t2 = ((-b - Math.sqrt(disc.toDouble())) / (2 * a)).toFloat()
        return floatArrayOf(t1, t2)
    }

    @JvmStatic
    fun lineIntersectCondition(coefs: FloatArray?): Boolean {
        if (coefs == null) return false
        val t1 = coefs[0]
        val t2 = coefs[1]
        val eps = 1e-3f
        return eps < t1 && t1 < 1 - eps || eps < t2 && t2 < 1 - eps
    }

    @JvmStatic
    fun doesLineIntersectCircle(line: Array<Vector2?>, circlePos: Vector2, circleR: Float): Boolean {
        val dir = line[1]!!.sub(line[0]!!)
        val x = circlePos.sub(line[0]!!)
        val intersectionCoefs = circleIntersectLineCoefficients(dir, x, circleR)
        return lineIntersectCondition(intersectionCoefs)
    }

    @JvmStatic
    fun isPointInsideCircle(circlePos: Vector2, radius: Float, p: Vector2): Boolean {
        return circlePos.sub(p).len2() <= radius * radius
    }

    @JvmStatic
    fun getSphereVolume(r: Float): Float {
        return (4 / 3 * Math.PI * r * r * r).toFloat()
    }
}