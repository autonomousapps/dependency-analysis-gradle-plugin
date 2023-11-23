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
public class BuildscriptBlock(
  private val repositories: Repositories,
  private val dependencies: Dependencies,
) : Element.Block {

  public constructor(
    repositories: MutableList<Repository>,
    dependencies: MutableList<Dependency>,
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

  public companion object {
    /**
     * This is a `buildscript {}` block that includes AGP in `dependencies.classpath`.
     */
    @JvmStatic
    public fun defaultAndroidBuildscriptBlock(agpVersion: String): BuildscriptBlock {
      return BuildscriptBlock(
        Repository.DEFAULT.toMutableList(),
        mutableListOf(Dependency.androidPlugin(agpVersion))
      )
    }
  }
}
