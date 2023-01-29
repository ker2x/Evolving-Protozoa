package protoevo.neat

import java.io.Serializable
import java.util.function.Function
import kotlin.math.exp
import kotlin.math.tanh

/**
 * Created by dylan on 26/05/2017.
 */
class Neuron(
    val id: Int,
    val inputs: Array<Neuron?>,
    val weights: FloatArray,
    var type: Type?,
    private var activation: (Float) -> Float,
    val label: String?
) : Comparable<Neuron>, Serializable {

    interface Activation : Function<Float, Float>, Serializable {
        companion object {
            val SIGMOID = fun(z: Float): Float { return 1 / (1 + exp(-z)) }
            val LINEAR = fun(z: Float): Float { return z }
            val TANH = fun(x: Float): Float  { return tanh(x) }
        }
    }



    enum class Type(private val value: String) : Serializable {
        SENSOR("SENSOR"), HIDDEN("HIDDEN"), OUTPUT("OUTPUT");

        override fun toString(): String {
            return value
        }
    }

    var state = 0f
    var lastState = 0f
        private set
    private var nextState :Float = 0f
    private var learningRate = 0f
    var depth = -1
    var graphicsX = -1
        private set
    var graphicsY = -1
        private set
    var isConnectedToOutput = true

    init {
        if (type == Type.OUTPUT) isConnectedToOutput = true
    }

    fun tick() {
        nextState = 0.0f
        for (i in inputs.indices) nextState += inputs[i]!!.state * weights[i]
        nextState = activation.invoke(nextState)
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

    companion object {
        private const val serialVersionUID = 1L
    }
}