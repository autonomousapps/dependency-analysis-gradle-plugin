// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("JvmFixtures")

package com.autonomousapps.fixtures

/**
 * A "multi-module" Java library project (has the `java-library` plugin applied). There is a root project with no
 * source, and one or more java-library subprojects.
 *
 * @param librarySpecs a list of library project names and types. Can be null. See [LibrarySpec] and
 * [LibraryType].
 */
class MultiModuleJavaLibraryProject(
  rootSpec: RootSpec = RootSpec(),
  librarySpecs: List<LibrarySpec>? = null,
) : ProjectDirProvider {

  private val rootProject = RootProject(rootSpec)

  /**
   * Feed this to a [GradleRunner][org.gradle.testkit.runner.GradleRunner].
   */
  override val projectDir = rootProject.projectDir

  // A collection of library modules, keyed by their respective names.
  private val modules: Map<String, Module> = mapOf(
    *librarySpecs?.map { spec ->
      spec.name to libraryFactory(projectDir, spec)
    }?.toTypedArray() ?: emptyArray()
  )

  override fun project(moduleName: String) = modules[moduleName] ?: error("No '$moduleName' project found!")
}

//region error test
val JAVA_ERROR = LibrarySpec(
  name = "error",
  type = LibraryType.JAVA_JVM_LIB,
  applyPlugin = true,
  dependencies = emptyList(),
  sources = mapOf(
    "Error.java" to """ 
    import $DEFAULT_PACKAGE_NAME.java.Error;
    
    public class Error {
      public void magic() {
        System.out.println("Magic = " + Producer.MAGIC);
      }
    }""".trimIndent()
  )
)
//endregion error test
