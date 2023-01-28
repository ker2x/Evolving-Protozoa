package protoevo.utils

object CachedMath {
    const val precision = 1000
    val sin = FloatArray(precision) // lookup table

    init {
        // a static initializer fills the table
        // in this implementation, units are in degrees
        for (i in sin.indices) {
            sin[i] = Math.sin(2 * Math.PI * i / precision.toFloat()).toFloat()
        }
    }

    // Private function for table lookup
    private fun sinLookup(a: Int): Float {
        return if (a >= 0) sin[a % precision] else -sin[-a % precision]
    }

    // These are your working functions:
    fun sin(a: Float): Float {
        return sinLookup((a * precision / (2 * Math.PI)).toInt())
    }

    fun cos(a: Float): Float {
        return sinLookup(((a + Math.PI / 2) * precision / (2 * Math.PI)).toInt())
    }
}