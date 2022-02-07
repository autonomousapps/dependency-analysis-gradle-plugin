package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.getAdvicePathV2
import com.autonomousapps.internal.getFinalAdvicePathV2
import com.autonomousapps.internal.getUsagesPath
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.intermediates.PublicDependencies
import java.io.File
import java.util.TreeSet

interface ProjectDirProvider {

  val projectDir: File

  fun project(moduleName: String): Module

  fun abiReportFor(moduleName: String): List<String> {
    return abiReportForV2(moduleName)
  }

  private fun abiReportForV2(moduleName: String): List<String> {
    val module = project(moduleName)
    val f = module.dir.resolve("build/${getUsagesPath(getVariantOrError(moduleName))}")
    return PublicDependencies.from(f)
  }

  fun adviceFor(spec: ModuleSpec): Set<Advice> = adviceFor(spec.name)

  fun adviceFor(moduleName: String): Set<Advice> {
    return adviceForV2(moduleName)
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
    return buildHealthForV2(moduleName)
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

  private fun buildHealthPath() = "build/${getFinalAdvicePathV2()}"

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
