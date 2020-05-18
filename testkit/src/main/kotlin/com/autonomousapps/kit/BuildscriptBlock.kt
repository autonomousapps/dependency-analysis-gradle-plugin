package com.autonomousapps.kit

class BuildscriptBlock(
  val repositories: List<Repository>,
  val dependencies: List<Dependency>
) {

  override fun toString(): String {
    if (repositories.isEmpty() && dependencies.isEmpty()) {
      return ""
    }

    val reposBlock = if (repositories.isEmpty()) {
      ""
    } else {
      "repositories {\n    ${repositories.joinToString("\n    ")}\n  }"
    }

    val depsBlock = if (dependencies.isEmpty()) {
      ""
    } else {
      "dependencies {\n    ${dependencies.joinToString("\n    ")}\n  }"
    }

    return "buildscript {\n  $reposBlock\n  $depsBlock\n}"
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
