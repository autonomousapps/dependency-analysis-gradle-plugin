package com.autonomousapps

import com.autonomousapps.advice.*
import com.autonomousapps.graph.Edge
import com.autonomousapps.kit.GradleProject

/**
 * Helps specs find advice output in test projects.
 */
final class AdviceHelper {

  private static final AdviceStrategy STRATEGY

  static {
    STRATEGY = new AdviceStrategy.V2(true)
  }

  static List<Edge> actualGraph(GradleProject gradleProject, String projectName, String variant = 'debug') {
    STRATEGY.actualGraph(gradleProject, projectName, variant)
  }

  static ComprehensiveAdvice actualProjectHealth(
    GradleProject gradleProject,
    String projectName
  ) {
    STRATEGY.actualProjectHealth(gradleProject, projectName)
  }

  static def actualBuildHealth(GradleProject gradleProject) {
    STRATEGY.actualBuildHealth(gradleProject)
  }

  static List<ComprehensiveAdvice> actualStrictBuildHealth(GradleProject gradleProject) {
    STRATEGY.actualStrictBuildHealth(gradleProject)
  }

  static List<ComprehensiveAdvice> actualMinimizedBuildHealth(GradleProject gradleProject) {
    STRATEGY.actualMinimizedBuildHealth(gradleProject)
  }

  static Pebble actualRipples(GradleProject gradleProject) {
    STRATEGY.actualRipples(gradleProject)
  }

  static ComprehensiveAdvice actualComprehensiveAdviceForProject(
    GradleProject gradleProject,
    String projectName
  ) {
    STRATEGY.actualComprehensiveAdviceForProject(gradleProject, projectName)
  }

  static List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject) {
    STRATEGY.actualAdviceForFirstSubproject(gradleProject)
  }

  static List<Advice> actualAdviceForSubproject(GradleProject gradleProject, String projectName) {
    STRATEGY.actualAdviceForSubproject(gradleProject, projectName)
  }

  static String actualConsoleAdvice(GradleProject gradleProject) {
    STRATEGY.actualConsoleAdvice(gradleProject)
  }

  static Dependency dependency(com.autonomousapps.kit.Dependency dep) {
    dependency(
      identifier: dep.identifier,
      resolvedVersion: dep.version,
      configurationName: dep.configuration
    )
  }

  static Dependency dependency(
    String identifier, String resolvedVersion = null, String configurationName = null
  ) {
    new Dependency(identifier, resolvedVersion, configurationName)
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  static Dependency dependency(Map<String, String> dependency) {
    new Dependency(
      dependency['identifier'],
      dependency['resolvedVersion'],
      dependency['configurationName']
    )
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  static TransitiveDependency transitiveDependency(Map<String, Object> map) {
    def dep = map['dependency']
    def resolvedVersion = map['resolvedVersion']
    if (dep instanceof String) {
      dep = dependency(dep, resolvedVersion)
    }

    transitiveDependency(
      dep as Dependency,
      (map['parents'] ?: []) as List<Dependency>,
      (map['variants'] ?: []) as Set<String>
    )
  }

  static TransitiveDependency transitiveDependency(
    Dependency dependency,
    List<Dependency> parents,
    Set<String> variants = [] as Set<String>
  ) {
    new TransitiveDependency(dependency, parents as Set<Dependency>, variants)
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  static ComponentWithTransitives componentWithTransitives(Map<String, Object> map) {
    componentWithTransitives(
      map['dependency'] as Dependency,
      map['usedTransitiveDependencies'] as Set<Dependency>
    )
  }

  static ComponentWithTransitives componentWithTransitives(
    Dependency dependency,
    Set<Dependency> usedTransitiveDependencies
  ) {
    new ComponentWithTransitives(dependency, usedTransitiveDependencies)
  }

  static List<ComprehensiveAdvice> emptyBuildHealthFor(String... projectPaths) {
    projectPaths.collect { emptyCompAdviceFor(it) }
  }

  static ComprehensiveAdvice emptyCompAdviceFor(String projectPath) {
    new ComprehensiveAdvice(projectPath, [] as Set<Advice>, [] as Set<PluginAdvice>, false)
  }

  static ComprehensiveAdvice compAdviceForDependencies(String projectPath, Set<Advice> advice) {
    new ComprehensiveAdvice(projectPath, advice, [] as Set<PluginAdvice>, false)
  }
}
