package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.parse.GradleBuildScriptDependenciesRewriter
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.reversed
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*

abstract class RewriteTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Rewrite build script for this project to match dependency advice"
  }

  @get:InputFile
  abstract val buildScript: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:Input
  abstract val dependencyMap: MapProperty<String, String>

  @TaskAction fun action() {
    val buildScript = buildScript.get().asFile
    val dslKind = DslKind.from(buildScript)
    val projectAdvice = projectAdvice.fromJson<ProjectAdvice>()

    val map = dependencyMap.get()
    val reversedMap = map.reversed()

    val rewriter = GradleBuildScriptDependenciesRewriter.newRewriter(
      file = buildScript.toPath(),
      advice = projectAdvice.dependencyAdvice,
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
}

// TODO move
internal fun Map<String, String>.toLambda(): (String) -> String = { s ->
  getOrDefault(s, s)
}
