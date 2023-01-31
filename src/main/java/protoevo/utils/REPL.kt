package protoevo.utils

import protoevo.core.Application.Companion.exit
import protoevo.core.Simulation
import java.io.*

/***
 * REPL - Read-Eval-Print-Loop
 */
class REPL(private val simulation: Simulation, private val window: Window) : Runnable {
    private val running = true
    @Throws(Exception::class)
    fun setTimeDilation(args: Array<String>) {
        if (args.size != 2) throw Exception("This command takes 2 arguments.")
        val d = args[1].toFloat()
        simulation.timeDilation = d
    }

    override fun run() {
        println("Starting REPL...")
        val bufferRead = BufferedReader(InputStreamReader(System.`in`))
        while (running) {
            var line: String
            try {
                print("> ")
                line = bufferRead.readLine()
                val args = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val cmd = args[0]
                when (cmd) {
                    "help" -> println("commands - help, quit, stats, settime, gettime")
                    "quit" -> {
                        simulation.close()
                        exit()
                    }

                    "stats" -> simulation.printStats()
                    "settime" -> setTimeDilation(args)
                    "gettime" -> println(simulation.timeDilation)
                    "toggledebug" -> {
                        println("Toggling debug mode.")
                        simulation.toggleDebug()
                    }

                    "toggleui" -> {
                        println("Toggling UI.")
                        window.frame.isVisible = !window.frame.isVisible
                        simulation.toggleUpdateDelay()
                    }

                    "pause" -> {
                        simulation.togglePause()
                        println("Toggling pause.")
                    }

                    else -> println("Command not recognised.")
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}