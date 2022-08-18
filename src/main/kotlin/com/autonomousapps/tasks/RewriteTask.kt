package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.parse.GradleBuildScriptDependenciesRewriter
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.reversed
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class RewriteTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Rewrite build script for this project to match dependency advice"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val buildScript: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:Input
  abstract val dependencyMap: MapProperty<String, String>

  @get:Optional
  @get:Input
  @get:Option(
    option = "upgrade",
    description = "Use --upgrade if you only want to add or upgrade (implementation -> api) dependencies"
  )
  abstract val upgrade: Property<Boolean>

  @TaskAction fun action() {
    val buildScript = buildScript.get().asFile

    val isUpgrade = upgrade.getOrElse(false)
    if (isUpgrade) {
      logger.quiet("Fixing dependencies for ${buildScript.path}. Upgrades only.")
    } else {
      logger.quiet("Fixing dependencies for ${buildScript.path}.")
    }

    val dslKind = DslKind.from(buildScript)
    val projectAdvice = projectAdvice.fromJson<ProjectAdvice>()

    val map = dependencyMap.get()
    val reversedMap = map.reversed()

    val rewriter = GradleBuildScriptDependenciesRewriter.newRewriter(
      file = buildScript.toPath(),
      advice = projectAdvice.dependencyAdvice.filtered(isUpgrade),
      advicePrinter = AdvicePrinter(
        dslKind = dslKind,
        dependencyMap = map.toLambda()
      ),
      reversedDependencyMap = reversedMap.toLambda()
    )

    try {
      val newText = rewriter.rewritten()
      buildScript.writeText(newText)
    } catch (e: BuildScriptParseException) {
      logger.warn("Can't fix dependencies for '${projectAdvice.projectPath}': ${e.localizedMessage}")
    }
  }

  private fun Set<Advice>.filtered(isUpgrade: Boolean): Set<Advice> =
    if (!isUpgrade) this
    else filterToSet { it.isUpgrade() }
}
