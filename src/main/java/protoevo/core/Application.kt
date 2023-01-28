package protoevo.core

import protoevo.utils.REPL
import protoevo.utils.TextStyle
import protoevo.utils.Window
import javax.swing.SwingUtilities

object Application {

    const val refreshDelay = 1000 / 120f

    @JvmStatic
    fun main(args: Array<String>) {
        val simulation = Simulation("gaia-ninetales-magni")
        val window = Window("Evolving Protozoa", simulation)

        SwingUtilities.invokeLater(window)
        Thread(REPL(simulation, window)).start()
        simulation.simulate()
    }

    @JvmStatic
	fun exit() {
        System.exit(0)
    }
}