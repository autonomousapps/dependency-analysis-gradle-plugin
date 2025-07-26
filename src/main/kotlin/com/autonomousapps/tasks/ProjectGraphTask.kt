package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@UntrackedTask(because = "Always prints output")
public abstract class ProjectGraphTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates several graph views of this project's local dependency graph"
  }

  /** Used for logging. */
  @get:Input
  public abstract val projectPath: Property<String>

  /**
   * Used for relativizing output paths for logging. Internal because we don't want Gradle to hash the entire project.
   */
  @get:Internal
  public abstract val rootDir: DirectoryProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputDirectory
  public abstract val graphsDir: DirectoryProperty

  @TaskAction public fun action() {
    val compileOutput = graphsDir.file(GenerateProjectGraphTask.PROJECT_COMPILE_CLASSPATH_GV).get().asFile
    val runtimeOutput = graphsDir.file(GenerateProjectGraphTask.PROJECT_RUNTIME_CLASSPATH_GV).get().asFile
    val combinedOutput = graphsDir.file(GenerateProjectGraphTask.PROJECT_COMBINED_CLASSPATH_GV).get().asFile

    // Print a message so users know how to do something with the generated .gv files.
    val msg = buildString {
      // convert ":foo:bar" to "foo-bar.svg"
      val svgName = projectPath.get().removePrefix(":").replace(':', '-') + ".svg"

      // Get relative paths to output for more readable logging
      val rootPath = rootDir.get().asFile
      val compilePath = compileOutput.relativeTo(rootPath)
      val runtimePath = runtimeOutput.relativeTo(rootPath)
      val combinedPath = combinedOutput.relativeTo(rootPath)

      appendLine("Graphs generated to:")
      appendLine(" - $compilePath")
      appendLine(" - $runtimePath")
      appendLine(" - $combinedPath")
      appendLine()
      appendLine("To generate an SVG with graphviz, you could run the following. (You must have graphviz installed.)")
      appendLine()
      appendLine("    dot -Tsvg $runtimePath -o $svgName")
    }

    logger.quiet(msg)
  }
}
