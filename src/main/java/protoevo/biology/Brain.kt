package protoevo.biology

import protoevo.core.Settings
import protoevo.core.Simulation
import java.io.Serializable

interface Brain : Serializable {
    fun tick(p: Protozoan?)
    fun turn(p: Protozoan?): Float
    fun speed(p: Protozoan?): Float
    fun attack(p: Protozoan?): Float
    fun wantToMateWith(p: Protozoan?): Boolean
    fun energyConsumption(): Float

    companion object {

        @JvmField
        val RANDOM: Brain = object : Brain {
            val serialVersionUID = 1648484737904226314L
            override fun tick(p: Protozoan?) {}
            override fun turn(p: Protozoan?): Float {
                val x = (2 * Simulation.RANDOM.nextDouble() - 1).toFloat()
                val t = Math.toRadians(35.0).toFloat()
                return t * x
            }

            override fun speed(p: Protozoan?): Float {
                return (Simulation.RANDOM.nextDouble() * Settings.maxProtozoaSpeed).toFloat()
            }

            override fun attack(p: Protozoan?): Float {
                return Simulation.RANDOM.nextFloat()
            }

            override fun wantToMateWith(p: Protozoan?): Boolean {
                return false
            }

            override fun energyConsumption(): Float {
                return 0f
            }
        }

        @JvmField
        val EMPTY: Brain = object : Brain {
            override fun tick(p: Protozoan?) {}
            override fun turn(p: Protozoan?): Float {
                return 0f
            }

            override fun speed(p: Protozoan?): Float {
                return 0f
            }

            override fun attack(p: Protozoan?): Float {
                return 0f
            }

            override fun wantToMateWith(p: Protozoan?): Boolean {
                return false
            }

            override fun energyConsumption(): Float {
                return 0f
            }
        }
    }
}