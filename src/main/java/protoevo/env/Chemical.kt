package protoevo.env

import protoevo.core.Settings
import java.io.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

class Chemical : Serializable {
    var currentPlantPheromoneDensity = 0f
    private var nextPlantPheromoneDensity = 0f

    @Transient
    private  var neighbours: Array<out Chemical>? = null
    fun propagate(delta: Float) {
        nextPlantPheromoneDensity = currentPlantPheromoneDensity
        if (neighbours != null && neighbours!!.isNotEmpty()) {
            var incoming = 0f
            for (chemical in neighbours!!) incoming += chemical.currentPlantPheromoneDensity
            incoming /= neighbours!!.size.toFloat()
            nextPlantPheromoneDensity += delta * incoming
        }
        nextPlantPheromoneDensity *= 1 - delta * Settings.chemicalsDecay
    }

    fun update() {
        currentPlantPheromoneDensity = max(min(nextPlantPheromoneDensity, 1f), 0f)
        if (java.lang.Float.isNaN(currentPlantPheromoneDensity)) currentPlantPheromoneDensity = 0f
    }

    private fun sigmoid(z: Float): Float {
        return 1 / (1 + exp(-z))
    }

    fun pheromoneFlow(other: Chemical): Float {
        val densityDiff = other.currentPlantPheromoneDensity - currentPlantPheromoneDensity
        val p = Settings.chemicalsFlow * tanh(densityDiff.toDouble()).toFloat()
        return p * densityDiff
    }

    fun setNeighbours(vararg neighbours: Chemical) {
        this.neighbours = neighbours
    }

    companion object {
        const val serialVersionUID = 1L
    }
}