package com.autonomousapps.convention.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * This is just a fancy way of running
 * ```
 * # -d decompression
 * # -c stdout
 * $ gzip -cd path/to/file.json.gz
 * ```
 */
@CacheableTask
abstract class GunzipTask @Inject constructor(
  private val execOperations: ExecOperations
) : DefaultTask() {

  init {
    description = "Gunzips a gzipped Dependency Analysis output file"
  }

  @get:Internal
  abstract val projectDir: DirectoryProperty

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Option(option = "file", description = "The file to gunzip")
  @get:Internal
  abstract val filePath: Property<String>

  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFile
  fun getFile(): File = fileToRead()

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction fun action() {
    val fileToRead = fileToRead()
    val outputFile = outputFile(fileToRead)

    execOperations.javaexec { spec ->
      spec.classpath = runtimeClasspath
      spec.mainClass.set("com.autonomousapps.tools.gzip.GunzipTaskOutput")
      spec.args(fileToRead.absolutePath, outputFile.absolutePath)
    }
  }

  private fun fileToRead(): File {
    val input = filePath.get()

    val inputFile = if (input.startsWith('/')) {
      // absolute
      File(input)
    } else {
      // interpret as relative to the project directory
      projectDir.file(input).get().asFile
    }

    return inputFile.absoluteFile.normalize().also {
      require(it.exists()) {
        "${it.absolutePath} does not exist!"
      }
      require(it.isFile) {
        "${it.absolutePath} is not a file!"
      }
    }
  }

  private fun outputFile(fileToRead: File): File {
    // Filename will be like `foo.json.gz` or `foo.txt.gz`. Remove the `.gz`, since the output will NOT be gzipped.
    val fileName = fileToRead.name.removeSuffix(".gz")

    val outputFile = outputDir.file(fileName).get().asFile
    outputFile.delete()

    return outputFile
  }
}
