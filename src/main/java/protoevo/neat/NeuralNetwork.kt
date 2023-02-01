package protoevo.neat

import java.io.Serializable
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.max

class NeuralNetwork(val neurons: Array<Neuron?>) : Serializable {
    private val outputNeurons: Array<Neuron?>
    private val inputNeurons: Array<Neuron?>
    private val outputs: FloatArray
    val depth: Int
    val inputSize: Int
    private var computedGraphics = false
    var graphicsNodeSpacing = 0

    init {
        var nSensors = 0
        var nOutputs = 0
        for (neuron in neurons) {
            requireNotNull(neuron) { "Cannot handle null neurons." }
            if (neuron.type == Neuron.Type.SENSOR) {
                nSensors++
            } else if (neuron.type == Neuron.Type.OUTPUT) {
                nOutputs++
            }
        }

        inputSize = nSensors
        inputNeurons = arrayOfNulls(inputSize)
        var i = 0
        for (neuron in neurons) if (neuron?.type == Neuron.Type.SENSOR) {
            inputNeurons[i] = neuron
            i++
        }

        outputNeurons = arrayOfNulls(nOutputs)
        i = 0
        for (neuron in neurons) if (neuron?.type == Neuron.Type.OUTPUT) {
            outputNeurons[i] = neuron
            i++
        }
        outputs = FloatArray(nOutputs)
        Arrays.fill(outputs, 0f)
        depth = calculateDepth()
    }

    fun calculateDepth(): Int {
        val visited = BooleanArray(neurons.size)
        Arrays.fill(visited, false)
        val depth = calculateDepth(outputNeurons, visited)
        for (n in outputNeurons) n?.depth = (depth)
        for (n in inputNeurons) n?.depth = (0)
        for (n in neurons) if (n?.depth == -1) n.depth = (depth)
        return depth
    }

    private fun calculateDepth(explore: Array<Neuron?>?, visited: BooleanArray): Int {
        val unexplored = Arrays.stream(explore)
            .filter { n: Neuron? -> !visited[n!!.id] }
            .collect(Collectors.toList())!!
        for (n in explore!!) if (n != null) {
            visited[n.id] = true
        }
        var maxDepth = 0
        for (n in unexplored) {
            val neuronDepth = 1 + calculateDepth(n?.inputs, visited)
            n?.depth = (neuronDepth)
            maxDepth = max(maxDepth, neuronDepth)
        }
        return maxDepth
    }

    fun setInput(vararg values: Float) {
        for (i in values.indices) inputNeurons[i]!!.state = values[i]
    }

    fun tick() {
        for (n in neurons) n!!.tick()
        for (n in neurons) n!!.update()
    }

    fun outputs(): FloatArray {
        for (i in outputNeurons.indices) outputs[i] = outputNeurons[i]!!.state
        return outputs
    }

    override fun toString(): String {
        return Stream.of(*neurons)
            .map { obj: Neuron? -> obj.toString() }
            .collect(Collectors.joining("\n"))
    }

    val size: Int
        get() = neurons.size

    fun hasComputedGraphicsPositions(): Boolean {
        return computedGraphics
    }

    fun setComputedGraphicsPositions(computedGraphics: Boolean) {
        this.computedGraphics = computedGraphics
    }

    fun disableInputsFrom(i: Int) {
        for (idx in i until inputNeurons.size) inputNeurons[idx]?.isConnectedToOutput = (false)
        //        disableOnlyConnectedToDisabled();
    }

    private fun disableOnlyConnectedToDisabled() {
        var check = true
        while (check) {
            check = false
            for (neuron in neurons) {
                if (!neuron!!.isConnectedToOutput) continue
                var allInputsDisabled = true
                for (input in neuron.inputs) if (!input!!.isConnectedToOutput) {
                    allInputsDisabled = false
                    break
                }
                if (allInputsDisabled) {
                    neuron.isConnectedToOutput = false
                    check = true
                }
            }
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}