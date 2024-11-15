package com.autonomousapps.tools.gzip

import okio.GzipSource
import okio.buffer
import okio.source
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.writeText

/** Decompresses a gzipped [input] and writes the result to [output]. */
class GunzipTaskOutput(
  private val fs: FileSystem,
  private val input: String,
  private val output: String,
) {

  internal fun run() {
    val input = fs.getPath(input)
    val output = fs.getPath(output)

    require(input.exists()) { "Input file does not exist. Expected ${input.absolutePathString()}" }

    println("Processing file input '$input'")
    println("Writing output to '$output'")

    val decompressed = GzipSource(input.source()).buffer().use { source ->
      source.readUtf8()
    }

    output.writeText(decompressed)
  }

  companion object {
    @JvmStatic
    fun main(vararg args: String) {
      require(args.size == 2) { "Expected two arguments. Was ${args.joinToString()}" }

      GunzipTaskOutput(fs = FileSystems.getDefault(), input = args[0], output = args[1]).run()
    }
  }
}
