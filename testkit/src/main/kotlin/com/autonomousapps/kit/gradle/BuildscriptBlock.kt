package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * The `buildscript` block:
 * ```
 * // build.gradle[.kts]
 * buildscript {
 *   repositories { ... }
 *   dependencies { ... }
 * }
 * ```
 */
class BuildscriptBlock(
  private val repositories: Repositories,
  private val dependencies: Dependencies
) : Element.Block {

  constructor(
    repositories: List<Repository>,
    dependencies: List<Dependency>
  ) : this(
    Repositories(repositories),
    Dependencies(dependencies)
  )

  override val name: String = "buildscript"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.render(s)
    dependencies.render(s)
  }

  override fun toString(): String {
    error("don't call toString()")
  }

  companion object {
    /**
     * This is a `buildscript {}` block that includes AGP in `dependencies.classpath`.
     */
    @JvmStatic
    fun defaultAndroidBuildscriptBlock(agpVersion: String): BuildscriptBlock {
      return BuildscriptBlock(Repository.DEFAULT, listOf(Dependency.androidPlugin(agpVersion)))
    }
  }
}
