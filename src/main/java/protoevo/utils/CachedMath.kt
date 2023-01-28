package protoevo.utils

object CachedMath {
    private const val precision = 1000
    private val lookupTable = FloatArray(precision) { i -> kotlin.math.sin(2 * Math.PI * i / precision).toFloat() }
    private fun sinLookup(a: Int): Float = if (a >= 0) lookupTable[a % precision] else -lookupTable[-a % precision]
    fun sin(a: Float): Float = sinLookup((a * precision / (2 * Math.PI)).toInt())
    fun cos(a: Float): Float = sinLookup(((a + Math.PI / 2) * precision / (2 * Math.PI)).toInt())
}