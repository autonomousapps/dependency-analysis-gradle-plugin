package com.autonomousapps.convention.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * TODO.
 *
 * Source:
 * https://cs.android.com/android/platform/superproject/main/+/main:tools/metalava/metalava/
 *
 * Exemplars:
 * 1. https://github.com/firebase/firebase-android-sdk/blob/2bfc0a5de4c3d384238b25f9b71ef36104a72fa0/plugins/src/main/java/com/google/firebase/gradle/plugins/Metalava.kt#L4
 * 2. https://github.com/google/ksp/blob/main/buildSrc/src/main/kotlin/com/google/devtools/ksp/ApiCheck.kt
 */
public abstract class GenerateApiTask @Inject constructor(
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

      // execOps.javaexec { spec ->
      //   spec.mainClass.set("com.android.tools.metalava.Driver")
      //   spec.classpath = parameters.metalava
      //   spec.args = listOf(
      //     "main", "--help",
      //     // "help", "issues"
      //   )
      // }
      //
      // if (true) return

      val jdkHome = parameters.jdkHome.get()

      val result = execOps.javaexec { spec ->
        spec.systemProperty("java.awt.headless", "true")
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "--jdk-home", jdkHome,
          "--classpath", classpath,
          "--source-path", sourcePath,
          // TODO: not sure about this
          // "--hide", "HiddenSuperclass",
          // "--hide", "HiddenAbstractMethod",
//          "--warnings-as-errors",

          "--api", output.absolutePath,
          "--format=v3",

          //ignoreFailure = true,
        )
      }
      result.assertNormalExitValue()
    }
  }
}
