package protoevo.neat

import protoevo.core.Simulation
import java.io.Serializable
import java.util.*

class SynapseGene @JvmOverloads constructor(
    var `in`: NeuronGene?,
    var out: NeuronGene?,
    var weight: Float = randomInitialWeight(),
    val innovation: Int = globalInnovation++
) : Comparable<SynapseGene>, Serializable {
    var isDisabled = false

    override fun compareTo(g: SynapseGene): Int {
        return innovation - g.innovation
    }

    override fun equals(o: Any?): Boolean {
        if (o is SynapseGene) {
            val otherSynGene = o
            val otherIn = otherSynGene.`in`
            val otherOut = otherSynGene.out
            return `in` == otherIn && out == otherOut
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(`in`?.id, out?.id)
    }

    override fun toString(): String {
        return String.format(
            "Synapse: innov=%d; in=%d; out=%d; w=%.5f; disabled=%b",
            innovation, `in`?.id, out?.id, weight, isDisabled
        )
    }

    companion object {
        private var globalInnovation = 0
        fun randomInitialWeight(): Float {
            return (2 * Simulation.RANDOM.nextDouble() - 1).toFloat()
        }
    }
}