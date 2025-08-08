package com.autonomousapps.convention.tasks.metalava

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Examples of invocations include:
 * 1. `./gradlew :metalavaHelp`
 * 1. `./gradlew :metalavaHelp --args="main --help"`
 * 1. `./gradlew :metalavaHelp --args="help issues"`
 */
@UntrackedTask(because = "Not worth tracking")
public abstract class MetalavaHelpTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = MetalavaConfigurer.TASK_GROUP
    description = "Runs metalava with arbitrary arguments."
  }

  @get:Classpath
  public abstract val metalava: ConfigurableFileCollection

  @get:Option(option = "args", description = "Metalava args")
  @get:Optional
  @get:Input
  public abstract val args: Property<String>

  @TaskAction public fun action() {
    workerExecutor
      .classLoaderIsolation()
      .submit(Action::class.java) { spec ->
        spec.metalava.setFrom(metalava)
        spec.args.set(args)
      }
  }

  public interface Parameters : WorkParameters {
    public val metalava: ConfigurableFileCollection
    public val args: Property<String>
  }

  public abstract class Action : WorkAction<Parameters> {

    private val logger = Logging.getLogger(MetalavaHelpTask::class.java)

    @get:Inject public abstract val execOps: ExecOperations

    override fun execute() {
      val args = parameters.args.orNull?.split(' ')?.map { it.trim() } ?: listOf("help")
      logger.lifecycle("Running metalava with args '$args'")

      execOps.javaexec { spec ->
        spec.mainClass.set("com.android.tools.metalava.Driver")
        spec.classpath = parameters.metalava
        spec.args = args
      }
    }
  }
}
