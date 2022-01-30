package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.intermediates.PublicDependencies
import java.io.File
import java.util.TreeSet

interface ProjectDirProvider {

  val projectDir: File

  fun project(moduleName: String): Module

  fun unusedDependenciesFor(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
      .readText().fromJsonList<ComponentWithTransitives>()
      .map { it.dependency.identifier }
  }

  fun completelyUnusedDependenciesFor(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
      .readText().fromJsonList<ComponentWithTransitives>()
      .filter { it.usedTransitiveDependencies.isNullOrEmpty() }
      .map { it.dependency.identifier }
  }

  fun abiReportFor(moduleName: String): List<String> {
    return if (isV1()) {
      abiReportForV1(moduleName)
    } else {
      abiReportForV2(moduleName)
    }
  }

  private fun abiReportForV1(moduleName: String): List<String> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getAbiAnalysisPath(getVariantOrError(moduleName))}")
      .readText().fromJsonList<PublicComponent>()
      .map { it.dependency.identifier }
  }

  private fun abiReportForV2(moduleName: String): List<String> {
    val module = project(moduleName)
    val f = module.dir.resolve("build/${getUsagesPath(getVariantOrError(moduleName))}")
    return PublicDependencies.from(f)
  }

  fun allUsedClassesFor(spec: LibrarySpec): List<VariantClass> = allUsedClassesFor(spec.name)

  fun allUsedClassesFor(moduleName: String): List<VariantClass> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getAllUsedClassesPath(getVariantOrError(moduleName))}")
      .readText()
      .fromJson()
  }

  fun adviceFor(spec: ModuleSpec): Set<Advice> = adviceFor(spec.name)

  fun adviceFor(moduleName: String): Set<Advice> {
    return if (isV1()) {
      adviceForV1(moduleName)
    } else {
      adviceForV2(moduleName)
    }
  }

  private fun adviceForV1(moduleName: String): Set<Advice> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getAdvicePath(getVariantOrError(moduleName))}")
      .readText()
      .fromJsonList<Advice>()
      .toSortedSet()
  }

  private fun adviceForV2(moduleName: String): Set<Advice> {
    val module = project(moduleName)
    return module.dir
      .resolve("build/${getAdvicePathV2()}")
      .readText()
      .fromJson<ProjectAdvice>()
      .dependencyAdvice
      .mapTo(TreeSet()) {
        fromOldAdvice(it)
      }
  }

  fun removeAdviceFor(spec: ModuleSpec): Set<String> {
    return removeAdviceFor(spec.name)
  }

  fun removeAdviceFor(moduleName: String): Set<String> {
    return adviceFor(moduleName).asSequence()
      .filter { it.isRemove() }
      .map { it.dependency.identifier }
      .toSortedSet()
  }

  fun buildHealthFor(spec: ModuleSpec): Set<ComprehensiveAdvice> = buildHealthFor(spec.name)

  fun buildHealthFor(moduleName: String): Set<ComprehensiveAdvice> {
    return if (isV1()) {
      buildHealthForV1(moduleName)
    } else {
      buildHealthForV2(moduleName)
    }
  }

  private fun buildHealthForV1(moduleName: String): Set<ComprehensiveAdvice> {
    val module = project(moduleName)
    return module.dir
      .resolve(buildHealthPath())
      .readText()
      .fromJsonSet()
  }

  private fun buildHealthForV2(moduleName: String): Set<ComprehensiveAdvice> {
    val module = project(moduleName)
    return module.dir
      .resolve(buildHealthPath())
      .readText()
      .fromJson<BuildHealth>()
      .projectAdvice
      .mapTo(TreeSet()) {
        ComprehensiveAdvice(
          projectPath = it.projectPath,
          dependencyAdvice = fromOldAdvice(it.dependencyAdvice),
          pluginAdvice = it.pluginAdvice,
          shouldFail = it.shouldFail
        )
      }
  }

  private fun buildHealthPath() = if (isV1()) {
    "build/${getFinalAdvicePath()}"
  } else {
    "build/${getFinalAdvicePathV2()}"
  }

  private fun fromOldAdvice(advice: Collection<com.autonomousapps.model.Advice>): Set<Advice> {
    return advice.mapTo(TreeSet()) { fromOldAdvice(it) }
  }

  private fun fromOldAdvice(advice: com.autonomousapps.model.Advice): Advice {
    return Advice(
      dependency = dependency(advice),
      usedTransitiveDependencies = emptySet(),
      parents = null,
      fromConfiguration = advice.fromConfiguration,
      toConfiguration = advice.toConfiguration
    )
  }

  private fun dependency(advice: com.autonomousapps.model.Advice): Dependency {
    val coordinates = advice.coordinates
    val version = if (coordinates is ModuleCoordinates) coordinates.resolvedVersion else null
    return Dependency(coordinates.identifier, version, advice.fromConfiguration)
  }

  private fun getVariantOrError(moduleName: String): String {
    val variant = project(moduleName).variant ?: error("No variant associated with module named $moduleName")
    return if (variant == "main") variant else "${variant}Main"
  }
}

private fun isV1() = System.getProperty("dependency.analysis.old.model").toBoolean()
