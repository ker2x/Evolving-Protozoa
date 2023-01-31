package protoevo.neat

import protoevo.utils.CachedMath
import java.io.Serializable
import kotlin.math.exp

/***
 * A Neuron is a node in the neural network.
 * It has a state, which is the output of the neuron, and a list of inputs, which are the neurons that feed into this neuron.
 * @constructor Creates a new Neuron with the given id, inputs, weights, type, and activation function.
 * @param id The id of the neuron.
 * @param inputs The inputs of the neuron.
 * @param weights The weights of the inputs.
 * @param type The type of the neuron.
 * @param activation The activation function of the neuron.
 * @param label The label of the neuron.
 */
class Neuron(
    val id: Int,
    val inputs: Array<Neuron?>,
    val weights: FloatArray,
    var type: Type,
    private var activation: (Float) -> Float,
    val label: String?
) : Comparable<Neuron>, Serializable {


    enum class Type() : Serializable {
        SENSOR,
        HIDDEN,
        OUTPUT;

        override fun toString(): String {
            return when (this) {
                SENSOR -> "SENSOR"
                HIDDEN -> "HIDDEN"
                OUTPUT -> "OUTPUT"
            }
        }
    }

    private var nextState :Float = 0f
    private var learningRate = 0f

    var state = 0f
    var lastState = 0f
    var depth = -1
    var graphicsX = -1
    var graphicsY = -1
    var isConnectedToOutput = true

    init {
        if (type == Type.OUTPUT) isConnectedToOutput = true
    }

    fun tick() {
        nextState = 0.0f
        for (i in inputs.indices) nextState += inputs[i]!!.state * weights[i]
        nextState = activation(nextState)
    }

    fun update() {
        lastState = state
        state = nextState
    }

    override fun equals(o: Any?): Boolean {
        return if (o is Neuron) o.id == id else false
    }

    fun setState(s: Float): Neuron {
        state = s
        return this
    }

    fun setActivation(activation: (Float) -> Float): Neuron {
        this.activation = activation
        return this
    }

    private fun setLearningRate(lr: Float): Neuron {
        learningRate = lr
        return this
    }

    override fun compareTo(o: Neuron): Int {
        return Comparator.comparingInt { obj: Neuron -> obj.id }.compare(this, o)
    }

    override fun toString(): String {
        val s = StringBuilder(String.format("id:%d, state:%.1f", id, state))
        s.append(", connections: [")
        for (i in weights.indices) s.append(String.format("(%d, %.1f)", i, weights[i]))
        s.append("]")
        return s.toString()
    }

    fun setGraphicsPosition(x: Int, y: Int) {
        graphicsX = x
        graphicsY = y
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + inputs.contentHashCode()
        result = 31 * result + weights.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + activation.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + lastState.hashCode()
        result = 31 * result + nextState.hashCode()
        result = 31 * result + learningRate.hashCode()
        result = 31 * result + depth
        result = 31 * result + graphicsX
        result = 31 * result + graphicsY
        result = 31 * result + isConnectedToOutput.hashCode()
        return result
    }

    companion object {
        private const val serialVersionUID = 1L
        val LINEAR : (Float) -> (Float) = { it }
        val TANH   : (Float) -> (Float) = { CachedMath.tanh(it) }
        val SIGMOID: (Float) -> (Float) = { 1 / (1 + exp(-it)) }
        val RELU   : (Float) -> (Float) = { if (it > 0) it else 0f }
    }
}