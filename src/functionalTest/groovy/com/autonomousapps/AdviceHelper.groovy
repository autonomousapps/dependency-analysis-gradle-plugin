package com.autonomousapps

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.*

/**
 * Helps specs find advice output in test projects.
 */
final class AdviceHelper {

  private static final AdviceStrategy STRATEGY

  static {
    STRATEGY = new AdviceStrategy.V2()
  }

  static Set<ProjectAdvice> actualProjectAdvice(GradleProject gradleProject) {
    //noinspection GroovyAssignabilityCheck
    return STRATEGY.actualBuildHealth(gradleProject)
  }

  static ProjectAdvice actualProjectAdviceForProject(
    GradleProject gradleProject,
    String projectName
  ) {
    return STRATEGY.actualComprehensiveAdviceForProject(gradleProject, projectName)
  }

  static List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject) {
    STRATEGY.actualAdviceForFirstSubproject(gradleProject)
  }

  static ModuleCoordinates moduleCoordinates(com.autonomousapps.kit.Dependency dep) {
    return moduleCoordinates(dep.identifier, dep.version)
  }

  static ModuleCoordinates moduleCoordinates(String gav) {
    def identifier = gav.substring(0, gav.lastIndexOf(':'))
    def version = gav.substring(gav.lastIndexOf(':') + 1, gav.length())
    return new ModuleCoordinates(identifier, version)
  }

  static ModuleCoordinates moduleCoordinates(String identifier, String version) {
    return new ModuleCoordinates(identifier, version)
  }

  static ProjectCoordinates projectCoordinates(com.autonomousapps.kit.Dependency dep) {
    return projectCoordinates(dep.identifier)
  }

  static ProjectCoordinates projectCoordinates(String projectPath) {
    return new ProjectCoordinates(projectPath)
  }

  static Coordinates includedBuildCoordinates(
    String identifier,
    String requestedVersion,
    ProjectCoordinates resolvedProject
  ) {
    return new IncludedBuildCoordinates(identifier, requestedVersion, resolvedProject)
  }

  static Set<ProjectAdvice> emptyProjectAdviceFor(String... projectPaths) {
    return projectPaths.collect { emptyProjectAdviceFor(it) }
  }

  static ProjectAdvice emptyProjectAdviceFor(String projectPath) {
    return new ProjectAdvice(projectPath, [] as Set<Advice>, [] as Set<PluginAdvice>, false)
  }

  static ProjectAdvice projectAdviceForDependencies(String projectPath, Set<Advice> advice) {
    return new ProjectAdvice(projectPath, advice, [] as Set<PluginAdvice>, false)
  }
}
