package com.autonomousapps.convention.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
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
public abstract class GenerateApiStubsTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
): DefaultTask() {

  init {
    group = "TODO"
    description = "TODO"
  }

  @get:Classpath
  public abstract val metalava: ConfigurableFileCollection

  @get:Classpath
  public abstract val classpath: ConfigurableFileCollection

  @get:InputFiles
  public abstract val sources: ConfigurableFileCollection

  @get:Input
  public abstract val jdkHome: Property<String>

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @get:OutputFile
  public abstract val outputApiText: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor
      .classLoaderIsolation { spec ->
        // TODO: or setFrom? Do I even need this since the worker actually invokes a JavaExec process?
        spec.classpath.from(metalava)
      }
      .submit(Action::class.java) { params ->
        params.metalava.setFrom(metalava)
        params.classpath.setFrom(classpath)
        params.sources.setFrom(sources)
        params.jdkHome.set(jdkHome)

        params.outputDir.set(outputDir)
        params.outputApiText.set(outputApiText)
      }
  }

  public interface Parameters : WorkParameters {
    public val metalava: ConfigurableFileCollection
    public val classpath: ConfigurableFileCollection
    public val sources: ConfigurableFileCollection
    public val jdkHome: Property<String>
    public val outputDir: DirectoryProperty
    public val outputApiText: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    @get:Inject public abstract val execOps: ExecOperations

    override fun execute() {
      val outputDir = parameters.outputDir.get()
      outputDir.asFile.deleteRecursively() // TODO(tsr): do this in other cases where an @OutputDirectory is used

      // Don't delete this.
      val outputApiText = parameters.outputApiText.get().asFile

      // A `:`-delimited list of directories containing source files, organized in a standard Java package hierarchy.
      val sourcePath = parameters.sources.files
        .filter(File::exists)
        .joinToString(":") { it.absolutePath }

      val classpath = parameters.classpath.files.asSequence()
        .map { it.absolutePath }
        .joinToString(":")

      execOps.javaexec { spec ->
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "main", "--help",
          // "help", "issues"
        )
      }

      if (true) return

      val result = execOps.javaexec { spec ->
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = listOf(
          "--jdk-home", parameters.jdkHome.get(),
          "--classpath", classpath,
          "--source-path", sourcePath,
          // TODO: not sure about this
          // "--hide", "HiddenSuperclass",
          // "--hide", "HiddenAbstractMethod",

          // "--doc-stubs", outputDir.asFile.absolutePath,
          // "--api", outputApiText.absolutePath,
          "--check-compatibility:api:released", outputApiText.absolutePath,
          "--format=v3",

          //ignoreFailure = true,
        )
      }
      result.assertNormalExitValue()
    }
  }
}
