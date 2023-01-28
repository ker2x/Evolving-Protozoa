package protoevo.core

import protoevo.utils.REPL
import protoevo.utils.TextStyle
import protoevo.utils.Window
import javax.swing.SwingUtilities

object Application {
    var simulation: Simulation? = null
    var window: Window? = null
    const val refreshDelay = 1000 / 120f
    fun parseArgs(args: Array<String>): Map<String, String> {
        val argsMap: MutableMap<String, String> = HashMap()
        for (arg in args) {
            val split = arg.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size == 2) {
                if (split[1] != "") argsMap[split[0]] = split[1]
            }
        }
        return argsMap
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val argsMap = parseArgs(args)
        if (argsMap.containsKey("-save")) simulation = Simulation(argsMap["-save"]) else simulation = Simulation()
        try {
            if (!(args.size > 0 && args[0] == "noui")) {
                TextStyle.loadFonts()
                window = Window("Evolving Protozoa", simulation)
                SwingUtilities.invokeLater(window)
            } else {
                simulation!!.setUpdateDelay(0f)
            }
            Thread(REPL(simulation, window)).start()
            simulation!!.simulate()
        } catch (e: Exception) {
            simulation!!.close()
            throw e
        }
    }

    @JvmStatic
	fun exit() {
        System.exit(0)
    }
}