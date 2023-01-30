package protoevo.biology.genes

import protoevo.core.Simulation
import java.io.Serializable

abstract class Gene<T> : Serializable {
    @JvmField
    var values: T
    //@JvmField
    open var numMutations = 0
        get() { return field}
    var isDisabled = false

    constructor() {
        values = newValue
    }

    constructor(values: T) {
        this.values = values
    }

    abstract fun <G : Gene<T>?> createNew(value: T): G
    fun <G : Gene<T>?> createNew(value: T, numMutations: Int): G {
        val gene = createNew<G>(value)
        gene!!.numMutations = numMutations
        totalMutations++
        return gene
    }

    open fun <G : Gene<T>?> mutate(genome: Array<Gene<*>?>?): G {
        return this.createNew(newValue, numMutations + 1)
    }

    fun crossover(other: Gene<*>): Gene<*> {
        return if (Simulation.RANDOM.nextBoolean()) this else other
    }

    abstract fun canDisable(): Boolean
    fun toggleEnabled() {
        isDisabled = !isDisabled
    }

    abstract fun disabledValue(): T
    abstract val newValue: T
    abstract val traitName: String?
    fun toggle(): Gene<T> {
        val newGene = this.createNew<Gene<T>>(values, numMutations + 1)
        newGene.isDisabled = !isDisabled
        return newGene
    }

    open fun valueString(): String {
        return values.toString()
    }

    override fun toString(): String {
        return valueString() + ":" + numMutations + ":" + if (isDisabled) "0" else "1"
    }

    companion object {
        const val serialVersionUID = -1504556284113269258L
        var totalMutations = 0
    }
}