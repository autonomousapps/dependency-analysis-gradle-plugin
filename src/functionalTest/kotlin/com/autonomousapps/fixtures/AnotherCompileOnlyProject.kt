package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice

/**
 * The purpose of this project is to validate that, if a dependency is declared `compileOnly`, we do not advise changing
 * it, even if it is unused.
 */
class AnotherCompileOnlyProject(private val agpVersion: String) {

  val androidKotlinLib = LibrarySpec(
    name = "lib",
    type = LibraryType.KOTLIN_ANDROID_LIB,
    sources = mapOf("KotlinLibrary.kt" to """ 
      class KotlinLibrary {
        fun doNothing() {
        }
      }""".trimIndent()),
    dependencies = listOf(
      "api" to KOTLIN_STDLIB_ID,
      "compileOnly" to COMMONS_TEXT // unused, but don't suggest removing
    )
  )

  private val librarySpecs = listOf(androidKotlinLib)

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion, librarySpecs = librarySpecs),
    librarySpecs = librarySpecs
  )

  val expectedAdviceForLib: Set<Advice> = emptySet()
}
