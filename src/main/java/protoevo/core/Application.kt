package protoevo.core

import protoevo.utils.REPL
import protoevo.utils.Window
import javax.swing.SwingUtilities
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    Application(args)
}

class Application(val args: Array<String>) {

     init {
         val simulation = Simulation("gaia-ninetales-magni")
         val window = Window("Evolving Protozoa", simulation)

         SwingUtilities.invokeLater(window)
         Thread(REPL(simulation, window)).start()
         simulation.simulate()
     }

    companion object {
        const val refreshDelay :Float = 1f
        fun exit(): Nothing = exitProcess(0)
    }
}