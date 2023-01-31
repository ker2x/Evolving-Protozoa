package protoevo.utils

import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/***
 * A class for saving and loading objects to and from files.
 */
object FileIO {
    @JvmStatic
    fun save(`object`: Any?, filename: String) {
        try {
            val fileOut = FileOutputStream("$filename.dat")
            val out = ObjectOutputStream(fileOut)
            out.writeObject(`object`)
            out.close()
            fileOut.close()
            println("Serialized data saved to:$filename.dat")
        } catch (i: IOException) {
            i.printStackTrace()
        }
    }

    @JvmStatic
    @Throws(IOException::class, ClassNotFoundException::class)
    fun load(filename: String): Any {
        val fileIn = FileInputStream("$filename.dat")
        val `in` = ObjectInputStream(fileIn)
        val result = `in`.readObject()
        `in`.close()
        fileIn.close()
        return result
    }

    @JvmStatic
    fun appendLine(filePath: String?, line: String) {
            Files.write(
                Paths.get(filePath),
                """$line""".toByteArray(),
                StandardOpenOption.APPEND
            )
    }
}