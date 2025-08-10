// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention.tasks.metalava

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
public abstract class GenerateApiTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = MetalavaConfigurer.TASK_GROUP
    description = "Generates a new api file based on current source."
  }

  @get:Classpath
  public abstract val metalava: ConfigurableFileCollection

  @get:Classpath
  public abstract val compileClasspath: ConfigurableFileCollection

  @get:Input
  public abstract val jdkHome: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val sourceFiles: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor
      .classLoaderIsolation()
      .submit(Action::class.java) { params ->
        params.metalava.setFrom(metalava)
        params.classpath.setFrom(compileClasspath)
        params.sourceFiles.setFrom(this@GenerateApiTask.sourceFiles)
        params.jdkHome.set(jdkHome)

        params.output.set(output)
      }
  }

  public interface Parameters : WorkParameters {
    public val metalava: ConfigurableFileCollection
    public val classpath: ConfigurableFileCollection
    public val jdkHome: Property<String>
    public val sourceFiles: ConfigurableFileCollection
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

      val classpath = parameters.classpath.files.asSequence()
        .map { it.absolutePath }
        .joinToString(":")

      val jdkHome = parameters.jdkHome.get()

      execOps.javaexec { spec ->
        spec.systemProperty("java.awt.headless", "true")
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "--format=v3",
          "--jdk-home", jdkHome,
          "--classpath", classpath,
          "--source-path", sourcePath,
          // First include everything, then exclude all internal packages.
          "--stub-packages", "+com.autonomousapps*:-com.autonomousapps.internal.*:-com.autonomousapps.tasks",
          "--api", output.absolutePath,
        )
      }
    }
  }
}
