package protoevo.neat

import java.io.Serializable
import java.util.*

class NeuronGene @JvmOverloads constructor(
    val id: Int,
    val type: Neuron.Type,
    val activation: (Float) -> Float,
    val label: String? = null
) : Comparable<NeuronGene>, Serializable {

    override fun compareTo(o: NeuronGene): Int {
        return id - o.id
    }

    override fun equals(o: Any?): Boolean {
        return if (o is NeuronGene) o.id == id else false
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        var str = String.format("Neuron: id=%d; type=%s", id, type)
        if (label != null) str += ", label=$label"
        return str
    }
}