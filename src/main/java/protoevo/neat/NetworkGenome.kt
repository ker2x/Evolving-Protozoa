package protoevo.neat

import protoevo.biology.Retina
import protoevo.core.Settings
import protoevo.core.Simulation
import java.io.Serializable
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.max

class NetworkGenome : Serializable {
    private lateinit var sensorNeuronGenes: Array<NeuronGene?>
    private lateinit var outputNeuronGenes: Array<NeuronGene?>
    private lateinit var hiddenNeuronGenes: Array<NeuronGene?>
    private var nNeuronGenes = 0
    private lateinit var synapseGenes: Array<SynapseGene?>
    private var random = Simulation.RANDOM
    private var mutationChance = Settings.globalMutationChance
    private var defaultActivation = Neuron.LINEAR
    private var fitness = 0.0f
    var numMutations = 0
        private set
    private var nSensors = 0
    private var nOutputs = 0

    constructor(other: NetworkGenome) {
        setProperties(other)
    }

    private fun setProperties(other: NetworkGenome) {
        sensorNeuronGenes = other.sensorNeuronGenes
        outputNeuronGenes = other.outputNeuronGenes
        hiddenNeuronGenes = other.hiddenNeuronGenes
        synapseGenes = other.synapseGenes
        nNeuronGenes = other.nNeuronGenes
        random = other.random
        mutationChance = other.mutationChance
        defaultActivation = other.defaultActivation
        fitness = other.fitness
        numMutations = other.numMutations
        nSensors = other.nSensors
        nOutputs = other.nOutputs
    }

    @JvmOverloads
    constructor(numInputs: Int = 0, numOutputs: Int = 0, defaultActivation: (input: Float) -> Float = Neuron.TANH) {
        nSensors = numInputs
        nOutputs = numOutputs
        nNeuronGenes = 0
        sensorNeuronGenes = arrayOfNulls(numInputs)
        for (i in 0 until numInputs) sensorNeuronGenes[i] =
            NeuronGene(nNeuronGenes++, Neuron.Type.SENSOR, Neuron.LINEAR)
        outputNeuronGenes = arrayOfNulls(numOutputs)
        for (i in 0 until numOutputs) outputNeuronGenes[i] =
            NeuronGene(nNeuronGenes++, Neuron.Type.OUTPUT, defaultActivation)
        hiddenNeuronGenes = arrayOfNulls(0)
        synapseGenes = arrayOfNulls(numInputs * numOutputs)
        for (i in 0 until numInputs) for (j in 0 until numOutputs) {
            val `in` = sensorNeuronGenes[i]
            val out = outputNeuronGenes[j]
            synapseGenes[i * numOutputs + j] = SynapseGene(`in`, out)
        }
        this.defaultActivation = defaultActivation
    }

    constructor(
        sensorGenes: Array<NeuronGene?>,
        outputGenes: Array<NeuronGene?>,
        hiddenGenes: Array<NeuronGene?>,
        synapseGenes: Array<SynapseGene?>,
        activation: (Float) -> Float
    ) {
        sensorNeuronGenes = sensorGenes
        outputNeuronGenes = outputGenes
        hiddenNeuronGenes = hiddenGenes
        this.synapseGenes = synapseGenes
        defaultActivation = activation
        nSensors = sensorGenes.size
        nOutputs = outputGenes.size
        nNeuronGenes = nSensors + nOutputs + hiddenGenes.size
    }

    fun addSensor(label: String?) {
        val n = NeuronGene(
            nNeuronGenes++, Neuron.Type.SENSOR, Neuron.LINEAR, label
        )
        sensorNeuronGenes = sensorNeuronGenes.copyOf(sensorNeuronGenes.size + 1)
        sensorNeuronGenes[sensorNeuronGenes.size - 1] = n
        nSensors++
        val originalLen = synapseGenes.size
        synapseGenes = synapseGenes.copyOf(originalLen + outputNeuronGenes.size)
        for (i in outputNeuronGenes.indices) synapseGenes[originalLen + i] = SynapseGene(n, outputNeuronGenes[i])
    }

    fun addOutput(label: String?) {
        val n = NeuronGene(
            nNeuronGenes++, Neuron.Type.OUTPUT, defaultActivation, label
        )
        outputNeuronGenes = outputNeuronGenes.copyOf(outputNeuronGenes.size + 1)
        outputNeuronGenes[outputNeuronGenes.size - 1] = n
        nOutputs++
        val originalLen = synapseGenes.size
        synapseGenes = synapseGenes.copyOf(originalLen + sensorNeuronGenes.size)
        for (i in sensorNeuronGenes.indices) synapseGenes[originalLen + i] = SynapseGene(sensorNeuronGenes[i], n)
    }

    private fun createHiddenBetween(g: SynapseGene?) {
        val n = NeuronGene(
            nNeuronGenes++, Neuron.Type.HIDDEN, defaultActivation
        )
        hiddenNeuronGenes = hiddenNeuronGenes.copyOf(hiddenNeuronGenes.size + 1)
        hiddenNeuronGenes[hiddenNeuronGenes.size - 1] = n
        val inConnection = SynapseGene(g!!.`in`, n, 1f)
        val outConnection = SynapseGene(n, g.out, g.weight)
        synapseGenes = synapseGenes.copyOf(synapseGenes.size + 2)
        synapseGenes[synapseGenes.size - 2] = inConnection
        synapseGenes[synapseGenes.size - 1] = outConnection
        g.isDisabled = (true)
    }

    private fun getSynapseGeneIndex(`in`: NeuronGene?, out: NeuronGene?): Int {
        for (i in 0 until synapseGenes.size - 2) {
            if (synapseGenes[i]!!.`in` == `in` && synapseGenes[i]!!.out == out &&
                !synapseGenes[i]!!.isDisabled
            ) {
                return i
            }
        }
        return -1
    }

    private fun mutateConnection(`in`: NeuronGene?, out: NeuronGene?) {
        numMutations++
        val geneIndex = getSynapseGeneIndex(`in`, out)
        if (geneIndex == -1) {
            synapseGenes = synapseGenes.copyOf(synapseGenes.size + 1)
            synapseGenes[synapseGenes.size - 1] = SynapseGene(`in`, out)
        } else {
            val g = synapseGenes[geneIndex]
            if (random.nextBoolean()) createHiddenBetween(g) else synapseGenes[geneIndex] =
                SynapseGene(`in`, out, SynapseGene.randomInitialWeight(), g!!.innovation)
        }
    }

    fun mutate() {
        val i = random.nextInt(sensorNeuronGenes.size + hiddenNeuronGenes.size)
        val `in`: NeuronGene?
        val out: NeuronGene?
        `in` = if (i < sensorNeuronGenes.size) sensorNeuronGenes[i] else hiddenNeuronGenes[i - sensorNeuronGenes.size]
        val j = random.nextInt(hiddenNeuronGenes.size + outputNeuronGenes.size)
        out = if (j < hiddenNeuronGenes.size) hiddenNeuronGenes[j] else outputNeuronGenes[j - hiddenNeuronGenes.size]
        mutateConnection(`in`, out)
    }

    fun crossover(other: NetworkGenome): NetworkGenome {
        val myConnections = Arrays.stream(synapseGenes)
            .collect(Collectors.toMap(
                { obj: SynapseGene? -> obj!!.innovation }, Function.identity()
            )
            )
        val theirConnections = Arrays.stream(other.synapseGenes)
            .collect(Collectors.toMap(
                { obj: SynapseGene? -> obj!!.innovation }, Function.identity()
            )
            )
        val innovationNumbers: MutableSet<Int> = HashSet()
        innovationNumbers.addAll(myConnections.keys)
        innovationNumbers.addAll(theirConnections.keys)
        val childSynapses = HashSet<SynapseGene?>()
        for (innovation in innovationNumbers) {
            val iContain = myConnections.containsKey(innovation)
            val theyContain = theirConnections.containsKey(innovation)
            var g: SynapseGene?
            if (iContain && theyContain) {
                g = if (Simulation.RANDOM.nextBoolean()) myConnections[innovation] else theirConnections[innovation]
                if (g!!.isDisabled && Simulation.RANDOM.nextFloat() < Settings.globalMutationChance) g.isDisabled =
                    false
                childSynapses.add(g)
                continue
            } else if (iContain) {
                g = myConnections[innovation]
            } else {
                g = theirConnections[innovation]
            }
            if (g!!.`in`!!.type == Neuron.Type.SENSOR || Simulation.RANDOM.nextBoolean()) childSynapses.add(g)
        }
        val childSynapseArray = childSynapses.toTypedArray()

        val neuronGenes = childSynapses.stream()
            .flatMap { s: SynapseGene? -> Stream.of(s!!.`in`, s.out) }
            .collect(Collectors.toSet())

        val childSensorGenes = neuronGenes.stream()
            .filter { n: NeuronGene? -> n!!.type == Neuron.Type.SENSOR }
            .sorted(Comparator.comparingInt { obj: NeuronGene? -> obj!!.id })
            .toArray<NeuronGene?> { length -> arrayOfNulls(length) }

        val childOutputGenes = neuronGenes.stream()
            .filter { n: NeuronGene? -> n!!.type == Neuron.Type.OUTPUT }
            .sorted(Comparator.comparingInt { obj: NeuronGene? -> obj!!.id })
            .toArray<NeuronGene?> { length -> arrayOfNulls(length) }

        val childHiddenGenes = neuronGenes.stream()
            .filter { n: NeuronGene? -> n!!.type == Neuron.Type.HIDDEN }
            .sorted(Comparator.comparingInt { obj: NeuronGene? -> obj!!.id })
            .toArray<NeuronGene?> { length -> arrayOfNulls(length) }

        return NetworkGenome(
            childSensorGenes,
            childOutputGenes,
            childHiddenGenes,
            childSynapseArray,
            defaultActivation
        )
    }

    private fun maxNeuronId(): Int {
        var id = 0
        for (g in sensorNeuronGenes) id = max(g!!.id, id)
        for (g in hiddenNeuronGenes) id = max(g!!.id, id)
        for (g in outputNeuronGenes) id = max(g!!.id, id)
        return id
    }

    fun phenotype(): NeuralNetwork {
        val neurons = arrayOfNulls<Neuron>(maxNeuronId() + 1)
        for (g in sensorNeuronGenes) {
            val inputs = arrayOfNulls<Neuron>(0)
            val weights = FloatArray(0)
            neurons[g!!.id] = Neuron(
                g.id, inputs, weights, g.type, g.activation, g.label
            )
        }
        val inputCounts = IntArray(neurons.size)
        Arrays.fill(inputCounts, 0)
        for (g in synapseGenes) inputCounts[g!!.out!!.id]++
        for (i in 0 until hiddenNeuronGenes.size + outputNeuronGenes.size) {
            var g: NeuronGene?
            g = if (i < hiddenNeuronGenes.size) hiddenNeuronGenes[i] else outputNeuronGenes[i - hiddenNeuronGenes.size]
            val inputs = arrayOfNulls<Neuron>(inputCounts[g!!.id])
            val weights = FloatArray(inputCounts[g.id])
            neurons[g.id] = Neuron(
                g.id, inputs, weights, g.type, g.activation, g.label
            )
        }
        Arrays.fill(inputCounts, 0)
        for (g in synapseGenes) {
            val i = inputCounts[g!!.out!!.id]++
            neurons[g.out!!.id]!!.inputs[i] = neurons[g.`in`!!.id]
            neurons[g.out!!.id]!!.weights[i] = g.weight
        }
        return NeuralNetwork(neurons)
    }

    fun distance(other: NetworkGenome?): Float {
//		int excess = 0;
//		int disjoint = 0;
        return 0f
    }

    override fun toString(): String {
        val str = StringBuilder()
        for (gene in sensorNeuronGenes) str.append(gene.toString()).append("\n")
        for (gene in hiddenNeuronGenes) str.append(gene.toString()).append("\n")
        for (gene in outputNeuronGenes) str.append(gene.toString()).append("\n")
        for (gene in synapseGenes) str.append(gene.toString()).append("\n")
        return str.toString()
    }

    fun numberOfSensors(): Int {
        return nSensors
    }

    private fun hasSensor(label: String): Boolean {
        for (gene in sensorNeuronGenes) if (gene!!.label == label) return true
        return false
    }

    fun ensureRetinaSensorsExist(retinaSize: Int) {
        for (i in 0 until retinaSize) {
            val label = Retina.retinaCellLabel(i)
            if (!hasSensor("$label R")) addSensor("$label R")
            if (!hasSensor("$label G")) addSensor("$label G")
            if (!hasSensor("$label B")) addSensor("$label B")
        }
    }

    companion object {
        const val serialVersionUID = 6145947068527764820L
    }
}