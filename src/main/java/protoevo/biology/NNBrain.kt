package protoevo.biology

import protoevo.core.Settings
import protoevo.neat.NeuralNetwork

class NNBrain @JvmOverloads constructor(val network: NeuralNetwork,
                                        private val maxTurn: Float = Math.toRadians(Settings.maxTurnAngle.toDouble())
                                            .toFloat()
) : Brain {
    private var outputs: FloatArray
    private val inputs: FloatArray

    init {
        outputs = network.outputs()
        inputs = FloatArray(network.inputSize)
    }

    override fun tick(p: Protozoan?) {
        var i = 0
        // ProtozoaGenome.nonVisualSensorSize
        inputs[i++] = 1f // bias term
        inputs[i++] = p!!.getHealth() * 2 - 1
        inputs[i++] = 2 * p.getRadius() / p.genome.splitRadius - 1
        inputs[i++] = 2 * p.constructionMassAvailable / p.constructionMassCap - 1
        for (sensor in p.contactSensors) inputs[i++] = if (sensor!!.inContact()) 1f else 0f
        if (Settings.enableChemicalField) {
            val chemicalSolution = p.tank.chemicalSolution
            val chemicalX1 = chemicalSolution!!.toChemicalGridX(p.pos!!.x - p.getRadius())
            val chemicalX2 = chemicalSolution.toChemicalGridX(p.pos!!.x + p.getRadius())
            val chemicalY1 = chemicalSolution.toChemicalGridY(p.pos!!.x - p.getRadius())
            val chemicalY2 = chemicalSolution.toChemicalGridY(p.pos!!.x + p.getRadius())
            inputs[i++] = chemicalSolution.getPlantPheromoneDensity(chemicalX1, chemicalY1) -
                    chemicalSolution.getPlantPheromoneDensity(chemicalX2, chemicalY2)
            inputs[i++] = chemicalSolution.getPlantPheromoneDensity(chemicalX1, chemicalY2) -
                    chemicalSolution.getPlantPheromoneDensity(chemicalX2, chemicalY1)
            val chemicalX = chemicalSolution.toChemicalGridX(p.pos!!.x)
            val chemicalY = chemicalSolution.toChemicalGridY(p.pos!!.y)
            inputs[i++] = 2 * chemicalSolution.getPlantPheromoneDensity(chemicalX, chemicalY) - 1
        }
        val retinaHealth = p.getRetina().health
        for (cell in p.getRetina()) {
            if (cell.anythingVisible()) {
                val colour = cell.colour
                inputs[i++] = retinaHealth * (-1 + 2 * colour.red / 255f)
                inputs[i++] = retinaHealth * (-1 + 2 * colour.green / 255f)
                inputs[i++] = retinaHealth * (-1 + 2 * colour.blue / 255f)
            } else {
                inputs[i++] = 0f
                inputs[i++] = 0f
                inputs[i++] = 0f
            }
        }
        network.setInput(*inputs)
        network.tick()
        outputs = network.outputs()
    }

    override fun turn(p: Protozoan?): Float {
        val turn = outputs[0]
        return turn * maxTurn
    }

    override fun speed(p: Protozoan?): Float {
        return Math.min(
            Settings.maxProtozoaSpeed * outputs[1],
            Settings.maxProtozoaSpeed
        )
    }

    override fun wantToMateWith(p: Protozoan?): Boolean {
        return outputs[2] > 0
    }

    override fun attack(p: Protozoan?): Float {
        return if (outputs[3] > 0) 1f else 0f
    }

    override fun energyConsumption(): Float {
        return 0f
    }
}