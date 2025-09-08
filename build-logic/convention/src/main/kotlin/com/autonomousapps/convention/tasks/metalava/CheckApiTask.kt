// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention.tasks.metalava

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.process.ProcessExecutionException
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
public abstract class CheckApiTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = MetalavaConfigurer.TASK_GROUP
    description =
      "Checks the validity of the api file at api/api.txt against current source, failing on any difference."
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:Classpath
  public abstract val metalava: ConfigurableFileCollection

  @get:Classpath
  public abstract val compileClasspath: ConfigurableFileCollection

  @get:Input
  public abstract val jdkHome: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val sourceFiles: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val referenceApiFile: RegularFileProperty

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor
      .classLoaderIsolation()
      .submit(Action::class.java) { params ->
        params.projectPath.set(projectPath)
        params.metalava.setFrom(metalava)
        params.classpath.setFrom(compileClasspath)
        params.sourceFiles.setFrom(sourceFiles)
        params.tempDir.set(temporaryDir)
        params.jdkHome.set(jdkHome)
        params.referenceApiFile.set(referenceApiFile)
        params.output.set(output)
      }
  }

  public interface Parameters : WorkParameters {
    public val projectPath: Property<String>
    public val metalava: ConfigurableFileCollection
    public val classpath: ConfigurableFileCollection
    public val jdkHome: Property<String>
    public val sourceFiles: ConfigurableFileCollection
    public val tempDir: DirectoryProperty
    public val referenceApiFile: RegularFileProperty
    public val output: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    @get:Inject public abstract val execOps: ExecOperations

    override fun execute() {
      val output = parameters.output.get().asFile

      // A `:`-delimited list of directories containing source files, organized in a standard Java package hierarchy.
      val sourcePath = parameters.sourceFiles.files
        .filter(File::exists)
        .joinToString(":") { it.absolutePath }
      val currentApi = parameters.tempDir.file("current-api.txt").get().asFile

      val jdkHome = parameters.jdkHome.get()
      val classpath = parameters.classpath.files.asSequence()
        .map { it.absolutePath }
        .joinToString(":")

      // Re-generate API
      execOps.javaexec { spec ->
        spec.systemProperty("java.awt.headless", "true")
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "--format=v3",
          "--jdk-home", jdkHome,
          "--classpath", classpath,
          "--source-path", sourcePath,
          "--api", currentApi.absolutePath,
        )
      }

      val result = execOps.javaexec { spec ->
        spec.systemProperty("java.awt.headless", "true")
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "--format=v3",
          "--jdk-home", jdkHome,
          "--classpath", classpath,
          "--source-files", currentApi.absolutePath,
          "--check-compatibility:api:released", parameters.referenceApiFile.get().asFile.absolutePath,
        )
        spec.isIgnoreExitValue = true
      }

      if (result.exitValue == 0) {
        output.writeText("SUCCESS")
      } else {
        val msg = """
            API changed! Run `./gradlew ${updateApi()}` and then commit the changes. For backwards-incompatible
            changes, be sure to update the major version. DO NOT USE JDK 24.
          """.trimIndent().wrapInStars(margin = 1)

        try {
          result.assertNormalExitValue()
        } catch (e: ProcessExecutionException) {
          throw ApiChangedException(msg, e)
        }
      }
    }

    private fun updateApi(): String {
      val path = parameters.projectPath.get()
      return if (path == ":") ":updateApi" else "$path:updateApi"
    }

    private fun String.wrapInStars(margin: Int = 0): String {
      require(margin >= 0) { "Expected margin >= 0, was $margin" }

      fun StringBuilder.insertVerticalMargin() {
        repeat(margin) {
          appendLine()
        }
      }

      fun StringBuilder.insertLeftMargin() {
        append(" ".repeat(margin))
      }

      fun StringBuilder.insertRightMargin() {
        appendLine(" ".repeat(margin))
      }

      val lines = lines()
      return buildString {
        // Add top margin
        insertVerticalMargin()

        // Get length of longest line
        val max = lines.maxBy { it.length }.length

        // Top line
        insertLeftMargin()
        append("*".repeat(max + 4))
        insertRightMargin()

        lines.forEach { line ->
          insertLeftMargin()
          append("* ")
          append(line)

          val padding = max - line.length
          append(" ".repeat(padding))
          append(" *")
          insertRightMargin()
        }

        // Bottom line
        insertLeftMargin()
        append("*".repeat(max + 4))
        appendLine(" ".repeat(margin))

        // Add bottom margin
        insertVerticalMargin()
      }
    }
  }
}
