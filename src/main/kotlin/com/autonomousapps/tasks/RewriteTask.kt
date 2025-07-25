// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.parse.BuildScriptDependenciesRewriter
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.reversed
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

// TODO(tsr): consider adding dummy output to enable caching
@UntrackedTask(because = "No outputs")
public abstract class RewriteTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Rewrite build script for this project to match dependency advice"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val buildScript: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val projectAdvice: RegularFileProperty

  @get:Input
  public abstract val dependencyMap: MapProperty<String, String>

  @get:Input
  public abstract val useTypesafeProjectAccessors: Property<Boolean>

  @get:Optional
  @get:Input
  @get:Option(
    option = "upgrade",
    description = "Use --upgrade if you only want to add or upgrade (implementation -> api) dependencies"
  )
  public abstract val upgrade: Property<Boolean>

  @TaskAction public fun action() {
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

    val rewriter = BuildScriptDependenciesRewriter.of(
      file = buildScript,
      advice = projectAdvice.dependencyAdvice.filtered(isUpgrade),
      advicePrinter = AdvicePrinter(
        dslKind = dslKind,
        dependencyMap = map.toLambda(),
        useTypesafeProjectAccessors = useTypesafeProjectAccessors.get(),
      ),
      reversedDependencyMap = { map.reversed().getOrDefault(it, it) }
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
