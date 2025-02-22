// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.*

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

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

  static ModuleCoordinates moduleCoordinates(com.autonomousapps.kit.gradle.Dependency dep) {
    return moduleCoordinates(dep.identifier, dep.version)
  }

  static ModuleCoordinates moduleCoordinates(String gav) {
    def identifier = gav.substring(0, gav.lastIndexOf(':'))
    def version = gav.substring(gav.lastIndexOf(':') + 1, gav.length())
    return moduleCoordinates(identifier, version)
  }

  static ModuleCoordinates moduleCoordinates(String identifier, String version, String capability = null) {
    return new ModuleCoordinates(identifier, version, defaultGVI(capability))
  }

  static ProjectCoordinates projectCoordinates(com.autonomousapps.kit.gradle.Dependency dep) {
    return projectCoordinates(dep.identifier)
  }

  static ProjectCoordinates projectCoordinates(String projectPath, String capability = null, String buildPath = ':') {
    return new ProjectCoordinates(projectPath, defaultGVI(capability), buildPath)
  }

  static Coordinates includedBuildCoordinates(
    String identifier, ProjectCoordinates resolvedProject, String capability = null
  ) {
    return new IncludedBuildCoordinates(identifier, resolvedProject, defaultGVI(capability))
  }

  static Set<ProjectAdvice> emptyProjectAdviceFor(String... projectPaths) {
    return projectPaths.collect { emptyProjectAdviceFor(it) }
  }

  static ProjectAdvice emptyProjectAdviceFor(String projectPath) {
    return new ProjectAdvice(projectPath, [] as Set<Advice>, [] as Set<PluginAdvice>, [] as Set<ModuleAdvice>, Warning.empty(), false)
  }

  static ProjectAdvice projectAdviceForDependencies(String projectPath, Set<Advice> advice) {
    return projectAdviceForDependencies(projectPath, advice, false)
  }

  static ProjectAdvice projectAdviceForDependencies(String projectPath, Set<Advice> advice, boolean shouldFail) {
    return new ProjectAdvice(projectPath, advice, [] as Set<PluginAdvice>, [] as Set<ModuleAdvice>, Warning.empty(), shouldFail)
  }

  static ProjectAdvice projectAdvice(String projectPath, Set<Advice> advice, Set<PluginAdvice> pluginAdvice) {
    return projectAdvice(projectPath, advice, pluginAdvice, false)
  }

  static ProjectAdvice projectAdvice(
    String projectPath, Set<Advice> advice, Set<PluginAdvice> pluginAdvice, boolean shouldFail
  ) {
    return projectAdvice(projectPath, advice, pluginAdvice, [] as Set<ModuleAdvice>, shouldFail)
  }

  static ProjectAdvice projectAdvice(
    String projectPath,
    Set<Advice> advice,
    Set<PluginAdvice> pluginAdvice,
    Set<ModuleAdvice> moduleAdvice,
    boolean shouldFail
  ) {
    return new ProjectAdvice(projectPath, advice, pluginAdvice, moduleAdvice, Warning.empty(), shouldFail)
  }

  /**
   * This is a workaround for a deficiency in the algorithm. KGP adds the stdlib to the `api` configuration. If Kotlin
   * is only used as an implementation detail, then the algo will suggest moving stdlib from api -> implementation.
   * This advice cannot be followed. We still don't have a good solution for default dependencies added by plugins.
   */
  static Set<Advice> downgradeKotlinStdlib() {
    return downgradeKotlinStdlib('main')
  }

  /**
   * This is a workaround for a deficiency in the algorithm. KGP adds the stdlib to the `api` configuration. If Kotlin
   * is only used as an implementation detail, then the algo will suggest moving stdlib from api -> implementation.
   * This advice cannot be followed. We still don't have a good solution for default dependencies added by plugins.
   */
  static Set<Advice> downgradeKotlinStdlib(String sourceSetName) {
    def from = sourceSetName == 'main' ? 'api' : "${sourceSetName}Api"
    def to = sourceSetName == 'main' ? 'implementation' : "${sourceSetName}Implementation"
    return [Advice.ofChange(moduleCoordinates(kotlinStdLib(from)), from, to)]
  }

  /**
   * This is a workaround for a deficiency in the algorithm. KGP adds the stdlib to the `api` configuration. If Kotlin
   * is only used as an implementation detail, then the algo will suggest moving stdlib from api -> implementation.
   * This advice cannot be followed. We still don't have a good solution for default dependencies added by plugins.
   */
  static Set<Advice> removeKotlinStdlib() {
    return [Advice.ofRemove(moduleCoordinates(kotlinStdLib('api')), 'api')]
  }

  private static GradleVariantIdentification defaultGVI(String capability) {
    new GradleVariantIdentification(capability ? [capability] as Set : [] as Set, [:])
  }

  static final Set<ModuleAdvice> emptyModuleAdvice = []

  static AndroidScoreBuilder androidScoreBuilder() {
    return new AndroidScoreBuilder()
  }

  static class AndroidScoreBuilder {
    boolean hasAndroidAssets = false
    boolean hasAndroidRes = false
    boolean usesAndroidClasses = false
    boolean hasBuildConfig = false
    boolean hasAndroidDependencies = false

    AndroidScore build() {
      return new AndroidScore(
        hasAndroidAssets,
        hasAndroidRes,
        usesAndroidClasses,
        hasBuildConfig,
        hasAndroidDependencies
      )
    }
  }

  static Map<String, Set<String>> duplicateDependenciesReport(GradleProject gradleProject) {
    return STRATEGY.getDuplicateDependenciesReport(gradleProject)
  }

  static List<String> resolvedDependenciesReport(GradleProject gradleProject, String projectPath) {
    return STRATEGY.getResolvedDependenciesReport(gradleProject, projectPath)
  }
}
