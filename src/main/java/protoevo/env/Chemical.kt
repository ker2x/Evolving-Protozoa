package protoevo.env

import protoevo.core.Settings
import java.io.Serializable

class Chemical : Serializable {
    var currentPlantPheromoneDensity = 0f
    var nextPlantPheromoneDensity = 0f

    @Transient
    private  var neighbours: Array<out Chemical>? = null
    fun propagate(delta: Float) {
        nextPlantPheromoneDensity = currentPlantPheromoneDensity
        if (neighbours != null && neighbours!!.size > 0) {
            var incoming = 0f
            for (chemical in neighbours!!) incoming += chemical.currentPlantPheromoneDensity
            incoming /= neighbours!!.size.toFloat()
            nextPlantPheromoneDensity += delta * incoming
        }
        nextPlantPheromoneDensity *= 1 - delta * Settings.chemicalsDecay
    }

    fun update() {
        currentPlantPheromoneDensity = Math.max(Math.min(nextPlantPheromoneDensity, 1f), 0f)
        if (java.lang.Float.isNaN(currentPlantPheromoneDensity)) currentPlantPheromoneDensity = 0f
    }

    private fun sigmoid(z: Float): Float {
        return 1 / (1 + Math.exp(-z.toDouble()).toFloat())
    }

    fun pheromoneFlow(other: Chemical): Float {
        val densityDiff = other.currentPlantPheromoneDensity - currentPlantPheromoneDensity
        val p = Settings.chemicalsFlow * Math.tanh(densityDiff.toDouble()).toFloat()
        return p * densityDiff
    }

    fun setNeighbours(vararg neighbours: Chemical) {
        this.neighbours = neighbours
    }

    companion object {
        const val serialVersionUID = 1L
    }
}