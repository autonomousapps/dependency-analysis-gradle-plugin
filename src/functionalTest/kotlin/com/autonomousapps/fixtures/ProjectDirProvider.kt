package com.autonomousapps.fixtures

import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.fromJsonList
import java.io.File

interface ProjectDirProvider {
  val projectDir: File
  fun project(moduleName: String): Module

  // Verification helpers
  fun unusedDependenciesFor(spec: ModuleSpec): List<String> = unusedDependenciesFor(spec.name)

  fun unusedDependenciesFor(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
        .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
        .readText().fromJsonList<UnusedDirectComponent>()
        .map { it.dependency.identifier }
  }

  fun completelyUnusedDependenciesFor(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
        .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
        .readText().fromJsonList<UnusedDirectComponent>()
        .filter { it.usedTransitiveDependencies.isEmpty() }
        .map { it.dependency.identifier }
  }

  fun abiReportFor(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
        .resolve("build/${getAbiAnalysisPath(getVariantOrError(moduleName))}")
        .readText().fromJsonList<Dependency>()
        .map { it.identifier }
  }

  fun allUsedClassesFor(spec: LibrarySpec): List<String> = allUsedClassesFor(spec.name)

  fun allUsedClassesFor(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
        .resolve("build/${getAllUsedClassesPath(getVariantOrError(moduleName))}")
        .readLines()
  }

  fun adviceFor(spec: ModuleSpec): Set<Advice> = adviceFor(spec.name)

  fun adviceFor(moduleName: String): Set<Advice> {
    val module = project(moduleName)
    return module.dir
        .resolve("build/${getAdvicePath(getVariantOrError(moduleName))}")
        .readText()
        .fromJsonList<Advice>()
        .toSortedSet()
  }

  private fun getVariantOrError(moduleName: String): String {
    return project(moduleName).variant ?: error("No variant associated with module named $moduleName")
  }
}