package com.autonomousapps.convention.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

public abstract class CheckApiTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
): DefaultTask() {

  init {
    group = "TODO"
    description = "TODO"
  }

  @get:Classpath
  public abstract val metalava: ConfigurableFileCollection

  @get:Classpath
  public abstract val compileClasspath: ConfigurableFileCollection

  @get:Input
  public abstract val jdkHome: Property<String>

  @get:InputFiles
  public abstract val sourceFiles: ConfigurableFileCollection

  @get:InputFile
  public abstract val referenceApiFile: RegularFileProperty

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor
      .classLoaderIsolation { spec ->
        // TODO: or setFrom? Do I even need this since the worker actually invokes a JavaExec process?
        spec.classpath.from(metalava)
      }
      .submit(Action::class.java) { params ->
        params.metalava.setFrom(metalava)
        params.classpath.setFrom(compileClasspath)
        params.sourceFiles.setFrom(this@CheckApiTask.sourceFiles)
        params.jdkHome.set(jdkHome)
        params.referenceApiFile.set(referenceApiFile)
        params.output.set(output)
      }
  }

  public interface Parameters : WorkParameters {
    public val metalava: ConfigurableFileCollection
    public val classpath: ConfigurableFileCollection
    public val jdkHome: Property<String>
    public val sourceFiles: ConfigurableFileCollection
    public val referenceApiFile: RegularFileProperty
    public val output: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    @get:Inject public abstract val execOps: ExecOperations

    override fun execute() {
//      val outputDir = parameters.outputDir.get()
//      outputDir.asFile.deleteRecursively() // TODO(tsr): do this in other cases where an @OutputDirectory is used

      val output = parameters.output.get().asFile

      // A `:`-delimited list of directories containing source files, organized in a standard Java package hierarchy.
      val sourcePath = parameters.sourceFiles.files
        .filter(File::exists)
        .joinToString(":") { it.absolutePath }

      val classpath = parameters.classpath.files.asSequence()
        .map { it.absolutePath }
        .joinToString(":")

      val result = execOps.javaexec { spec ->
        spec.systemProperty("java.awt.headless", "true")
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "--jdk-home", parameters.jdkHome.get(),
          "--classpath", classpath,
          "--source-path", sourcePath,
          // TODO: not sure about this
          // "--hide", "HiddenSuperclass",
          // "--hide", "HiddenAbstractMethod",
          "--warnings-as-errors",

          "--check-compatibility:api:released", parameters.referenceApiFile.get().asFile.absolutePath,
          "--format=v3",

          //ignoreFailure = true,
        )
      }
      result.assertNormalExitValue()

      // TODO write output to disk
    }
  }
}
